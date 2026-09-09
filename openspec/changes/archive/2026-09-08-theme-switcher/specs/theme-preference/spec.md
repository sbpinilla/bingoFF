# Theme Preference Specification

## Purpose

Let users explicitly override the app's Light/Dark appearance instead of always following the OS setting, while preserving today's system-following behavior as the default for anyone who never opens the setting.

## Requirements

### Requirement: Persisted Theme Mode Default

The system MUST expose a 3-state `ThemeMode` (`LIGHT`, `DARK`, `SYSTEM`) backed by a DataStore Preferences-backed `ThemeRepository`. On first launch, with no prior selection persisted, the effective mode MUST be `SYSTEM`.

#### Scenario: First launch with no prior selection defaults to Sistema

- GIVEN the app has never been opened before and no theme preference is persisted
- WHEN the app launches
- THEN the effective theme mode is `SYSTEM`
- AND the app's appearance follows `isSystemInDarkTheme()`

### Requirement: Selecting Claro Persists and Applies Immediately

Selecting "Claro" on the theme selection screen MUST persist `ThemeMode.LIGHT` via `ThemeRepository` and MUST apply the light appearance app-wide immediately, without an app restart.

#### Scenario: Selecting Claro switches appearance immediately

- GIVEN the user is on the theme selection screen
- WHEN the user selects "Claro"
- THEN `ThemeMode.LIGHT` is persisted
- AND the app's appearance switches to light immediately, with no restart required

### Requirement: Selecting Oscuro Persists and Applies Immediately

Selecting "Oscuro" on the theme selection screen MUST persist `ThemeMode.DARK` via `ThemeRepository` and MUST apply the dark appearance app-wide immediately, without an app restart.

#### Scenario: Selecting Oscuro switches appearance immediately

- GIVEN the user is on the theme selection screen
- WHEN the user selects "Oscuro"
- THEN `ThemeMode.DARK` is persisted
- AND the app's appearance switches to dark immediately, with no restart required

### Requirement: Selecting Sistema Reverts to Following the OS Setting

Selecting "Sistema" on the theme selection screen MUST persist `ThemeMode.SYSTEM` via `ThemeRepository` and MUST make the app's appearance follow `isSystemInDarkTheme()` immediately, without an app restart.

#### Scenario: Selecting Sistema reverts to OS-driven appearance

- GIVEN the user previously selected "Claro" or "Oscuro"
- WHEN the user selects "Sistema"
- THEN `ThemeMode.SYSTEM` is persisted
- AND the app's appearance immediately follows `isSystemInDarkTheme()` instead of the prior explicit choice

### Requirement: Theme Choice Survives App Restart

The selected `ThemeMode` MUST be persisted such that it survives an app process restart, and MUST be the effective mode the next time the app launches.

#### Scenario: Restarting the app preserves the last selection

- GIVEN the user selected "Oscuro" (or "Claro" or "Sistema")
- WHEN the app process is killed and relaunched
- THEN the app's appearance matches the previously selected mode
- AND no further user action is required to restore it

### Requirement: Theme Selection Screen Shows Three Options

The system MUST provide a dedicated `ThemeScreen` with its own `TopAppBar` and back navigation, listing exactly 3 `RadioButton` options — "Claro", "Oscuro", "Sistema" — with the option matching the currently persisted `ThemeMode` shown as selected.

#### Scenario: Theme screen lists exactly three radio options

- GIVEN the user opens the theme selection screen
- WHEN the screen renders
- THEN exactly 3 `RadioButton` items are visible: "Claro", "Oscuro", "Sistema"

#### Scenario: The active theme option is pre-selected

- GIVEN the persisted `ThemeMode` is `DARK`
- WHEN the theme selection screen renders
- THEN the "Oscuro" `RadioButton` is shown as selected
- AND "Claro" and "Sistema" are shown as not selected

### Requirement: App-Wide Reactive Theme Application

`MainActivity` MUST collect the persisted `ThemeMode` reactively from `ThemeRepository`, resolve it to the `darkTheme: Boolean` passed into `BingoFFTheme`, and re-render the entire app's appearance whenever the persisted mode changes, without requiring an app restart.

#### Scenario: MainActivity re-renders on a theme change from another screen

- GIVEN the app is running with the theme selection screen open
- WHEN the user selects a different `ThemeMode`
- THEN `MainActivity` recomposes with the newly resolved `darkTheme` value
- AND every visible screen reflects the new appearance without restarting the app
