# StudyPartner Planner — Architecture, Security & Code Quality Audit

Date: 2026-09-25  
Author: Senior Android Reviewer  
Status: Phase 0 Complete — Awaiting Approval to Begin Phase 1

---

## Executive Summary

A comprehensive code audit was performed on **StudyPartner Planner** across architecture, security, persistence, background processing, adaptive UI, and test coverage.

Both `./gradlew assembleDebug` and `./gradlew test` compile and pass in the current repository state (5 test classes, 18 unit tests passing). However, critical vulnerabilities and architectural defects were identified:
- **P0 Security Vulnerabilities**: Permissive `groups` creation rules allowing arbitrary document spoofing; public commit of `app/google-services.json`; and README documentation recommending insecure rules that expose all study group data to strangers.
- **P0 Functional Regressions**: Broken deep link handling where links received via `onNewIntent` or while logged out are dropped; exact alarm scheduling that silently drops reminders when permissions are not granted; and background receivers launching unmanaged coroutines vulnerable to process death.
- **P1 Gaps Against Spec**: Complete absence of real-time Firestore listeners (all sync is manual one-shot `get()`); mock UI stubs (`MonthView`, `EventDetail`, `TaskDetail`); unhandled system back navigation in adaptive `ListDetailPaneScaffold`; unpersisted group selection; and missing WorkManager scheduling triggers on boot and timezone change.

---

## Detailed Findings Matrix

