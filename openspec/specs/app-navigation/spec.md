# App Navigation Specification

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

### Requirement: Home Top App Bar

`BoardListScreen`'s `Scaffold` MUST include a `topBar` that is a `CenterAlignedTopAppBar` showing only the app name as its title — no navigation icon, no action icons. The bar's container color MUST change via `TopAppBarDefaults` scroll behavior as the board list scrolls beneath it. `WindowInsets(0,0,0,0)` conventions on the screen's `Scaffold` MUST remain intact.

#### Scenario: Top app bar renders title only

- GIVEN the user opens the board list (home) screen
- WHEN the screen renders
- THEN a `CenterAlignedTopAppBar` is visible showing the app name
- AND no navigation icon or action icons are present

#### Scenario: Container color shifts on scroll

- GIVEN the board list has enough entries to scroll
- WHEN the user scrolls the list up beneath the top app bar
- THEN the top app bar's container color changes per `TopAppBarDefaults` scroll behavior

#### Scenario: Color resets at the top of the list

- GIVEN the top app bar has changed color from scrolling
- WHEN the user scrolls back to the very top of the list
- THEN the top app bar returns to its resting container color

## MODIFIED Requirements

### Requirement: Home Screen Bottom Navigation Bar

`BoardListScreen` MUST render its content inside a Material3 `Scaffold` whose `bottomBar` is a single 3-slot row containing exactly 3 items in this order: "Configuración" (left, navigates to the Configuración screen), "Jugar" (center, visually prominent/primary), and "Agregar" (right). The inner `Scaffold`'s `innerPadding` MUST be the only padding applied to content; it MUST NOT double-apply system bar insets already consumed by the outer `Scaffold` in `MainActivity`.
(Previously: exactly 2 items — "Agregar" and "Configuración" — with "Jugar" rendered separately as a full-width button above the list. "Configuración" was inert with no navigation.)

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
- AND the previous inert behavior (no navigation, no side effect) no longer occurs

### Requirement: Board List Content Rendering

The scrollable list of saved boards MUST render each entry with its identifier, sequential number, full grid preview, and a divider between entries. The list scrolls beneath the top app bar and above the 3-slot bottom bar; it no longer coexists with a separate top full-width "Jugar" button.
(Previously: described the list as scrolling independently of a separate full-width "Jugar" button positioned above it.)

#### Scenario: Board list entries remain unchanged

- GIVEN one or more boards have been created
- WHEN the board list screen renders
- THEN each entry shows its identifier, sequential number, full grid, and a divider
- AND the list scrolls independently of the top app bar and bottom bar

## REMOVED Requirements

### Requirement: Full-Width Play Action

(Reason: the "Jugar" action is consolidated into the center slot of the new single 3-slot bottom bar; a separate full-width button above the list added unnecessary visual weight that competed with the list.)
(Migration: "Jugar" functionality and its `GameSetupScreen` navigation target move to the bottom bar's center slot; no functionality is lost, only presentation and location change.)
