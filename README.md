# StudyPartner Planner

StudyPartner Planner is a modern, collaborative Android application designed for study partners and small study groups to manage shared calendar events, assignments, tasks, and notifications seamlessly across devices.

---

## Features

- **Google Sign-In & Authentication**: Secure sign-in using Android Credential Manager and Firebase Authentication.
- **Group Collaboration & Invites**: Create study groups or join existing groups via 6-character invite codes or deep links.
- **Shared Calendar & Event Scheduling**: Create, edit, and sync study sessions, group meetings, and exam dates across partners.
- **Shared Task Management**: Track open and completed tasks with assignee support and real-time category filtering (All, Mine, Partners, Overdue, Completed).
- **Offline-First Storage**: Local Room database ensuring full offline functionality with Firestore synchronization when online.
- **Notifications & Reminders**: Scheduled event alarms via `AlarmManager`, daily study summary notifications via `WorkManager`, and real-time push updates via Firebase Cloud Messaging (FCM).
- **Adaptive Material 3 UI**: Full Material Design 3 Expressive UI supporting phones, foldables, and tablets with `NavigationSuiteScaffold` and List-Detail adaptive panes (`androidx.compose.adaptive`).
- **Type-Safe Navigation 3**: Driven by Jetpack Navigation 3 (`androidx.navigation3`) with serializable route keys.

---

## Tech Stack & Architecture

- **Language & Runtime**: Kotlin 2.2 / JDK 17 / compileSdk 37
- **UI Framework**: Jetpack Compose with Material Design 3 Expressive & Adaptive Navigation Suite
- **Navigation**: Jetpack Navigation 3 (`navigation3-runtime`, `navigation3-ui`, `lifecycle-viewmodel-navigation3`)
- **Dependency Injection**: Hilt (`hilt-android`, `hilt-work`)
- **Local Persistence**: Room Database (`EventDao`, `TaskDao`) + DataStore Preferences
- **Backend Services**: Firebase Auth, Cloud Firestore, Cloud Functions, Cloud Messaging (FCM)
- **Background Tasks**: WorkManager & AlarmManager
- **Testing**: JUnit 4, MockK, Truth, Turbine, Coroutines Test, Compose UI Test (`ui-test-junit4`)

---

## Deep Link Schema

StudyPartner Planner supports type-safe custom deep link URIs with the `studypartner://` scheme.

| Deep Link Pattern | Destination Route | Action / Description |
| --- | --- | --- |
| `studypartner://join/{inviteCode}` | `JoinGroup(code)` | Prompts the user to join a study group with the specified invite code. |
| `studypartner://event/{eventId}` | `EventDetail(id)` | Opens the calendar event details for the given event ID. |
| `studypartner://task/{taskId}` | `TaskDetail(id)` | Opens the task details / editor for the given task ID. |

Deep links can be triggered via standard Android Intent filters or browser URIs. `DeepLinkParser` parses incoming URIs into type-safe Navigation 3 `AppRoute` keys.

---

## Firebase Setup Instructions

