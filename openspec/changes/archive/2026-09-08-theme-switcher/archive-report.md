# Archive Report: theme-switcher

**Change**: theme-switcher  
**Archive Date**: 2026-09-08  
**Archive Path**: `openspec/changes/archive/2026-09-08-theme-switcher/`  
**Artifact Store Mode**: hybrid (Engram + OpenSpec)

---

## Artifact Traceability

All required artifacts were retrieved from Engram during archive phase:

| Artifact | Observation ID | Retrieved | Status |
|----------|----------------|-----------|--------|
| Proposal | #453 | ✓ | Used for context |
| Spec | #459 | ✓ | Delta specs merged into main specs |
| Design | #465 | ✓ | Used for context |
| Tasks | #476 | ✓ | Task Completion Gate validated |
| Apply Progress | #482 | ✓ | Used for context |
| Verify Report | #488 | ✓ | Verdict PASS confirmed |

---

## Final-State Authority: Task Completion and Verification

### Task Completion Gate

Per Engram observation #476 (`sdd/theme-switcher/tasks`), all 24 implementation tasks are marked complete:

- Phase 1 (Dependencies): 3/3 ✓
- Phase 2 (Domain & Data): 2/2 ✓
- Phase 3 (Dependency Injection): 2/2 ✓
- Phase 4 (Theme Selection Screen UI): 3/3 ✓
- Phase 5 (Settings Integration): 2/2 ✓
- Phase 6 (Navigation): 2/2 ✓
- Phase 7 (Activity Wiring): 2/2 ✓
- Phase 8 (Tests): 5/5 ✓
- Phase 9 (Verification): 4/4 ✓

Manual smoke test (task 9.4) marked complete with expected-deferred note (consistent with all 12 prior archived changes; no adb/emulator in environment).

### Verification Verdict

Per Engram observation #488 (`sdd/theme-switcher/verify-report`):
- **Verdict**: PASS
- **Critical Issues**: 0
- **Warnings**: 1 (expected-deferred device smoke test — non-blocking)
- **Suggestions**: 0

Build gates independently re-run and green:
- `./gradlew testDebugUnitTest --rerun-tasks` → BUILD SUCCESSFUL, 128 tests (120 baseline + 8 new), 0 failures, 0 errors
- `./gradlew clean build` → BUILD SUCCESSFUL

Theme.kt hard constraint confirmed: zero byte diff vs. git.

---

## Capabilities Delivered

### NEW: theme-preference

A complete new capability providing users with explicit Light/Dark/System theme override via DataStore Preferences.

**Capability Spec**: `openspec/specs/theme-preference/spec.md` (NEW)

**Requirements** (7 total, all covered by tests or verified source):
1. Persisted Theme Mode Default — SYSTEM on first launch
2. Selecting Claro persists LIGHT and applies immediately
3. Selecting Oscuro persists DARK and applies immediately
4. Selecting Sistema reverts to OS-driven appearance
5. Theme choice survives app restart
6. Theme Selection Screen shows 3 RadioButton options
7. App-wide reactive theme application via MainActivity

**Implementation Evidence**:
- New domain/repository/ThemeRepository.kt (interface + ThemeMode enum)
- New data/repository/DataStoreThemeRepository.kt (DataStore Preferences backing)
- New di/PreferencesModule.kt (DI configuration)
- New ui/boards/theme/{ThemeUiState, ThemeViewModel, ThemeScreen}.kt (UI layer)
- New MainActivityViewModel.kt (reactive theme application)
- New tests: DataStoreThemeRepositoryTest.kt, ThemeViewModelTest.kt, MainActivityViewModelTest.kt
- Modified MainActivity.kt (collects themeMode, resolves to darkTheme, passes to BingoFFTheme)
- Modified gradle/libs.versions.toml, app/build.gradle.kts (2 new dependencies)

**Dependencies Added**:
- androidx.datastore:datastore-preferences (1.2.1, current stable) — NEW to project
- androidx.compose.material:material-icons-extended (BOM-managed, current) — NEW to project

### MODIFIED: board-export-import

Enhanced the Configuración screen to include a new "Tema" menu item as the first entry, navigating to the theme selection screen.

**Capability Spec**: `openspec/specs/board-export-import/spec.md` (MODIFIED)

**Delta Applied**:
- Updated "Configuración Screen" requirement: 2 items (Exportar, Importar) → 3 items (Tema, Exportar, Importar)
- Added scenario: tapping "Tema" navigates to theme selection screen
- All existing export/import requirements remain unchanged

