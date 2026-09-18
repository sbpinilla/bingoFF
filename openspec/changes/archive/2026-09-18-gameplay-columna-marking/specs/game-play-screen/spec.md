# Game Play Screen Specification

## Purpose

Describe `GamePlayScreen`'s shared layout across all 5 `GameMode` values: the per-letter calls grid presentation, the `GameMode.COLUMNA`-only manual letter dismissal (a session-local marker that a column already has an outside winner), its effect on "Posibles ganadores," and the `TopAppBar` end-game action. Brand-new capability — no existing spec covers this layout.

## Requirements

### Requirement: Session-Local Dismissed Letters State

`GamePlayViewModel` MUST hold `dismissedLetters: Set<BingoLetter>`, persisted via `SavedStateHandle` the same way as `calledNumbers` (dedicated key). It MUST start empty for a fresh ViewModel/new game, MUST NOT be written to any repository, and a toggle action `onToggleLetterDismissed(letter)` MUST add the letter when absent and remove it when present.

#### Scenario: Fresh game starts with no dismissed letters

- GIVEN a new `GamePlayViewModel` with no prior dismissal data
- WHEN the initial state emits
- THEN `dismissedLetters` is empty

#### Scenario: Dismissed letters survive simulated process death

- GIVEN a `SavedStateHandle` restored with `BingoLetter.B` in the persisted dismissed-letters entry (mirroring `calledNumbers` restoration in `GamePlayViewModelTest.kt`)
- WHEN a new `GamePlayViewModel` is constructed from it
- THEN the initial state's `dismissedLetters` contains `BingoLetter.B`

#### Scenario: Toggle is reversible

- GIVEN `BingoLetter.G` is in `dismissedLetters`
- WHEN `onToggleLetterDismissed(BingoLetter.G)` is invoked again
- THEN `dismissedLetters` no longer contains `BingoLetter.G`

### Requirement: Dismissal Scoped Exclusively to Columna Mode

Dismissal state and UI MUST have zero effect for `GameMode.O`, `L`, `I`, `CARTON_COMPLETO`: no swipe/dismiss affordance renders, and any leftover `dismissedLetters` MUST NOT filter `possibleWinners` for these modes.

#### Scenario: Non-Columna modes ignore dismissal entirely

- GIVEN `state.mode` is `O`, `L`, `I`, or `CARTON_COMPLETO`, and `dismissedLetters` may be non-empty from a prior Columna session
- WHEN the calls grid renders and `possibleWinners` is computed
- THEN no row exposes a swipe-dismiss action or closed indicator, and `possibleWinners` matches a run with `dismissedLetters` empty

### Requirement: Dismissed Letters Excluded from Possible Winners

`possibleWinners` MUST exclude every candidate whose letter is in `dismissedLetters`, for all boards, immediately after toggling. `predictPossibleWinners` MUST stay unchanged and keep computing every letter; filtering happens in the ViewModel after it returns, before assigning `possibleWinners`.

#### Scenario: Dismissing removes it from possible winners immediately

- GIVEN Columna mode with a near-complete column `B` producing a `possibleWinners` candidate
- WHEN `onToggleLetterDismissed(BingoLetter.B)` is invoked
- THEN the next emitted `possibleWinners` has no `BingoLetter.B` candidate, for any board

#### Scenario: Reopening restores prediction eligibility

- GIVEN `BingoLetter.B` is dismissed and still numerically qualifies as a near-win
- WHEN it is toggled again to reopen
- THEN the next emitted `possibleWinners` includes it again, re-evaluated against current called numbers

### Requirement: Dismissal Does Not Affect Automatic Win Detection

Dismissing a letter MUST NOT alter `BingoWinChecker`, `WinAnnouncement`, or `AnnouncedWin`. A dismissed column that later genuinely completes via real called numbers MUST still announce normally.

#### Scenario: A dismissed column can still announce a real win later

- GIVEN `BingoLetter.B` dismissed and a board's column `B` incomplete
- WHEN the remaining numbers for that column are called and submitted
- THEN the board's column-`B` win is announced in `winners` exactly as for a non-dismissed letter, and `rebuildSession` reproduces the same `winners`/`announced` regardless of `dismissedLetters`

### Requirement: Columna Swipe-to-Dismiss Interaction

For Columna only, each letter row MUST support a swipe revealing a "mark as closed" action invoking `onToggleLetterDismissed(letter)`. A dismissed row MUST render a distinct closed indicator; tapping it MUST reopen the letter via the same toggle.

#### Scenario: Swiping reveals and confirms the dismiss action

- GIVEN Columna mode, `BingoLetter.N` not dismissed
- WHEN the user swipes its row and confirms the revealed action
- THEN `onToggleLetterDismissed(BingoLetter.N)` fires and the row renders closed

#### Scenario: Tapping a closed indicator reopens the row

- GIVEN `BingoLetter.N` dismissed and rendered closed
- WHEN the user taps its closed indicator
- THEN `onToggleLetterDismissed(BingoLetter.N)` fires and the row returns to open

### Requirement: Bordered Grid Presentation for Per-Letter Calls

The per-letter calls section MUST render as a bordered/divided grid mirroring `BingoGridDisplay`'s Material3 `colorScheme.outline` convention, for all 5 `GameMode` values, always showing all 5 `BingoLetter` rows (B, I, N, G, O). This is presentation-only; swipe/dismiss affordances appear only in Columna, per the scoping requirement above.

#### Scenario: All 5 letters render for every mode

- GIVEN any `GameMode` value
- WHEN the calls section renders
- THEN exactly 5 bordered/divided rows show, one per `BingoLetter` (B, I, N, G, O)

### Requirement: End Game Action Relocated to TopAppBar

"Terminar juego" MUST trigger from an `IconButton` in `TopAppBar`'s `actions`, not a body `Button`. It MUST open the same confirmation `AlertDialog` (title, message, confirm, cancel) unchanged: confirm invokes `onEndGame`; cancel dismisses without ending the game.

#### Scenario: Tapping the icon opens the confirmation dialog

- GIVEN `GamePlayScreen`'s `TopAppBar`
- WHEN the user taps the end-game `IconButton` in `actions`
- THEN the same confirmation `AlertDialog` shown pre-relocation appears

#### Scenario: Confirming triggers onEndGame

- GIVEN the dialog is open
- WHEN the user confirms
- THEN `onEndGame` is invoked and the dialog dismisses (cancelling instead dismisses without invoking `onEndGame`)

### Requirement: New Strings Localized in Both Locales

Every new string (closed-state label, swipe/dismiss action label, end-game icon content description) MUST exist in both `values/strings.xml` (Spanish) and `values-en/strings.xml` (English), no key missing from either.

#### Scenario: New keys exist in both locale files

- GIVEN the new string keys for this change
- WHEN both locale files are compared key-by-key
- THEN every new key is present in both, with a non-empty value
