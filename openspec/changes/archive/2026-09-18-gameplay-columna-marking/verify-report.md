```yaml
schema: gentle-ai.verify-result/v1
change: gameplay-columna-marking
mode: full-artifacts
verdict: pass_with_warnings
evidence_revision: sha256:460ebffd3e8b9e30db89ebe7580fa23ca8fd7f60faadc2814d95005cf92aa056
requirements_total: 8
scenarios_total: 13
scenarios_covered_by_runtime_test: 5
scenarios_covered_by_source_or_config_inspection: 8
tasks_total: 23
tasks_complete: 23
tasks_incomplete: 0
critical_count: 0
warning_count: 4
suggestion_count: 1
test_command: "./gradlew testDebugUnitTest --rerun-tasks"
test_exit_code: 0
tests_total: 132
tests_failed: 0
tests_errors: 0
test_output_hash: sha256:cb3bd22d49460cd886aff474fa9ee3a09a14a82a9c8c7d78921d827ec65ee318
build_command: "./gradlew clean build"
build_exit_code: 0
build_output_hash: sha256:cea92330561c9141f3174ea90961cfece84e698f735b8113822741faf656524c
```

## Verification Report: Columna Manual Win Dismissal, Grid Calls Presentation, End Game Action Relocation

**Change**: `gameplay-columna-marking`
**Mode**: Full artifacts (spec + design + tasks + apply-progress)

### Completeness

23/23 tasks marked `[x]` in `tasks.md` and `apply-progress.md`, cross-checked against actual code state — every task's described change is present and matches. Task 6.4 (manual/device smoke test) is correctly marked expected-deferred, consistent with the project's standing convention (no Compose UI test infrastructure, no emulator/adb in this environment) applied identically across all 14 prior archived changes.

### Build / Test Evidence (re-run independently, not trusted from apply's report)

