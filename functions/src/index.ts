import { onDocumentWritten } from "firebase-functions/v2/firestore";
import { onCall, HttpsError } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";

admin.initializeApp();

/**
 * Callable function to securely join a study group using an invite code.
 */
export const joinGroup = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "User must be authenticated to join a group.");
  }

  const uid = request.auth.uid;
  const inviteCode = request.data?.inviteCode;

  if (!inviteCode || typeof inviteCode !== "string") {
    throw new HttpsError("invalid-argument", "An invite code is required.");
  }

  const db = admin.firestore();
  const groupsRef = db.collection("groups");
  const querySnapshot = await groupsRef.where("inviteCode", "==", inviteCode.trim()).get();

  if (querySnapshot.empty) {
    throw new HttpsError("not-found", `No group found with invite code: ${inviteCode}`);
  }

  const groupDoc = querySnapshot.docs[0];
  const groupId = groupDoc.id;
  const groupData = groupDoc.data();

  // Add user to memberIds in group document
  await groupsRef.doc(groupId).update({
    memberIds: admin.firestore.FieldValue.arrayUnion(uid)
  });

  // Add group to user's groupIds array
  await db.collection("users").doc(uid).set({
    groupIds: admin.firestore.FieldValue.arrayUnion(groupId)
  }, { merge: true });

  const updatedMemberIds = Array.from(new Set([...(groupData.memberIds || []), uid]));

  return {
    success: true,
    group: {
      groupId: groupId,
      name: groupData.name || "",
      inviteCode: groupData.inviteCode || inviteCode,
      createdBy: groupData.createdBy || "",
      memberIds: updatedMemberIds
    }
  };
});

/**
 * Firestore trigger for event writes: notifies group members via FCM.
 */
export const onEventWrite = onDocumentWritten("groups/{groupId}/events/{eventId}", async (event) => {
  const snapshot = event.data;
  if (!snapshot) return;

  const afterData = snapshot.after.exists ? snapshot.after.data() : null;
  if (!afterData) return; // Ignore deletions

  const groupId = event.params.groupId;
  const eventId = event.params.eventId;
  const eventTitle = afterData.title || "Study Event";

  const db = admin.firestore();
  const groupDoc = await db.collection("groups").doc(groupId).get();
  if (!groupDoc.exists) return;

  const groupData = groupDoc.data();
  const memberIds: string[] = groupData?.memberIds || [];
  const createdBy = afterData.createdBy;

  const targetMemberIds = memberIds.filter((id) => id !== createdBy);
  if (targetMemberIds.length === 0) return;

  const tokens: string[] = [];
  for (const memberId of targetMemberIds) {
    const userDoc = await db.collection("users").doc(memberId).get();
    if (userDoc.exists) {
      const fcmToken = userDoc.data()?.fcmToken;
      if (fcmToken) {
        tokens.push(fcmToken);
      }
    }
  }

  if (tokens.length === 0) return;

  const deepLink = `studypartner://event/${eventId}`;
  const message = {
    notification: {
      title: "Group Event Update",
      body: `New/updated event: ${eventTitle}`
    },
    data: {
      link: deepLink,
      eventId: eventId,
      type: "event"
    },
    tokens: tokens
  };

  await admin.messaging().sendEachForMulticast(message);
});

/**
 * Firestore trigger for task writes: notifies group members via FCM.
 */
export const onTaskWrite = onDocumentWritten("groups/{groupId}/tasks/{taskId}", async (event) => {
  const snapshot = event.data;
  if (!snapshot) return;

  const afterData = snapshot.after.exists ? snapshot.after.data() : null;
  if (!afterData) return; // Ignore deletions

  const groupId = event.params.groupId;
  const taskId = event.params.taskId;
  const taskTitle = afterData.title || "Study Task";

  const db = admin.firestore();
  const groupDoc = await db.collection("groups").doc(groupId).get();
  if (!groupDoc.exists) return;

  const groupData = groupDoc.data();
  const memberIds: string[] = groupData?.memberIds || [];
  const createdBy = afterData.createdBy;

  const targetMemberIds = memberIds.filter((id) => id !== createdBy);
  if (targetMemberIds.length === 0) return;

  const tokens: string[] = [];
  for (const memberId of targetMemberIds) {
    const userDoc = await db.collection("users").doc(memberId).get();
    if (userDoc.exists) {
      const fcmToken = userDoc.data()?.fcmToken;
      if (fcmToken) {
        tokens.push(fcmToken);
      }
    }
  }

  if (tokens.length === 0) return;

  const deepLink = `studypartner://task/${taskId}`;
  const message = {
    notification: {
      title: "Group Task Update",
      body: `New/updated task: ${taskTitle}`
    },
    data: {
      link: deepLink,
      taskId: taskId,
      type: "task"
    },
    tokens: tokens
  };

  await admin.messaging().sendEachForMulticast(message);
});
