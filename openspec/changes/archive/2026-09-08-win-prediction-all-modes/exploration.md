# Exploration: win-prediction-all-modes

## Current State

**Win DETECTION is already fully generic across all 5 `GameMode` values — no gap, not a bug.** `GameMode.kt` defines real coordinate-based `patterns` for every mode: `COLUMNA` (5 per-letter patterns), `O` (1 pattern, 16 cells), `L` (1 pattern, 9 cells), `I` (1 pattern, 13 cells), `CARTON_COMPLETO` (1 pattern, `FULL_CARD`, all 25 cells). `BingoWinChecker.newWins(boards, called, mode, announced)` iterates `mode.patterns` generically, calling `isSatisfied`/`matchCount` — pure, pattern-agnostic functions with zero Columna-specific branching. `GamePlayScreen` renders real wins generically (`"¡Bingo! #${sequentialNumber} ${identifier}"`), with no per-mode copy. Proof: `BingoWinCheckerTest.newWins_announcesEachNewlyCompletingBoardOncePerPattern` explicitly exercises `mode = GameMode.CARTON_COMPLETO`, and `oMode_requiresAllSixteenOuterCells` tests O-mode `isSatisfied`. Conclusion: the user's "el bingo (si no esta)" phrasing was hedging, not describing a real defect.

**Missing-count computation is already generic.** `BingoWinChecker.matchCount(board, called, pattern): Int` is pure (`pattern.cells.count { FREE || in called }`); `missing = pattern.cells.size - matchCount(...)` works identically for any pattern and already recomputes live via `GamePlayViewModel`'s reactive `combine{}` over `calledNumbers`. The "decrement on each call" mechanism already exists — it's simply never invoked for non-Columna modes today.

**The prediction gate is the ONLY Columna-specific logic, and it's one line.** `WinPrediction.kt:44`: `if (mode != GameMode.COLUMNA || called.size > MAX_PREDICTION_CALLS) return emptyList()`. This was a deliberate, documented scope lock from the prior `game-play-prediction` change (Engram #135/#205): with `MAX_PREDICTION_CALLS = 6`, the old `missing<=2` threshold was mathematically unreachable for non-Columna patterns. The user's new thresholds (missing<=3 for O/L/I, missing<=10 for Completo) are specifically reachable without any call-count ceiling — the request doesn't mention retaining one for the new modes.

**`GameMode` already flows correctly end-to-end and is already available at the prediction call site.** `GameSetupViewModel.selectedMode` defaults to `COLUMNA` but is fully user-selectable (all 5 modes offered). `GameSetupScreen.onStartGame(mode)` → `BingoNavHost` navigates `BingoRoute.gamePlay(mode.name)` (String nav arg) → `GamePlayViewModel` parses it via `GameMode.valueOf(...)` into an immutable `val mode`, used for both `rebuildSession(mode,...)` (detection, already works) and `predictPossibleWinners(mode, boards, called.toSet(), session.announced)` (prediction, already receives `mode` — just gated). No new plumbing needed; only the gate/threshold logic inside `predictPossibleWinners` must branch on `mode`.

**Display format gap — genuine design work needed.** `PredictionCandidate(boardId, identifier, letter: BingoLetter, missing)` hardcodes a single letter, derived as `pattern.cells.first().column`. For COLUMNA this is meaningful (per-column patterns); for O/L/I/Completo (single multi-letter/whole-card patterns) it would always resolve to `BingoLetter.B` — meaningless. `GamePlayScreen.PossibleWinnersSection` renders `"Cartón ${identifier} (${letter.name})"`, assuming a letter is always meaningful. Both need generalizing.

**Minor FREE-cell asymmetry** (FREE only at N row 3): `O` (16 cells, no N3) and `L` (9 cells, no N column at all) get no free bonus; `I` (13 cells, includes N3) and `CARTON_COMPLETO` (25 cells, includes N3) each get 1 free cell, needing one fewer real number than their raw cell count implies. Mirrors the already-accepted Columna N-column asymmetry from the prior change.

## Affected Areas

