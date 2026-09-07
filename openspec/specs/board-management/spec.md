# Delta for Board Management

## ADDED Requirements

### Requirement: Empty Board List State

`BoardListScreen` MUST render a shared `EmptyState` composable in place of the scrollable list when zero boards exist. The copy MUST be in Spanish and MUST communicate that no boards have been added (e.g. "Sin cartones agregados"). This composable MUST live in `ui/common/` alongside other shared composables.

#### Scenario: Empty state shown when no boards exist

- GIVEN no boards have been created
- WHEN the board list screen renders
- THEN the `EmptyState` composable is shown with Spanish copy indicating there are no boards
- AND no scrollable list, divider, or grid preview is rendered

#### Scenario: Empty state replaced once a board exists

- GIVEN the board list is currently showing the empty state
- WHEN the user creates the first board and returns to the board list
- THEN the empty state is no longer shown
- AND the scrollable list renders that board's entry
