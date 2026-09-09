# Board Export & Import Specification

## Purpose

Let users export their locally persisted boards as a shareable JSON payload and re-import a previously exported payload, so boards survive a reinstall or move across devices.

## Requirements

### Requirement: Export All Boards as JSON

The system MUST serialize every persisted board into a JSON array using Gson, with each element mirroring `BoardCard` 1:1 (`id`, `identifier`, `numbers`). Tapping "Exportar" MUST trigger `Intent.ACTION_SEND` (`EXTRA_TEXT` = the JSON string) via `Intent.createChooser`, with no `FileProvider` and no manifest changes.

#### Scenario: Exporting with zero boards

- GIVEN the board list is empty
- WHEN the user triggers export
- THEN the produced JSON is a valid empty array
- AND the share sheet still opens with that JSON in `EXTRA_TEXT`

#### Scenario: Exporting with one board

- GIVEN exactly one board is persisted
- WHEN the user triggers export
- THEN the produced JSON is a single-element array matching that board's `id`, `identifier`, and `numbers`

#### Scenario: Exporting with N boards

- GIVEN N boards are persisted (N > 1)
- WHEN the user triggers export
- THEN the produced JSON is an array of exactly N objects, one per board

#### Scenario: Export triggers the native share sheet

- GIVEN one or more boards are persisted
- WHEN the user taps "Exportar"
- THEN `Intent.createChooser` opens the native Android share sheet
- AND the underlying `ACTION_SEND` intent carries the JSON string in `EXTRA_TEXT`

### Requirement: JSON Schema Round-Trip Compatibility

The exported JSON schema MUST be directly consumable as valid import input, with no transformation required between export output and import input.

#### Scenario: Exported JSON round-trips as import input

- GIVEN a set of boards has been exported to JSON
- WHEN that exact JSON string is pasted into the Import screen and submitted
- THEN every entry parses successfully as a valid board record

### Requirement: Import Well-Formed JSON

When submitted JSON parses successfully, the system MUST insert each entry whose `id` and `identifier` are both absent from the database, preserving the entry's original exported `id`.

#### Scenario: Importing all-new boards

- GIVEN a well-formed JSON array where no entry's `id` or `identifier` exists in the DB
- WHEN the user submits the JSON
- THEN every entry is inserted
- AND each inserted board keeps its original `id` from the JSON

### Requirement: Import Dedup by Existing ID

The system MUST silently skip any import entry whose `id` already exists in the database, without surfacing an error for that skip.

#### Scenario: Some entries have a duplicate id

- GIVEN a well-formed JSON array where some entries' `id` values already exist in the DB
- WHEN the user submits the JSON
- THEN entries with a duplicate `id` are skipped without error
- AND the remaining, non-duplicate entries are inserted

### Requirement: Import Dedup by Existing Identifier

The system MUST silently skip any import entry whose `identifier` already exists in the database under a different `id`, preventing a unique-index `SQLiteConstraintException`, without surfacing an error for that skip.

#### Scenario: Some entries have a duplicate identifier under a different id

- GIVEN a well-formed JSON array where some entries have a new `id` but an `identifier` already used by a different existing board
- WHEN the user submits the JSON
- THEN entries with a duplicate `identifier` are skipped without error
- AND the remaining, non-duplicate entries are inserted

### Requirement: Malformed JSON Import Handling

If the submitted text fails to parse as valid JSON matching the expected schema, the system MUST fail the entire import (no partial import), clear the textarea content, and set the field to an error state (`isError = true`, nullable `jsonError: String?`).

#### Scenario: Submitting malformed JSON

- GIVEN the user pastes text that is not valid JSON, or valid JSON that does not match the expected board schema
- WHEN the user submits it
- THEN no boards are imported
- AND the textarea is cleared
- AND the field shows the red-error state with a non-null `jsonError`

#### Scenario: Submitting empty or blank content

- GIVEN the textarea is empty or contains only whitespace
- WHEN the user submits it
- THEN no boards are imported
- AND the field shows the red-error state with a non-null `jsonError`

### Requirement: Configuración Screen

The system MUST provide a dedicated Configuración screen with its own `TopAppBar` and back navigation, listing exactly 3 items — "Tema", "Exportar", and "Importar", in that order — each shown with an icon.
(Previously: listed exactly 2 items — "Exportar" and "Importar" — with no "Tema" item.)

#### Scenario: Configuración lists exactly three items with icons in order

- GIVEN the user opens the Configuración screen
- WHEN the screen renders
- THEN exactly 3 items are visible, in this order: "Tema", "Exportar", "Importar"
- AND each item shows an icon

#### Scenario: Tapping "Tema" navigates to the theme selection screen

- GIVEN the user is on the Configuración screen
- WHEN the user taps "Tema"
- THEN the app navigates to the theme selection screen

#### Scenario: Tapping "Exportar" triggers export

- GIVEN the user is on the Configuración screen
- WHEN the user taps "Exportar"
- THEN the export JSON is built and the native share sheet opens

#### Scenario: Tapping "Importar" navigates to the Import screen

- GIVEN the user is on the Configuración screen
- WHEN the user taps "Importar"
- THEN the app navigates to the Import screen

### Requirement: Import Screen

The system MUST provide a dedicated Import screen with its own `TopAppBar` and back navigation, containing a paste-JSON textarea and a submit button.

#### Scenario: Import screen renders its input elements

- GIVEN the user navigates to the Import screen
- WHEN the screen renders
- THEN a textarea for pasting JSON and a submit button are visible

#### Scenario: Submitting runs the import logic

- GIVEN the user has pasted content into the textarea
- WHEN the user taps the submit button
- THEN the pasted content is parsed and processed per the import requirements above

### Requirement: Post-Import Result Summary

The system SHOULD display a lightweight summary after an import completes, stating the count of imported and skipped boards (e.g. "N importados, M omitidos"). This is a nice-to-have and MAY be omitted without affecting the other import requirements.

#### Scenario: Summary shown after import

- GIVEN an import has just completed with some boards inserted and some skipped
- WHEN the import finishes processing
- THEN a summary is displayed stating the number imported and the number omitted
