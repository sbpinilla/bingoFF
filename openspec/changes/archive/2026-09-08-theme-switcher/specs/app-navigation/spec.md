# Delta for App Navigation

## ADDED Requirements

### Requirement: Theme Navigation Route

`BingoRoute` MUST define a new `THEME` route constant, following the existing plain `const val` pattern already used for the Configuración and Importar routes. `BingoNavHost` MUST register a composable destination for the `THEME` route pointing at the theme selection screen.

#### Scenario: Navigating to the Theme route renders the theme selection screen

- GIVEN the app's navigation graph is set up
- WHEN the `THEME` route is navigated to
- THEN the theme selection screen composable renders with its own `TopAppBar`

#### Scenario: Back navigation from the theme selection screen returns to Configuración

- GIVEN the user navigated to the theme selection screen from the Configuración screen
- WHEN the user taps the back navigation icon
- THEN the app returns to the Configuración screen
