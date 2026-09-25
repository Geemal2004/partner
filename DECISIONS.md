# Architecture & dependency decisions

Recorded during implementation so Navigation 3 / Adaptive / Firebase choices stay explicit as those APIs move quickly.

## Phase 0 — toolchain (2026-09-20)

| Choice | Version / approach | Why |
| --- | --- | --- |
| AGP | **9.2.0** | Matches Gradle **9.4.1** already in the wrapper. Adaptive 1.3 / Compose 1.12 require AGP ≥ 9.1 and `compileSdk` **37**. Skipped AGP 9.4 (needs Gradle 9.6). |
| JVM | Bytecode **17**; run on local JDK 21+ | Removed generated `gradle-daemon-jvm.properties` (pinned Foojay Temurin 25 and failed to download). No Foojay toolchain auto-provisioning; developers use an installed JDK. |
| Kotlin | **2.2.10** (AGP built-in) | AGP 9.0 ships built-in Kotlin / KGP **2.2.10**. Removed `org.jetbrains.kotlin.android`. Compose + serialization plugins still declared at 2.2.10. KSP **2.3.5** (AGP 9 / built-in Kotlin compatible). `android.disallowKotlinSourceSets=false` kept as a safety valve if KSP still registers via `kotlin.sourceSets`. |
| Compose BOM | **2026.08.00** | Current stable BOM aligned with Material3 1.4 / Compose 1.12. `compileSdk = 37` (required by Compose 1.12 / Adaptive 1.3); `targetSdk` remains **35** per product brief. |
| Navigation 3 | **1.1.7** (`navigation3-runtime` + `navigation3-ui`) | Current stable from [Navigation 3 get-started](https://developer.android.com/guide/navigation/navigation-3/get-started). **Not** Navigation Compose / `NavHost`. |
| ViewModel ↔ Nav3 | `lifecycle-viewmodel-navigation3` **2.11.0** | Stable Lifecycle release (get-started still shows `2.12.0-alpha0x`; we prefer stable). Provides `rememberViewModelStoreNavEntryDecorator()`. |
| Material3 Adaptive | **1.3.0** (`adaptive`, `adaptive-layout`, `adaptive-navigation`, **`adaptive-navigation3`**) | Stable Adaptive line that ships the Nav3 list-detail scene strategy. |
| Navigation suite | `material3-adaptive-navigation-suite` via Compose BOM | `NavigationSuiteScaffold` for compact bar / medium rail / expanded drawer. |
| Serialization | Kotlin serialization plugin + `kotlinx-serialization-json` **1.9.0** | Required for `@Serializable` `NavKey`s with `rememberNavBackStack`. |
| Hilt | **2.60.1** + `hilt-work` | App-wide DI; `StudyPartnerApplication` implements `Configuration.Provider` with `HiltWorkerFactory`. |
| Firebase BoM | **34.18.0** | Auth / Firestore / FCM / Cloud Functions on the BoM. Google services plugin enabled. |
| Time API | **`java.time`** | Single choice for the project (minSdk 26). No kotlinx-datetime. |

## Navigation 3 shape

- Single `MainActivity`.
- Destinations are `@Serializable` types implementing `NavKey` (`AppRoute`).
- Three top-level back stacks (Calendar / Tasks / Settings) via separate `rememberNavBackStack` instances; `AppNavigator` switches the active stack so tab changes preserve nested state.
- `NavDisplay` + `entryProvider` + entry decorators: `rememberSaveableStateHolderNavEntryDecorator()` and `rememberViewModelStoreNavEntryDecorator()`.
- Deep links: Custom scheme `studypartner://` parsed by `DeepLinkParser` into type-safe `AppRoute` destinations (`JoinGroup`, `EventDetail`, `TaskDetail`).

## Adaptive UI shape

- `currentWindowAdaptiveInfo()` drives `NavigationSuiteScaffold` layout type.
- Expanded width (≥ `WIDTH_DP_EXPANDED_LOWER_BOUND`) uses a navigation drawer; otherwise `NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo`.
- List-detail via `adaptive-navigation3` scene strategies for Calendar and Tasks screens.
- `ContentWidthLimiter` caps readable content width on large screens.

## Data & Sync Architecture

- **Room Database**: Local single source of truth (`StudyPartnerDatabase`) for `EventEntity` and `TaskEntity`.
- **Firestore Synchronization**: Synchronizing local changes exclusively with Firebase Firestore subcollections (`groups/{groupId}/events`, `groups/{groupId}/tasks`). Root-level collections `/events` and `/tasks` removed to ensure single-path security rules and FCM triggering.
- **Group Invite Code Lookup**: Routed through the secure callable Cloud Function (`joinGroup`) using Firebase Admin privileges. The client never queries across all groups, maintaining strict privacy where groups are readable only by their members.
- **User Preferences**: Jetpack DataStore Preferences for daily summary time, reminder offsets, and partner update toggles.

## Notifications & Background Processing

- **WorkManager**: `DailySummaryWorker` scheduled periodically or at user-specified daily time to summarize today's events and open tasks.
- **AlarmManager**: Exact alarms scheduled by `ReminderScheduler` based on event start times and offset preferences.
- **Firebase Cloud Messaging**: `StudyPartnerFcmService` receives push notifications for partner updates and invites.

## Testing Strategy

- **Unit Tests**: JUnit 4, MockK, Truth, Turbine, and Coroutines Test (`TasksViewModelTest`, `ReminderTimeCalculatorTest`, `DailySummaryBuilderTest`, `DeepLinkParserTest`, `NavDestinationsTest`, `EventRepositoryTest`, `TaskRepositoryTest`, `GroupRepositoryTest`).
- **Room DAO Tests**: Instrumented Android JUnit 4 tests using in-memory Room database (`EventDaoTest`, `TaskDaoTest`).
- **Compose UI Tests**: Instrumented Compose testing rules verifying task filtering (`TasksScreenTest`) and event creation form inputs (`EventEditorScreenTest`).

## Phase 3 — Architecture & Reliability Decisions (2026-09-25)

- **Exact-Alarm Fallback & Notice**: On Android 12+ (API 31+), if exact alarm permission (`SCHEDULE_EXACT_ALARM`) is denied, `ReminderScheduler` falls back to `setAndAllowWhileIdle()`. `SettingsScreen` actively inspects `canScheduleExactAlarms()` and displays an actionable warning card directing the user to system settings so that reminder imprecision is not silent.
- **Undated Task Daily Summary Policy (WM-04)**: Tasks without due dates (`dueDate == null`) are segregated from dated tasks due today. To prevent repetitive notification spam for stale backlog items, `DailySummaryWorker` queries up to 3 most recently updated undated tasks and formats them under a distinct "Ongoing Tasks" section with a preview of titles and a count of remaining items.
- **WorkManager Date Calculation (WM-03)**: Replaced mutable single-instance `java.util.Calendar` usage in `DailySummaryWorker` with immutable `java.time.LocalDate` and `ZoneId.systemDefault()`, preventing date boundary corruption.
- **Top-Level Tab State Persistence (NAV-01)**: `selectedTopLevel` in `AppNavigator` is persisted across process death using `rememberSaveable` with a dedicated `TopLevelNavKeySaver`, ensuring users return to their active tab upon process restoration.
- **Group Selection Persistence (DATA-02)**: Active `currentGroupId` is backed by Jetpack DataStore Preferences via `UserPreferencesRepository`. Selection changes immediately persist to disk and restore asynchronously on app startup.
- **Reactive Current User & Filter Integrity (CODE-01)**: `TasksViewModel` observes `AuthRepository.currentUser` as an active `StateFlow`, eliminating stale `auth.currentUser` references and updating `filteredTasks` dynamically across authentication lifecycle events.
- **Sync Exception & Realtime Listener Propagation (CODE-02)**: Swallowed exceptions in `EventRepository` and `TaskRepository` were replaced with proper error propagation (`close(error)` on snapshot channels and rethrown exceptions on one-shot syncs). `CalendarViewModel` and `TasksViewModel` expose `syncError: StateFlow<String?>` surfaced to users via actionable Retry UI banners.
- **Firestore Listener Teardown Verification**: Explicit unit tests in `EventRepositoryTest` and `TaskRepositoryTest` confirm that `ListenerRegistration.remove()` is called when the collecting coroutine scope is cancelled.

## Phase 4 — Quality, Static Analysis & Accessibility (2026-09-25)

- **Detekt Integration & Linter Configuration**: Integrated Detekt with custom configuration in `config/detekt/detekt.yml`. Configured Compose-aware rules: Composable annotations ignored for method length, parameter lists, and naming patterns; line length capped at 140; test packages excluded from generic exception throws; and matching declaration rules adjusted for multi-definition Compose files.
- **Accessibility Hardening**:
  - *Calendar & MonthView*: Day cells annotated with explicit semantics (`cellDescription`) announcing the full date (e.g. "Monday, September 28, 2026"), "Today" indicator, selection status, and exact event counts. Integrated back navigation icon into `EventDetailPane` `TopAppBar` with accessible descriptions.
  - *Tasks*: Checkbox controls annotated with contextual toggle descriptions ("Mark {title} as complete/incomplete"). Swipe-to-dismiss delete action annotated with item-specific description ("Delete {title}").
  - *Touch Targets & Font Scaling*: Preserved minimum 48dp touch targets across all interactive elements (`sizeIn(minWidth = 48.dp, minHeight = 48.dp)` and Material 3 component standards). Typography sizes use relative `sp` units within flexible scrollable containers.
- **Idiomatic Kotlin & Error Hygiene**:
  - Replaced `throw IllegalStateException(...)` with Kotlin standard `error(...)` throughout `GroupRepository`.
  - Replaced swallowed timeout exceptions with chained exception propagation to preserve diagnostic context.
  - Replaced `printStackTrace()` with structured `Log.e()` calls.
  - Extracted `TopLevelDestination` enum into a separate source file (`TopLevelDestination.kt`).
  - Extracted Google Play Services resolution helper in `AuthRepository` to reduce function complexity.

