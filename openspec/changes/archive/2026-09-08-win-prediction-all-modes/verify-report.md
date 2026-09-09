```yaml
change: win-prediction-all-modes
mode: full-artifacts
verdict: PASS WITH WARNINGS
requirements_total: 3
scenarios_total: 13
scenarios_covered_by_runtime_test: 11
scenarios_covered_by_source_inspection_only: 2
tasks_total: 18
tasks_complete: 17
tasks_incomplete: 1
critical_count: 0
warning_count: 1
suggestion_count: 0
test_command: "./gradlew testDebugUnitTest --rerun-tasks"
test_exit_code: 0
build_command: "./gradlew clean build"
build_exit_code: 0
test_totals_baseline: 112
test_totals_current: 120
test_totals_delta: 8
test_failures: 0
```

## Verification Report: win-prediction-all-modes

### Completeness

| Task phase | Status |
|---|---|
| Phase 1: Domain — per-mode prediction rule (1.1–1.4) | Complete |
| Phase 2: UI — render format branch (2.1) | Complete |
| Phase 3: Tests — WinPredictionTest.kt (3.1–3.7) | Complete |
| Phase 4: Optional — BingoWinCheckerTest.kt L/I coverage (4.1–4.2) | Complete |
| Phase 5: Verification (5.1–5.3) | Complete |
| Phase 5.4: Manual/device smoke test | Incomplete — deferred (no emulator in this environment, established pattern across 12 prior archived changes) |

17/18 tasks complete, matching apply-progress.md. Task 5.4 is correctly marked incomplete with an explicit, non-silent reason and is treated as WARNING per this project's standing convention, not CRITICAL.

### Build/Test Evidence (re-run independently, not trusted from apply's report)

