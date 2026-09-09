# Delta for Board Export & Import

## MODIFIED Requirements

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
