import {
  initializeTestEnvironment,
  assertFails,
  type RulesTestEnvironment,
} from "@firebase/rules-unit-testing";
import * as fs from "fs";
import * as path from "path";
import * as assert from "assert";

describe("Multi-Client Real-Time Sync & Permissions Integration", () => {
  let testEnv: RulesTestEnvironment;

  before(async () => {
    if (!process.env.FIRESTORE_EMULATOR_HOST) {
      process.env.FIRESTORE_EMULATOR_HOST = "127.0.0.1:8080";
    }

    const rulesPath = fs.existsSync("firestore.rules")
      ? path.resolve("firestore.rules")
      : path.resolve(process.cwd(), "../firestore.rules");
    const rules = fs.readFileSync(rulesPath, "utf8");

    testEnv = await initializeTestEnvironment({
      projectId: "studypartner-realtime-test",
      firestore: {
        rules: rules,
        host: "127.0.0.1",
        port: 8080,
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

      // Seed a study group where Alice and Bob are co-members
      await testEnv.withSecurityRulesDisabled(async (context) => {
        const db = context.firestore();
        await db.collection("groups").doc("shared-group").set({
          groupId: "shared-group",
          name: "Study Group Shared",
          memberIds: ["alice", "bob"],
          inviteCode: "SYNC99",
          createdBy: "alice",
        });
      });
    }
  });

  it("propagates event changes in real-time between group members (Alice -> Bob)", async () => {
    const aliceDb = testEnv.authenticatedContext("alice").firestore();
    const bobDb = testEnv.authenticatedContext("bob").firestore();

    const receivedDocs: any[] = [];

    // Bob subscribes to the group's events subcollection
    const unsubscribe = bobDb
      .collection("groups")
      .doc("shared-group")
      .collection("events")
      .onSnapshot((snapshot) => {
        snapshot.docChanges().forEach((change) => {
          if (change.type === "added" || change.type === "modified") {
            receivedDocs.push(change.doc.data());
          }
        });
      });

    try {
      // Alice creates an event
      await aliceDb
        .collection("groups")
        .doc("shared-group")
        .collection("events")
        .doc("event-123")
        .set({
          id: "event-123",
          groupId: "shared-group",
          title: "Realtime Calculus Study Session",
          description: "Chapter 4 limits & derivatives",
          startTime: 1774000000000,
          endTime: 1774003600000,
          allDay: false,
          location: "Library Room 2",
          reminderMinutes: 15,
          createdBy: "alice",
          updatedAt: 1774000000000,
        });

      // Wait for Bob's real-time snapshot listener to receive the event
      await new Promise<void>((resolve, reject) => {
        const timeout = setTimeout(() => {
          reject(new Error("Timed out waiting for real-time propagation to Bob"));
        }, 5000);

        const checkInterval = setInterval(() => {
          if (receivedDocs.some((d) => d.id === "event-123" && d.title === "Realtime Calculus Study Session")) {
            clearTimeout(timeout);
            clearInterval(checkInterval);
            resolve();
          }
        }, 100);
      });

      assert.strictEqual(receivedDocs.some((d) => d.id === "event-123"), true);
    } finally {
      unsubscribe();
    }
  });

  it("propagates task changes in real-time between group members (Alice -> Bob)", async () => {
    const aliceDb = testEnv.authenticatedContext("alice").firestore();
    const bobDb = testEnv.authenticatedContext("bob").firestore();

    const receivedTasks: any[] = [];

    // Bob subscribes to the group's tasks subcollection
    const unsubscribe = bobDb
      .collection("groups")
      .doc("shared-group")
      .collection("tasks")
      .onSnapshot((snapshot) => {
        snapshot.docChanges().forEach((change) => {
          if (change.type === "added" || change.type === "modified") {
            receivedTasks.push(change.doc.data());
          }
        });
      });

    try {
      // Alice creates a task
      await aliceDb
        .collection("groups")
        .doc("shared-group")
        .collection("tasks")
        .doc("task-456")
        .set({
          id: "task-456",
          groupId: "shared-group",
          title: "Complete Chapter 4 Homework",
          notes: "Problems 1 through 15",
          dueDate: 1774000000000,
          assignedTo: ["bob"],
          isDone: false,
          doneBy: null,
          createdBy: "alice",
          updatedAt: 1774000000000,
        });

      // Wait for Bob's real-time snapshot listener to receive the task
      await new Promise<void>((resolve, reject) => {
        const timeout = setTimeout(() => {
          reject(new Error("Timed out waiting for real-time task propagation to Bob"));
        }, 5000);

        const checkInterval = setInterval(() => {
          if (receivedTasks.some((t) => t.id === "task-456" && t.title === "Complete Chapter 4 Homework")) {
            clearTimeout(timeout);
            clearInterval(checkInterval);
            resolve();
          }
        }, 100);
      });

      assert.strictEqual(receivedTasks.some((t) => t.id === "task-456"), true);
    } finally {
      unsubscribe();
    }
  });

  it("rejects non-members from accessing group events or tasks in real-time (Charlie)", async () => {
    const charlieDb = testEnv.authenticatedContext("charlie").firestore();

    // Verify Charlie cannot query the subcollections
    await assertFails(
      charlieDb
        .collection("groups")
        .doc("shared-group")
        .collection("events")
        .get()
    );

    await assertFails(
      charlieDb
        .collection("groups")
        .doc("shared-group")
        .collection("tasks")
        .get()
    );
  });

  it("stops receiving updates when unsubscribe is invoked (clean teardown)", async () => {
    const aliceDb = testEnv.authenticatedContext("alice").firestore();
    const bobDb = testEnv.authenticatedContext("bob").firestore();

    let eventCount = 0;

    const unsubscribe = bobDb
      .collection("groups")
      .doc("shared-group")
      .collection("events")
      .onSnapshot((snapshot) => {
        eventCount = snapshot.size;
      });

    // Alice creates first event
    await aliceDb
      .collection("groups")
      .doc("shared-group")
      .collection("events")
      .doc("event-1")
      .set({
        id: "event-1",
        groupId: "shared-group",
        title: "Event 1",
        description: "",
        startTime: 1000,
        endTime: 2000,
        allDay: false,
        location: "",
        reminderMinutes: 15,
        createdBy: "alice",
        updatedAt: 1000,
      });

    // Wait until Bob gets event 1
    await new Promise((r) => setTimeout(r, 600));
    assert.strictEqual(eventCount, 1);

    // Bob unregisters / cancels listener
    unsubscribe();

    // Alice creates second event
    await aliceDb
      .collection("groups")
      .doc("shared-group")
      .collection("events")
      .doc("event-2")
      .set({
        id: "event-2",
        groupId: "shared-group",
        title: "Event 2",
        description: "",
        startTime: 3000,
        endTime: 4000,
        allDay: false,
        location: "",
        reminderMinutes: 15,
        createdBy: "alice",
        updatedAt: 3000,
      });

    // Wait and verify Bob's count did not increase
    await new Promise((r) => setTimeout(r, 600));
    assert.strictEqual(eventCount, 1);
  });
});
