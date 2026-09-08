# Apply Progress: Export & Import Boards

**Status**: All 29 tasks across 7 phases complete (29/29). First and only apply run for this change — no prior progress existed.
**Mode**: Standard (no strict TDD flag recorded for BingoFF).
**Delivery**: single unit of work, `size:exception` (project has no PR/review workflow; all work lands as direct commit(s) on `master`).

## Work Unit Evidence

| Evidence | Value |
|---|---|
| Focused test command and result | `./gradlew testDebugUnitTest` → BUILD SUCCESSFUL, 112/112 test cases across 18 classes (new: `BoardJsonCodecTest`, `RoomBoardRepositoryTest`, `ImportBoardsViewModelTest`) |
| Runtime harness command/scenario and result | `./gradlew build` (excl. `connectedDebugAndroidTest`) → BUILD SUCCESSFUL incl. lint. `connectedDebugAndroidTest` deferred — `adb` not found in this environment, confirming no emulator/device attached, consistent with all 9 prior archived changes. |
| Rollback boundary | Revert: `gradle/libs.versions.toml`, `app/build.gradle.kts` (Gson lines); `data/json/BoardJsonCodec.kt` (delete); `data/local/dao/BoardDao.kt`, `domain/repository/BoardRepository.kt`, `data/repository/RoomBoardRepository.kt` (additive methods); `ui/boards/settings/`, `ui/boards/importexport/` (delete dirs); `ui/navigation/BingoRoute.kt`, `ui/navigation/BingoNavHost.kt`, `ui/boards/list/BoardListScreen.kt` (route/wiring additions); test files listed below. |

## Completed Tasks (29/29)

### Phase 1: Foundation — Dependency & Data Contracts
- [x] 1.1 Gson version + library alias in `gradle/libs.versions.toml` (2.14.0, latest Maven Central release at apply time)
- [x] 1.2 `implementation(libs.gson)` in `app/build.gradle.kts`
- [x] 1.3/1.4 `data/json/BoardJsonCodec.kt` created: `BoardExportDto`, `encode()`, `decode()` (TypeToken-based, no internal catch — propagates)
- [x] 1.5 `BoardDao.kt`: `getAllIds()`, `getAllIdentifiers()`, `insertAll()` added (additive; `observeAll`/`insert` unchanged)
- [x] 1.6 `BoardRepository.kt`: `ImportResult` data class + `importBoards()` added to interface (additive)
- [x] 1.7 `RoomBoardRepository.kt`: `importBoards()` implemented — 2-query prefetch + in-batch dedup + `insertAll()`

### Phase 2: Data/Domain Testing
- [x] 2.1 `data/json/BoardJsonCodecTest.kt` (JVM unit): round-trip, empty-list, malformed JSON throws, missing-field NPE-via-Gson throws
- [x] 2.2 `data/repository/RoomBoardRepositoryTest.kt` (JVM unit, fake `BoardDao`): all-new imports fully, existing-id skipped, existing-identifier-different-id skipped, in-batch duplicate-id second entry skipped
- [x] 2.3/2.4 `BoardDaoTest.kt` (instrumented) extended: explicit non-zero PK survives `insertAll()`; subsequent `insert()` autoincrements past imported max id. Written and compiles; execution deferred (no `adb`/emulator in this environment)

### Phase 3: Navigation Wiring
- [x] 3.1 `BingoRoute.kt`: `SETTINGS`, `IMPORT_BOARDS` constants added (plain `const val`, unchanged pattern)
- [x] 3.2 `BingoNavHost.kt`: `composable(SETTINGS)` and `composable(IMPORT_BOARDS)` registered with back-nav wiring

### Phase 4: Settings & Export UI
- [x] 4.1 `ui/boards/settings/SettingsViewModel.kt`: `exportJson(onReady)` — snapshot `observeBoards().first()`, encode, callback
- [x] 4.2 `ui/boards/settings/SettingsScreen.kt`: zeroed-inset `Scaffold`/`TopAppBar` matching `GameSetupScreen.kt` chrome exactly; 2 `ListItem`s (Exportar/Share icon, Importar/List icon — see deviation note)
- [x] 4.3 "Exportar" wired to `SettingsViewModel.exportJson` → `Intent.ACTION_SEND` + `Intent.createChooser`, `LocalContext.current`
- [x] 4.4 "Importar" wired to navigate to `BingoRoute.IMPORT_BOARDS`
- [x] 4.5 `BoardListScreen.kt`: added `onNavigateToSettings` param; `onClick = {}` → `onClick = onNavigateToSettings` on the Configuración bottom-bar button
- [x] 4.6 `BingoNavHost.kt`: threaded `onNavigateToSettings = { navController.navigate(SETTINGS) }` into `BoardListScreen`

### Phase 5: Import UI
- [x] 5.1 `ui/boards/importexport/ImportBoardsUiState.kt`: `jsonText`, `jsonError: String?`, `resultMessage: String?`
- [x] 5.2/5.3/5.4 `ui/boards/importexport/ImportBoardsViewModel.kt`: `onJsonTextChange`, `onSubmit()` (blank→immediate error no decode attempt; decode wrapped in `catch (e: Exception)`; success calls `importBoards` + sets `resultMessage` "N importados, M omitidos"), `onResultMessageShown()`
- [x] 5.5 `ui/boards/importexport/ImportBoardsScreen.kt`: zeroed-inset `Scaffold`/`TopAppBar` matching `GameSetupScreen.kt`; multi-line `OutlinedTextField(isError = jsonError != null)`; submit `Button`; `Snackbar`/`SnackbarHost` bound to `resultMessage`