1. **Add `google-services.json`**:
   - Create a Firebase project in the [Firebase Console](https://console.firebase.google.com/).
   - Add an Android app with package name `com.studypartner.planner`.
   - Download `google-services.json` and place it at `app/google-services.json`.

2. **Configure SHA-1 Fingerprint & Google Sign-In**:
   - Obtain your local debug keystore SHA-1 fingerprint via Gradle task:
     ```bash
     ./gradlew signingReport
     ```
     or via `keytool`:
     ```bash
     keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
     ```
   - Register the SHA-1 fingerprint in Firebase Console under **Project Settings → Your Android app (`com.studypartner.planner`)**.
   - Enable Google Sign-In under **Authentication → Sign-in method**.

### Troubleshooting DEVELOPER_ERROR (Status Code 10) in Google Play Services / Firebase Auth

`DEVELOPER_ERROR` (Status Code 10) indicates a configuration mismatch between the app's signing certificate / package name and the OAuth 2.0 settings in Firebase / Google Cloud Console.

To fix `DEVELOPER_ERROR` (Status Code 10):
1. **Verify Package Name**: Check `app/google-services.json` to confirm `"package_name": "com.studypartner.planner"`.
2. **Obtain Local SHA-1**: Run `./gradlew signingReport` (or `gradlew signingReport` on Windows) and copy the SHA-1 hash under `Variant: debug`.
3. **Update Firebase Console**: In Firebase Console -> **Project Settings** -> **Your apps**, click **Add fingerprint** and paste your SHA-1. Re-download `google-services.json` into `app/google-services.json`.
4. **Verify Web Client ID**: Check `app/src/main/res/values/strings.xml` to ensure `default_web_client_id` matches the Web client ID (`client_type: 3`) from `google-services.json` (`1049276433931-2dc90chq25jpsuiki43dvrcvucef5idv.apps.googleusercontent.com`).

3. **Firestore Security Rules**:
   Apply the following rules in Firebase Console → Firestore Database → Rules:
   ```javascript
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {

       function isAuthenticated() {
         return request.auth != null;
       }

       function isUser(uid) {
         return isAuthenticated() && request.auth.uid == uid;
       }

       function isGroupMember(groupId) {
         return isAuthenticated() &&
           request.auth.uid in get(/databases/$(database)/documents/groups/$(groupId)).data.memberIds;
       }

       function isValidGroupCreate(groupId) {
         let data = request.resource.data;
         let requiredFields = ['groupId', 'name', 'memberIds', 'inviteCode', 'createdBy', 'createdAt'];
         return isAuthenticated() &&
           data.keys().hasOnly(requiredFields) &&
           data.keys().hasAll(requiredFields) &&
           data.groupId == groupId &&
           data.name is string && data.name.size() > 0 &&
           data.inviteCode is string && data.inviteCode.size() == 6 &&
           data.createdBy == request.auth.uid &&
           data.memberIds == [request.auth.uid];
       }

       // users/{uid}
       match /users/{uid} {
         allow read, write: if isUser(uid);
       }

       // groups/{groupId}
       match /groups/{groupId} {
         allow create: if isValidGroupCreate(groupId);
         allow read, update: if isAuthenticated() && request.auth.uid in resource.data.memberIds;
         allow delete: if isAuthenticated() && resource.data.createdBy == request.auth.uid;

         // Events subcollection
         match /events/{eventId} {
           allow read, write: if isGroupMember(groupId);
         }

         // Tasks subcollection
         match /tasks/{taskId} {
           allow read, write: if isGroupMember(groupId);
         }
       }
     }
   }
   ```

4. **Firestore Indexes**:
   Standard subcollection queries are indexed automatically via single-field indexes. Composite indexes (if needed) are configured in `firestore.indexes.json`.

5. **Cloud Functions Deployment**:
   If using server-side group join validation, deploy the Cloud Functions from the `functions/` directory:
   ```bash
   cd functions
   npm install
   firebase deploy --only functions
   ```

---

## Build & Test Instructions

### Gradle Build
Assemble the debug APK:
```bash
./gradlew assembleDebug
```

### Run Unit Tests
Run all local JVM unit tests (including `ReminderTimeCalculatorTest`, `DailySummaryBuilderTest`, `DeepLinkParserTest`, `NavDestinationsTest`, `TasksViewModelTest`, `CalendarViewModelTest`, `EventRepositoryTest`, `TaskRepositoryTest`, `GroupRepositoryTest`, and `BootReceiverTest`):
```bash
./gradlew test
```

### Static Analysis (Detekt)
Run Detekt code quality and style checks:
```bash
./gradlew detekt
```

### Run Firebase Security Rules & Sync Emulator Tests
Verify Firestore security rules and multi-client real-time sync against the Firebase Local Emulator Suite:
```bash
npx firebase-tools emulators:exec --only firestore "npm --prefix functions test"
```

### Run Instrumented & UI Tests
Run Room DAO and Compose UI tests on an attached emulator or connected device:
```bash
./gradlew connectedDebugAndroidTest
```

---

## Architectural Highlights

- **Group Invite Security**: Joining groups via 6-character invite code is resolved securely through the `joinGroup` callable Cloud Function with atomic transaction validation. Clients never execute collection-wide queries across group records.
- **Real-Time Synchronization & Clean Teardown**: `EventRepository` and `TaskRepository` implement `startRealtimeSync()` using Kotlin coroutine `callbackFlow`. Firestore snapshot listener registrations are guaranteed to unregister on scope cancellation (`awaitClose { listener.remove() }`), preventing memory and background network leaks.
- **Active State Persistence**: The user's active group selection (`currentGroupId`) is persistently stored in Jetpack DataStore Preferences via `UserPreferencesRepository`. Navigation 3 active top-level destination (`selectedTopLevel`) is saved via `rememberSaveable` with `TopLevelNavKeySaver`, ensuring immediate state restoration across configuration changes and process recreation.
- **Exact-Alarm Fallback**: On Android 12+ (API 31+), if exact alarm scheduling is not granted, `ReminderScheduler` gracefully falls back to `setAndAllowWhileIdle()` to ensure reminders are never lost. `SettingsScreen` actively surfaces this degradation with an actionable rationale card and direct intent link to system settings.

