# Proposal: Home Navigation Redesign

## Intent

`BoardListScreen`'s home chrome is split awkwardly: a bottom `NavigationBar` with only "Agregar"/"Configuración", plus a separate full-width "Jugar" button above the list — extra visual weight that competes with the list. There is no top app bar (no app identity) and no empty-state feedback for zero boards. `GameSetupScreen`'s segmented mode picker also under-communicates the active selection. This change consolidates home navigation into one 3-slot bottom bar, adds a title-only top app bar, adds an empty state, and restyles the mode picker for clearer selected-state feedback.

## Scope

### In Scope
- 3-slot bottom bar on `BoardListScreen`: Configuración (left, inert) / Jugar (center, prominent) / Agregar (right); removes the separate full-width Jugar button.
- `CenterAlignedTopAppBar` (title = app name only) with `TopAppBarDefaults` scroll-behavior — container color shifts as the list scrolls beneath it.
- New shared `ui/common/EmptyState.kt` shown on `BoardListScreen` when there are no boards (Spanish copy, e.g. "Sin cartones agregados").
- Swap `GameSetupScreen`'s `SingleChoiceSegmentedButtonRow`/`SegmentedButton` for a `FilterChip` row; selected chip shows a check icon and a new green `Theme.kt` token as its container color.
- New semantic "selected" color token in `Theme.kt` (name/hex is design's call) — no hardcoded literals at call sites.

### Out of Scope
- Real "Configuración" destination — stays inert, no navigation, no 5th route.
- Compose UI test harness — none exists project-wide; deferred to manual/device smoke test like all 8 prior changes.
- Any change to `CreateBoardScreen`/`GamePlayScreen` business logic.
- Leading/trailing icon slots on the top app bar — title only.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `app-navigation`: supersedes board-list-navigation's "exactly 2 items" bottom-bar requirement and its separate full-width Jugar button (Engram obs #146) with one 3-slot bar; adds a title-only, scroll-linked top app bar.
- `mode-selection`: `GameSetupScreen` picker changes from `SingleChoiceSegmentedButtonRow` to `FilterChip` with a check icon and green selected token.
- `board-management`: `BoardListScreen` gains an empty-state branch via the new `EmptyState` composable.

## Approach

Preserve the screen-owns-its-chrome convention: `topBar` and the restyled `bottomBar` both live in `BoardListContent`'s existing screen-local `Scaffold`, not `MainActivity`'s. Bottom bar becomes a custom 3-slot `Row` (a docked-FAB `BottomAppBar` can't cleanly hold two side icons plus one true center action), letting Jugar render visually distinct. Top bar uses `TopAppBarDefaults.pinnedScrollBehavior()` via `Modifier.nestedScroll`; both bars keep zeroing `WindowInsets(0,0,0,0)` — scroll-linking only changes color, never inset ownership. `EmptyState` follows the existing `ui/common/` shared-composable convention (`BingoNumberField`, `BingoGridDisplay`). Mode picker becomes `FilterChip(selected, leadingIcon = Check when selected)` with `selectedContainerColor` bound to the new token.

## Affected Areas

| Area | Impact | Description |
|------|--------|--------------|
| `ui/boards/list/BoardListScreen.kt` | Modified | `topBar`; 3-slot `bottomBar`; remove full-width Jugar; empty-state branch |
| `ui/common/EmptyState.kt` | New | Shared empty-state composable |
| `ui/game/setup/GameSetupScreen.kt` | Modified | `SegmentedButton` → `FilterChip` |
| `ui/theme/Theme.kt` | Modified | Add green "selected" token |
| `MainActivity.kt` | Verify only | Outer `Scaffold`/inset convention stays compatible |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|--------------|
| Nested-scroll topBar double-applies insets with existing zeroed `Scaffold`s | Med | Scroll wiring affects color only, not padding; manual smoke test |
| `Check`/`PlayArrow` icons unconfirmed in this project's `material-icons-core` pin | Low | Both are standard core icons; verify at compile time |
| MODIFIED, not additive, change against archived spec obs #146 | Med | Explicit delta spec with REMOVED/MODIFIED requirements |
| No Compose UI test infra | Med | Manual/device smoke test, per standing convention |
| Diff may approach 400-line review budget (5 files, 1 new) | Med | Flag for `sdd-tasks` sizing; delivery strategy `ask-on-risk` |

## Rollback Plan

Revert `BoardListScreen.kt`, `GameSetupScreen.kt`, `Theme.kt`; delete `EmptyState.kt`. No schema/DI/domain/route changes to unwind — fully file-scoped revert.

## Dependencies

- None new. Reuses BOM-pinned `material-icons-core` 1.7.8 and Compose BOM `2026.02.01`.

## Success Criteria

- [ ] Bottom bar has exactly 3 items (Configuración, Jugar, Agregar); old full-width Jugar button removed.
- [ ] Title-only top app bar visible; container color changes on scroll.
- [ ] Empty board list shows `EmptyState` instead of a blank screen.
- [ ] Mode picker uses `FilterChip`s with check icon + green token on selection.
- [ ] `./gradlew build` and full unit suite pass with no regressions.
- [ ] `WindowInsets(0,0,0,0)` conventions on both `Scaffold`s remain intact.
