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
- **Firestore Synchronization**: Two-way sync via `EventRepository` and `TaskRepository` syncing local changes with Firebase Firestore collections (`groups/{groupId}/events`, `groups/{groupId}/tasks`).
- **User Preferences**: Jetpack DataStore Preferences for daily summary time, reminder offsets, and partner update toggles.

## Notifications & Background Processing

- **WorkManager**: `DailySummaryWorker` scheduled periodically or at user-specified daily time to summarize today's events and open tasks.
- **AlarmManager**: Exact alarms scheduled by `ReminderScheduler` based on event start times and offset preferences.
- **Firebase Cloud Messaging**: `StudyPartnerFcmService` receives push notifications for partner updates and invites.

## Testing Strategy

- **Unit Tests**: JUnit 4, MockK, Truth, Turbine, and Coroutines Test (`TasksViewModelTest`, `ReminderTimeCalculatorTest`, `DailySummaryBuilderTest`, `DeepLinkParserTest`, `NavDestinationsTest`).
- **Room DAO Tests**: Instrumented Android JUnit 4 tests using in-memory Room database (`EventDaoTest`, `TaskDaoTest`).
- **Compose UI Tests**: Instrumented Compose testing rules verifying task filtering (`TasksScreenTest`) and event creation form inputs (`EventEditorScreenTest`).
