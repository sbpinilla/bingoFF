# Tasks: Win Prediction for All Game Modes

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~160–220 (domain ~25, UI ~10, tests ~125–185 incl. optional) |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Single commit — no PR/review workflow exists in this project |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending (not applicable — risk is Low, no chaining decision required) |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Low

Note: this project has no PR/review workflow — all work lands as direct commit(s) to `master`. Chaining has no practical meaning here regardless of size. Estimated size is comfortably under the 400-line budget, so no `size:exception` framing is required either; the whole change ships as a single commit.

### Suggested Work Units

| Unit | Goal | Likely PR | Focused test command | Runtime harness | Rollback boundary |
|------|------|-----------|----------------------|-----------------|-------------------|
| 1 | Per-mode prediction rule + UI format + tests | Single commit (no PR workflow) | `./gradlew testDebugUnitTest --tests "*.WinPredictionTest"` | N/A — manual device smoke deferred (no Compose UI test infra, no emulator in this environment, per standing project convention) | Revert the single commit touching `WinPrediction.kt`, `GamePlayScreen.kt`, `WinPredictionTest.kt` (and optionally `BingoWinCheckerTest.kt`); no persisted-data or migration coupling |

## Phase 1: Domain — Per-Mode Prediction Rule

- [x] 1.1 Add private `missingThreshold(mode: GameMode): Int` `when`-helper (COLUMNA=2, O/L/I=3, CARTON_COMPLETO=10) to `app/src/main/java/com/sergiodev/bingo/domain/game/WinPrediction.kt`
- [x] 1.2 Change `PredictionCandidate.letter` from `BingoLetter` to `BingoLetter?` in `app/src/main/java/com/sergiodev/bingo/domain/game/WinPrediction.kt`
- [x] 1.3 Replace the `mode != GameMode.COLUMNA || called.size > MAX_PREDICTION_CALLS` blanket gate in `predictPossibleWinners` with `if (mode == GameMode.COLUMNA && called.size > MAX_PREDICTION_CALLS) return emptyList()` plus `missingThreshold(mode)`-based filtering, in `app/src/main/java/com/sergiodev/bingo/domain/game/WinPrediction.kt`
- [x] 1.4 Set `letter = if (mode == GameMode.COLUMNA) pattern.cells.first().column else null` per candidate and update the sort tiebreak to `letter?.ordinal ?: 0`, in `app/src/main/java/com/sergiodev/bingo/domain/game/WinPrediction.kt`

## Phase 2: UI — Render Format Branch

- [x] 2.1 In `PossibleWinnersSection`, branch entry text on `candidate.letter` nullability — `"Cartón <id> (<letra>)"` when non-null, `"Cartón <id>"` when null — in `app/src/main/java/com/sergiodev/bingo/ui/game/play/GamePlayScreen.kt`

## Phase 3: Tests — WinPredictionTest.kt

- [x] 3.1 Remove `nonColumnaModes_alwaysReturnEmpty_regardlessOfProgressOrCallCount` from `app/src/test/java/com/sergiodev/bingo/domain/game/WinPredictionTest.kt` (asserts now-obsolete behavior)
- [x] 3.2 Add `oLIModes_missingThreeOrLess_isIncluded` and `oLIModes_missingFour_isExcluded`, parameterized over O/L/I, to `app/src/test/java/com/sergiodev/bingo/domain/game/WinPredictionTest.kt` (spec: missing==3 shows, missing==4 doesn't)
- [x] 3.3 Add `cartonCompleto_missingTenOrLess_isIncluded` and `cartonCompleto_missingEleven_isExcluded` to `app/src/test/java/com/sergiodev/bingo/domain/game/WinPredictionTest.kt` (spec: missing==10 shows, missing==11 doesn't)
- [x] 3.4 Add `oLIAndCompleto_noCallCountCeiling_stillIncludedAtHighCallCount` to `app/src/test/java/com/sergiodev/bingo/domain/game/WinPredictionTest.kt`, padding calls past `MAX_PREDICTION_CALLS` before the qualifying threshold is reached
- [x] 3.5 Add `nonColumnaModes_alreadyAnnouncedPair_isExcludedEvenIfStillQualifying` to `app/src/test/java/com/sergiodev/bingo/domain/game/WinPredictionTest.kt`, mirroring the existing Columna exclusion test
- [x] 3.6 Add `nonColumnaCandidate_hasNullLetter` to `app/src/test/java/com/sergiodev/bingo/domain/game/WinPredictionTest.kt`, asserting `letter == null` for O/L/I/Completo candidates
- [x] 3.7 Confirm existing Columna tests (`columna_exactlySixCalls_isIncluded`, `columna_sevenCalls_isExcluded`, `columna_belowThreshold_isExcluded`, `alreadyAnnouncedPair_isExcludedEvenIfStillQualifying`, `boardQualifyingOnTwoColumns_producesTwoEntries`) remain unchanged and pass

## Phase 4: Optional — Win Detection Coverage (low priority, not required for this change's success criteria)

- [x] 4.1 (Optional) Add L-mode win-detection test cases to `app/src/test/java/com/sergiodev/bingo/domain/game/BingoWinCheckerTest.kt` (pre-existing coverage gap)
- [x] 4.2 (Optional) Add I-mode win-detection test cases to `app/src/test/java/com/sergiodev/bingo/domain/game/BingoWinCheckerTest.kt` (pre-existing coverage gap)

## Phase 5: Verification

- [x] 5.1 Run `./gradlew testDebugUnitTest` and confirm all `WinPredictionTest` cases pass, including the new per-mode boundaries
- [x] 5.2 Run `./gradlew build` as the full build gate
- [x] 5.3 Run `rg "mode != GameMode.COLUMNA"` against `app/src/main/java/com/sergiodev/bingo/domain/game/WinPrediction.kt` (read-only check) to confirm no remaining reference to the old blanket Columna-only gate
- [ ] 5.4 Manual/device smoke test of the new non-Columna render format — expected-deferred per this project's standing convention (no Compose UI test infra, no emulator in this environment)