| ID | Category | Severity | File Path | Summary |
|---|---|---|---|---|
| **SEC-01** | Security Rules | **P0** | [`firestore.rules:25`](file:///d:/Projects/partner/partner/firestore.rules#L25) | `groups` creation rule allows any authenticated user to create groups with unvalidated `memberIds` and `createdBy`. |
| **SEC-02** | Security Rules / Spec | **P0** | [`README.md:89-95`](file:///d:/Projects/partner/partner/README.md#L89-L95) | Documented Firestore rules allow any authenticated user to read/write all groups' events and tasks. |
| **SEC-03** | Secrets Hygiene | **P0** | [`app/google-services.json`](file:///d:/Projects/partner/partner/app/google-services.json) | `google-services.json` containing live project credentials and API keys is tracked in public git repo. |
| **DL-01** | Deep Linking | **P0** | [`MainActivity.kt:64-86, 94-109`](file:///d:/Projects/partner/partner/app/src/main/java/com/studypartner/planner/MainActivity.kt#L64-L86) | Deep links received via `onNewIntent` while running are ignored; signed-out deep links are discarded during auth/group state races. |
| **ALM-01** | Alarms & Reminders | **P0** | [`ReminderScheduler.kt:20-24`](file:///d:/Projects/partner/partner/app/src/main/java/com/studypartner/planner/notifications/ReminderScheduler.kt#L20-L24) | Exact alarms silently dropped with no fallback to inexact alarms when `canScheduleExactAlarms()` is false. |
| **ALM-02** | Background Tasks | **P0** | [`BootReceiver.kt:29-41`](file:///d:/Projects/partner/partner/app/src/main/java/com/studypartner/planner/notifications/BootReceiver.kt#L29-L41) | `BootReceiver` launches coroutine without `goAsync()`, risking process death before rescheduling alarms. |
| **DATA-01** | Offline / Real-Time | **P0** | [`EventRepository.kt:25-50`](file:///d:/Projects/partner/partner/app/src/main/java/com/studypartner/planner/data/repository/EventRepository.kt#L25-L50), [`TaskRepository.kt:29-42`](file:///d:/Projects/partner/partner/app/src/main/java/com/studypartner/planner/data/repository/TaskRepository.kt#L29-L42) | Zero real-time listeners (`addSnapshotListener`); partner edits never sync until manual trigger. |
| **SEC-04** | Security / Data | **P1** | [`GroupRepository.kt:148`](file:///d:/Projects/partner/partner/app/src/main/java/com/studypartner/planner/data/repository/GroupRepository.kt#L148) | Client-side query by `inviteCode` across `groups` collection is blocked by Firestore rules, causing 8-second timeout before fallback. |
| **SEC-05** | Firestore Schema | **P1** | [`EventRepository.kt:70-77`](file:///d:/Projects/partner/partner/app/src/main/java/com/studypartner/planner/data/repository/EventRepository.kt#L70-L77), [`firestore.rules:41-45`](file:///d:/Projects/partner/partner/firestore.rules#L41-L45) | Redundant top-level `/events` writes and rules duplicate data and bypass Cloud Function FCM triggers. |
| **NAV-01** | Navigation 3 | **P1** | [`Navigator.kt:25-28`](file:///d:/Projects/partner/partner/app/src/main/java/com/studypartner/planner/ui/navigation/Navigator.kt#L25-L28) | `selectedTopLevel` tab is stored in transient `mutableStateOf` and resets to `Calendar` on process death. |
| **NAV-02** | Adaptive Navigation | **P1** | [`CalendarScreen.kt:46`](file:///d:/Projects/partner/partner/app/src/main/java/com/studypartner/planner/ui/calendar/CalendarScreen.kt#L46), [`TasksScreen.kt:36`](file:///d:/Projects/partner/partner/app/src/main/java/com/studypartner/planner/ui/tasks/TasksScreen.kt#L36) | Single-pane `ListDetailPaneScaffold` does not handle system back navigation; pressing back pops the root destination. |
| **NAV-03** | UI Completeness | **P1** | [`AppNavDisplay.kt:64-87`](file:///d:/Projects/partner/partner/app/src/main/java/com/studypartner/planner/ui/navigation/AppNavDisplay.kt#L64-L87) | `EventDetail`, `TaskDetail`, and `TaskEditor` are stubbed with `PlaceholderScreen`. |
| **UI-01** | Adaptive UI | **P1** | [`CalendarScreen.kt:171-180`](file:///d:/Projects/partner/partner/app/src/main/java/com/studypartner/planner/ui/calendar/CalendarScreen.kt#L171-L180) | `MonthView` in expanded mode is a hardcoded mock composable (`"Month Grid View (Mock)"`). |
| **WM-01** | Background Tasks | **P1** | [`DailySummaryScheduler.kt:17-45`](file:///d:/Projects/partner/partner/app/src/main/java/com/studypartner/planner/notifications/DailySummaryScheduler.kt#L17-L45) | Delay calculation uses legacy `Calendar` with missing millisecond clearance; untestable private logic. |
| **WM-02** | Background Tasks | **P1** | [`BootReceiver.kt:28`](file:///d:/Projects/partner/partner/app/src/main/java/com/studypartner/planner/notifications/BootReceiver.kt#L28), [`StudyPartnerApplication.kt:24`](file:///d:/Projects/partner/partner/app/src/main/java/com/studypartner/planner/StudyPartnerApplication.kt#L24) | `DailySummaryWorker` is not scheduled on app launch or rescheduled on `TIMEZONE_CHANGED`/`BOOT_COMPLETED`. |
| **DATA-02** | State Persistence | **P1** | [`GroupRepository.kt:26-31`](file:///d:/Projects/partner/partner/app/src/main/java/com/studypartner/planner/data/repository/GroupRepository.kt#L26-L31) | Current `groupId` is only held in an in-memory `MutableStateFlow` and is lost across restarts. |
| **TEST-01** | Test Coverage | **P1** | [`app/src/test`](file:///d:/Projects/partner/partner/app/src/test) | Missing unit tests for `DailySummaryScheduler`, deep link replay, rules, and repository error paths. |
| **CODE-01** | Code Smells | **P1** | [`TasksViewModel.kt:37`](file:///d:/Projects/partner/partner/app/src/main/java/com/studypartner/planner/ui/tasks/TasksViewModel.kt#L37) | Stale `currentUser` evaluated once during ViewModel instantiation. |
| **CODE-02** | Error Handling | **P1** | [`EventRepository.kt:47, 67`](file:///d:/Projects/partner/partner/app/src/main/java/com/studypartner/planner/data/repository/EventRepository.kt#L47), [`TaskRepository.kt:40, 56`](file:///d:/Projects/partner/partner/app/src/main/java/com/studypartner/planner/data/repository/TaskRepository.kt#L40) | Remote synchronization exceptions are silently swallowed with `e.printStackTrace()`. |
| **NAV-04** | UI Navigation | **P2** | [`AppNavigationSuiteScaffold.kt:59`](file:///d:/Projects/partner/partner/app/src/main/java/com/studypartner/planner/ui/components/AppNavigationSuiteScaffold.kt#L59) | `JoinGroup` screen displays the main navigation suite scaffold bar/drawer. |
| **UI-02** | Dead Code | **P2** | [`ContentWidthLimiter.kt:16`](file:///d:/Projects/partner/partner/app/src/main/java/com/studypartner/planner/ui/adaptive/ContentWidthLimiter.kt#L16) | `ContentWidthLimiter` composable is declared and documented in `DECISIONS.md` but never used. |
| **WM-03** | WorkManager Logic | **P2** | [`DailySummaryWorker.kt:32-46`](file:///d:/Projects/partner/partner/app/src/main/java/com/studypartner/planner/notifications/DailySummaryWorker.kt#L32-L46) | In-place mutation of single `Calendar` instance across `startOfDay` and `endOfDay`. |
| **WM-04** | DAO Query | **P2** | [`TaskDao.kt:26`](file:///d:/Projects/partner/partner/app/src/main/java/com/studypartner/planner/data/local/TaskDao.kt#L26) | Undated open tasks (`dueDate IS NULL`) are included every day in the daily summary indefinitely. |

---

## Detailed Section Analyses

### 1. Firestore Security Rules (P0 / P1)
- **Reality vs README**:
  - In `README.md` lines 89-94, the documented rule for events and tasks is:
    ```javascript
    match /events/{eventId} { allow read, write: if request.auth != null; }
    match /tasks/{taskId} { allow read, write: if request.auth != null; }
    ```
    This allows any signed-in user to read or tamper with any group's events and tasks.
  - In `firestore.rules`, subcollections check `isGroupMember(groupId)`. However, `groups/{groupId}` creation has **no data validation**:
    ```javascript
    match /groups/{groupId} {
      allow create: if isAuthenticated();
      ...
    }
    ```
    An attacker can create a group setting `memberIds` to arbitrary UIDs or an empty list, and spoof `createdBy`.
- **Invite Code Query**:
  - `GroupRepository.directJoinGroupByCode` queries `firestore.collection("groups").whereEqualTo("inviteCode", inviteCode)`.
  - Under `allow read: if request.auth.uid in resource.data.memberIds`, Firestore rejects the collection query with `PERMISSION_DENIED` because the user is not a member of all documents in the collection.
  - The request hangs until timeout (8s), then falls back to Cloud Functions.
- **Top-level collection**:
  - `EventRepository` saves events to both `groups/{groupId}/events/{id}` and `/events/{id}`. The root collection does not trigger FCM push notifications (which listen only to `groups/{groupId}/events/{eventId}`).

### 2. Deep Link Handling (P0 / P1)
- **Intent Filter & Parser**:
  - `AndroidManifest.xml` defines intent filters for `studypartner://join/*`, `studypartner://event/*`, and `studypartner://task/*`.
  - `DeepLinkParser.kt` parses these into `JoinGroup`, `EventDetail`, and `TaskDetail`.
- **Lifecycle & Replay Flaws**:
  - In `MainActivity.kt`, `onNewIntent` sets `pendingInviteCode`, `pendingEventId`, or `pendingTaskId`.
  - The navigation trigger `LaunchedEffect(currentUser, groups)` only observes `currentUser` and `groups`. When a deep link arrives while the activity is already running and authenticated, `LaunchedEffect` is **not triggered**, dropping the deep link.
  - When signed out, a deep link sets the pending variable. After sign-in, `currentUser` changes, triggering navigation. But when `groupViewModel.fetchGroups()` finishes and emits `groups`, `LaunchedEffect` triggers again. If `groups.isEmpty()`, it forcibly calls `navigator.resetTo(GroupSetup)`, blowing away the deep-linked screen!
  - `pending*` fields are plain variables and are destroyed on process death.

### 3. Navigation 3 Correctness (P1 / P2)
- **State Preservation**:
  - Back stacks use `rememberNavBackStack` which leverages `rememberSaveable`.
  - However, `AppNavigator.selectedTopLevel` is backed by standard `mutableStateOf`. On configuration change or process death, the active tab reverts to `Calendar`.
- **Back Handling in ListDetailPaneScaffold**:
  - On compact phones, `ListDetailPaneScaffold` operates in single-pane mode. Opening an event or task detail shifts the scaffold to the detail role.
  - No `BackHandler` is installed in `CalendarScreen` or `TasksScreen`. Pressing the system back button triggers `NavDisplay.onBack`, popping the entire tab destination rather than closing the detail pane.
- **ViewModel Scoping**:
  - Entry decorators include `rememberViewModelStoreNavEntryDecorator()`. Each entry has its own ViewModelStore.
  - However, `EventEditorScreen` directly acquires `CalendarViewModel = hiltViewModel()` rather than an event-specific ViewModel or receiving the ID through saved state.

### 4. Adaptive Layout (P1 / P2)
- **Adaptive Scaffolding**:
  - `AppNavigationSuiteScaffold` calculates layout type via `currentWindowAdaptiveInfoV2()` and breakpoint `WIDTH_DP_EXPANDED_LOWER_BOUND` to display `NavigationDrawer` on wide screens and `NavigationBar` / `NavigationRail` on smaller screens.
- **Unimplemented / Mocked Components**:
  - In `CalendarScreen.kt`, when width is `EXPANDED`, `CalendarListPane` displays `MonthView` alongside `AgendaView`. `MonthView` is literally a mock text view: `Text("Month Grid View (Mock)")`.
  - `ContentWidthLimiter` is implemented in `ui/adaptive/ContentWidthLimiter.kt` but is completely unused.

### 5. Alarm & Reminder Correctness (P0 / P1)
- **Exact Alarms**:
  - `ReminderScheduler.scheduleReminder` checks `alarmManager.canScheduleExactAlarms()`. If `false`, it immediately returns with no fallback. On Android 12+ where exact alarm permission is not granted by default, alarms are dropped.
- **BootReceiver**:
  - `BootReceiver.onReceive` launches a coroutine on `CoroutineScope(Dispatchers.IO)` without calling `goAsync()`. The BroadcastReceiver completes immediately, allowing Android to kill the process while database reads are pending.
  - `BootReceiver` does not reschedule `DailySummaryWorker`.

### 6. WorkManager Daily Summary (P1 / P2)
- **Initial Delay Calculation**:
  - Embedded in `DailySummaryScheduler.scheduleDailySummary`:
    ```kotlin
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
    }
    if (target.before(now)) {
        target.add(Calendar.DAY_OF_YEAR, 1)
    }
    val initialDelay = target.timeInMillis - now.timeInMillis
    ```
  - Milliseconds are not zeroed. If `now` is within the same minute, millisecond discrepancies can produce off-by-one errors or immediate triggers.
  - Uses legacy `java.util.Calendar` instead of the project standard `java.time`.
  - Untested in unit tests.
- **Scheduling Triggers**:
  - Work is only scheduled when the user changes settings in `SettingsViewModel`. It is not scheduled in `Application.onCreate` or `BootReceiver`.

### 7. Offline-First Correctness (P0 / P1)
- **Room as Source of Truth**:
  - `CalendarScreen` and `TasksScreen` read purely from Room via `eventDao.getEventsForGroup` and `taskDao.getTasks`.
  - Mappers `toEntity()` and `toDto()` correctly persist and convert data.
- **Missing Real-Time Sync**:
  - There are **zero** Firestore snapshot listeners in the entire app. `EventRepository.syncEvents` and `TaskRepository.syncTasks` only perform a one-time `.get().await()`. Changes made by study partners on other devices never appear unless a manual sync is triggered.

### 8. Secrets Hygiene (P0)
- **Tracked `google-services.json`**:
  - `app/google-services.json` is committed to git and tracked.
  - Remote is GitHub (`https://github.com/Geemal2004/partner.git`).
  - Contains API key `AIzaSyAUVUJAG0DORW-UyORkBxqiJGz01wTmDk8` and OAuth Web Client ID `1049276433931-2dc90chq25jpsuiki43dvrcvucef5idv.apps.googleusercontent.com`.
  - **Action Plan**:
    1. Confirm with project owner before removing from git (to avoid breaking builds for others).
    2. Add `app/google-services.json` to `.gitignore`.
    3. Restrict the API key in Google Cloud Console to Android app package `com.studypartner.planner` and the debug/release SHA-1 certificate fingerprint.

### 9. Test Coverage Evaluation (P1)
- **Passing Tests**:
  - `DailySummaryBuilderTest`: 7 unit tests pass.
  - `ReminderTimeCalculatorTest`: 5 unit tests pass.
  - `DeepLinkParserTest`: 6 unit tests pass.
  - `NavDestinationsTest`: 3 unit tests pass.
  - `TasksViewModelTest`: 4 unit tests pass.
- **Missing Tests**:
  - Delay calculation in `DailySummaryScheduler`.
  - Deep link handling and replay after login in `MainActivity`.
  - Firestore security rules unit tests (`@firebase/rules-unit-testing`).
  - `EventRepository` and `TaskRepository` sync and offline mapping.
  - Inexact alarm fallback in `ReminderScheduler`.

### 10. Code Smells & Lifecycle (P1 / P2)
- **Stale Auth State**: `TasksViewModel` caches `val currentUser = authRepository.getCurrentUserSync()` once at construction.
- **Swallowed Exceptions**: Repository sync methods catch all `Exception`s and print stack traces without error state propagation or retry handling.
- **Ephemeral State**: Selected group ID is kept only in an in-memory `MutableStateFlow` in `GroupRepository`.
- **Missing Permission Flow**: Notification and alarm permissions in `SettingsScreen` lack rationale handling.

---

## Next Steps: Phase 1 Readiness

Upon your confirmation, we will proceed immediately with **PHASE 1: FIX P0 SECURITY ISSUES**:
1. **Fix `firestore.rules`**:
   - `groups/{groupId}`: Read/write only if `request.auth.uid in resource.data.memberIds`.
   - `groups` create: Enforce creator uid in `memberIds`, `memberIds.size() == 1`, and `createdBy == request.auth.uid`.
   - Subcollections: Enforce parent group membership via `get(/databases/$(database)/documents/groups/$(groupId)).data.memberIds`.
   - Remove redundant top-level `/events` rule.
2. **Fix invite-code lookup**:
   - Route group join lookup securely without listing all groups client-side.
3. **Update `firestore.indexes.json`** to match required query patterns.
4. **Create rules unit tests** (`@firebase/rules-unit-testing` or test suite).
5. **Git & Secrets hygiene**:
   - Add `app/google-services.json` to `.gitignore`.
   - Document key restriction guidance for Google Cloud Console.
6. Verify `./gradlew assembleDebug` and `./gradlew test` pass.
