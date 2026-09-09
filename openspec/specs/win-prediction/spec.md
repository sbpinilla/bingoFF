# Delta for win-prediction

## MODIFIED Requirements

### Requirement: Per-Mode Possible-Winner Qualifying Rule

A (board, pattern) pair MUST qualify for "Posibles ganadores" per `GameMode`: `COLUMNA` qualifies when `missing <= 2` AND total called numbers `<= MAX_PREDICTION_CALLS`; `O`, `L`, `I` qualify when their single pattern has `missing <= 3`, with NO call-count ceiling; `CARTON_COMPLETO` qualifies when the full-card pattern has `missing <= 10`, with NO call-count ceiling. The rule MUST be re-evaluated on every newly called number. FREE-cell counts differ by mode (O/L: none; I/Completo: one, at N row 3), so the calls needed to reach a given `missing` value differs by mode — expected, existing behavior, not a defect.
(Previously: only `COLUMNA` could ever qualify; every other mode returned an empty list unconditionally, regardless of progress or call count.)

#### Scenario: Columna behavior is unchanged (regression guard)
- GIVEN mode is COLUMNA, a board's column pattern has `missing = 2`, and total calls = 5
- WHEN prediction is computed
- THEN the (board, column) pair appears in the result, exactly as before this change

#### Scenario: O, L, or I board qualifies at exactly missing == 3
- GIVEN mode is O, L, or I and a board's pattern has exactly 3 real cells remaining uncalled
- WHEN prediction is computed
- THEN the board appears in the result

#### Scenario: O, L, or I board does not qualify at missing == 4
- GIVEN mode is O, L, or I and a board's pattern has exactly 4 real cells remaining uncalled
- WHEN prediction is computed
- THEN the board is absent from the result

#### Scenario: Cartón Completo board qualifies at exactly missing == 10
- GIVEN mode is CARTON_COMPLETO and a board has exactly 10 real cells remaining uncalled
- WHEN prediction is computed
- THEN the board appears in the result

#### Scenario: Cartón Completo board does not qualify at missing == 11
- GIVEN mode is CARTON_COMPLETO and a board has exactly 11 real cells remaining uncalled
- WHEN prediction is computed
- THEN the board is absent from the result

#### Scenario: No call-count ceiling and live recalculation for O, L, I, and Completo
- GIVEN mode is O, L, I, or CARTON_COMPLETO and total calls this session = 40
- WHEN a board's missing count first crosses its mode's threshold on call 40
- THEN the board appears in the result immediately, because these 4 modes apply no call-count ceiling and re-evaluate every call

### Requirement: Panel Hides Once the Call Ceiling Is Exceeded (Columna Only)

Once total called numbers exceed `MAX_PREDICTION_CALLS`, the system MUST stop computing and stop showing COLUMNA prediction entries for the rest of the session; the last computed state MUST NOT be frozen or displayed. This ceiling MUST NOT apply to `O`, `L`, `I`, or `CARTON_COMPLETO`.
(Previously: the ceiling applied to the only mode that could ever qualify, COLUMNA; no other mode ever reached a qualifying state to test it against.)

#### Scenario: Columna at exactly the ceiling
- GIVEN mode is COLUMNA and total calls = `MAX_PREDICTION_CALLS` (6)
- WHEN prediction is computed
- THEN qualifying entries are still returned normally

#### Scenario: Columna one call past the ceiling
- GIVEN mode is COLUMNA and total calls = `MAX_PREDICTION_CALLS + 1` (7)
- WHEN prediction is computed
- THEN the result is empty, even if boards still satisfy the missing-count threshold

### Requirement: Entry Format, Sorting, and Exclusion of Announced Wins

Each qualifying candidate MUST produce exactly one entry. COLUMNA entries MUST be formatted as `"Cartón <identifier> (<letra>)"`. O, L, I, and CARTON_COMPLETO entries MUST be formatted as `"Cartón <identifier>"`, with no parenthetical letter or mode name. Entries MUST be sorted fewest-missing-cells-first. A (board, pattern) pair already present in the announced-wins set MUST be excluded from predictions, even if it still numerically satisfies the qualifying rule — for every `GameMode`.
(Previously: format was always `"Cartón <identifier> (<letra>)"`, since only COLUMNA could qualify; exclusion of announced wins was only ever exercised for COLUMNA.)

#### Scenario: Columna entry format is unchanged
- GIVEN a qualifying pair for board "3" on column N
- WHEN the entry is rendered
- THEN its text is exactly `"Cartón 3 (N)"`

#### Scenario: Non-Columna entry format has no parenthetical
- GIVEN mode is O, L, I, or CARTON_COMPLETO and board "3" qualifies
- WHEN the entry is rendered
- THEN its text is exactly `"Cartón 3"`, with no letter and no mode name appended

#### Scenario: Sorted fewest-missing-first
- GIVEN two qualifying candidates, one missing 1 cell and one missing 2 cells
- WHEN the list is produced
- THEN the candidate missing 1 cell appears before the candidate missing 2 cells

#### Scenario: Already-announced win is excluded in every mode
- GIVEN a board has already won its mode's pattern (present in the announced-wins set) and still numerically satisfies the qualifying rule
- WHEN prediction is computed, for COLUMNA or for any of O, L, I, CARTON_COMPLETO
- THEN no entry for that (board, pattern) pair appears in the result

#### Scenario: Columna board qualifying on two columns produces two entries
- GIVEN board "5" simultaneously satisfies the qualifying rule on column B and column O
- WHEN prediction is computed
- THEN two separate entries appear: `"Cartón 5 (B)"` and `"Cartón 5 (O)"`