**Implementation Evidence**:
- Modified SettingsScreen.kt: added "Tema" ListItem with Icons.Default.Palette, positioned first
- Modified SettingsViewModel.kt / caller: wired onNavigateToTheme callback

### MODIFIED: app-navigation

Added a new THEME navigation route alongside existing Configuración and Importar routes.

**Capability Spec**: `openspec/specs/app-navigation/spec.md` (MODIFIED)

**Delta Applied**:
- Added "Theme Navigation Route" requirement to ADDED Requirements section
- Defined BingoRoute.THEME constant and BingoNavHost composable destination
- Specified back navigation from theme screen returns to Configuración
- All existing navigation requirements remain unchanged

**Implementation Evidence**:
- Modified BingoRoute.kt: added const val THEME
- Modified BingoNavHost.kt: registered composable(BingoRoute.THEME) with back navigation
- Wired onNavigateToTheme from SettingsScreen

---

## Scope and Line Count

**Files Changed**: 9 modified, 6 new (15 total)
**Total Lines**: 434 (394 new-file + 40 modified-file)
- Exceeds 400-line budget by 34 lines; pre-accepted as `size:exception` before sdd-apply ran (Medium risk per tasks forecast)

**Regression Check**: git diff --stat confirmed zero unexpected changes to BoardRepository.kt, RoomBoardRepository.kt, BoardDao.kt, or unrelated screens.

---

## Correction History

An earlier `sdd-spec` run (obs #459 v1, not persisted downstream) mistakenly used the capability name `theme-switcher` (which is the CHANGE name) and omitted the `app-navigation` delta entirely. This was caught and corrected in a second `sdd-spec` run (obs #459 v2, persisted revision) before design/tasks/apply proceeded. No incorrect artifact reached implementation.

**Impact**: None — corrected before implementation started; design/tasks/apply all reference the correct capability names and deltas.

---

## Spec Merge Summary

| Spec | Action | Details |
|------|--------|---------|
| theme-preference | Created (NEW) | Mechanically copied from delta; 7 requirements for persisted 3-state theme selection |
| board-export-import | Updated (MODIFIED) | Replaced Configuración Screen requirement: 2 items → 3 items (added Tema first) |
| app-navigation | Updated (MODIFIED) | Added Theme Navigation Route requirement to ADDED section |

All merges completed via mechanical copy (NEW) or direct requirement replacement (MODIFIED), preserving all unchanged requirements.

---

## Archive Verification Checklist

- [x] Task Completion Gate: all 24 tasks marked complete
- [x] Verify Report: PASS with 0 CRITICAL issues
- [x] Verify Report: 1 non-blocking WARNING (expected-deferred device smoke test)
- [x] Verify Report: Build gates independently re-run, both green
- [x] Verify Report: Theme.kt hard constraint satisfied (zero diff)
- [x] Spec merges: theme-preference (NEW) created, board-export-import (MODIFIED) updated, app-navigation (MODIFIED) updated
- [x] Archive move: openspec/changes/theme-switcher → openspec/changes/archive/2026-09-08-theme-switcher
- [x] Source removed: openspec/changes/theme-switcher no longer exists
- [x] Archive contents verified: proposal, specs/, design, tasks, verify-report, apply-progress, exploration all present
- [x] No stale unchecked tasks in archived tasks.md
- [x] No truncation or alteration in archive (verified by successful move)

---

## Change Complete

The theme-switcher change has been fully implemented (24/24 tasks), verified (PASS), and archived. The three capabilities (theme-preference NEW, board-export-import MODIFIED, app-navigation MODIFIED) are now persisted in the source of truth (`openspec/specs/`) and ready for the next change.

**Archived by**: sdd-archive phase executor  
**Timestamp**: 2026-09-08 (ISO format, per skill directive)  
**SDD Cycle Status**: Complete ✓

---

## SDD Baseline Update Required

Update the project baseline context (`sdd-init/BingoFF`, Engram obs #1) to reflect this as the next capability entry. Include:
- NEW capability: `theme-preference`
- MODIFIED capabilities: `board-export-import`, `app-navigation`
- Dependency Foundation table: add androidx.datastore:datastore-preferences (1.2.1) and androidx.compose.material:material-icons-extended (BOM-managed)

This is the BingoFF project's 13th archived SDD change overall, and 4th OpenSpec-backed (hybrid) change after home-navigation-redesign, export-import-boards, and win-prediction-all-modes.
