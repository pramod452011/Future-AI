package com.example.data

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val AUTH_TAG = "Auth"

fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    Firebase.auth.addAuthStateListener(listener)
    trySend(Firebase.auth.currentUser)
    awaitClose {
        Firebase.auth.removeAuthStateListener(listener)
    }
}

/**
 * Attempts silent sign-in for previously authorized returning Google users.
 * Always uses a single credential option per GetCredentialRequest for API 36 compatibility.
 */
suspend fun attemptAutoSignIn(activityContext: Context): FirebaseUser? {
    val existing = Firebase.auth.currentUser
    if (existing != null) return existing

    val webClientId = activityContext.getString(R.string.default_web_client_id)
    if (webClientId.isBlank()) return null

    return try {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setAutoSelectEnabled(true)
            .setServerClientId(webClientId)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val credentialManager = CredentialManager.create(activityContext)
        val result = credentialManager.getCredential(
            request = request,
            context = activityContext,
        )
        exchangeCredentialForFirebaseUser(result.credential)
    } catch (e: NoCredentialException) {
        Log.d(AUTH_TAG, "No previously authorized Google credential found for silent sign-in.")
        null
    } catch (e: GetCredentialCancellationException) {
        Log.w(AUTH_TAG, "Silent Google sign-in cancelled.", e)
        null
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(AUTH_TAG, "Silent Google sign-in skipped: ${e.message}", e)
        null
    }
}

/**
 * Interactive Google Sign-In flow invoked when the user taps the Sign in with Google button.
 */
suspend fun onGoogleSignInClicked(activityContext: Context): Result<FirebaseUser> {
    return try {
        val idToken = requestInteractiveGoogleIdToken(activityContext)
            ?: return Result.failure(IllegalStateException("Google साइन-इन पूरा नहीं हो सका।"))
        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
        val authResult = Firebase.auth.signInWithCredential(firebaseCredential).await()
        val user = authResult.user
            ?: return Result.failure(IllegalStateException("Google साइन-इन पूरा नहीं हो सका।"))
        Result.success(user)
    } catch (e: GetCredentialCancellationException) {
        Log.w(AUTH_TAG, "User cancelled Google Sign-In.", e)
        Result.failure(e)
    } catch (e: GetCredentialException) {
        Log.e(AUTH_TAG, "Credential Manager error during Google Sign-In: ${e.message}", e)
        Result.failure(e)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(AUTH_TAG, "Unexpected Google Sign-In error: ${e.message}", e)
        Result.failure(e)
    }
}

private suspend fun requestInteractiveGoogleIdToken(activityContext: Context): String? {
    val webClientId = activityContext.getString(R.string.default_web_client_id)
    val signInOption = GetSignInWithGoogleOption.Builder(webClientId)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    val credentialManager = CredentialManager.create(activityContext)
    val result = credentialManager.getCredential(
        request = request,
        context = activityContext,
    )
    val credential = result.credential
    if (credential is CustomCredential &&
        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
    ) {
        return GoogleIdTokenCredential.createFrom(credential.data).idToken
    }
    return null
}

private suspend fun exchangeCredentialForFirebaseUser(
    credential: androidx.credentials.Credential,
): FirebaseUser? {
    if (credential is CustomCredential &&
        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
    ) {
        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
        val firebaseCredential = GoogleAuthProvider.getCredential(
            googleIdTokenCredential.idToken,
            null,
        )
        val authResult = Firebase.auth.signInWithCredential(firebaseCredential).await()
        return authResult.user
    }
    return null
}

/**
 * Signs out cleanly from both Credential Manager and Firebase Auth under NonCancellable.
 */
suspend fun signOutWithCredentialManager(context: Context) {
    withContext(NonCancellable) {
        try {
            val credentialManager = CredentialManager.create(context)
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.w(AUTH_TAG, "Unable to clear Credential Manager state: ${e.message}", e)
        } finally {
            Firebase.auth.signOut()
        }
    }
}

/**
 * Permanently deletes the signed-in user's private Firestore data and Firebase Auth account.
 * If Firebase requires recent login, automatically prompts Google OAuth re-authentication.
 */
suspend fun deleteAccountWithReauthIfNeeded(
    activityContext: Context,
    deleteCloudData: suspend () -> Result<Unit>,
): Result<Unit> {
    val user = Firebase.auth.currentUser
        ?: return Result.failure(IllegalStateException("कोई सक्रिय अकाउंट नहीं मिला।"))

    return try {
        val dataResult = deleteCloudData()
        if (dataResult.isFailure) {
            return dataResult
        }

        try {
            user.delete().await()
        } catch (recentLoginRequired: FirebaseAuthRecentLoginRequiredException) {
            Log.w(AUTH_TAG, "Recent login required before deleting account; re-authenticating.", recentLoginRequired)
            val freshToken = requestInteractiveGoogleIdToken(activityContext)
                ?: return Result.failure(
                    IllegalStateException("अकाउंट हटाने के लिए Google से पुनः पुष्टि आवश्यक है।"),
                )
            user.reauthenticate(GoogleAuthProvider.getCredential(freshToken, null)).await()
            deleteCloudData()
            user.delete().await()
        }

        withContext(NonCancellable) {
            try {
                CredentialManager.create(activityContext)
                    .clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(AUTH_TAG, "Credential state clear after delete: ${e.message}", e)
            } finally {
                Firebase.auth.signOut()
            }
        }
        Result.success(Unit)
    } catch (e: GetCredentialCancellationException) {
        Log.w(AUTH_TAG, "Account deletion re-authentication cancelled.", e)
        Result.failure(IllegalStateException("Google पुष्टि रद्द की गई। अकाउंट सुरक्षित है।"))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(AUTH_TAG, "Failed to delete user account: ${e.message}", e)
        Result.failure(e)
    }
}