- `./gradlew testDebugUnitTest --rerun-tasks` → exit 0, BUILD SUCCESSFUL.
- Summed JUnit XML `tests=`/`failures=`/`errors=` attributes across all 21 `TEST-*.xml` result files: **tests=132, failures=0, errors=0**.
- Baseline before this change (from the most recently archived change, `string-localization`, Engram obs #541): 128 tests, 0 failures. This change adds exactly **+4 tests** (132 − 128 = 4), matching the 4 new test cases added in tasks 5.1–5.4. No regression, no unexplained count drift.
- `GamePlayViewModelTest` alone: 12/12 passed, including the 4 new cases (`dismissingLetter_removesItFromPossibleWinners`, `dismissingLetter_doesNotAffectAutomaticWinDetection`, `reopeningDismissedLetter_restoresPredictionEligibility`, `dismissedLetters_surviveSimulatedProcessDeath`).
- `./gradlew clean build` → exit 0, BUILD SUCCESSFUL (assembleDebug, assembleRelease, lint, test, check — no errors).

### Spec Compliance Matrix

| Requirement | Scenario | Evidence | Status |
|---|---|---|---|
| Session-Local Dismissed Letters State | Fresh game starts with no dismissed letters | Structural: `GamePlayUiState.dismissedLetters` defaults to `emptySet()`; `MutableStateFlow` init falls back to `emptySet()` when `SavedStateHandle` has no entry; every other test's `handle()` call with no `dismissedLetters` arg implicitly exercises this path successfully | PASS (source/config inspection; no dedicated assertion — see Issues) |
| | Dismissed letters survive simulated process death | `dismissedLetters_surviveSimulatedProcessDeath` — passes | PASS (runtime test) |
| | Toggle is reversible | `reopeningDismissedLetter_restoresPredictionEligibility` toggles `B` off then on, asserts final `BingoLetter.B !in state.dismissedLetters` | PASS (runtime test) |
| Dismissal Scoped Exclusively to Columna Mode | Non-Columna modes ignore dismissal entirely | Source inspection: `LetterCallsGrid` sets `dismissible = state.mode == GameMode.COLUMNA`, gating both the swipe affordance and the closed-row branch in `LetterCallsRow`'s `when`; `WinPrediction.kt:55` sets `candidate.letter = if (mode == GameMode.COLUMNA) ... else null`, so `.filterNot { it.letter in dismissed }` is a structural no-op for non-Columna modes (a `Set<BingoLetter>` can never contain `null`) | PASS (source inspection; no dedicated runtime test combining non-empty `dismissedLetters` with a non-Columna mode — see Issues) |
| Dismissed Letters Excluded from Possible Winners | Dismissing removes it from possible winners immediately | `dismissingLetter_removesItFromPossibleWinners` — passes | PASS (runtime test) |
| | Reopening restores prediction eligibility | `reopeningDismissedLetter_restoresPredictionEligibility` — passes; filter is re-applied inside the reactive `combine` block against live `calledNumbers`, not a stale snapshot | PASS (runtime test) |
| Dismissal Does Not Affect Automatic Win Detection | A dismissed column can still announce a real win later | `dismissingLetter_doesNotAffectAutomaticWinDetection` — dismisses `B`, then completes board1's B column via real calls, asserts `winners` still contains `COLUMN_B` exactly once | PASS (runtime test) |
| Columna Swipe-to-Dismiss Interaction | Swiping reveals and confirms the dismiss action | Source inspection: `SwipeToDismissBox` `confirmValueChange` invokes `onDismissToggled()` on `EndToStart`, then returns `false` to spring back | PASS (source inspection only — no Compose UI test infra, per project convention) |
| | Tapping a closed indicator reopens the row | Source inspection: closed-row branch renders `IconButton(onClick = onDismissToggled)` with `Icons.Default.Lock` | PASS (source inspection only) |
| Bordered Grid Presentation for Per-Letter Calls | All 5 letters render for every mode | Source inspection: `LetterCallsGrid` iterates `BingoLetter.entries.forEachIndexed` unconditionally (no mode gate), bordered with `colorScheme.outline`, divided with `HorizontalDivider` | PASS (source inspection only) |
| End Game Action Relocated to TopAppBar | Tapping the icon opens the confirmation dialog | Source inspection: `TopAppBar` `actions` `IconButton` sets `showEndGameDialog = true`; `EndGameAction` renders the same `AlertDialog` (title/message/confirm/cancel resource keys byte-identical, confirmed via `git diff` — zero change to `game_play_end_game_dialog_title/message/confirm_button/cancel_button`) | PASS (source inspection only) |
| | Confirming triggers onEndGame | Source inspection: `onConfirm = { onEndGame(); showEndGameDialog = false }`; cancel path (`onDismiss`) only sets `showEndGameDialog = false`, never calls `onEndGame` | PASS (source inspection only) |
| New Strings Localized in Both Locales | New keys exist in both locale files | `rg` confirms all 4 new keys (`game_play_column_mark_won`, `game_play_column_closed_label`, `game_play_column_reopen_description`, `game_play_end_game_icon_description`) present with non-empty ES and EN values in both files | PASS (direct file inspection) |

### Correctness / Design Coherence

| Design Decision | Implementation Match |
|---|---|
| `dismissedLetters` serialization via `ArrayList<String>` of `.name` / `valueOf` | Confirmed at `GamePlayViewModel.kt:49-53` and `onLetterDismissToggled` — exact match to design's `Interfaces/Contracts` section |
| Filter applied post-hoc in `combine`, not inside `predictPossibleWinners` | Confirmed — `predictPossibleWinners`'s signature/body has zero diff (see Domain-Untouched Constraint below); filter (`.filterNot { it.letter in dismissed }`) lives entirely in `GamePlayViewModel.kt:71-72` | Matches design exactly |
| New `LetterCallsGrid`/`LetterCallsRow` composables mirroring `BingoGridDisplay`'s outline convention | Confirmed at `GamePlayScreen.kt:192-308` — `border(1.dp, colorScheme.outline)` + `HorizontalDivider(color = colorScheme.outline)` | Matches design |
| Swipe/reopen interaction: `SwipeToDismissBox` always returns `false` from `confirmValueChange`, closed rows render as a plain non-swipeable row with reopen `IconButton` | Confirmed exactly as designed | Matches design |
| `EndGameAction` trimmed to dialog-only, `show` lifted to `GamePlayContent` via `rememberSaveable` | Confirmed — `EndGameAction(show, onDismiss, onConfirm)` signature matches; `showEndGameDialog` lives in `GamePlayContent` | Matches design |

### Domain-Untouched Constraint (hard requirement)

`git diff --stat master` against `BingoWinChecker.kt`, `WinAnnouncement.kt`, `AnnouncedWin.kt`, `GameMode.kt` produces **zero output** — confirmed zero diff on all four files. `predictPossibleWinners`'s signature (`mode: GameMode, boards: List<BoardCard>, called: Set<Int>, announced: Set<AnnouncedWin>`) is unchanged. This is the change's hard domain-untouched constraint and it holds.

### Task Completion

All 23 tasks (`1.1`–`6.4`) marked `[x]` in `tasks.md` and `apply-progress.md`, verified against actual code state — every described change is present. `git diff --stat` shows exactly the 6 expected files touched (`+253/-46`... actually current working-tree diff measures `299` insertions total against `master`), matching the forecasted 180-260 authored range plus test additions, well under the 400-line review budget.

### Issues

**CRITICAL**: None.

**WARNING**:
1. Manual/device smoke test for the swipe gesture, closed-row reopen, and grid layout across all 5 `GameMode` values is correctly marked expected-deferred — no Compose UI test infrastructure and no emulator/adb available in this environment, consistent with the project's standing convention applied identically across all 14 prior archived changes. Non-blocking, documented gap.
2. 6 of the 13 spec scenarios that describe pure Compose rendering/interaction behavior (swipe reveal/confirm, closed-row tap-to-reopen, all-5-letters grid rendering, TopAppBar icon opening the dialog, confirm triggering `onEndGame`) are verified by source inspection only, not by an automated runtime test, per the same standing no-UI-test-infra convention. Non-blocking.
3. The "Non-Columna modes ignore dismissal entirely" scenario has strong structural proof (candidate `letter` is always `null` outside `COLUMNA`, and a `Set<BingoLetter>` can never contain `null`) but no dedicated `GamePlayViewModelTest` case sets a non-empty `dismissedLetters` on a non-Columna mode and asserts `possibleWinners` equality against an empty-`dismissedLetters` run. This is easily addable at the ViewModel level (no UI infra needed) and would close a real, if narrow, coverage gap. Recommend adding in a follow-up.
4. No dedicated test explicitly asserts "fresh game starts with `dismissedLetters` empty" as its own scenario — it is only implicitly exercised via every other test's default `handle()` call and the type-level default. Recommend a one-line addition in a follow-up; not blocking given the structural guarantee (`?: emptySet()` fallback, `emptySet()` state default).

**SUGGESTION**:
1. `game_play_end_game_button` string key (both `values/strings.xml` and `values-en/strings.xml`) is now dead: `rg` across `app/src` finds zero remaining code references to it, only its own definitions in the two resource files. It became unused when the body `Button` end-game trigger was replaced by the `TopAppBar` action. Apply's report correctly flagged this and correctly chose not to remove it (out of scope for this change). Recommend a small cleanup change to delete both entries.

### Accepted Risk (not a defect)

Apply's report flagged using `@Suppress("DEPRECATION")` on the `rememberSwipeToDismissBoxState` overload that accepts `confirmValueChange`, because the pinned Material3 1.4.0 (verified: `androidx.compose.material3:material3-android:1.4.0` in the resolved Gradle cache, matching design's stated `composeBom = 2026.02.01`) only exposes that parameter on a `@Deprecated(level = DeprecationLevel.WARNING)` overload — confirmed genuine by extracting and reading `material3-android-1.4.0-sources.jar`'s `SwipeToDismissBox.kt`: the non-deprecated `rememberSwipeToDismissBoxState(initialValue, positionalThreshold)` overload has no `confirmValueChange` parameter at all; only the deprecated 3-arg overload does. The project has no `allWarningsAsErrors`/`Werror` Kotlin compiler flag (confirmed via `rg` against both `build.gradle.kts` files — no match), so this is a compiler warning, not a build error, and `./gradlew clean build` and `compileDebugKotlin` both succeed cleanly. This is a real, documented, accepted risk (deprecated-but-functional public API), not a fabricated justification and not a defect.

### Final Verdict

**PASS WITH WARNINGS**. All 8 spec requirements and 13 scenarios are satisfied: 5 scenarios have direct runtime-test proof, 1 has direct config-file evidence, and 7 rely on source inspection consistent with this project's long-standing no-Compose-UI-test-infra convention (applied identically across 14 prior archived changes). The hard domain-untouched constraint holds exactly (`BingoWinChecker.kt`, `WinAnnouncement.kt`, `AnnouncedWin.kt`, `GameMode.kt` — zero diff; `predictPossibleWinners` signature unchanged). Full unit test suite is green (132 tests, 0 failures, +4 over the 128-test baseline, matching the 4 new cases exactly) across both `testDebugUnitTest --rerun-tasks` and `clean build`. All 23 tasks are complete and match code state. No CRITICAL issues. 4 WARNINGs (3 UI-scenario coverage gaps consistent with project convention, plus 1 narrow-but-real ViewModel-level test gap for the Columna-scoping invariant) and 1 SUGGESTION (dead `game_play_end_game_button` string key) — none blocking archive.
