# Design: Home Navigation Redesign

Proposal: Engram `sdd/home-navigation-redesign/proposal` (#302) / `openspec/changes/home-navigation-redesign/proposal.md`.

## Technical Approach

All new chrome stays screen-local inside `BoardListContent`'s existing `Scaffold`, per the established convention from `sdd/board-list-navigation/design` (#164) — `MainActivity`'s outer `Scaffold` (verified: default `contentWindowInsets`, `Modifier.padding(innerPadding)` around `BingoNavHost`) is untouched. Both new bars must zero insets the same way the prior `NavigationBar` did, because the outer `Scaffold` already reserves system-bar space; failing to zero either bar reintroduces the double-inset bug #164 fixed. The bottom bar becomes a hand-built 3-slot `Row` (not `NavigationBar`/`BottomAppBar`) because neither offers an asymmetric icon-icon-prominent-action layout. `GameSetupScreen` swaps `SingleChoiceSegmentedButtonRow` for a `FilterChip` row bound to a new fixed-hex green token added to `ui/theme/Color.kt`.

## Verified Facts

| Claim | Verified against | Result |
|---|---|---|
| `BoardListContent` signature/state | `BoardListScreen.kt:48-93` (read 2026-09-06) | `(state: BoardListUiState, onCreateBoard, onStartGame, modifier)` — no change needed |
| Outer `Scaffold` insets | `MainActivity.kt:22-23` | Default insets consumed via `padding(innerPadding)`, not `consumeWindowInsets` — confirms zeroing is still required on both new bars |
| `material-icons-core` availability | obs #164 decision | Already a dependency (BOM-pinned 1.7.8) from the prior change — `Check`/`PlayArrow` need no new Gradle line |
| Color literal location | `ui/theme/Color.kt` vs `Theme.kt` | Corrects the proposal's affected-file row: literals live in `Color.kt` (`Purple80` pattern); `Theme.kt` itself (scheme wiring) needs **no edit** |
| `GameSetupContent` state | `GameSetupScreen.kt:40-44` | `GameSetupUiState.selectedMode: GameMode?`, `availableModes: List<GameMode>` — reused as-is |

## Architecture Decisions

### Decision: Top app bar — `CenterAlignedTopAppBar` with zeroed, pinned scroll behavior

| Option | Tradeoff | Decision |
|---|---|---|
| `TopAppBarDefaults.pinnedScrollBehavior()` | Bar never hides, only container color shifts on scroll | **Chosen** — matches proposal's "color shifts as list scrolls beneath it" |
| `enterAlwaysScrollBehavior()` | Bar hides/reveals on scroll — extra motion not requested | Rejected |
| Default `windowInsets` (`TopAppBarDefaults.windowInsets`) | Re-applies top status-bar inset on top of the outer `Scaffold`'s, producing a gap identical to #164's bottom-bar bug | Rejected |
| `windowInsets = WindowInsets(0,0,0,0)` | No inset gap; scroll wiring only ever changes color, never padding | **Chosen** |

`Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)` goes on the screen-local `Scaffold`, not on the `LazyColumn` directly, so the connection intercepts scroll before the list consumes it.

### Decision: Bottom bar — custom 3-slot `Row` in a `Surface`, not `NavigationBar`

| Option | Tradeoff | Decision |
|---|---|---|
| `NavigationBar` + 3 `NavigationBarItem`s | Forces all 3 items to equal visual weight; can't make Jugar visually primary | Rejected |
| Docked-FAB `BottomAppBar` | `BottomAppBar`'s FAB slot is single-purpose; can't also hold 2 side icons cleanly | Rejected (per proposal) |
| Custom `Row` in `Surface(tonalElevation = NavigationBarDefaults.Elevation)`, height 80.dp | Must hand-roll spacing/elevation; no built-in inset zeroing needed since plain `Surface` applies none by default | **Chosen** |

Center slot: `FilledIconButton` (large size, `IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)`, `Icons.Default.PlayArrow`) — visually distinct via size/color without the collision risk of a true floating `FloatingActionButton` docked over a custom bar. Side slots: plain `IconButton`s (Configuración → `Icons.Default.Settings`, no-op `onClick`; Agregar → `Icons.Default.Add`, `onClick = onCreateBoard`).

### Decision: Icon-only buttons now require non-null `contentDescription`

Obs #164 set `contentDescription = null` on `NavigationBarItem` icons because the visible `Text` label carried the accessible name. The new 3-slot `Row` renders **no visible text labels** (icon-only, unlike the old bar) — so this change must set explicit `contentDescription` ("Configuración", "Jugar", "Agregar") on every icon, reversing the prior decision's rationale for this file only.

### Decision: Empty state as a shared `ui/common` composable

`EmptyState(message: String, modifier: Modifier = Modifier)` — icon (`Icons.Default.Info` or similar neutral core icon) + `Text`, centered `Column`, following the parameter-driven, no-ViewModel-dependency pattern already used by `BingoGridDisplay`/`BingoNumberField`. Rejected: inlining the empty-state UI directly in `BoardListContent` — breaks the existing shared-composable convention and isn't reusable if another list ever needs it. Copy: `"No hay cartones agregados"`.

### Decision: Green selected-chip token — fixed hex pair, not colorScheme-derived

`Color.kt` gains `val Success = Color(0xFF2E7D32)` (Material green 800) and `val OnSuccess = Color(0xFFFFFFFF)`, applied via `FilterChipDefaults.filterChipColors(selectedContainerColor = Success, selectedLabelColor = OnSuccess, selectedLeadingIconColor = OnSuccess)`. Rejected: adding `success`/`onSuccess` as roles on `ColorScheme` (M3 has no such role; would require a custom `CompositionLocal` wrapper, out of scope). Because the pair is fixed rather than scheme-derived, the same two hexes are used in both light and dark theme — `0xFF2E7D32` against white content maintains ≥4.5:1 contrast (WCAG AA) regardless of the surrounding scheme, since the chip owns its own isolated background/foreground pair.

## Data Flow

    MainActivity Scaffold (unchanged, consumes systemBars → padding)
     └─→ BingoNavHost (unchanged)
          └─→ BoardListScreen (VM state, unchanged)
               └─→ BoardListContent
                    ├─ Scaffold(contentWindowInsets = 0, nestedScroll)
                    │   ├─ topBar: CenterAlignedTopAppBar(windowInsets = 0, pinnedScrollBehavior)
                    │   ├─ bottomBar: Surface { Row }
                    │   │    ├─ IconButton "Configuración" → {} (no-op)
                    │   │    ├─ FilledIconButton "Jugar"    → onStartGame() → GAME_SETUP
                    │   │    └─ IconButton "Agregar"        → onCreateBoard() → CREATE_BOARD
                    │   └─ content(innerPadding)
                    │        └─ if (state.boards.isEmpty()) EmptyState(...) else LazyColumn(...)

    GameSetupContent
     └─ FilterChip row (one per GameMode) → onModeSelected(mode) [ViewModel, unchanged]
          selected chip: leadingIcon = Check, colors = Success/OnSuccess

## File Changes

| File | Action | Description |
|---|---|---|
| `app/src/main/java/com/sergiodev/bingo/ui/boards/list/BoardListScreen.kt` | Modify | Add `topBar` + `pinnedScrollBehavior`; replace `NavigationBar` with 3-slot `Row`/`Surface`; remove full-width Jugar `Button`; branch to `EmptyState` when `state.boards` is empty |
| `app/src/main/java/com/sergiodev/bingo/ui/common/EmptyState.kt` | Create | Shared empty-state composable (icon + message) |
| `app/src/main/java/com/sergiodev/bingo/ui/game/setup/GameSetupScreen.kt` | Modify | `SingleChoiceSegmentedButtonRow`/`SegmentedButton` → `FilterChip` row with check icon + `Success`/`OnSuccess` colors |
| `app/src/main/java/com/sergiodev/bingo/ui/theme/Color.kt` | Modify | Add `Success`, `OnSuccess` constants (corrects proposal's `Theme.kt` reference) |
| `app/src/main/java/com/sergiodev/bingo/MainActivity.kt` | Verify only | Confirm outer `Scaffold`/inset convention stays compatible — no edit expected |

## Interfaces / Contracts

No `ViewModel`/`UiState` signature changes. `BoardListContent(state, onCreateBoard, onStartGame, modifier)` and `GameSetupContent(state, onModeSelected, onStartGame, modifier)` keep their existing parameters — this is a presentation-only change.

```kotlin
@Composable
fun EmptyState(message: String, modifier: Modifier = Modifier) { /* icon + Text, centered */ }
```

## Testing Strategy

| Layer | What to Test | Approach |
|---|---|---|
| Unit | No ViewModel/domain logic changes | Existing suite runs as regression only |
| Build gate | Compiles with `Check`/`PlayArrow` from pinned `material-icons-core` 1.7.8; `FilterChip`/`CenterAlignedTopAppBar` are stable (non-experimental) APIs | `./gradlew clean build` |
| Structural | Bottom bar has exactly 3 clickable slots; no `SegmentedButton` literal remains; `EmptyState` referenced from `BoardListContent` | `rg` checks |
| Manual (device) | (1) no inset gap above/below either bar; (2) Jugar reachable/tappable and visually primary; (3) empty board list shows message, non-empty shows list; (4) selected mode chip shows check + green; (5) top bar color shifts on scroll | Per standing project convention — no Compose UI test infra exists |

## Threat Matrix

N/A — no routing, shell, subprocess, VCS/PR automation, executable-file classification, or process-integration boundary. Route set stays at 4; no new destination.

## Migration / Rollout

No migration. Revert `BoardListScreen.kt`, `GameSetupScreen.kt`, `Color.kt`; delete `EmptyState.kt`. No schema/DI/domain/route changes to unwind.

## Open Questions

- [ ] Confirm `Icons.Default.Check` and `Icons.Default.PlayArrow` resolve from `material-icons-core` 1.7.8 at compile time (both are standard core-set icons; low risk per proposal).
- [ ] Confirm exact empty-state icon choice (e.g. `Icons.Default.Info` vs a bingo-specific glyph) — not architecturally significant, deferred to implementation.
