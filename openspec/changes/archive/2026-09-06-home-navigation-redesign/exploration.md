# Exploration: home-navigation-redesign

## Current State

**Top app bar** — None exists. `MainActivity.kt` wraps `BingoNavHost` in one app-wide `Scaffold` (no `topBar`). `BoardListScreen.kt`'s `BoardListContent` has its own screen-local `Scaffold` (added in archived change `board-list-navigation`) with only a `bottomBar`. No `TopAppBarDefaults` scroll-behavior usage anywhere in the codebase yet.

**Bottom bar** — `BoardListContent.Scaffold.bottomBar` is a Material3 `NavigationBar` with exactly 2 items: "Agregar" (Add → CreateBoard) then "Configuración" (Settings → inert), both always `selected = false`, both zeroing `WindowInsets(0,0,0,0)` (deliberate convention vs. the outer `MainActivity` Scaffold). "Jugar" is a separate full-width `Button` above the `LazyColumn`, not in the bottom bar. This exact shape is an already-archived, spec-locked requirement (`sdd/board-list-navigation/spec`, Engram obs #146: "exactly 2 items" + a distinct full-width Jugar button). Moving Play into the bottom bar is therefore a MODIFIED requirement, not additive.

**Empty state** — Does not exist anywhere (grep for "sin cartones|no hay|EmptyState|placeholder" across `app/src/main` → zero matches). The only loose precedent is `GameSetupContent`'s inline `if (!state.hasBoards) Text(...)` guard.

**Game-setup mode picker** — `GameSetupScreen.kt`/`GameSetupContent` uses `SingleChoiceSegmentedButtonRow` + 5 `SegmentedButton`s (one per `GameMode`), added in archived change `game-play-prediction`. Confirm button is `Button(onClick = onStartGame, enabled = state.canStart)` where `canStart = hasBoards && selectedMode != null`.

**Theming** — Stock `MaterialTheme` (`Theme.kt`) with Purple/PurpleGrey/Pink light/dark schemes + dynamic color on API 31+. No green/success token exists. Only icon dependency is `material-icons-core` 1.7.8 (BOM-pinned); `Add`/`Settings` confirmed used, `PlayArrow`/`Check` not yet used anywhere.

**Testing convention** — No Compose UI test infrastructure exists. All 8 prior archived changes deferred manual/device smoke tests identically — expect the same for this change.

## Affected Areas

- `app/src/main/java/com/sergiodev/bingo/ui/boards/list/BoardListScreen.kt` — add `topBar`; restyle `bottomBar` to a 3-slot layout; add empty-state branch.
- `app/src/main/java/com/sergiodev/bingo/MainActivity.kt` — outer `Scaffold`/inset-zeroing convention must stay consistent with a new nested-scroll-connected topBar.
- `app/src/main/java/com/sergiodev/bingo/ui/game/setup/GameSetupScreen.kt` — swap `SegmentedButton` row for a checkmark-style selectable (e.g. `FilterChip`).
- `app/src/main/java/com/sergiodev/bingo/ui/theme/Theme.kt` — no green token exists; decision needed.
- Potential new `app/src/main/java/com/sergiodev/bingo/ui/common/EmptyState.kt` — no existing shared empty-state composable; `ui/common/` is the established location for cross-screen composables.

## Approaches Considered

1. **Top app bar** — screen-local `topBar` in `BoardListContent`'s existing `Scaffold` with `TopAppBarDefaults.pinnedScrollBehavior()` + `scrolledContainerColor` (recommended: matches existing screen-owns-its-chrome precedent) vs. an app-level topBar in `MainActivity` (breaks precedent, needs route-aware logic).
2. **Bottom bar restyle** — a real M3 `BottomAppBar` + docked/cutout FAB (the docked-FAB slot model doesn't cleanly support 2 opposite icons + a true center FAB) vs. a custom `Row`-based 3-slot bar (Settings left, Play center, Add right) — recommended, pending explicit user confirmation of the reference screenshot layout since the explorer worked from a text description only.
3. **Empty state** — inline conditional branch in `BoardListContent` vs. a new shared `ui/common/EmptyState.kt` (recommended: matches the project's established shared-composable convention, reusable by `GameSetupScreen` too).
4. **Mode picker** — a `FilterChip` row with `leadingIcon = Check` when selected + `selectedContainerColor` tinted green (recommended: native M3 selectable component, minimal 1:1 swap) vs. a custom `Card`/`OutlinedButton` with manual check overlay (reimplements selection semantics for no functional gain).

## Risks

- Bottom-bar exact layout is unconfirmed (text description only, not the actual reference image) — must be confirmed before locking design; it changes an already-archived, verified spec (obs #146) as a MODIFIED block, not an addition.
- No Compose UI test infra exists; expect the same manual-device-smoke-test deferral pattern as all 8 prior changes.
- No green/success theme token exists yet — hardcoded color vs. a new `Theme.kt` token is an open decision.
- `Icons.Default.PlayArrow`/`Icons.Default.Check` from `material-icons-core` are not yet confirmed in use anywhere in this codebase (expected present in the core icon set, unverified here).
- Two nested `Scaffold`s already exist with careful mutual inset-zeroing; adding a `nestedScroll`-connected topBar increases coupling that must preserve that convention.

## Open Questions for Proposal

1. Exact bottom-bar visual style (spacing, FAB-style raised center button vs. flat icon, icon set).
2. Whether "Configuración" gains a real destination screen or stays inert for now.
3. Green "selected" color: literal hardcoded color vs. a new semantic token added to `Theme.kt`.
4. Top-bar scope: title only, or leading/trailing icon slots too.

## Ready for Proposal

Yes, pending user answers to the open questions above.