- `app/src/main/java/com/sergiodev/bingo/domain/game/WinPrediction.kt` — remove/generalize the Columna-only gate; add per-mode thresholds (O/L/I: missing<=3, Completo: missing<=10, Columna: existing missing<=2 + call-ceiling, unless unified); `PredictionCandidate.letter` needs generalizing.
- `app/src/main/java/com/sergiodev/bingo/ui/game/play/GamePlayScreen.kt` — `PossibleWinnersSection`'s `"Cartón <id> (<letra>)"` format needs a mode-aware variant.
- `app/src/test/java/com/sergiodev/bingo/domain/game/WinPredictionTest.kt` — `nonColumnaModes_alwaysReturnEmpty_regardlessOfProgressOrCallCount` currently asserts the OPPOSITE of the new requirement and must be rewritten, not merely extended; new per-mode boundary tests needed.
- `app/src/test/java/com/sergiodev/bingo/domain/game/BingoWinCheckerTest.kt` — no code change needed, but L and I modes have zero dedicated win-detection test cases today (pre-existing gap, low risk, adjacent to this change).
- No changes needed: `GameMode.kt`, `BingoWinChecker.kt` (detection), `GamePlayViewModel.kt`'s mode plumbing, `GameSetupViewModel.kt`/`GameSetupUiState.kt`/`BingoNavHost.kt` (mode selection/flow already fully generic).

## Approaches Considered

1. **Per-mode threshold branch inside `predictPossibleWinners`, no call-ceiling for new modes** — matches the user's literal request; only Columna keeps `MAX_PREDICTION_CALLS`. Minimal surface change, reuses generic `matchCount`/`GameMode.patterns`; `PredictionCandidate` display-field redesign still required. Effort: Low-Medium.
2. **Generic per-mode config map (`GameMode -> threshold [+ optional call ceiling]`)** — same behavior as #1, framed as data instead of if/else. More extensible if a 6th mode is ever added; slightly more upfront structure than needed today. Effort: Medium.
3. **Separate prediction function/type for the 4 new modes, keep Columna's function untouched** — avoids touching tested code, but duplicates the loop-and-filter logic and fragments the single `combine{}` call site. Effort: Medium, worse maintainability.

## Recommendation

Approach 1 (or 2). Generalize `predictPossibleWinners` with an internal per-mode threshold decision, drop the blanket Columna-only gate, and redesign `PredictionCandidate` to carry a mode-appropriate display label instead of an always-present `BingoLetter`. `GameMode` already reaches this call site — no new plumbing needed.

## Risks

- `PredictionCandidate.letter: BingoLetter` is a breaking/nullable-type change; existing Columna assertions in `WinPredictionTest.kt` must keep working alongside non-Columna candidates that have no single letter.
- The existing `nonColumnaModes_alwaysReturnEmpty...` test locks in the OPPOSITE of the new requirement — must be replaced, not extended.
- Whether Columna's `MAX_PREDICTION_CALLS = 6` should stay Columna-only, generalize, or be dropped is unresolved — user's request implies fixed thresholds alone suffice for the new modes.
- FREE-cell asymmetry changes the effective real-number count needed per mode (O/L: none, I/Completo: 1 free cell) — should be called out explicitly so it isn't later mistaken for a bug.
- L and I modes have zero dedicated `BingoWinCheckerTest` coverage today (shared/generic code path, presumably correct but untested).
- No Compose UI test infra exists — new render format for non-Columna modes will be manually verified only, per standing project convention.

## Open Questions for Proposal

1. Should O/L/I/Completo predictions have an early-game call-count ceiling analogous to Columna's `MAX_PREDICTION_CALLS = 6`, or should the fixed missing-count thresholds (3 / 10) apply with no ceiling, as the user's request literally implies?
2. Exact display label for non-Columna prediction entries, since there's no single "letra" to show (e.g. just `"Cartón <identifier>"` with no parenthetical, or a mode name instead of a letter).

## Ready for Proposal

Yes — no blockers. All investigation questions were answered with concrete code evidence.
