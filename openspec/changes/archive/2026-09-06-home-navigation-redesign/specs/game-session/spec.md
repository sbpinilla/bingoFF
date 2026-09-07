# Delta for Game Session

> Note: the proposal refers to this change informally as capability `mode-selection`; the underlying requirement being modified ("Mode Selection Control") is owned by the existing `game-session` capability (Engram `sdd/game-play-prediction/spec`, obs #154), so this delta targets `game-session` to merge correctly against that base at archive time.

## MODIFIED Requirements

### Requirement: Mode Selection Control

`GameSetupScreen` MUST present the 5 `GameMode` values via a row of `FilterChip`s (one per mode), with short readable Spanish labels (not raw enum names). The currently selected chip MUST show a check indicator and MUST use a new green semantic color token, defined in `Theme.kt`, as its selected container color. No hardcoded color literal MAY be used at the call site. The "Iniciar" button and navigation to `GamePlayScreen` MUST behave exactly as before this change.
(Previously: rendered via a single `SingleChoiceSegmentedButtonRow` containing one `SegmentedButton` per mode, with no distinct selected-state check indicator or dedicated color token.)

#### Scenario: FilterChip row renders all modes

- GIVEN `GameSetupScreen` loads with `state.availableModes` containing all 5 `GameMode` entries
- WHEN the screen composes
- THEN a row renders with exactly 5 `FilterChip`s, each showing a readable label, not the enum constant name

#### Scenario: Selecting a chip shows the check indicator and green token

- GIVEN the chip row is displayed with no mode selected or a different mode selected
- WHEN the user taps a chip
- THEN that chip becomes selected in UI state
- AND the selected chip shows a check indicator and uses the new green selected-color token as its container color

#### Scenario: Start behavior unchanged

- GIVEN a mode is selected in the chip row
- WHEN the user taps "Iniciar"
- THEN navigation to `GamePlayScreen` occurs with the selected mode, identical to pre-change behavior
