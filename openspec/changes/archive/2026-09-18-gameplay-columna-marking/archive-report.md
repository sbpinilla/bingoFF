# Archive Report: Columna Manual Win Dismissal, Grid Calls Presentation, End Game Action Relocation

**Change**: `gameplay-columna-marking`  
**Archive Date**: 2026-09-18  
**Verdict**: CLOSED — Pass with Warnings

## Artifact Lineage

| Artifact | Observation ID | Source | Scope |
|----------|----------------|--------|-------|
| spec | 565 | Engram `sdd/gameplay-columna-marking/spec` | 8 requirements, 13 scenarios |
| design | 571 | Engram `sdd/gameplay-columna-marking/design` | Technical approach, architecture decisions, file changes |
| tasks | 577 | Engram `sdd/gameplay-columna-marking/tasks` | 23 implementation tasks (6 phases) |
| apply-progress | 583 | Engram `sdd/gameplay-columna-marking/apply-progress` | 23/23 tasks complete |
| verify-report | 589 | Engram `sdd/gameplay-columna-marking/verify-report` | PASS WITH WARNINGS: 0 CRITICAL, 4 WARNING, 1 SUGGESTION |

## Final State Summary

### Task Completion
- **Total**: 23 tasks across 6 phases
- **Complete**: 23 (100%)
- **Status**: All implementation and verification tasks marked `[x]` in `tasks.md` and confirmed in `apply-progress.md`

### Build & Test Gates (re-run independently per verify-report)
- **Unit Tests**: `./gradlew testDebugUnitTest --rerun-tasks` → exit 0, BUILD SUCCESSFUL
  - Total: 132 tests (up from baseline 128, +4 new test cases)
  - Failures: 0 | Errors: 0
  - New tests in `GamePlayViewModelTest.kt`: 5.1 (dismiss removes from possible winners), 5.2 (dismiss does not affect automatic win detection), 5.3 (reopening restores eligibility), 5.4 (persistence via SavedStateHandle)
- **Build**: `./gradlew clean build` → exit 0, BUILD SUCCESSFUL

### Capability Added
**Brand-New Capability**: `game-play-screen` (no prior spec existed)

**Scope**: `GamePlayScreen`'s shared layout across all 5 `GameMode` values, with `GameMode.COLUMNA`-only manual letter dismissal feature.

**Key Features**:
- Session-local `dismissedLetters: Set<BingoLetter>` state persisted via `SavedStateHandle` (mirrors existing `calledNumbers` pattern)
- `Columna` mode: swipe-to-dismiss rows revealing a "mark as closed" action; dismissed rows render dimmed/struck-through with reopen icon
- Non-`Columna` modes: no swipe/dismiss affordance; `dismissedLetters` structurally has zero effect (all `PredictionCandidate.letter` values are `null` per `WinPrediction.kt:55`, making the dismissal filter a no-op)
- Dismissed letters excluded from `possibleWinners` immediately after toggling, without touching domain `predictPossibleWinners` signature or `BingoWinChecker`/`WinAnnouncement`/`AnnouncedWin`
- Bordered/divided grid presentation for per-letter calls section, all 5 modes, all 5 `BingoLetter` rows (B, I, N, G, O)
- "Terminar juego" (End Game) relocated from body `Button` to `TopAppBar` `actions` icon (`Icons.Default.Stop`); confirmation `AlertDialog` unchanged

### Files Changed
| File | Action | Notes |
|------|--------|-------|
| `app/src/main/java/com/sergiodev/bingo/ui/game/play/GamePlayUiState.kt` | Modified | Added `dismissedLetters: Set<BingoLetter> = emptySet()` |
| `app/src/main/java/com/sergiodev/bingo/ui/game/play/GamePlayViewModel.kt` | Modified | Added `KEY_DISMISSED_LETTERS`, `dismissedLetters` state, `onLetterDismissToggled(letter)`, 4th `combine` source + post-`predictPossibleWinners` filter |
| `app/src/main/java/com/sergiodev/bingo/ui/game/play/GamePlayScreen.kt` | Modified | New `LetterCallsGrid`/`LetterCallsRow` composables; `TopAppBar(actions = {...})` icon; `EndGameAction` trimmed to dialog-only with lifted state |
| `app/src/main/res/values/strings.xml` | Modified | 4 new keys (ES): `game_play_column_mark_won`, `game_play_column_closed_label`, `game_play_column_reopen_description`, `game_play_end_game_icon_description` |
| `app/src/main/res/values-en/strings.xml` | Modified | Same 4 keys (EN) |
| `app/src/test/java/com/sergiodev/bingo/ui/game/play/GamePlayViewModelTest.kt` | Modified | New `handle(dismissedLetters = ...)` test helper param + 4 new test cases |

**Zero changes** to `BingoWinChecker.kt`, `WinAnnouncement.kt`, `AnnouncedWin.kt`, `GameMode.kt`, `predictPossibleWinners` signature — confirmed via `git diff --stat master`.

