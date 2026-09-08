# Tasks: Export & Import Boards

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~340 authored (design estimate; 6 new files + 9 modified) |
| 400-line budget risk | Medium |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 (data/domain) → PR 2 (UI/navigation) |
| Delivery strategy | ask-on-risk |
| Chain strategy | resolved: size:exception (no PR/review workflow on this project; single apply run) |

Decision needed before apply: Resolved — this project has no PR/review workflow (all work lands as direct commit(s) on `master`); the maintainer accepted `size:exception` with no chaining, since there is nothing to chain against.
Chained PRs recommended: No (superseded by the above resolution)
Chain strategy: size:exception
400-line budget risk: Medium (accepted)

Design's own ~340-line estimate sits only ~60 lines under the 400 budget; icon imports, doc comments, and instrumented-test boilerplate can easily push it over. The originally suggested data/domain vs UI PR split does not apply here since delivery is a single direct-to-`master` commit, not a PR review — all 7 phases / 29 tasks were implemented as one unit of work in a single `sdd-apply` run.

### Suggested Work Units

| Unit | Goal | Likely PR | Focused test command | Runtime harness | Rollback boundary |
|------|------|-----------|----------------------|-----------------|-------------------|
| 1 | Data/domain layer: Gson dep, `BoardJsonCodec`, DAO bulk-insert/prefetch, `importBoards()` dedup, unit + instrumented tests | PR 1 | `./gradlew testDebugUnitTest --tests "*BoardJsonCodec*" --tests "*RoomBoardRepository*"` | `./gradlew connectedDebugAndroidTest --tests "*BoardDaoTest*"` (deferred — no emulator in apply/verify env, per standing project convention) | Revert `data/json/`, `BoardDao.kt`, `BoardRepository.kt`, `RoomBoardRepository.kt`, `BoardDaoTest.kt`, `libs.versions.toml`/`build.gradle.kts` Gson lines — no UI depends on it yet |
| 2 | UI/navigation layer: routes, `SettingsScreen`, `ImportBoardsScreen`, ViewModels, bottom-bar wiring | PR 2 | `./gradlew testDebugUnitTest --tests "*ImportBoardsViewModel*"` | Manual/device smoke test (deferred — no Compose UI test infra, per standing project convention) | Revert `ui/boards/settings/`, `ui/boards/importexport/`, `BingoRoute.kt`, `BingoNavHost.kt`, `BoardListScreen.kt` onClick — independent of PR 1's DB internals once `importBoards()` contract exists |

## Phase 1: Foundation — Dependency & Data Contracts

- [x] 1.1 Add `gson` version + library alias to `gradle/libs.versions.toml`.
- [x] 1.2 Add `implementation(libs.gson)` to `app/build.gradle.kts`.
- [x] 1.3 Create `app/src/main/java/com/sergiodev/bingo/data/json/BoardJsonCodec.kt` with `data class BoardExportDto(val id: Long, val identifier: String, val numbers: List<Int>)` and `object BoardJsonCodec` exposing `encode(List<BoardCard>): String`.
- [x] 1.4 In `BoardJsonCodec.kt`, implement `decode(json: String): List<BoardCard>` using `TypeToken<List<BoardExportDto>>`, mapping DTOs to `BoardCard`; let any Gson/NPE exception propagate (no internal catch).
- [x] 1.5 Add `getAllIds(): List<Long>`, `getAllIdentifiers(): List<String>`, and `insertAll(boards: List<BoardEntity>)` (`OnConflictStrategy.ABORT`) to `app/src/main/java/com/sergiodev/bingo/data/local/dao/BoardDao.kt`.
- [x] 1.6 Add `data class ImportResult(val imported: Int, val skipped: Int)` and `suspend fun importBoards(boards: List<BoardCard>): ImportResult` to `app/src/main/java/com/sergiodev/bingo/domain/repository/BoardRepository.kt`.
- [x] 1.7 Implement `importBoards()` in `app/src/main/java/com/sergiodev/bingo/data/repository/RoomBoardRepository.kt`: prefetch `getAllIds()` + `getAllIdentifiers()`, dedup against DB and in-batch (seen-ids/seen-identifiers sets), `insertAll()` the survivors, return `ImportResult(imported, skipped)`.

## Phase 2: Data/Domain Testing (PR 1)

- [x] 2.1 Add unit test file for `BoardJsonCodec`: round-trip encode→decode preserves `id`/`identifier`/`numbers`; malformed JSON string throws; JSON missing a required field throws (NPE-via-Gson case per design decision).
- [x] 2.2 Add/extend unit tests for `RoomBoardRepository.importBoards()` using a fake `BoardDao` test double (mirroring `CreateBoardViewModelTest` convention): all-new boards import fully; entry with existing `id` skipped; entry with existing `identifier` (new `id`) skipped; two in-batch entries sharing an `id` — second skipped.
- [x] 2.3 Extend `app/src/androidTest/java/com/sergiodev/bingo/data/local/dao/BoardDaoTest.kt` with: explicit non-zero PK survives `insertAll()` unchanged.
- [x] 2.4 Extend `BoardDaoTest.kt` with: a subsequent regular `insert()` after an explicit-PK `insertAll()` autoincrements past the imported max id without colliding.

## Phase 3: Navigation Wiring (Foundation for PR 2)

- [x] 3.1 Add `SETTINGS` and `IMPORT_BOARDS` route constants to `app/src/main/java/com/sergiodev/bingo/ui/navigation/BingoRoute.kt`, following the existing plain `const val` pattern.
- [x] 3.2 Register `composable(BingoRoute.SETTINGS)` and `composable(BingoRoute.IMPORT_BOARDS)` destinations in `app/src/main/java/com/sergiodev/bingo/ui/navigation/BingoNavHost.kt`, wiring back-navigation and thread-through of `onNavigateToSettings`/`onNavigateToImport`.

