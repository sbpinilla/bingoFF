# Design: Columna Manual Win Dismissal, Grid Calls Presentation, End Game Action Relocation

Proposal: Engram `sdd/gameplay-columna-marking/proposal` / `openspec/changes/gameplay-columna-marking/proposal.md`.

## Technical Approach

`GamePlayViewModel` gains a second `SavedStateHandle`-persisted set, `dismissedLetters: Set<BingoLetter>`, mutated only by a new `onLetterDismissToggled(letter)` action, mirroring the existing `calledNumbers`/`KEY_CALLED_NUMBERS` pattern exactly. Inside the existing `combine` block, `predictPossibleWinners(...)`'s result is filtered by `candidate.letter !in dismissedLetters` before being assigned to `GamePlayUiState.possibleWinners` — `predictPossibleWinners`, `BingoWinChecker`, `WinAnnouncement`, `AnnouncedWin`, `GameMode` stay untouched. `GamePlayContent`'s per-letter `Text` loop is replaced by two new private composables in `GamePlayScreen.kt` — `LetterCallsGrid` (bordered/divided Material3 grid, mirrors `BingoGridDisplay`'s `colorScheme.outline` convention, all 5 modes) and `LetterCallsRow` (wraps each Columna row in `SwipeToDismissBox` when open, renders a plain dimmed/struck-through row with a reopen `IconButton` when closed). `EndGameAction` is trimmed to just its `AlertDialog`; its `show` boolean is lifted to `GamePlayContent` and set by a new `TopAppBar(actions = {...})` `IconButton` (`Icons.Default.Stop`).

## Architecture Decisions

| Decision | Choice | Alternatives considered | Rationale |
|---|---|---|---|
| `dismissedLetters` serialization | `ArrayList<String>` of `BingoLetter.name`, deserialized via `BingoLetter.valueOf(...)` wrapped defensively (`mapNotNull { runCatching { BingoLetter.valueOf(it) }.getOrNull() }`) | Store `Set<Int>` of `ordinal` | Mirrors `GameMode`'s own `.name`/`.valueOf` serialization already used one line above `KEY_CALLED_NUMBERS`; string names survive enum reordering, ordinals don't |
| Filter location | Filter `predictPossibleWinners(...)`'s output inline in the `combine` block, before constructing `GamePlayUiState` | Filter inside `predictPossibleWinners` itself | Proposal explicitly forbids touching the domain function's signature/logic; a post-hoc `.filterNot` keeps the domain layer pure and mode-agnostic (non-Columna candidates always have `letter == null`, so the filter is a structural no-op for O/L/I/CARTON_COMPLETO) |
| Grid composable reuse | New `LetterCallsGrid`/`LetterCallsRow` in `GamePlayScreen.kt`, mirroring only `BingoGridDisplay`'s visual convention (`border`, `HorizontalDivider`/`VerticalDivider` with `colorScheme.outline`) | Extend `BingoGridDisplay` to accept variable-length columns | `BingoGridDisplay` renders one fixed 5×5 `BoardCard`; this section is 5 rows of variable-length number lists with no `BoardCard` — different data shape, forcing an unrelated API onto it would couple two unrelated concerns |
| Swipe/reopen interaction | Open rows wrap in `SwipeToDismissBox` (`enableDismissFromStartToEnd = false`, only `EndToStart`); `confirmValueChange` calls `onLetterDismissToggled(letter)` and always returns `false` so the box springs back to `Settled` instead of staying visually dismissed. Closed rows render as a **plain** (non-swipeable) dimmed row with `textDecoration = TextDecoration.LineThrough` and a small reopen `IconButton` (`Icons.Default.Lock`) that calls the same toggle | Keep the row inside `SwipeToDismissBox` permanently and swipe again to reopen | `SwipeToDismissBox` has no built-in "settle at Settled after being dismissed" semantics for a row that stays in the list (it's designed for list-item removal); a plain closed row with a dedicated tap target is simpler, discoverable, and avoids fighting the component against its intended use — same rationale the proposal already committed to |
| `TopAppBar` / `EndGameAction` split | `EndGameAction` keeps only the `AlertDialog`, taking `show`/`onDismiss`/`onConfirm`; `GamePlayContent` owns `var showEndGameDialog by rememberSaveable`, set by the new `TopAppBar` `IconButton` | Keep `EndGameAction` owning its own `show` state, expose a public `openDialog()` lambda | Lifting state to the caller is the standard Compose pattern already used for `pending`/`inputError` elsewhere in this screen; avoids inventing an imperative "open" callback shape |

## Data Flow

    TopAppBar IconButton (Stop icon)
          │ showEndGameDialog = true
          ▼
    EndGameAction(show, onDismiss, onConfirm=onEndGame)  (dialog only, unchanged internals)

    LetterCallsRow swipe (Columna only) ──► onLetterDismissToggled(letter)
                                                  │
                                                  ▼
                                    GamePlayViewModel.dismissedLetters (MutableStateFlow)
                                                  │ savedStateHandle[KEY_DISMISSED_LETTERS]
                                                  ▼
                              combine(boards, calledNumbers, pending, dismissedLetters)
                                                  │
                              predictPossibleWinners(...).filterNot { it.letter in dismissedLetters }
                                                  ▼
                                     GamePlayUiState.possibleWinners / .dismissedLetters
                                                  ▼
                                     PossibleWinnersSection / LetterCallsGrid (recompose)

## File Changes

| File | Action | Description |
|---|---|---|
| `ui/game/play/GamePlayViewModel.kt` | Modify | `dismissedLetters` state + `KEY_DISMISSED_LETTERS`, `onLetterDismissToggled(letter)`, filter added to `combine` block, `dismissedLetters` added as 4th `combine` source |
| `ui/game/play/GamePlayUiState.kt` | Modify | New `dismissedLetters: Set<BingoLetter> = emptySet()` field |
| `ui/game/play/GamePlayScreen.kt` | Modify | `LetterCallsGrid`/`LetterCallsRow` composables replace the `Text` loop; `TopAppBar(actions = {...})`; `EndGameAction` trimmed to dialog-only, state lifted |
| `res/values/strings.xml` | Modify | 4 new keys (ES) |
| `res/values-en/strings.xml` | Modify | Same 4 keys (EN) |
| `test/.../GamePlayViewModelTest.kt` | Modify | New tests for toggle/persistence/filtering |

## Interfaces / Contracts

```kotlin
// GamePlayUiState.kt
data class GamePlayUiState(
    // ...existing fields unchanged...
    val dismissedLetters: Set<BingoLetter> = emptySet(),
)

// GamePlayViewModel.kt
private const val KEY_DISMISSED_LETTERS = "dismissedLetters"

private val dismissedLetters = MutableStateFlow(
    savedStateHandle.get<ArrayList<String>>(KEY_DISMISSED_LETTERS)
        ?.mapNotNull { runCatching { BingoLetter.valueOf(it) }.getOrNull() }
        ?.toSet() ?: emptySet(),
)

fun onLetterDismissToggled(letter: BingoLetter) {
    val updated = dismissedLetters.value.let {
        if (letter in it) it - letter else it + letter
    }
    dismissedLetters.value = updated
    savedStateHandle[KEY_DISMISSED_LETTERS] = ArrayList(updated.map { it.name })
}
// combine(..., dismissedLetters) { boards, called, pendingEntry, dismissed -> ... }
// possibleWinners = predictPossibleWinners(...).filterNot { it.letter in dismissed }
```

New strings (both `values/strings.xml` and `values-en/strings.xml`):

| Key | ES | EN |
|---|---|---|
| `game_play_column_mark_won` | Marcar como ganado | Mark as won |
| `game_play_column_closed_label` | Cerrado | Closed |
| `game_play_column_reopen_description` | Reabrir columna | Reopen column |
| `game_play_end_game_icon_description` | Terminar juego | End game |

## Testing Strategy

| Layer | What to Test | Approach |
|---|---|---|
| Unit | `onLetterDismissToggled` adds/removes from `dismissedLetters`, reversible | `GamePlayViewModelTest`, direct state assertion |
| Unit | `SavedStateHandle` round-trip (`KEY_DISMISSED_LETTERS`) survives simulated process death | New `handle(dismissedLetters = ...)` test helper param, mirroring `calledNumbers` helper |
| Unit | Dismissed letter's candidates excluded from `possibleWinners`; reopening restores them if still qualifying | Extend `columnaMode_populatesPossibleWinnersAfterThreeCalls`-style setup |
| Unit | Non-Columna modes: `dismissedLetters` has no effect (`possibleWinners` already empty) | Assert against existing `nonColumnaMode_possibleWinnersAlwaysEmpty` invariant |
| Manual (device) | Swipe reveals action, closed row dims + reopens on tap, `TopAppBar` icon opens confirm dialog | Per standing convention — no Compose UI test infra exists |

## Threat Matrix

N/A — no routing, shell, subprocess, VCS/PR automation, executable-file classification, or process-integration boundary.

## Migration / Rollout

No migration required. `dismissedLetters` is session-local `SavedStateHandle` state, never reaches Room, and defaults to empty on first launch post-update. Rollback: revert the three Kotlin files and both string files; no persisted data beyond the current process is affected.

## Open Questions

- [ ] Confirm exact `SwipeToDismissBox`/`rememberSwipeToDismissBoxState` parameter names (`enableDismissFromStartToEnd` vs. a `directions` set) against the pinned `composeBom = 2026.02.01` at apply time — the API has been stable Material3 surface for several BOM cycles, not expected to block, but not compiled here.