### Specification Compliance
Per verify-report (obs #589):
- **Requirements**: 8/8 satisfied
- **Scenarios**: 13/13 satisfied
  - Runtime-test-covered (5): SavedStateHandle restoration, toggle reversibility, dismiss-removes-from-possibleWinners, reopen-restores-eligibility, dismissal-does-not-affect-automatic-win-detection
  - Config-evidence-covered (1): new string keys present in both locale files
  - Source-inspection-only per standing no-Compose-UI-test-infra convention (7): non-Columna scoping, swipe-reveal-confirm, closed-row tap-to-reopen, all-5-letters grid, TopAppBar icon opens dialog, confirm triggers onEndGame, fresh-game-starts-empty

### Verification Issues
**CRITICAL**: None.

**WARNING (4)**:
1. Manual/device smoke test (swipe gesture, grid layout across all 5 modes) deferred per standing convention — no Compose UI test infra, no emulator/adb in this environment (consistent with all 14 prior archived changes)
2. Seven pure-Compose-rendering scenarios verified by source inspection only per same convention
3. Non-Columna-scoping scenario has structural proof (`PredictionCandidate.letter` is `null` outside `COLUMNA`, making `null in dismissed` always false) but no dedicated ViewModel test combining non-empty `dismissedLetters` with non-Columna mode — recommend follow-up test
4. No dedicated test for "fresh game starts with `dismissedLetters` empty" — only implicit via defaults; recommend follow-up test

**SUGGESTION (1)**:
- Dead string key: `game_play_end_game_button` confirmed zero code references (was labeled the body `Button` replaced by `TopAppBar` action). Both `values/strings.xml` and `values-en/strings.xml` still define it. Out of scope for this change; recorded as follow-up cleanup candidate.

### Technical Deviations (Accepted)
**Deprecation Warning**: `rememberSwipeToDismissBoxState`'s `confirmValueChange` parameter is only available on a `DeprecationLevel.WARNING` overload in Material3 1.4.0 (pinned `composeBom = 2026.02.01`). The non-deprecated 2-arg overload (`initialValue`, `positionalThreshold`) has no `confirmValueChange` parameter, making the design's "always return false to settle back at `Settled`" veto semantics unavailable. Used the deprecated 3-arg overload with `@Suppress("DEPRECATION")` in `LetterCallsRow`. Verified genuine by extracting and inspecting `material3-android-1.4.0-sources.jar`. Project has no `allWarningsAsErrors` / `Werror` compiler flag, so the deprecation is a warning, not a build error. Verified compiling cleanly via `./gradlew :app:compileDebugKotlin`.

## Archive Operations

### Spec Merge
**Action**: Create main spec from brand-new delta spec.

**Delta Spec Location**: `openspec/changes/gameplay-columna-marking/specs/game-play-screen/spec.md`  
**Main Spec Location**: `openspec/specs/game-play-screen/spec.md`  
**Merge Type**: Full-copy (brand-new capability, no prior main spec existed)  
**Merge Verification**: `diff -u` passed — source and destination byte-identical

### Change Folder Archive Move
**Source**: `openspec/changes/gameplay-columna-marking/`  
**Destination**: `openspec/changes/archive/2026-09-18-gameplay-columna-marking/`  
**Method**: `git mv` attempted; fell back to `mv` (source was untracked or empty in git index)  
**Verification**: Pre-move snapshot created, `diff -r` against snapshot post-move: zero differences (byte-identical)  
**Confirmation**: Source directory removed; archive contains all original artifacts (proposal, specs, design, tasks, apply-progress)

## Cycle Closure

**SDD Cycle Status**: CLOSED

The change `gameplay-columna-marking` has been fully:
- Proposed and specified (proposal, spec)
- Designed (design, architecture decisions)
- Tasked (23 implementation tasks)
- Applied (all tasks complete, files changed)
- Verified (PASS WITH WARNINGS, 0 CRITICAL, 132 tests green, build green)
- Archived (main spec created, change folder moved to archive, this report written)

The SDD cycle is complete. No follow-up phases are required. Ordinary repository policy governs delivery.

### Known Limitations & Follow-ups
- **Manual smoke test**: Device/emulator test for swipe gesture and grid layout deferred (no Compose UI test infra, no emulator in this environment)
- **Test gap — non-Columna scoping invariant**: Structural proof exists (`PredictionCandidate.letter` is `null` for non-Columna modes) but no dedicated ViewModel-level test; recommend unit test combining non-empty `dismissedLetters` with non-Columna mode assertion
- **Test gap — fresh-game default**: No dedicated test for "fresh game starts with `dismissedLetters` empty"; only implicit via `emptySet()` default; recommend unit test for explicit coverage
- **Dead string key**: `game_play_end_game_button` (zero code references after `TopAppBar` relocation) remains in both `values/strings.xml` and `values-en/strings.xml`; recommend cleanup in a future change

---

**Archive Report Generated**: 2026-09-18  
**Archived By**: sdd-archive executor  
**Project**: BingoFF (Android, Kotlin, Jetpack Compose, MVVM, Hilt)  
**Artifact Store Mode**: hybrid (OpenSpec + Engram)
