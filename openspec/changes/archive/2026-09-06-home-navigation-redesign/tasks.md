# Tasks: Home Navigation Redesign

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~150-220 (Color.kt ~5, EmptyState.kt ~35 new, BoardListScreen.kt ~100, GameSetupScreen.kt ~55, MainActivity.kt 0) |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Low

### Suggested Work Units

| Unit | Goal | Likely PR | Focused test command | Runtime harness | Rollback boundary |
|------|------|-----------|----------------------|-----------------|-------------------|
| 1 | Theme token + `EmptyState` composable | PR 1 | `./gradlew compileDebugKotlin` | N/A — no Compose UI test infra | Revert `Color.kt` additions; delete `EmptyState.kt` |
| 2 | Top bar + bottom bar wiring in `BoardListScreen.kt` | PR 1 | `./gradlew compileDebugKotlin` + `rg` structural checks | Manual/device smoke test (deferred, no adb/emulator) | Revert `BoardListScreen.kt` to prior `NavigationBar` + full-width button |
| 3 | `FilterChip` mode row in `GameSetupScreen.kt` | PR 1 | `./gradlew compileDebugKotlin` + `rg` for no `SegmentedButton` | Manual/device smoke test (deferred) | Revert `GameSetupScreen.kt` to `SingleChoiceSegmentedButtonRow` |
| 4 | Build gate + `MainActivity.kt` verification | PR 1 | `./gradlew clean build` | N/A — verification only | N/A — no code change to roll back |

## Phase 1: Theme Token

- [x] 1.1 Add `val Success = Color(0xFF2E7D32)` and `val OnSuccess = Color(0xFFFFFFFF)` to `app/src/main/java/com/sergiodev/bingo/ui/theme/Color.kt`. (Req: Mode Selection Control)

## Phase 2: Empty State Composable

- [x] 2.1 Create `app/src/main/java/com/sergiodev/bingo/ui/common/EmptyState.kt` with `EmptyState(message: String, modifier: Modifier = Modifier)` — centered `Column`, neutral icon (`Icons.Default.Info`) + `Text`. (Req: Empty Board List State)

## Phase 3: Home Top App Bar

- [x] 3.1 In `app/src/main/java/com/sergiodev/bingo/ui/boards/list/BoardListScreen.kt`, create `scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()` and attach `Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)` to the screen-local `Scaffold`. (Req: Home Top App Bar)
- [x] 3.2 Add `topBar = { CenterAlignedTopAppBar(title = { Text(appName) }, windowInsets = WindowInsets(0,0,0,0), scrollBehavior = scrollBehavior) }` — title only, no navigation/action icons. (Req: Home Top App Bar)

## Phase 4: Home Bottom Navigation Bar

- [x] 4.1 In `BoardListScreen.kt`, remove the existing `NavigationBar`/`NavigationBarItem` bottom bar and the full-width "Jugar" `Button` above the list. (Req: Home Screen Bottom Navigation Bar; Full-Width Play Action removed)
- [x] 4.2 Add a custom 3-slot `Row` in `Surface(tonalElevation = NavigationBarDefaults.Elevation)` as `bottomBar`: `IconButton` "Configuración" (`Icons.Default.Settings`, no-op `onClick`, `contentDescription = "Configuración"`); `FilledIconButton` "Jugar" (`Icons.Default.PlayArrow`, `onClick = onStartGame`, `containerColor = MaterialTheme.colorScheme.primary`, `contentDescription = "Jugar"`); `IconButton` "Agregar" (`Icons.Default.Add`, `onClick = onCreateBoard`, `contentDescription = "Agregar"`). (Req: Home Screen Bottom Navigation Bar)
- [x] 4.3 Branch `content(innerPadding)` on `state.boards.isEmpty()`: render `EmptyState("No hay cartones agregados")` when empty, else the existing `LazyColumn` unchanged. (Req: Empty Board List State; Board List Content Rendering)

## Phase 5: Mode Selection FilterChip Row

- [x] 5.1 In `app/src/main/java/com/sergiodev/bingo/ui/game/setup/GameSetupScreen.kt`, replace `SingleChoiceSegmentedButtonRow`/`SegmentedButton` with a `Row` of `FilterChip`s, one per `state.availableModes` entry, using short readable Spanish labels. (Req: Mode Selection Control)
- [x] 5.2 On the selected chip, set `leadingIcon = { Icon(Icons.Default.Check, contentDescription = null) }` and `colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Success, selectedLabelColor = OnSuccess, selectedLeadingIconColor = OnSuccess)`; keep `onModeSelected` and "Iniciar" → `GamePlayScreen` navigation unchanged. (Req: Mode Selection Control)

## Phase 6: Verification

- [x] 6.1 Read `app/src/main/java/com/sergiodev/bingo/MainActivity.kt` (read-only) to confirm the outer `Scaffold`'s default `contentWindowInsets` + `Modifier.padding(innerPadding)` still compose correctly around the new screen-local `topBar`/`bottomBar`; no edit expected.
- [x] 6.2 Run `./gradlew clean build` to confirm compile success, including `Check`/`PlayArrow` icon resolution and the stable (non-experimental) `FilterChip`/`CenterAlignedTopAppBar` APIs.
- [x] 6.3 Run `rg` structural checks: bottom bar has exactly 3 clickable slots; no `SegmentedButton` literal remains in `GameSetupScreen.kt`; `EmptyState(` is referenced from `BoardListScreen.kt`.
- [ ] 6.4 Manual/device smoke test (expected-deferred per project convention — no adb/emulator in the apply/verify environment): confirm no inset gap above/below either bar, "Jugar" is reachable and visually primary, empty vs non-empty list rendering, selected chip shows check + green, top bar color shifts on scroll and resets at top.
