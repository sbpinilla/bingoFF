# Proposal: Win Prediction for All Game Modes

## Intent

The "Posibles ganadores" prediction panel only ever renders for `GameMode.COLUMNA` — `WinPrediction.kt` short-circuits with `if (mode != GameMode.COLUMNA || called.size > MAX_PREDICTION_CALLS) return emptyList()`. This was a deliberate scope lock from the prior `game-play-prediction` change: at `MAX_PREDICTION_CALLS = 6`, the old `missing<=2` threshold was unreachable for O/L/I/Cartón Completo patterns. Players using those 4 modes never see who is close to winning.

Note: win **detection** ("¡Bingo!") already works generically for all 5 modes today via `BingoWinChecker.newWins` iterating `mode.patterns` — confirmed by existing tests for `CARTON_COMPLETO` and `O`. There is no detection bug to fix; this change is scoped entirely to the prediction panel, the only Columna-gated logic in the codebase.

## Scope

### In Scope
- Generalize `predictPossibleWinners` in `WinPrediction.kt` to branch per `GameMode`:
  - Columna: unchanged — `missing<=2` AND `called.size <= MAX_PREDICTION_CALLS`.
  - O, L, I: `missing<=3`, no call-count ceiling.
  - Cartón Completo: `missing<=10`, no call-count ceiling.
- Generalize `PredictionCandidate`'s `letter: BingoLetter` field (nullable or mode-aware label) so O/L/I/Completo candidates don't carry a meaningless default `BingoLetter.B`.
- Update `GamePlayScreen.kt`'s `PossibleWinnersSection` to branch on letter presence: `"Cartón <id> (<letra>)"` for Columna, `"Cartón <id>"` for the other 4 modes.
- Rewrite `WinPredictionTest.kt`'s `nonColumnaModes_alwaysReturnEmpty_regardlessOfProgressOrCallCount` (asserts the opposite of the new behavior) with per-mode boundary tests: e.g. missing==3 shows / missing==4 hides for O/L/I; missing==10 shows / missing==11 hides for Completo; live recalculation on each call.
- Optional, low-cost addition: dedicated `BingoWinCheckerTest` win-detection cases for L and I modes, closing a pre-existing coverage gap directly adjacent to this change.

### Out of Scope
- Any change to win detection/announcement logic (`BingoWinChecker.kt`) — already generic and correct.
- Any change to `GameMode.kt`, `GameSetupViewModel.kt`/`GameSetupUiState.kt`, `BingoNavHost.kt` — mode selection and plumbing already work end-to-end.
- Compose UI test infrastructure — none exists project-wide (standing convention); new render format is manually/device verified.
- Adding an early-game call-count ceiling for O/L/I/Completo — thresholds alone gate visibility, per confirmed decision.
- FREE-cell rebalancing — the O/L (no bonus) vs. I/Completo (1 free cell at N row 3) asymmetry is expected, existing behavior, not addressed here.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `win-prediction`: extends possible-winner prediction from Columna-only to all 5 `GameMode` values, with per-mode missing-count thresholds and a mode-aware display label.

## Approach

Branch inside `predictPossibleWinners` on `mode` to pick a threshold (and, for Columna only, retain the call-count ceiling), reusing the already-generic `matchCount`/`GameMode.patterns` machinery — no new plumbing needed since `mode` already reaches this call site via `GamePlayViewModel`. Replace `PredictionCandidate.letter: BingoLetter` with a nullable/mode-aware label so the UI can format Columna vs. non-Columna entries differently.

## Affected Areas

| Area | Impact | Description |
|------|--------|--------------|
| `domain/game/WinPrediction.kt` | Modified | Per-mode threshold branching; `PredictionCandidate` label field generalized |
| `ui/game/play/GamePlayScreen.kt` | Modified | `PossibleWinnersSection` mode-aware format |
| `test/.../WinPredictionTest.kt` | Modified | Replace stale test; add per-mode boundary tests |
| `test/.../BingoWinCheckerTest.kt` | Optional | Add L/I win-detection coverage (nice-to-have) |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|--------------|
| `PredictionCandidate.letter` type change breaks existing Columna assertions | Low | Keep Columna's letter populated; only add nullability/label for other modes |
| Stale test asserts opposite of new requirement, silently re-locking old scope | Med | Explicitly replace (not extend) `nonColumnaModes_alwaysReturnEmpty...` |
| No Compose UI test infra for new render format | Low | Manual/device smoke test, per standing convention |

## Rollback Plan

Revert `WinPrediction.kt` and `GamePlayScreen.kt` to the Columna-only gate and single-letter format; restore the original `WinPredictionTest.kt` assertions. No schema/data-layer changes exist to migrate back.

## Dependencies

None.

## Success Criteria

- [ ] O/L/I show a possible-winner entry once a board's missing count is <=3, hidden at >=4, with no call-count gate.
- [ ] Cartón Completo shows an entry once missing<=10, hidden at missing>=11, with no call-count gate.
- [ ] Columna's existing behavior (missing<=2, call ceiling 6) is unchanged.
- [ ] Non-Columna entries render as `"Cartón <id>"`; Columna entries render unchanged as `"Cartón <id> (<letra>)"`.
- [ ] `./gradlew build` and full unit suite pass with no regressions.

---
Estimated diff size: ~120-180 changed lines (well under the 400-line review budget).
