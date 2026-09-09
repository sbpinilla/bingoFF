# Design: Win Prediction for All Game Modes

Proposal: Engram `sdd/win-prediction-all-modes/proposal` (#405) / `openspec/changes/win-prediction-all-modes/proposal.md`.

## Technical Approach

`predictPossibleWinners` in `WinPrediction.kt` drops the blanket `mode != GameMode.COLUMNA` gate and instead picks a per-mode missing-count threshold via a small `when` helper, keeping `MAX_PREDICTION_CALLS` scoped to Columna only. `PredictionCandidate.letter` becomes nullable so O/L/I/Cartón Completo candidates carry no fabricated `BingoLetter.B`. `GamePlayScreen.PossibleWinnersSection` branches on null to pick the display format. No plumbing changes: `mode` already reaches `predictPossibleWinners` via `GamePlayViewModel`'s existing `combine{}`, and `GameMode.kt`, `BingoWinChecker.kt`, `GameSetupViewModel.kt`/`GameSetupUiState.kt`, `BingoNavHost.kt` stay untouched, matching the proposal's scope.

## Architecture Decisions

### Decision: `PredictionCandidate.letter` becomes `BingoLetter?`

| Option | Tradeoff | Decision |
|---|---|---|
| `letter: BingoLetter?`, null for non-Columna | Smallest change; Columna call sites/tests (`it.letter == BingoLetter.B`) keep compiling unchanged; UI does one null check | **Chosen** |
| Sealed `PredictionLabel` (`Column(letter)` / `WholeCard`) | Type-safe, but adds a new type + `when` at every call/render site for a single-field distinction | Rejected — over-engineered for 1 optional field |
| Keep non-null `letter` + add `hasLetter: Boolean` | Two fields must stay in sync; still fabricates a meaningless `BingoLetter.B` default | Rejected — doesn't remove the actual code smell the proposal flags |

### Decision: Per-mode threshold via inline `when`, not a config map

| Option | Tradeoff | Decision |
|---|---|---|
| Private `when (mode)` helper in `WinPrediction.kt` | Matches this file's own style (`GameMode.patterns` is a plain `when`) and `GameSetupScreen.label()`'s convention of direct `when` over data-driven config; only 3 distinct values (2, 3, 10) | **Chosen** |
| `Map<GameMode, Int>` / `GameMode` extension property | Slightly more "data-driven" but requires touching `GameMode.kt` (forbidden by scope) or a file-local `Map` that still needs a `when`/`getValue` fallback for missing entries — no real gain over `when` at this size | Rejected |

`missingThreshold` stays a private top-level function in `WinPrediction.kt` (not a `GameMode` extension), so `GameMode.kt` is not touched at all, per the explicit constraint.

### Decision: Call-count ceiling stays Columna-only, applied as a single early guard

**Choice**: `if (mode == GameMode.COLUMNA && called.size > MAX_PREDICTION_CALLS) return emptyList()` — inverted from today's `if (mode != COLUMNA || ...)`, same short-circuit shape, now scoped correctly.
**Alternatives considered**: threading the ceiling through `missingThreshold` or a per-mode predicate object — rejected, adds a second dimension of config for a rule that only ever applies to one mode.
**Rationale**: confirmed decision #2/#3 — only Columna keeps the ceiling; O/L/I/Completo have no call-count gate.

## Data Flow

    GamePlayViewModel.combine{} (unchanged trigger)
              │ mode, boards, called, session.announced
              ▼
    predictPossibleWinners(mode, boards, called, announced)
              │ when(mode) → threshold (2/3/10); Columna also gates on called.size
              │ per board × per mode.patterns: missing = cells - matchCount
              │ letter = pattern.cells.first().column if COLUMNA else null
              ▼
    List<PredictionCandidate> ──► GamePlayUiState.possibleWinners (unchanged field)
              ▼
    GamePlayScreen.PossibleWinnersSection
              │ letter != null → "Cartón <id> (<letra>)"
              │ letter == null → "Cartón <id>"

## File Changes

| File | Action | Description |
|---|---|---|
| `domain/game/WinPrediction.kt` | Modify | Replace Columna-only gate with per-mode `missingThreshold` `when`; scope call ceiling to Columna; `letter: BingoLetter?`; sort tiebreak uses `letter?.ordinal ?: 0` |
| `ui/game/play/GamePlayScreen.kt` | Modify | `PossibleWinnersSection` branches on `candidate.letter` nullability for display format |
| `test/.../WinPredictionTest.kt` | Modify | Remove `nonColumnaModes_alwaysReturnEmpty...`; add per-mode boundary + no-ceiling + null-letter tests (see Testing Strategy) |
| `test/.../BingoWinCheckerTest.kt` | Optional | Add L/I win-detection cases (pre-existing gap, adjacent, not required for this change's success criteria) |

No changes to `GameMode.kt`, `BingoWinChecker.kt`, `GameSetupViewModel.kt`/`GameSetupUiState.kt`, `BingoNavHost.kt`, or `GamePlayUiState.kt` (its `possibleWinners: List<PredictionCandidate>` field type is unchanged — only the element's `letter` field becomes nullable).

## Interfaces / Contracts

```kotlin
// domain/game/WinPrediction.kt
data class PredictionCandidate(
    val boardId: Long,
    val identifier: String,
    val letter: BingoLetter?,   // null for O, L, I, CARTON_COMPLETO
    val missing: Int,
)

private fun missingThreshold(mode: GameMode): Int = when (mode) {
    GameMode.COLUMNA -> 2
    GameMode.O, GameMode.L, GameMode.I -> 3
    GameMode.CARTON_COMPLETO -> 10
}

fun predictPossibleWinners(
    mode: GameMode,
    boards: List<BoardCard>,
    called: Set<Int>,
    announced: Set<AnnouncedWin>,
): List<PredictionCandidate> {
    if (mode == GameMode.COLUMNA && called.size > MAX_PREDICTION_CALLS) return emptyList()
    val threshold = missingThreshold(mode)
    // ...same loop, missing > threshold continue, letter = if (mode == COLUMNA) ... else null
}
```

```kotlin
// ui/game/play/GamePlayScreen.kt — PossibleWinnersSection
possibleWinners.forEach { candidate ->
    val suffix = candidate.letter?.let { " (${it.name})" }.orEmpty()
    Text("Cartón ${candidate.identifier}$suffix")
}
```

## Testing Strategy

| Layer | What to Test | Approach |
|---|---|---|
| Unit | Remove `nonColumnaModes_alwaysReturnEmpty_regardlessOfProgressOrCallCount` (asserts the opposite of new behavior) | Delete, replace below |
| Unit | O/L/I: `missing==3` included, `missing==4` excluded | New `oLIModes_missingThreeOrLess_isIncluded` / `oLIModes_missingFour_isExcluded`, parameterized over `listOf(GameMode.O, GameMode.L, GameMode.I)` using `board1` subsets sized per pattern |
| Unit | Cartón Completo: `missing==10` included, `missing==11` excluded | New `cartonCompleto_missingTenOrLess_isIncluded` / `cartonCompleto_missingEleven_isExcluded` |
| Unit | No call-count ceiling for O/L/I/Completo even with `called.size > MAX_PREDICTION_CALLS` | New `oLIAndCompleto_noCallCountCeiling_stillIncludedAtHighCallCount` — qualifying missing count reached only after >6 distinct calls (padded with unrelated numbers) |
| Unit | Non-Columna candidate has `letter == null` | New `nonColumnaCandidate_hasNullLetter` |
| Unit | Already-announced exclusion holds for non-Columna modes too | New `nonColumnaModes_alreadyAnnouncedPair_isExcludedEvenIfStillQualifying`, mirrors existing Columna test |
| Unit | Existing Columna tests (`columna_exactlySixCalls_isIncluded`, `columna_sevenCalls_isExcluded`, `columna_belowThreshold_isExcluded`, `alreadyAnnouncedPair_isExcludedEvenIfStillQualifying`, `boardQualifyingOnTwoColumns_producesTwoEntries`) | Unchanged, must keep passing as-is |
| Manual (device) | Non-Columna panel renders `"Cartón <id>"` with no parenthetical; Columna renders unchanged | Per standing project convention — no Compose UI test infra exists |

## Threat Matrix

N/A — no routing, shell, subprocess, VCS/PR automation, executable-file classification, or process-integration boundary.

## Migration / Rollout

No migration required — no schema/persisted-data changes. Rollback: restore the `mode != GameMode.COLUMNA || called.size > MAX_PREDICTION_CALLS` gate, revert `letter` to non-nullable `BingoLetter`, revert `PossibleWinnersSection`'s single-format string, restore the original `WinPredictionTest.kt` assertions.

## Open Questions

None — all confirmed by the product decisions in the proposal; no blocking unknowns remain.
