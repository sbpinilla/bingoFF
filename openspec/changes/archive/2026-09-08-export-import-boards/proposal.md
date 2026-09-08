# Proposal: Export & Import Boards

## Intent

Boards live only in the local Room database with no way to move them between devices/installs or back them up. `BoardListScreen`'s "Configuración" bottom-bar item is already present but inert (`onClick = {}`). This change gives that item a real destination and lets users export their boards as JSON (via Android's native share sheet) and import a previously exported JSON payload, so boards survive a reinstall or move across devices.

## Scope

### In Scope
- New "Configuración" screen (own `TopAppBar` + back nav) with exactly 2 items: "Exportar", "Importar".
- Wire the inert bottom-bar `IconButton(onClick = {})` in `BoardListScreen.kt` to navigate to this screen.
- Export: serialize all boards to JSON (Gson) and fire `Intent.ACTION_SEND` (`EXTRA_TEXT`) via `Intent.createChooser` — no `FileProvider`, no manifest changes.
- Import screen (own `TopAppBar` + back nav): paste-JSON textarea + submit button.
- Import logic: parse JSON, skip boards whose `id` OR `identifier` already exists in the DB (silent skip, no error), insert the rest preserving their exported `id`.
- Parse/format error: clear the textarea and set `isError = true` (nullable `jsonError: String?` in `UiState`), matching `CreateBoardViewModel`'s convention.
- New Gson dependency in `libs.versions.toml`/`app/build.gradle.kts`.
- `BoardDao`/`BoardRepository`/`RoomBoardRepository` extension: an `importBoards(boards): ImportResult`-shaped bulk operation checking existing ids/identifiers before insert.
- ADDED (nice-to-have, droppable): post-import "N importados, M omitidos" result summary in the Import screen.

### Out of Scope
- `FileProvider`/real-file export — plain-text share only.
- Export format versioning/migration.
- Compose UI test harness — none exists project-wide; manual/device smoke test only, per standing convention.
- Editing/deleting boards from the new screens.

## Capabilities

### New Capabilities
- `board-export-import`: export all boards to a shareable JSON payload; import boards from pasted JSON with id/identifier dedup and a Configuración/Import screen pair.

### Modified Capabilities
- `app-navigation`: the inert "Configuración" bottom-bar item now navigates to a new Configuración screen; 2 new routes (Configuración, Import) added to `BingoRoute`/`BingoNavHost`.

## Approach

Gson for JSON (pure-JVM, preserves the existing plain-JUnit test convention, no new Gradle plugin). A `BoardExportDto(id, identifier, numbers)` mirrors `BoardCard` 1:1 for a directly round-trippable schema. Export builds the JSON in the ViewModel/use-case layer (testable without Android framework classes) and triggers the share `Intent` from the Composable via `LocalContext.current`. Import extends `BoardDao` with a bulk insert plus existing-id/existing-identifier lookup queries, exposed through `BoardRepository.importBoards()`; Room accepts an explicit non-zero PK on `@Insert` when it doesn't collide, so exported ids are preserved. New screens replicate `GameSetupScreen`'s `Scaffold(topBar = TopAppBar(...))` + zeroed-inset convention. Routes follow the existing plain `const val` pattern (no `@Serializable` routes).

## Affected Areas

| Area | Impact | Description |
|------|--------|--------------|
| `data/local/dao/BoardDao.kt` | Modified | Bulk insert + exists-by-id/identifier queries |
| `domain/repository/BoardRepository.kt` / `data/repository/RoomBoardRepository.kt` | Modified | `exportBoards()`/`importBoards()` |
| `ui/boards/list/BoardListScreen.kt` | Modified | Wire Configuración `onClick` |
| `ui/navigation/BingoRoute.kt` / `BingoNavHost.kt` | Modified | 2 new routes |
| `ui/boards/settings/` (new) | New | `SettingsScreen` (Exportar/Importar list) |
| `ui/boards/importexport/` (new) | New | `ImportBoardsScreen` + ViewModel + UiState |
| `app/build.gradle.kts`, `gradle/libs.versions.toml` | Modified | Add Gson |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|--------------|
| Room explicit-PK insert-with-autoGenerate semantics behave unexpectedly | Low | Confirm via unit test against in-memory/fake DAO before relying on it |
| Diff spans data/domain/ui, likely exceeds 400-line review budget | High | Flag for `sdd-tasks` to chain/slice PRs; `ask-on-risk` strategy |
| No Compose UI test infra for new screens | Med | Manual/device smoke test, per standing convention |
| Malformed JSON with partial valid entries | Low | Fail the whole parse (clear + error) rather than partial-import ambiguity |

## Rollback Plan

Revert `BoardDao.kt`/`BoardRepository.kt`/`RoomBoardRepository.kt` changes, delete `ui/boards/settings/` and `ui/boards/importexport/`, remove the 2 new routes, restore `onClick = {}`, and drop the Gson dependency. No schema migration needed — `BoardEntity` itself is unchanged.

## Dependencies

- Gson (new runtime dependency, pure-JVM).

## Success Criteria

- [ ] Tapping "Configuración" opens the new screen with Exportar/Importar.
- [ ] Exportar opens Android's native share sheet with valid JSON in `EXTRA_TEXT`.
- [ ] Importar accepts pasted JSON, skips existing id/identifier boards silently, imports the rest.
- [ ] Invalid JSON clears the textarea and shows the red-error state.
- [ ] `./gradlew build` and full unit suite pass with no regressions.
