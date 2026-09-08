# Delta for App Navigation

## ADDED Requirements

### Requirement: Configuración and Importar Navigation Routes

`BingoRoute` MUST define two new route constants — Configuración and Importar — following the existing plain `const val` pattern. `BingoNavHost` MUST register a composable destination for each route.

#### Scenario: Navigating to the Configuración route renders the Configuración screen

- GIVEN the app's navigation graph is set up
- WHEN the Configuración route is navigated to
- THEN the Configuración screen composable renders

#### Scenario: Navigating to the Importar route renders the Import screen

- GIVEN the app's navigation graph is set up
- WHEN the Importar route is navigated to
- THEN the Import screen composable renders

#### Scenario: Back navigation returns to the previous screen

- GIVEN the user is on the Configuración screen after navigating from the board list
- WHEN the user taps the back navigation icon
- THEN the app returns to the board list screen
- AND the same behavior applies from the Import screen back to the Configuración screen

## MODIFIED Requirements

### Requirement: Home Screen Bottom Navigation Bar

`BoardListScreen` MUST render its content inside a Material3 `Scaffold` whose `bottomBar` is a single 3-slot row containing exactly 3 items in this order: "Configuración" (left, navigates to the Configuración screen), "Jugar" (center, visually prominent/primary), and "Agregar" (right). The inner `Scaffold`'s `innerPadding` MUST be the only padding applied to content; it MUST NOT double-apply system bar insets already consumed by the outer `Scaffold` in `MainActivity`.
(Previously: "Configuración" was a literal no-op `IconButton(onClick = {})` with no navigation target.)

#### Scenario: Bottom bar renders with three items

- GIVEN the user opens the board list (home) screen
- WHEN the screen renders
- THEN the bottom bar contains exactly 3 items: "Configuración", "Jugar", "Agregar"
- AND "Jugar" is visually distinct/prominent compared to the other two

#### Scenario: "Agregar" navigates to board creation

- GIVEN the board list screen is visible with the bottom bar
- WHEN the user taps "Agregar"
- THEN the app navigates to `CreateBoardScreen`

#### Scenario: "Jugar" navigates to game setup from the bottom bar

- GIVEN the board list screen is visible with the bottom bar
- WHEN the user taps "Jugar"
- THEN the app navigates to `GameSetupScreen`

#### Scenario: "Configuración" navigates to the Configuración screen

- GIVEN the board list screen is visible with the bottom bar
- WHEN the user taps "Configuración"
- THEN the app navigates to the new Configuración screen
- AND the previous no-op behavior (no navigation, no side effect) no longer occurs

### Requirement: Board List Content Rendering

The scrollable list of saved boards MUST render each entry with its identifier, sequential number, full grid preview, and a divider between entries. The list scrolls beneath the top app bar and above the 3-slot bottom bar; it no longer coexists with a separate top full-width "Jugar" button.
(Previously: described the list as scrolling independently of a separate full-width "Jugar" button positioned above it.)

#### Scenario: Board list entries remain unchanged

- GIVEN one or more boards have been created
- WHEN the board list screen renders
- THEN each entry shows its identifier, sequential number, full grid, and a divider
- AND the list scrolls independently of the top app bar and bottom bar
