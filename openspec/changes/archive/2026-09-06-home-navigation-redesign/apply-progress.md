# Apply Progress: Home Navigation Redesign

**Mode**: Standard (no strict TDD; JVM unit test suite kept green as build gate)
**Status**: 12/13 tasks complete. Task 6.4 (manual/device smoke test) is expected-deferred — no adb/emulator in this apply environment, consistent with all 8 prior archived changes for this project.

## Completed Tasks

- [x] 1.1 Added `Success`/`OnSuccess` color tokens to `ui/theme/Color.kt`
- [x] 2.1 Created `ui/common/EmptyState.kt`
- [x] 3.1 Added `pinnedScrollBehavior` + `nestedScroll` to `BoardListContent`'s Scaffold
- [x] 3.2 Added `CenterAlignedTopAppBar` topBar (title-only, zeroed insets, scroll behavior)
- [x] 4.1 Removed old `NavigationBar`/`NavigationBarItem` bottom bar and full-width "Jugar" `Button`
- [x] 4.2 Added custom 3-slot `Row` (`IconButton` Configuración, `FilledIconButton` Jugar, `IconButton` Agregar) in `Surface`
- [x] 4.3 Branched content on `state.boards.isEmpty()` → `EmptyState` vs `LazyColumn`
- [x] 5.1 Replaced `SingleChoiceSegmentedButtonRow`/`SegmentedButton` with `Row` of `FilterChip`s in `GameSetupScreen.kt`
- [x] 5.2 Selected chip shows `Check` leading icon + `Success`/`OnSuccess` colors; `onModeSelected`/"Iniciar" navigation unchanged
- [x] 6.1 Verified `MainActivity.kt` outer `Scaffold` (default `contentWindowInsets` + `Modifier.padding(innerPadding)`) is unchanged and compatible — read-only, no edit made
- [x] 6.2 `./gradlew clean build` — BUILD SUCCESSFUL (109 actionable tasks, includes `testDebugUnitTest`, `lint`, `assembleDebug`, `assembleRelease`)
- [x] 6.3 `rg` structural checks: 3 `onClick` slots in `BoardListScreen.kt` bottom bar; zero `SegmentedButton` occurrences in `GameSetupScreen.kt`; `EmptyState(` referenced from `BoardListScreen.kt`
- [ ] 6.4 Manual/device smoke test — deferred (no adb/emulator available)

## Files Changed

| File | Action | What Was Done |
|------|--------|---------------|
| `app/src/main/java/com/sergiodev/bingo/ui/theme/Color.kt` | Modified | Added `Success = Color(0xFF2E7D32)`, `OnSuccess = Color(0xFFFFFFFF)` |
| `app/src/main/java/com/sergiodev/bingo/ui/common/EmptyState.kt` | Created | `EmptyState(message, modifier)` — centered `Column`, `Icons.Default.Info` + `Text` |
| `app/src/main/java/com/sergiodev/bingo/ui/boards/list/BoardListScreen.kt` | Modified | Added zeroed-inset `CenterAlignedTopAppBar` with pinned scroll behavior; replaced 2-item `NavigationBar` + full-width Jugar button with a 3-slot `Row`/`Surface` bottom bar; branched content to `EmptyState` when boards list is empty |
| `app/src/main/java/com/sergiodev/bingo/ui/game/setup/GameSetupScreen.kt` | Modified | Replaced `SingleChoiceSegmentedButtonRow`/`SegmentedButton` with a `Row` of `FilterChip`s; selected chip shows check icon + green `Success`/`OnSuccess` colors |
| `app/src/main/java/com/sergiodev/bingo/MainActivity.kt` | Verified, not edited | Outer `Scaffold` default insets confirmed compatible |

## Work Unit Evidence

| Evidence | Value |
|---|---|
| Focused test command and exact result | `./gradlew testDebugUnitTest` → BUILD SUCCESSFUL (existing JVM unit suite, no regressions; no new unit-testable logic introduced — this is a presentation-only change) |
| Runtime harness command/scenario and exact result | `./gradlew clean build` → BUILD SUCCESSFUL (compile, lint, assembleDebug, assembleRelease all pass, confirming `Check`/`PlayArrow`/`FilterChip`/`CenterAlignedTopAppBar` resolve and are stable non-experimental APIs). Device/emulator manual smoke test (task 6.4) is N/A in this environment — no adb/emulator, per standing project convention (all 8 prior archived changes deferred this the same way) |
| Rollback boundary | Revert `Color.kt` (remove `Success`/`OnSuccess`), delete `EmptyState.kt`, revert `BoardListScreen.kt` to prior `NavigationBar` + full-width button, revert `GameSetupScreen.kt` to `SingleChoiceSegmentedButtonRow`. Each file is independently revertible; no shared state or migration to unwind. |

## Deviations from Design

None — implementation matches design.md exactly, including the corrected color-literal location (`Color.kt`, not `Theme.kt`) and the reversed icon-only `contentDescription` convention (non-null on all 3 bottom-bar icons).

## Issues Found

None.

## Remaining Tasks

- [ ] 6.4 Manual/device smoke test (expected-deferred, no adb/emulator in this environment)

## Workload / PR Boundary

- Mode: single PR
- Current work unit: all 4 suggested work units (theme token + EmptyState; top+bottom bar wiring; FilterChip mode row; build gate/verification) — completed together as one cohesive PR per the Low-risk, ~150-220 line forecast
- Boundary: starts from a clean `master` tree, ends with all 4 files above changed/created and both build gates green
- Estimated review budget impact: well under the 400-line budget (forecast ~150-220 lines; no chaining needed)
