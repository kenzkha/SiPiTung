const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_parent_123";
const BOB_UID = "bob_parent_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProfilePayload(uid, ts = new Date(Date.now() - 5000)) {
  return {
    userId: uid,
    childName: "Raka",
    grade: 2,
    totalStars: 50,
    totalXp: 120,
    streakDays: 1,
    totalQuestionsSolved: 10,
    totalCorrectAnswers: 9,
    unlockedBadges: ["first_step"],
    ownedAvatarItems: ["owl_classic"],
    equippedAvatar: "owl_classic",
    createdAt: ts,
    updatedAt: ts,
  };
}

function validSessionPayload(uid, ts = new Date(Date.now() - 3000)) {
  return {
    userId: uid,
    mode: "WORKSHEET",
    topic: "PENGURANGAN",
    grade: 2,
    totalQuestions: 5,
    correctAnswers: 5,
    starsEarned: 75,
    durationSeconds: 92,
    createdAt: ts,
  };
}

function validRewardPayload(uid, ts = new Date(Date.now() - 3000)) {
  return {
    userId: uid,
    title: "Es Krim Cokelat",
    starCost: 100,
    iconKey: "icecream",
    status: "AVAILABLE",
    createdAt: ts,
    updatedAt: ts,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID))
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("sessions").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("rewards").get()
  );
});

test("ChildProfile: owner can create and run all 4 valid partial update actions", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, created))
  );

  // Read own profile
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());

  // Listing root users collection is strictly forbidden
  await assertFails(aliceDb.collection("users").get());

  // Action 1: Update childName & grade
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      childName: "Raka Pratama",
      grade: 3,
      updatedAt: new Date(Date.now() - 2000),
    })
  );

  // Action 2: Record learning progress
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      totalStars: 125,
      totalXp: 240,
      streakDays: 2,
      totalQuestionsSolved: 15,
      totalCorrectAnswers: 14,
      unlockedBadges: ["first_step", "stack_master"],
      updatedAt: new Date(Date.now() - 1500),
    })
  );

  // Action 3: Purchase & equip avatar item
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      totalStars: 75,
      ownedAvatarItems: ["owl_classic", "rocket_star"],
      equippedAvatar: "rocket_star",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Action 4: Spend stars on parent reward
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      totalStars: 25,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("ChildProfile: rejects shadow fields, invalid bounds, immutable mutations, and future timestamps", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  // Reject ghost/shadow field on create
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      ...validProfilePayload(ALICE_UID, created),
      isAdmin: true,
    })
  );

  // Reject grade out of 1..6 bounds
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      ...validProfilePayload(ALICE_UID, created),
      grade: 7,
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set(
      validProfilePayload(ALICE_UID, new Date(Date.now() + 60000))
    )
  );

  // Seed valid profile
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, created))
  );

  // Reject immutable createdAt modification
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      childName: "Raka",
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject totalCorrectAnswers > totalQuestionsSolved
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      totalStars: 80,
      totalXp: 150,
      streakDays: 1,
      totalQuestionsSolved: 10,
      totalCorrectAnswers: 15,
      unlockedBadges: ["first_step"],
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("LearningSession: owner can create & list sessions; updates and invalid payloads fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const sessionsRef = aliceDb.collection("users").doc(ALICE_UID).collection("sessions");

  // Empty subcollection list must succeed
  await assertSucceeds(sessionsRef.orderBy("createdAt", "desc").limit(50).get());

  // Valid Kotlin create payload (no @DocumentId `id` key)
  await assertSucceeds(sessionsRef.doc("sess_1").set(validSessionPayload(ALICE_UID)));

  // Reject extra `id` field
  await assertFails(
    sessionsRef.doc("sess_2").set({
      ...validSessionPayload(ALICE_UID),
      id: "sess_2",
    })
  );

  // Reject invalid mode enum
  await assertFails(
    sessionsRef.doc("sess_3").set({
      ...validSessionPayload(ALICE_UID),
      mode: "CHEAT_MODE",
    })
  );

  // Reject updating immutable session record
  await assertFails(
    sessionsRef.doc("sess_1").update({
      correctAnswers: 100,
    })
  );
});

test("ParentReward: enforces AVAILABLE -> REDEEMED -> FULFILLED lifecycle and terminal lock", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const rewardsRef = aliceDb.collection("users").doc(ALICE_UID).collection("rewards");
  const created = new Date(Date.now() - 10000);

  // Empty list query succeeds
  await assertSucceeds(rewardsRef.orderBy("createdAt", "desc").get());

  // Create must start as AVAILABLE
  await assertFails(
    rewardsRef.doc("rew_bad").set({
      ...validRewardPayload(ALICE_UID, created),
      status: "FULFILLED",
    })
  );

  await assertSucceeds(rewardsRef.doc("rew_1").set(validRewardPayload(ALICE_UID, created)));

  // State shortcut AVAILABLE -> FULFILLED must fail
  await assertFails(
    rewardsRef.doc("rew_1").update({
      status: "FULFILLED",
      updatedAt: new Date(Date.now() - 4000),
    })
  );

  // Valid redeem: AVAILABLE -> REDEEMED
  await assertSucceeds(
    rewardsRef.doc("rew_1").update({
      status: "REDEEMED",
      updatedAt: new Date(Date.now() - 3000),
    })
  );

  // Valid fulfill: REDEEMED -> FULFILLED
  await assertSucceeds(
    rewardsRef.doc("rew_1").update({
      status: "FULFILLED",
      updatedAt: new Date(Date.now() - 2000),
    })
  );

  // Terminal state lock: cannot modify FULFILLED reward
  await assertFails(
    rewardsRef.doc("rew_1").update({
      status: "AVAILABLE",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Cross-user isolation: Alice cannot read or modify Bob's profile, sessions, or rewards", async () => {
  const created = new Date(Date.now() - 10000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const adminDb = ctx.firestore();
    await adminDb.collection("users").doc(BOB_UID).set(validProfilePayload(BOB_UID, created));
    await adminDb
      .collection("users")
      .doc(BOB_UID)
      .collection("sessions")
      .doc("bob_sess")
      .set(validSessionPayload(BOB_UID, created));
    await adminDb
      .collection("users")
      .doc(BOB_UID)
      .collection("rewards")
      .doc("bob_rew")
      .set(validRewardPayload(BOB_UID, created));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).update({
      childName: "Hacked",
      grade: 1,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("sessions").get());
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("sessions").doc("bob_sess").get()
  );
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("rewards").get());
  await assertFails(
    aliceDb
      .collection("users")
      .doc(BOB_UID)
      .collection("rewards")
      .doc("bob_rew")
      .update({ status: "REDEEMED", updatedAt: new Date(Date.now() - 1000) })
  );
});
