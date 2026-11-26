const {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails,
} = require('@firebase/rules-unit-testing');
const { describe, it, before, after, beforeEach } = require('node:test');
const fs = require('node:fs');

const PROJECT_ID = 'demo-no-project';
const HOST = '127.0.0.1';
const PORT = 8085;

let testEnv;

function validUserProfile(uid, now = new Date(Date.now() - 1000)) {
  return {
    userId: uid,
    displayName: 'Kamlesh Creator',
    email: 'kkp452011@gmail.com',
    photoUrl: 'https://example.com/avatar.png',
    preferredLanguage: 'Hindi',
    glowBoostEnabled: true,
    createdAt: now,
    updatedAt: now,
  };
}

function validChatEntry(uid, now = new Date(Date.now() - 1000)) {
  return {
    userId: uid,
    title: 'AI kya hai?',
    prompt: 'AI kya hai? Asaan Hindi me samjhao.',
    response: 'AI yaani Artificial Intelligence ek smart technology hai.',
    toolType: 'ai_chat',
    createdAt: now,
    updatedAt: now,
  };
}

describe('Future AI Firestore Security Rules', () => {
  before(async () => {
    testEnv = await initializeTestEnvironment({
      projectId: PROJECT_ID,
      firestore: {
        rules: fs.readFileSync('firestore.rules', 'utf8'),
        host: HOST,
        port: PORT,
      },
    });
  });

  beforeEach(async () => {
    await testEnv.clearFirestore();
  });

  after(async () => {
    if (testEnv) {
      await testEnv.cleanup();
    }
  });

  // =========================================================================
  // 1. Unauthenticated Access Denials
  // =========================================================================
  it('denies unauthenticated read and write on user profiles and chat history', async () => {
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      const db = ctx.firestore();
      await db.collection('users').doc('alice').set(validUserProfile('alice'));
      await db.collection('users').doc('alice').collection('chats').doc('chat1').set(validChatEntry('alice'));
    });

    const unauthDb = testEnv.unauthenticatedContext().firestore();
    await assertFails(unauthDb.collection('users').doc('alice').get());
    await assertFails(unauthDb.collection('users').doc('alice').set(validUserProfile('alice')));
    await assertFails(unauthDb.collection('users').doc('alice').collection('chats').doc('chat1').get());
    await assertFails(unauthDb.collection('users').doc('alice').collection('chats').get());
    await assertFails(unauthDb.collection('users').doc('alice').collection('chats').add(validChatEntry('alice')));
  });

  // =========================================================================
  // 2. User Profile (/users/{userId}) — Owner CRUD, Isolation & Validation
  // =========================================================================
  it('allows owner to create, get, update settings, and delete their own profile', async () => {
    const aliceDb = testEnv.authenticatedContext('alice').firestore();
    const createdTs = new Date(Date.now() - 5000);

    // Create without optional photoUrl (mirrors filterValues { it != null })
    const initialProfile = validUserProfile('alice', createdTs);
    delete initialProfile.photoUrl;
    await assertSucceeds(aliceDb.collection('users').doc('alice').set(initialProfile));

    // Direct owner get succeeds
    await assertSucceeds(aliceDb.collection('users').doc('alice').get());

    // Settings update mirroring FutureAiRepository.updateUserSettings
    await assertSucceeds(
      aliceDb.collection('users').doc('alice').update({
        displayName: 'Kamlesh Pro',
        preferredLanguage: 'Hinglish',
        glowBoostEnabled: false,
        updatedAt: new Date(Date.now() - 1000),
      }),
    );

    // Owner delete succeeds
    await assertSucceeds(aliceDb.collection('users').doc('alice').delete());
  });

  it('denies collection listing on /users and cross-user profile access', async () => {
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      await ctx.firestore().collection('users').doc('alice').set(validUserProfile('alice'));
    });

    const bobDb = testEnv.authenticatedContext('bob').firestore();
    const aliceDb = testEnv.authenticatedContext('alice').firestore();

    // Listing /users is always denied to prevent PII enumeration
    await assertFails(aliceDb.collection('users').get());
    await assertFails(bobDb.collection('users').get());

    // Cross-user read/write denied
    await assertFails(bobDb.collection('users').doc('alice').get());
    await assertFails(bobDb.collection('users').doc('alice').update({ displayName: 'Hacked' }));
    await assertFails(bobDb.collection('users').doc('alice').delete());
  });

  it('rejects ghost fields, invalid language, future timestamps, and immutable field tampering on profile', async () => {
    const aliceDb = testEnv.authenticatedContext('alice').firestore();
    const past = new Date(Date.now() - 5000);

    // Ghost field (e.g. privilege escalation role/isPremium) rejected on create
    await assertFails(
      aliceDb.collection('users').doc('alice').set({
        ...validUserProfile('alice', past),
        role: 'admin',
      }),
    );

    // Invalid preferredLanguage enum rejected
    await assertFails(
      aliceDb.collection('users').doc('alice').set({
        ...validUserProfile('alice', past),
        preferredLanguage: 'Klingon',
      }),
    );

    // Future timestamp rejected
    await assertFails(
      aliceDb.collection('users').doc('alice').set(
        validUserProfile('alice', new Date(Date.now() + 60 * 60 * 1000)),
      ),
    );

    // Valid seed then test immutable createdAt & userId tampering
    await assertSucceeds(aliceDb.collection('users').doc('alice').set(validUserProfile('alice', past)));

    await assertFails(
      aliceDb.collection('users').doc('alice').update({
        createdAt: new Date(Date.now() - 1000),
        updatedAt: new Date(Date.now() - 1000),
      }),
    );

    await assertFails(
      aliceDb.collection('users').doc('alice').update({
        userId: 'bob',
        updatedAt: new Date(Date.now() - 1000),
      }),
    );
  });

  // =========================================================================
  // 3. Cloud Chat History (/users/{userId}/chats/{chatId})
  // =========================================================================
  it('allows owner to add, list ordered by createdAt desc, read, and delete chat history', async () => {
    const aliceDb = testEnv.authenticatedContext('alice').firestore();
    const chatsRef = aliceDb.collection('users').doc('alice').collection('chats');

    // Mirrors FutureAiRepository.saveChatEntry
    const docRef = await assertSucceeds(chatsRef.add(validChatEntry('alice')));

    // Mirrors FutureAiRepository.observeChatHistory query
    await assertSucceeds(chatsRef.orderBy('createdAt', 'desc').limit(50).get());

    // Direct get & delete
    await assertSucceeds(docRef.get());
    await assertSucceeds(docRef.delete());
  });

  it('denies cross-user access and invalid payloads on chat history', async () => {
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      await ctx.firestore()
        .collection('users')
        .doc('alice')
        .collection('chats')
        .doc('c1')
        .set(validChatEntry('alice'));
    });

    const bobDb = testEnv.authenticatedContext('bob').firestore();
    const aliceChats = bobDb.collection('users').doc('alice').collection('chats');

    await assertFails(aliceChats.doc('c1').get());
    await assertFails(aliceChats.orderBy('createdAt', 'desc').limit(50).get());
    await assertFails(aliceChats.add(validChatEntry('bob')));
    await assertFails(aliceChats.doc('c1').delete());

    // Owner cannot inject ghost fields, empty prompt, or wrong toolType
    const aliceDb = testEnv.authenticatedContext('alice').firestore();
    const ownChats = aliceDb.collection('users').doc('alice').collection('chats');

    await assertFails(
      ownChats.add({
        ...validChatEntry('alice'),
        extraField: 'injected',
      }),
    );
    await assertFails(
      ownChats.add({
        ...validChatEntry('alice'),
        toolType: 'unknown_tool',
      }),
    );
    await assertFails(
      ownChats.add({
        ...validChatEntry('alice'),
        prompt: '',
      }),
    );
  });

  // =========================================================================
  // 4. Per-User Gemini Usage Tracking (/users/{userId}/usage/{eventId})
  // =========================================================================
  it('allows owner to log, list, and delete their own Gemini usage events, and blocks updates and cross-user reads', async () => {
    const aliceDb = testEnv.authenticatedContext('alice').firestore();
    const bobDb = testEnv.authenticatedContext('bob').firestore();
    const unauthDb = testEnv.unauthenticatedContext().firestore();

    const validUsage = {
      userId: 'alice',
      toolType: 'ai_chat',
      modelName: 'gemini-3.8-flash',
      promptChars: 42,
      responseChars: 215,
      createdAt: new Date(Date.now() - 1000),
    };

    const aliceUsage = aliceDb.collection('users').doc('alice').collection('usage');

    // Owner create & ordered list succeed
    const docRef = await assertSucceeds(aliceUsage.add(validUsage));
    await assertSucceeds(aliceUsage.orderBy('createdAt', 'desc').limit(100).get());

    // Usage records are immutable: owner update must fail
    await assertFails(docRef.update({ promptChars: 999 }));

    // Unauthenticated and cross-user read/write must fail
    await assertFails(unauthDb.collection('users').doc('alice').collection('usage').get());
    await assertFails(bobDb.collection('users').doc('alice').collection('usage').get());
    await assertFails(bobDb.collection('users').doc('alice').collection('usage').add({ ...validUsage, userId: 'bob' }));

    // Invalid payloads rejected (ghost field, negative chars, invalid tool)
    await assertFails(aliceUsage.add({ ...validUsage, extraKey: true }));
    await assertFails(aliceUsage.add({ ...validUsage, promptChars: -5 }));
    await assertFails(aliceUsage.add({ ...validUsage, toolType: 'invalid_tool' }));

    // Owner delete succeeds (for account deletion cleanup)
    await assertSucceeds(docRef.delete());
  });
});