- `./gradlew testDebugUnitTest --rerun-tasks` → BUILD SUCCESSFUL, 32 tasks executed.
- `./gradlew clean build` → BUILD SUCCESSFUL, 109 tasks (107 executed, 2 up-to-date); includes compile, lint, assembleDebug, assembleRelease, test, check.
- JUnit XML aggregation across all 18 `app/build/test-results/testDebugUnitTest/*.xml` files (summed `tests=`/`failures=` attributes, this project's established convention): **120 tests total, 0 failures.**
- Baseline was 112 tests before this change → **+8 net new tests**, confirming test count increased as required: 6 in `WinPredictionTest` (13 tests total, up from 7) and 2 in `BingoWinCheckerTest` (11 tests total, up from 9).

### Spec Compliance Matrix

**Requirement: Per-Mode Possible-Winner Qualifying Rule**

| Scenario | Status | Evidence |
|---|---|---|
| Columna behavior unchanged (regression guard) | PASS | `columna_exactlySixCalls_isIncluded`, `boardQualifyingOnTwoColumns_producesTwoEntries`, etc. — all pre-existing Columna tests intact and passing byte-for-byte |
| O/L/I qualifies at exactly missing==3 | PASS | `oLIModes_missingThreeOrLess_isIncluded` |
| O/L/I does not qualify at missing==4 | PASS | `oLIModes_missingFour_isExcluded` |
| Cartón Completo qualifies at exactly missing==10 | PASS | `cartonCompleto_missingTenOrLess_isIncluded` |
| Cartón Completo does not qualify at missing==11 | PASS | `cartonCompleto_missingEleven_isExcluded` |
| No call-count ceiling + live recalculation for O/L/I/Completo | PASS | `oLIAndCompleto_noCallCountCeiling_stillIncludedAtHighCallCount` — asserts `called.size > MAX_PREDICTION_CALLS` (padded past 6) and the candidate still qualifies |

**Requirement: Panel Hides Once the Call Ceiling Is Exceeded (Columna Only)**

| Scenario | Status | Evidence |
|---|---|---|
| Columna at exactly the ceiling (6) | PASS | `columna_exactlySixCalls_isIncluded` |
| Columna one call past the ceiling (7) | PASS | `columna_sevenCalls_isExcluded` |

**Requirement: Entry Format, Sorting, and Exclusion of Announced Wins**

| Scenario | Status | Evidence |
|---|---|---|
| Columna entry format unchanged (`"Cartón 3 (N)"`) | PASS (source inspection) | No automated UI string test exists (no Compose UI test infra, project-wide gap); `GamePlayScreen.kt:154–157` inspected directly: `val suffix = candidate.letter?.let { " (${it.name})" }.orEmpty(); Text("Cartón ${candidate.identifier}$suffix")` produces the required format when `letter != null` |
| Non-Columna entry format has no parenthetical (`"Cartón 3"`) | PASS (source inspection) | Same code path: `letter == null` → `suffix` is empty string → `"Cartón <id>"` exactly, no letter, no mode name |
| Sorted fewest-missing-first | PASS | `results_sortedFewestMissingFirst_thenBoardId_thenLetterOrdinal` |
| Already-announced win excluded in every mode | PASS | `alreadyAnnouncedPair_isExcludedEvenIfStillQualifying` (Columna) + `nonColumnaModes_alreadyAnnouncedPair_isExcludedEvenIfStillQualifying` (O/L/I/Completo, parameterized) |
| Columna board qualifying on two columns produces two entries | PASS | `boardQualifyingOnTwoColumns_producesTwoEntries` |

11 of 13 scenarios have direct runtime-test coverage. The 2 UI-string-format scenarios have no covering automated test because this project has no Compose UI test infrastructure (a pre-existing, project-wide condition, not introduced by this change) — verified instead by direct source inspection of the exact string-building code, and explicitly scoped to the deferred manual/device smoke test (task 5.4) in both `tasks.md` and `design.md`'s own Testing Strategy table. Treated as WARNING, not CRITICAL, consistent with the same convention applied to task 5.4.

### Explicit Checks Requested

1. **Columna behavior byte-for-byte unchanged**: Confirmed. `missing <= 2` (via `missingThreshold(GameMode.COLUMNA) = 2`) AND `called.size <= MAX_PREDICTION_CALLS` (6) via the early guard `if (mode == GameMode.COLUMNA && called.size > MAX_PREDICTION_CALLS) return emptyList()`. Display format `"Cartón <id> (<letra>)"` still produced when `letter != null`. All 7 pre-existing Columna tests pass unmodified.
2. **O, L, I no-ceiling behavior**: Confirmed via `oLIModes_missingThreeOrLess_isIncluded`/`oLIModes_missingFour_isExcluded` (threshold=3) and `oLIAndCompleto_noCallCountCeiling_stillIncludedAtHighCallCount`, which explicitly asserts `called.size > MAX_PREDICTION_CALLS` while the candidate still appears.
3. **Cartón Completo missing<=10, no ceiling**: Confirmed via `cartonCompleto_missingTenOrLess_isIncluded`/`cartonCompleto_missingEleven_isExcluded` and the same no-ceiling test (parameterized to include `CARTON_COMPLETO`, threshold=10).
4. **Display format for O/L/I/Completo is `"Cartón <id>"` with no parenthetical**: Confirmed directly in `GamePlayScreen.kt` lines 154–157 (not just inferred from tests) — `candidate.letter?.let { " (${it.name})" }.orEmpty()` yields an empty suffix when `letter == null`.
5. **Already-announced exclusion for every mode**: Confirmed. The `AnnouncedWin` check in `predictPossibleWinners` (`WinPrediction.kt:63`) runs before any mode-specific threshold logic and is unconditional on mode; verified by both the pre-existing Columna test and the new parameterized `nonColumnaModes_alreadyAnnouncedPair_isExcludedEvenIfStillQualifying` covering O/L/I/Completo.
6. **`PredictionCandidate.letter` genuinely nullable, non-Columna candidates construct `null`**: Confirmed. `WinPrediction.kt:22` declares `val letter: BingoLetter?`; line 66 constructs `letter = if (mode == GameMode.COLUMNA) pattern.cells.first().column else null` — no sentinel value. `nonColumnaCandidate_hasNullLetter` asserts `it.letter == null` across O/L/I/Completo.
7. **No leftover reference to the old blanket gate**: Confirmed. `rg "mode != GameMode.COLUMNA" app/src/main/java/com/sergiodev/bingo/domain/game/WinPrediction.kt` returns no matches (exit code 1).
8. **Scope boundary held — zero changes to `GameMode.kt`, `BingoWinChecker.kt` (detection logic), `GameSetupViewModel.kt`, `GameSetupUiState.kt`, `BingoNavHost.kt`, `GamePlayUiState.kt`**: Confirmed. `git diff --stat` against all six paths produces zero output (no diff hunks). `git status --short` shows only 4 modified files: `WinPrediction.kt`, `GamePlayScreen.kt`, `WinPredictionTest.kt`, `BingoWinCheckerTest.kt`, plus the untracked `openspec/changes/win-prediction-all-modes/` directory (expected SDD artifacts).
9. **New L/I `BingoWinCheckerTest.kt` coverage is meaningful, not tautological**: Confirmed. `lMode_requiresAllNineCells` and `iMode_freeCellCountsButAllTwelveRealCellsStillRequired` each assert `isSatisfied` is `false` on an "all real cells but one" set and `true` only once every real cell is present — a genuine boundary/off-by-one check on real win-detection logic, not a trivial always-true assertion.
10. **Task 5.4 correctly deferred**: Confirmed. Marked `[ ]` (not silently dropped) in both `tasks.md` and `apply-progress.md`, with an explicit stated reason ("no emulator in this environment") consistent with the project's established pattern across 12 prior archived changes. Treated as WARNING, non-blocking.

### Design Coherence

All architecture decisions in `design.md` were implemented exactly as specified: nullable `letter: BingoLetter?` (not a sealed type, not a `hasLetter` boolean); per-mode threshold via a private inline `when` helper in `WinPrediction.kt` (not a config map, `GameMode.kt` untouched); call-count ceiling scoped to Columna only via a single inverted early guard. No deviations found, matching apply-progress.md's own "Deviations from Design: None" claim.

### Issues

**CRITICAL**: None.

**WARNING**:
1. Task 5.4 (manual/device smoke test of the new non-Columna render format) remains incomplete. This is an accepted, non-blocking gap per this project's standing convention (no Compose UI test infrastructure, no emulator available in this environment), consistent with all 12 prior archived changes and explicitly called out as "Manual (device)" in `design.md`'s own Testing Strategy table. Two spec scenarios (Columna and non-Columna entry string format) are consequently verified by source inspection only, not by a runtime-executed test.

**SUGGESTION**: None.

### Final Verdict: PASS WITH WARNINGS

Implementation matches specs, design, and tasks. All runtime-testable spec scenarios pass (11/13, 0 failures across 120 total unit tests, +8 over the 112 baseline). The scope boundary (six untouched files) holds exactly as designed. The sole gap — deferred manual smoke test / source-inspection-only verification of two UI string-format scenarios — is a pre-existing, accepted project convention, not a defect introduced by this change, and is non-blocking for archive.
