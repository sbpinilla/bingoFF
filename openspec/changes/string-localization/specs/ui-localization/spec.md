# UI Localization Specification

## Purpose

Establish string-resource infrastructure (Spanish default + English) for BingoFF and migrate every hardcoded UI-facing literal to `stringResource()`. This is a NEW capability spec, not a delta against `app-navigation`, `board-export-import`, `game-session`, or any other existing capability: those capabilities' user-facing behavior, screens, flows, and validation rules are unchanged — only the string SOURCE changes from Kotlin literal to Android string resource. No existing capability spec is touched by this change.

Out of scope: any language-switcher UI (Android's standard device-locale resolution is the only mechanism) and `<plurals>` resources.

## Requirements

### Requirement: Spanish/English Resource Parity

The system MUST define every one of the ~54 UI-facing string keys in both `app/src/main/res/values/strings.xml` (Spanish, default) and `app/src/main/res/values-en/strings.xml` (English), with no key present in one file and missing from the other.

#### Scenario: Every key exists in both locale files

- GIVEN the complete set of string keys used across the 12 affected files
- WHEN `values/strings.xml` and `values-en/strings.xml` are compared key-by-key
- THEN every key present in one file is present in the other
- AND no key resolves to an empty or placeholder value in either file

### Requirement: Composable Strings Sourced from Resources

Every Composable listed in the exploration inventory (`BoardListScreen`, `CreateBoardScreen`, `GameSetupScreen`, `GamePlayScreen`, `ImportBoardsScreen`, `SettingsScreen`, `ThemeScreen`, `BingoNumberField`, `BingoGridDisplay`) MUST render its UI-facing text via `stringResource()` and MUST NOT contain hardcoded Spanish or English string literals for user-visible text or content descriptions.

#### Scenario: Board card identifier line renders from a resource

- GIVEN a board with id `7` and identifier `"Cartón A"`
- WHEN `BoardListScreen` renders that board's card
- THEN the displayed line matches the resource-backed format `"#%1$s · %2$s"` filled with `7` and `"Cartón A"`

#### Scenario: Bingo announcement renders from a resource

- GIVEN a win with `sequentialNumber = 3` and `identifier = "Cartón A"`
- WHEN `GamePlayScreen` renders the win announcement
- THEN the displayed text matches the resource-backed format filled with `3` and `"Cartón A"` (e.g. `"¡Bingo! #3 Cartón A"` in Spanish)

#### Scenario: Import result message renders from a resource

- GIVEN an import result with `imported = 1` and `skipped = 1`
- WHEN `ImportBoardsScreen` renders the result message
- THEN the displayed text matches the single 2-placeholder resource format filled with `1` and `1` (e.g. `"1 importados, 1 omitidos"` in Spanish, `"1 imported, 1 skipped"` in English)
- AND no `<plurals>` resource is used to produce this text

### Requirement: Deduplicated Shared Literals

The 5 "Atrás" content-description call sites MUST resolve to a single shared string resource, and the 2 "FREE" cell-label call sites MUST resolve to a single shared string resource. Neither MUST have a per-call-site duplicate resource key.

#### Scenario: All back-navigation content descriptions share one resource

- GIVEN the 5 screens/components that previously hardcoded "Atrás" as a content description
- WHEN each one renders its back-navigation affordance
- THEN each resolves to the same shared string resource key
- AND changing that key's value changes the content description everywhere at once

#### Scenario: Both FREE-cell labels share one resource

- GIVEN `BingoNumberField.kt` and `BingoGridDisplay.kt`, which both display the FREE cell label
- WHEN either renders the FREE cell
- THEN both resolve to the same shared string resource key
- AND the rendered label text is identical between the manual-entry grid and the display grid

### Requirement: ViewModel Reason Type Instead of Literal Strings

`CreateBoardViewModel`, `GamePlayViewModel`, and `ImportBoardsViewModel` MUST expose a reason/enum/sealed type in their `UiState` for every condition that previously produced a literal error or result string, instead of a `String` literal. The Composable layer MUST resolve that type to display text via `stringResource(R.string.x, ...)`. None of the three ViewModels MUST take a `Context` or `Application` dependency.

#### Scenario: Blank identifier still blocks board creation

- GIVEN `CreateBoardScreen` with an empty identifier field
- WHEN the user submits the form
- THEN `CreateBoardViewModel.uiState` exposes the "identifier blank" reason (not a literal string)
- AND the board is not persisted

#### Scenario: Duplicate identifier still blocks board creation

- GIVEN a board identifier that already exists in the repository
- WHEN the user submits a new board with that identifier
- THEN `CreateBoardViewModel.uiState` exposes the "identifier already exists" reason (not a literal string)

#### Scenario: Already-called number still blocks the call

- GIVEN a `GamePlayViewModel` session where number `42` was already called
- WHEN the user submits `42` again
- THEN `uiState.inputError`-equivalent field exposes the "number already called" reason (not the literal `"Número ya cantado"`)
- AND the call is not added to the called-numbers history

#### Scenario: Invalid number and letter-mismatch reasons are distinct

- GIVEN a `GamePlayViewModel` pending entry
- WHEN the user submits a non-numeric or out-of-range value, or an override letter that contradicts the number
- THEN `uiState` exposes a reason distinguishable from "number already called" for each of these two cases

#### Scenario: Import result and errors expose reason/data types

- GIVEN blank import text, malformed JSON, and a well-formed import respectively
- WHEN `ImportBoardsViewModel.onSubmit()` runs for each case
- THEN `uiState` exposes a distinct reason type for the blank-text case and the malformed-JSON case
- AND the well-formed case exposes the imported/skipped counts as data (not a pre-formatted literal string)

### Requirement: Locale-Driven Resource Resolution

The system MUST rely exclusively on Android's standard resource resolution (device locale → matching `values-XX/` → fallback to default `values/`) to select between Spanish and English text. No in-app language-switcher UI or persisted locale override MUST be introduced.

#### Scenario: English device locale renders English strings

- GIVEN the device locale is set to English
- WHEN any screen in the 12 affected files renders
- THEN every string renders from `values-en/strings.xml`

#### Scenario: Non-English device locale renders Spanish strings

- GIVEN the device locale is set to any locale other than English (or is unset)
- WHEN any screen in the 12 affected files renders
- THEN every string renders from the default `values/strings.xml` (Spanish), unchanged from pre-migration behavior

### Requirement: Unchanged Business Logic and Validation Behavior

This migration MUST NOT alter existing validation rules, error-trigger conditions, or business logic in any of the 12 affected files or their ViewModels. Only the representation of user-facing text changes.

#### Scenario: Validation triggers are identical before and after migration

- GIVEN the same user input sequence applied to `CreateBoardViewModel`, `GamePlayViewModel`, or `ImportBoardsViewModel` before and after the migration
- WHEN each ViewModel processes that input
- THEN the same conditions trigger an error/reason state in both versions
- AND no additional or removed validation branch exists post-migration

### Requirement: Pinned Unit Tests Assert on Reason Types

`GamePlayViewModelTest.kt` (previously asserting literal text at line 186) and `ImportBoardsViewModelTest.kt` (previously asserting literal text at line 86) MUST assert against the new reason/enum/data type exposed by `uiState`, not against literal display text.

#### Scenario: GamePlayViewModelTest asserts on the reason type

- GIVEN the test scenario that previously called `assertEquals("Número ya cantado", state.inputError)`
- WHEN the test is updated for this migration
- THEN it asserts the "number already called" reason value, not a literal string

#### Scenario: ImportBoardsViewModelTest asserts on the reason/data type

- GIVEN the test scenario that previously called `assertEquals("1 importados, 1 omitidos", state.resultMessage)`
- WHEN the test is updated for this migration
- THEN it asserts on the imported/skipped counts (or equivalent result data), not a literal formatted string

#### Scenario: Full unit test suite stays green

- GIVEN the completed migration with both pinned tests updated
- WHEN the JVM unit test suite is run
- THEN all tests pass, including the two updated assertions and every previously-passing test
