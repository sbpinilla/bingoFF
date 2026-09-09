# Apply Progress: Win Prediction for All Game Modes

**Change**: win-prediction-all-modes
**Mode**: Standard (no strict TDD)
**Status**: 17/18 tasks complete (1 expected-deferred: manual/device smoke test, no emulator in this environment)

## Completed Tasks

### Phase 1: Domain — Per-Mode Prediction Rule
- [x] 1.1 Added private `missingThreshold(mode: GameMode): Int` `when`-helper (COLUMNA=2, O/L/I=3, CARTON_COMPLETO=10)
- [x] 1.2 Changed `PredictionCandidate.letter` from `BingoLetter` to `BingoLetter?`
- [x] 1.3 Replaced the blanket Columna-only gate with `if (mode == GameMode.COLUMNA && called.size > MAX_PREDICTION_CALLS) return emptyList()` plus `missingThreshold(mode)`-based filtering
- [x] 1.4 Set `letter = if (mode == GameMode.COLUMNA) pattern.cells.first().column else null`; sort tiebreak now `letter?.ordinal ?: 0`

### Phase 2: UI — Render Format Branch
- [x] 2.1 `PossibleWinnersSection` branches on `candidate.letter` nullability (`"Cartón <id> (<letra>)"` vs `"Cartón <id>"`)

### Phase 3: Tests — WinPredictionTest.kt
- [x] 3.1 Removed `nonColumnaModes_alwaysReturnEmpty_regardlessOfProgressOrCallCount`
- [x] 3.2 Added `oLIModes_missingThreeOrLess_isIncluded` / `oLIModes_missingFour_isExcluded`
- [x] 3.3 Added `cartonCompleto_missingTenOrLess_isIncluded` / `cartonCompleto_missingEleven_isExcluded`
- [x] 3.4 Added `oLIAndCompleto_noCallCountCeiling_stillIncludedAtHighCallCount`
- [x] 3.5 Added `nonColumnaModes_alreadyAnnouncedPair_isExcludedEvenIfStillQualifying`
- [x] 3.6 Added `nonColumnaCandidate_hasNullLetter`
- [x] 3.7 Confirmed existing Columna tests unchanged and passing

### Phase 4: Optional — Win Detection Coverage
- [x] 4.1 Added `lMode_requiresAllNineCells` to `BingoWinCheckerTest.kt`
- [x] 4.2 Added `iMode_freeCellCountsButAllTwelveRealCellsStillRequired` to `BingoWinCheckerTest.kt`

### Phase 5: Verification
- [x] 5.1 `./gradlew testDebugUnitTest --tests "*.WinPredictionTest" --tests "*.BingoWinCheckerTest"` — BUILD SUCCESSFUL, all tests passed
- [x] 5.2 `./gradlew build` — BUILD SUCCESSFUL
- [x] 5.3 `rg "mode != GameMode.COLUMNA" app/src/main/java/com/sergiodev/bingo/domain/game/WinPrediction.kt` — no matches, old gate confirmed removed
- [ ] 5.4 Manual/device smoke test — expected-deferred (no emulator in this environment, consistent with all 12 prior archived changes)

## Work Unit Evidence

| Evidence | Value |
|---|---|
| Focused test command and result | `./gradlew testDebugUnitTest --tests "*.WinPredictionTest" --tests "*.BingoWinCheckerTest"` → BUILD SUCCESSFUL, all cases passed |
| Runtime harness command/scenario and result | N/A — no Compose UI test infra exists, no emulator in this environment (standing project convention); full `./gradlew build` (compile + lint + assembleDebug/Release + test) → BUILD SUCCESSFUL serves as the closest available runtime/integration gate |
| Rollback boundary | Revert the single commit touching `WinPrediction.kt`, `GamePlayScreen.kt`, `WinPredictionTest.kt`, `BingoWinCheckerTest.kt` — no persisted-data or migration coupling |

## Files Changed

| File | Action | What Was Done |
|------|--------|----------------|
| `app/src/main/java/com/sergiodev/bingo/domain/game/WinPrediction.kt` | Modified | Added `missingThreshold` helper, made `letter` nullable, replaced blanket Columna-only gate with per-mode threshold + Columna-only call ceiling, updated sort tiebreak |
| `app/src/main/java/com/sergiodev/bingo/ui/game/play/GamePlayScreen.kt` | Modified | `PossibleWinnersSection` branches display format on `candidate.letter` nullability |
| `app/src/test/java/com/sergiodev/bingo/domain/game/WinPredictionTest.kt` | Modified | Removed obsolete blanket-empty test; added 6 new per-mode boundary/ceiling/exclusion/null-letter tests; existing Columna tests untouched |
| `app/src/test/java/com/sergiodev/bingo/domain/game/BingoWinCheckerTest.kt` | Modified | Added L-mode and I-mode win-detection coverage (optional Phase 4, included per design recommendation) |

## Deviations from Design

None — implementation matches design exactly (per-mode `when` threshold helper, nullable `letter`, Columna-only call ceiling, UI null-check branch).

## Issues Found

None.

## Remaining Tasks

- [ ] 5.4 Manual/device smoke test of the new non-Columna render format — expected-deferred, no emulator available in this environment

## Status

17/18 tasks complete. Ready for verify (the single remaining task is an expected-deferred manual/device smoke test consistent with this project's standing convention across all 12 prior archived changes).
