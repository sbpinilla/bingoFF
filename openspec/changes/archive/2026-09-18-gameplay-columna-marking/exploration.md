# Exploration: gameplay-columna-marking

## Current State

`GamePlayScreen.kt` is one shared composable for all 5 `GameMode` values (COLUMNA, O, L, I, CARTON_COMPLETO) — there is no separate "Columna screen." Layout top-to-bottom: `TopAppBar` (title + back `navigationIcon` only — zero uses of `actions =` on any `TopAppBar` app-wide, confirmed by grep), `PossibleWinnersSection`, mode/call-count text, `winners.forEach { Card { "¡Bingo! #N identifier" } }`, number input + letter `FilterChip` row + submit button, `HorizontalDivider`, then the per-letter calls section (`BingoLetter.entries.forEach { Text("B: 1,2,3,4") }`, plain text, rendered for every mode), then `EndGameAction` (full-width red `Button` + `AlertDialog` confirm).

Automatic win detection is fully generic and has zero manual-override path today. `BingoWinChecker.newWins(boards, called, mode, announced)` iterates every `BoardCard` × `mode.patterns`, using pure `isSatisfied`/`matchCount`. Emits `WinAnnouncement` only for pairs not already in `announced: Set<AnnouncedWin>`. `GamePlayViewModel.rebuildSession` replays the full call history on every state emission to rebuild `announced`/`winners` deterministically. No manual "force a win" or "mark as resolved" mechanism exists anywhere.

Only `GameMode.COLUMNA` has per-letter patterns: COLUMNA → 5 independent `WinPattern`s, one per `BingoLetter`. O/L/I/CARTON_COMPLETO each have exactly ONE pattern spanning multiple letters. A per-letter manual marker is structurally meaningful only under COLUMNA — for any other mode a single letter never equals a complete pattern.

`callsByLetter` is pure derived UI data, no marking state exists. `predictPossibleWinners` (`WinPrediction.kt`) is a separate, independent near-win feed with its own per-mode thresholds — also has no manual input.

`BingoGridDisplay.kt` renders one `BoardCard`'s FIXED 5×5 grid (`colorScheme.outline` borders + dividers, bold centered letter headers). Not directly reusable for the per-letter calls section: each letter can accumulate 0–15 called numbers (variable length), not a fixed 5-cell column. Only the visual convention (bordered/divided Material3 grid) transfers, not the composable itself.

No swipe-to-reveal pattern exists anywhere — grep for `SwipeToDismiss`/`AnchoredDraggable`/`swipeable` returns zero matches. The Compose BOM already pulls material3, so `SwipeToDismissBox` needs no new dependency, but it is a brand-new interaction pattern with zero precedent or test coverage in this codebase.

Testing convention confirmed: JVM-only unit tests with hand-written fakes. No Compose UI test infra in active use, consistent with all prior archived changes.

Localization: parallel `values/strings.xml` (Spanish default) and `values-en/strings.xml` both already exist; any new strings need both locales.

No other navigation surface exists on `GamePlayScreen` — no bottom bar, no drawer, just `Scaffold(topBar = ...)`. A `TopAppBar` action icon is the only candidate for "item de la navegación."

## Affected Areas

- `app/src/main/java/com/sergiodev/bingo/ui/game/play/GamePlayScreen.kt` — per-letter calls section (grid restructure + mark UI), `EndGameAction` relocation into `TopAppBar` `actions`.
- `app/src/main/java/com/sergiodev/bingo/ui/game/play/GamePlayViewModel.kt` — wiring for any new mark action/state.
- `app/src/main/java/com/sergiodev/bingo/ui/game/play/GamePlayUiState.kt` — new field only if marking is session-local UI state.
- `app/src/main/java/com/sergiodev/bingo/domain/game/BingoWinChecker.kt`, `WinAnnouncement.kt`, `AnnouncedWin.kt` — affected ONLY if manual marking must force/interact with automatic detection.
- `app/src/main/res/values/strings.xml` + `values-en/strings.xml` — new strings, both locales.
- `app/src/test/java/com/sergiodev/bingo/ui/game/play/GamePlayViewModelTest.kt` — new coverage for any ViewModel-level marking logic.
- Not affected: `BingoGridDisplay.kt` (pattern reuse only), `GameMode.kt`, domain win logic (if light interpretation chosen).

## Approaches Considered

**Requirement 1 (manual "con ganador" marking) — central ambiguity:**
1. **Light: pure UI/session-local marker** — swipe-revealed toggle on a per-letter flag in `GamePlayUiState`, no `WinAnnouncement`/`AnnouncedWin`/`BingoWinChecker` involvement, no board picker. Matches literal wording (marks the row, not a specific board+pattern). Small, testable, naturally Columna-scoped; but no linkage to a later automatic detection for the same column. Effort: Low.
2. **Heavy: force-creates a real `WinAnnouncement`** for a specific board+column, feeding `announced`/`winners`. Requires a board-selection UI never mentioned by the user. Single source of truth, but much larger, unrequested surface. Effort: High.

**Requirement 2 (grid presentation):**
1. Mirror `BingoGridDisplay`'s bordered/divider Material3 convention in a new composable suited to variable-length lists. Effort: Low-Medium.
2. `LazyVerticalGrid`/chip-based per-number grid — bigger visual departure. Effort: Medium.

**Requirement 3 (End Game as nav item):**
1. `TopAppBar` action icon reusing the existing `AlertDialog` confirmation unchanged — only viable navigation surface on this screen; first `actions` usage app-wide but a standard Material3 pattern. Effort: Low.

## Recommendation

Requirement 3 is unambiguous (TopAppBar action icon, same dialog). Requirement 2 should mirror `BingoGridDisplay`'s visual language, not its code. Requirement 1 needs explicit user resolution before design — the two interpretations diverge by an order of magnitude in effort and touch domain code very differently.

## Risks

- Requirement 1 is fundamentally underspecified (light visual flag vs. heavy domain-forcing override with an unmentioned board picker).
- If light interpretation ships, manual marks and automatic `BingoWinChecker` detection can visually coexist with no linkage — intentional, not a bug, but should be documented as such.
- Swipe-to-reveal is new interaction surface with zero test coverage precedent (no Compose UI test infra in this project).
- "pantalla de juego de columnas" could misleadingly suggest a distinct screen; `GamePlayScreen` is shared across all 5 modes — scoping via `state.mode == GameMode.COLUMNA` is the almost-certain intended read, reinforced by COLUMNA being the only mode with per-letter patterns, but should be confirmed.
- New strings must land in both `values/strings.xml` and `values-en/strings.xml`.

## Open Questions for Proposal

1. Does "con ganador" marking need to interact with `BingoWinChecker`/`WinAnnouncement`/`AnnouncedWin` (force/record a win for a chosen board), or is it a lightweight, board-agnostic visual marker only?
2. Is this scoped exclusively to `GameMode.COLUMNA` (recommended), with no effect on O/L/I/Completo?
3. Exact swipe interaction: does the swipe itself confirm, or does it reveal a secondary tap target? What resets/un-marks a column?
4. Is a wireframe needed for "más cuadricular," or does mirroring `BingoGridDisplay`'s outline-border/divider convention suffice as guidance?
5. Confirm TopAppBar action icon is acceptable for "item de la navegación" (first such usage in the app) — no other navigation surface exists on this screen.

## Ready for Proposal

Partial — requirements 2 and 3 are proposal-ready. Requirement 1 needs one clarifying round-trip with the user before scoping design, since its two candidate interpretations differ by an order of magnitude in effort.