## Phase 4: Settings & Export UI (PR 2)

- [x] 4.1 Create `app/src/main/java/com/sergiodev/bingo/ui/boards/settings/SettingsViewModel.kt` with `exportJson(onReady: (String) -> Unit)`: snapshot `repository.observeBoards().first()`, encode via `BoardJsonCodec.encode`, invoke callback with the JSON string.
- [x] 4.2 Create `app/src/main/java/com/sergiodev/bingo/ui/boards/settings/SettingsScreen.kt`: zeroed-inset `Scaffold`/`TopAppBar` + back nav matching `GameSetupScreen.kt`'s chrome exactly; 2 `ListItem`s ("Exportar" with share icon, "Importar" with icon).
- [x] 4.3 Wire "Exportar" tap in `SettingsScreen.kt` to `SettingsViewModel.exportJson`, firing `Intent.ACTION_SEND` (`EXTRA_TEXT` = JSON) via `Intent.createChooser` using `LocalContext.current`.
- [x] 4.4 Wire "Importar" tap in `SettingsScreen.kt` to navigate to `BingoRoute.IMPORT_BOARDS`.
- [x] 4.5 In `app/src/main/java/com/sergiodev/bingo/ui/boards/list/BoardListScreen.kt`, add an `onNavigateToSettings` param and replace the Configuración bottom-bar button's `onClick = {}` with `onClick = onNavigateToSettings`.
- [x] 4.6 In `BingoNavHost.kt`, pass the board-list-to-settings navigation lambda into `BoardListScreen`'s new `onNavigateToSettings` param.

## Phase 5: Import UI (PR 2)

- [x] 5.1 Create `app/src/main/java/com/sergiodev/bingo/ui/boards/importexport/ImportBoardsUiState.kt` with `jsonText: String`, `jsonError: String?`, `resultMessage: String?`.
- [x] 5.2 Create `app/src/main/java/com/sergiodev/bingo/ui/boards/importexport/ImportBoardsViewModel.kt` with `onJsonTextChange(text: String)`, `onSubmit()`, `onResultMessageShown()`.
- [x] 5.3 In `ImportBoardsViewModel.onSubmit()`, wrap `BoardJsonCodec.decode(jsonText)` in `catch (e: Exception)`: on failure, clear `jsonText`, set non-null `jsonError`; on success, call `repository.importBoards(list)` and set `resultMessage` from the returned `ImportResult` ("N importados, M omitidos").
- [x] 5.4 Handle blank/empty submitted text as an immediate error path in `onSubmit()` (no decode attempt), matching the "empty/blank textarea" spec scenario.
- [x] 5.5 Create `app/src/main/java/com/sergiodev/bingo/ui/boards/importexport/ImportBoardsScreen.kt`: zeroed-inset `Scaffold`/`TopAppBar` + back nav matching `GameSetupScreen.kt`; multi-line `OutlinedTextField(isError = uiState.jsonError != null)`, submit `Button`, `Snackbar` bound to `resultMessage`.

## Phase 6: UI Testing (PR 2)

- [x] 6.1 Add unit test file for `ImportBoardsViewModel` (mirroring `CreateBoardViewModelTest.kt` convention, fake `BoardRepository`): malformed JSON submit sets `jsonError` and clears `jsonText`; blank submit sets error without calling the repository; well-formed JSON submit calls `importBoards` and sets `resultMessage` from the returned counts.

## Phase 7: Verification

- [x] 7.1 Run `./gradlew testDebugUnitTest` and confirm all new and existing unit tests pass.
- [x] 7.2 Attempt `./gradlew connectedDebugAndroidTest --tests "*BoardDaoTest*"`; if no emulator/device is available in this environment, defer per standing project convention and note it as a manual follow-up.
- [x] 7.3 Run `rg` structural checks: confirm no remaining `onClick = {}` on the Configuración button in `BoardListScreen.kt`, confirm `BoardRepository`/`RoomBoardRepository`/`BoardDao` existing method signatures are unchanged (additive-only diff).
- [x] 7.4 Defer manual/device smoke test (Configuración → Exportar opens share sheet with valid JSON; Importar accepts/red-borders JSON; end-to-end export→import round trip) per standing project convention (consistent with the 9 prior archived changes).

### Verification Evidence

- `./gradlew testDebugUnitTest`: BUILD SUCCESSFUL, 112/112 test cases passed (18 test classes), including new `BoardJsonCodecTest`, `RoomBoardRepositoryTest`, `ImportBoardsViewModelTest`, and the updated fakes in `CreateBoardViewModelTest`/`BoardListViewModelTest`/`GameSetupViewModelTest`/`GamePlayViewModelTest`.
- `./gradlew build` (connectedDebugAndroidTest excluded): BUILD SUCCESSFUL, including `lintDebug`/`lintVitalRelease`.
- `connectedDebugAndroidTest`: deferred — `adb` is not present in this environment (command not found), confirming no emulator/device is attached. Consistent with all 9 prior archived changes. The 2 new `BoardDaoTest` cases are written and compile-checked but unexecuted; manual follow-up needed on a real device/emulator.
- `rg` structural checks: no `onClick = {}` remains in `BoardListScreen.kt`; `BoardRepository.observeBoards`/`addBoard`, `RoomBoardRepository.observeBoards`/`addBoard`, and `BoardDao.observeAll`/`insert` signatures are byte-for-byte unchanged (additive-only diff confirmed).
- Manual/device smoke test: deferred per standing project convention — no Compose UI test infrastructure exists in this project.
