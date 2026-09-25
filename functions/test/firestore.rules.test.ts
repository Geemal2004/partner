import {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
  type RulesTestEnvironment,
} from "@firebase/rules-unit-testing";
import * as fs from "fs";
import * as path from "path";

describe("Firestore Security Rules", () => {
  let testEnv: RulesTestEnvironment;

  before(async () => {
    if (!process.env.FIRESTORE_EMULATOR_HOST) {
      process.env.FIRESTORE_EMULATOR_HOST = "127.0.0.1:8080";
    }

    const rulesPath = fs.existsSync("firestore.rules")
      ? path.resolve("firestore.rules")
      : path.resolve(process.cwd(), "../firestore.rules");
    const rules = fs.readFileSync(rulesPath, "utf8");

    try {
      testEnv = await initializeTestEnvironment({
        projectId: "studypartner-test-project",
        firestore: {
          rules: rules,
          host: "127.0.0.1",
          port: 8080,
        },
      });
    } catch (e: any) {
      console.error("Rules load error detail:", e?.message, e?.response?.data || e);
      throw e;
    }
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

  // 1. Non-member cannot read another group's events or tasks
  describe("Group isolation for non-members", () => {
    it("non-member cannot read another group's events or tasks", async () => {
      // Seed a group owned by Alice with Alice as the sole member
      await testEnv.withSecurityRulesDisabled(async (context) => {
        const db = context.firestore();
        await db.collection("groups").doc("group-alice").set({
          groupId: "group-alice",
          name: "Alice Study Group",
          memberIds: ["alice"],
          inviteCode: "ABC123",
          createdBy: "alice",
          createdAt: Date.now(),
        });

        await db
          .collection("groups")
          .doc("group-alice")
          .collection("events")
          .doc("event-1")
          .set({
            id: "event-1",
            groupId: "group-alice",
            title: "Calculus Review",
            startTime: Date.now() + 10000,
            endTime: Date.now() + 20000,
            allDay: false,
            location: "Room 101",
            reminderMinutes: 15,
            createdBy: "alice",
            updatedAt: Date.now(),
          });

        await db
          .collection("groups")
          .doc("group-alice")
          .collection("tasks")
          .doc("task-1")
          .set({
            id: "task-1",
            groupId: "group-alice",
            title: "Homework 3",
            notes: "Exercises 1-5",
            dueDate: Date.now() + 50000,
            assignedTo: ["alice"],
            isDone: false,
            doneBy: null,
            createdBy: "alice",
            updatedAt: Date.now(),
          });
      });

      // Bob tries to read Alice's group events and tasks
      const bobDb = testEnv.authenticatedContext("bob").firestore();

      await assertFails(
        bobDb
          .collection("groups")
          .doc("group-alice")
          .collection("events")
          .doc("event-1")
          .get()
      );

      await assertFails(
        bobDb
          .collection("groups")
          .doc("group-alice")
          .collection("tasks")
          .doc("task-1")
          .get()
      );
    });
  });

  // 2. Member can read/write their own group's events and tasks
  describe("Group member access", () => {
    it("member can read and write their own group's events and tasks", async () => {
      // Seed a group where Alice is a member
      await testEnv.withSecurityRulesDisabled(async (context) => {
        const db = context.firestore();
        await db.collection("groups").doc("group-alice").set({
          groupId: "group-alice",
          name: "Alice Study Group",
          memberIds: ["alice"],
          inviteCode: "ABC123",
          createdBy: "alice",
          createdAt: Date.now(),
        });
      });

      const aliceDb = testEnv.authenticatedContext("alice").firestore();

      // Write event
      await assertSucceeds(
        aliceDb
          .collection("groups")
          .doc("group-alice")
          .collection("events")
          .doc("event-alice-1")
          .set({
            id: "event-alice-1",
            groupId: "group-alice",
            title: "Physics Session",
            startTime: Date.now() + 10000,
            endTime: Date.now() + 20000,
            allDay: false,
            location: "Lab B",
            reminderMinutes: 15,
            createdBy: "alice",
            updatedAt: Date.now(),
          })
      );

      // Read event
      await assertSucceeds(
        aliceDb
          .collection("groups")
          .doc("group-alice")
          .collection("events")
          .doc("event-alice-1")
          .get()
      );

      // Write task
      await assertSucceeds(
        aliceDb
          .collection("groups")
          .doc("group-alice")
          .collection("tasks")
          .doc("task-alice-1")
          .set({
            id: "task-alice-1",
            groupId: "group-alice",
            title: "Lab Report",
            notes: "Section 2",
            dueDate: Date.now() + 50000,
            assignedTo: ["alice"],
            isDone: false,
            doneBy: null,
            createdBy: "alice",
            updatedAt: Date.now(),
          })
      );

      // Read task
      await assertSucceeds(
        aliceDb
          .collection("groups")
          .doc("group-alice")
          .collection("tasks")
          .doc("task-alice-1")
          .get()
      );
    });
  });

  // 3. Non-creator cannot delete a group
  describe("Group deletion authorization", () => {
    it("non-creator cannot delete a group even if they are a member", async () => {
      // Seed a group where Alice is creator, but Bob is also a member
      await testEnv.withSecurityRulesDisabled(async (context) => {
        const db = context.firestore();
        await db.collection("groups").doc("shared-group").set({
          groupId: "shared-group",
          name: "Shared Study Group",
          memberIds: ["alice", "bob"],
          inviteCode: "SHR123",
          createdBy: "alice",
          createdAt: Date.now(),
        });
      });

      const bobDb = testEnv.authenticatedContext("bob").firestore();
      const aliceDb = testEnv.authenticatedContext("alice").firestore();

      // Bob is a member, but NOT the creator -> fails
      await assertFails(
        bobDb.collection("groups").doc("shared-group").delete()
      );

      // Alice is the creator -> succeeds
      await assertSucceeds(
        aliceDb.collection("groups").doc("shared-group").delete()
      );
    });
  });

  // 4. A user cannot create a group listing other users in memberIds
  describe("Group creation validation", () => {
    it("a user cannot create a group listing other users in memberIds", async () => {
      const bobDb = testEnv.authenticatedContext("bob").firestore();

      // Attempt to create a group where memberIds contains someone other than bob
      await assertFails(
        bobDb.collection("groups").doc("bob-group-invalid").set({
          groupId: "bob-group-invalid",
          name: "Invalid Group",
          memberIds: ["bob", "alice"],
          inviteCode: "BOB123",
          createdBy: "bob",
          createdAt: Date.now(),
        })
      );

      // Attempt to create a group with memberIds not containing the creator
      await assertFails(
        bobDb.collection("groups").doc("bob-group-empty").set({
          groupId: "bob-group-empty",
          name: "Invalid Group",
          memberIds: ["alice"],
          inviteCode: "BOB123",
          createdBy: "bob",
          createdAt: Date.now(),
        })
      );

      // Valid group creation: memberIds == [createdBy]
      await assertSucceeds(
        bobDb.collection("groups").doc("bob-group-valid").set({
          groupId: "bob-group-valid",
          name: "Bob Valid Group",
          memberIds: ["bob"],
          inviteCode: "BOB123",
          createdBy: "bob",
          createdAt: Date.now(),
        })
      );
    });
  });

  // 5. A user cannot read another user's users/{uid} doc
  describe("User document privacy", () => {
    it("a user cannot read another user's users/{uid} doc", async () => {
      await testEnv.withSecurityRulesDisabled(async (context) => {
        const db = context.firestore();
        await db.collection("users").doc("alice").set({
          uid: "alice",
          displayName: "Alice",
          email: "alice@example.com",
          photoUrl: "https://example.com/alice.png",
          groupIds: ["group-alice"],
        });
      });

      const bobDb = testEnv.authenticatedContext("bob").firestore();
      const aliceDb = testEnv.authenticatedContext("alice").firestore();

      // Bob cannot read Alice's user profile
      await assertFails(bobDb.collection("users").doc("alice").get());

      // Alice can read her own profile
      await assertSucceeds(aliceDb.collection("users").doc("alice").get());
    });
  });
});