### Phase 6: UI Testing
- [x] 6.1 `ui/boards/importexport/ImportBoardsViewModelTest.kt` (JVM unit, fake `BoardRepository`): malformed JSON → error + cleared text; blank submit → error, repository not called; well-formed JSON → `importBoards` called, `resultMessage` set from returned counts

### Phase 7: Verification
- [x] 7.1 `./gradlew testDebugUnitTest` — BUILD SUCCESSFUL, 112/112 passing
- [x] 7.2 `connectedDebugAndroidTest` attempted — deferred, no `adb`/emulator present
- [x] 7.3 `rg` structural checks — no `onClick = {}` remains; existing `BoardRepository`/`RoomBoardRepository`/`BoardDao` method signatures unchanged
- [x] 7.4 Manual/device smoke test — deferred per standing convention (no Compose UI test infra), consistent with 9 prior archived changes

## Files Changed

| File | Action |
|---|---|
| `gradle/libs.versions.toml` | Modified — gson version + alias |
| `app/build.gradle.kts` | Modified — gson implementation dep |
| `app/src/main/java/com/sergiodev/bingo/data/json/BoardJsonCodec.kt` | Created |
| `app/src/main/java/com/sergiodev/bingo/data/local/dao/BoardDao.kt` | Modified — additive |
| `app/src/main/java/com/sergiodev/bingo/domain/repository/BoardRepository.kt` | Modified — additive |
| `app/src/main/java/com/sergiodev/bingo/data/repository/RoomBoardRepository.kt` | Modified — additive |
| `app/src/main/java/com/sergiodev/bingo/ui/navigation/BingoRoute.kt` | Modified — 2 new route constants |
| `app/src/main/java/com/sergiodev/bingo/ui/navigation/BingoNavHost.kt` | Modified — 2 new destinations + wiring |
| `app/src/main/java/com/sergiodev/bingo/ui/boards/list/BoardListScreen.kt` | Modified — `onNavigateToSettings` param + wiring |
| `app/src/main/java/com/sergiodev/bingo/ui/boards/settings/SettingsViewModel.kt` | Created |
| `app/src/main/java/com/sergiodev/bingo/ui/boards/settings/SettingsScreen.kt` | Created |
| `app/src/main/java/com/sergiodev/bingo/ui/boards/importexport/ImportBoardsUiState.kt` | Created |
| `app/src/main/java/com/sergiodev/bingo/ui/boards/importexport/ImportBoardsViewModel.kt` | Created |
| `app/src/main/java/com/sergiodev/bingo/ui/boards/importexport/ImportBoardsScreen.kt` | Created |
| `app/src/test/java/com/sergiodev/bingo/data/json/BoardJsonCodecTest.kt` | Created |
| `app/src/test/java/com/sergiodev/bingo/data/repository/RoomBoardRepositoryTest.kt` | Created |
| `app/src/test/java/com/sergiodev/bingo/ui/boards/importexport/ImportBoardsViewModelTest.kt` | Created |
| `app/src/androidTest/java/com/sergiodev/bingo/data/local/dao/BoardDaoTest.kt` | Modified — 2 new instrumented cases |
| `app/src/test/java/com/sergiodev/bingo/ui/boards/create/CreateBoardViewModelTest.kt` | Modified — added `importBoards` override to `FakeBoardRepository` (interface grew a new abstract member) |
| `app/src/test/java/com/sergiodev/bingo/ui/boards/list/BoardListViewModelTest.kt` | Modified — same reason |
| `app/src/test/java/com/sergiodev/bingo/ui/game/setup/GameSetupViewModelTest.kt` | Modified — same reason |
| `app/src/test/java/com/sergiodev/bingo/ui/game/play/GamePlayViewModelTest.kt` | Modified — same reason |

## Deviations from Design

1. **Import icon**: design's open question flagged `Icons.Default.FileDownload` as unconfirmed against the pinned `material-icons-core` artifact. Verified by inspecting the actual `material-icons-core` jar contents: `FileDownload` is NOT present in that artifact (only in `material-icons-extended`, which isn't a project dependency). Used `Icons.AutoMirrored.Filled.List` instead — compiles and resolves at build time, confirmed via the passing full `./gradlew build`. `Icons.Default.Share` (used for Exportar) was confirmed present in the same jar.
2. **`BoardListScreen`/`BoardListContent` fake test doubles**: adding the abstract `importBoards()` method to `BoardRepository` is a breaking interface change for every existing test double. Added a no-op override (`ImportResult(0, 0)`) to the 4 pre-existing fakes (`CreateBoardViewModelTest`, `BoardListViewModelTest`, `GameSetupViewModelTest`, `GamePlayViewModelTest`) purely to keep them compiling — this was implied but not itemized as a task in tasks.md.
3. Everything else matches the design/tasks exactly — no other deviations.

## Issues Found
None.

## Remaining Tasks
None — 29/29 complete.
