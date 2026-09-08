# Design: Export & Import Boards

Proposal: Engram `sdd/export-import-boards/proposal` (#357) / `openspec/changes/export-import-boards/proposal.md`.

## Technical Approach

Gson (pure-JVM, no new Gradle plugin) serializes/deserializes a bare JSON array of `BoardExportDto` records isolated in one new `data/json/BoardJsonCodec.kt`. Export flows `RoomBoardRepository.observeBoards()` (unchanged) → `SettingsViewModel` (new, first() snapshot) → `BoardJsonCodec.encode` → `Intent.ACTION_SEND` fired from the Composable via `LocalContext.current`. Import flows pasted text → `BoardJsonCodec.decode` (whole-payload try/catch, no partial recovery) → new `BoardRepository.importBoards()` → `BoardDao` bulk-insert with a 2-query existing-id/identifier prefetch (no N+1). Both new screens (`SettingsScreen`, `ImportBoardsScreen`) replicate `GameSetupScreen`'s exact zeroed-inset `Scaffold`/`TopAppBar` chrome. `BoardEntity`, `BoardCard`, and all existing `BoardRepository`/`RoomBoardRepository`/`BoardDao` methods are unchanged — additive only.

## Architecture Decisions

### Decision: JSON schema — bare top-level array, not `{"boards": [...]}`

| Option | Tradeoff | Decision |
|---|---|---|
| Bare `[{...}, {...}]` | Simplest `TypeToken<List<BoardExportDto>>`; no future-metadata slot | **Chosen** — versioning is explicitly out of scope; YAGNI |
| `{"boards": [...], "version": 1}` | Extensible, but adds a wrapper type nobody consumes yet | Rejected |

`BoardExportDto(id: Long, identifier: String, numbers: List<Int>)` mirrors `BoardCard`'s property names exactly, so Gson's default reflection needs no `@SerializedName`.

### Decision: DTO/codec isolated in `data/json/`, not reusing `BoardCard` directly

| Option | Tradeoff | Decision |
|---|---|---|
| New `BoardExportDto` + `BoardJsonCodec` object in `data/json/` | One extra small file; keeps Gson entirely out of `domain/` | **Chosen** |
| Serialize `BoardCard` directly with Gson | Zero extra file, but `numberAt()` gets reflected over harmlessly and any future domain-only field silently leaks into the export format | Rejected |

### Decision: Gson exception handling — catch broadly, not just `JsonSyntaxException`

Gson bypasses Kotlin constructor validation via `Unsafe`-based reflection: a missing/wrong-typed JSON field silently produces `null` in a non-null Kotlin property instead of throwing at parse time. The resulting `NullPointerException` surfaces later (e.g. on `.map`), not as `JsonSyntaxException`. `ImportBoardsViewModel.onSubmit()` therefore wraps decode in `catch (e: Exception)`, matching the proposal's "fail the whole parse" rule (item 7) with one `try/catch`, no per-board recovery.

### Decision: Bulk import — 2 prefetch queries + in-memory dedup, not N+1

| Option | Tradeoff | Decision |
|---|---|---|
| `SELECT id FROM board` + `SELECT identifier FROM board` once, filter in Kotlin | 2 queries total regardless of payload size | **Chosen** |
| Per-candidate `EXISTS` query | N+1 query cost | Rejected |

`RoomBoardRepository.importBoards()` also accumulates seen ids/identifiers *within* the incoming batch (not just against the DB), so a payload containing two boards with the same id skips the second — the proposal only specifies DB-existing dedup, but leaving in-batch duplicates unhandled would let `insertAll` throw `SQLiteConstraintException` on the entity's own unique index mid-batch.

### Decision: Explicit non-zero PK with `autoGenerate = true` is safe — confirmed, not assumed

Room maps `@PrimaryKey(autoGenerate = true)` on a `Long` to SQLite `INTEGER PRIMARY KEY`. Room omits the column from the generated INSERT only when the field's value is exactly `0`; a non-zero value is inserted literally. SQLite accepts any unique explicit rowid, and `OnConflictStrategy.ABORT` throws `SQLiteConstraintException` on a real collision instead of silently overwriting. This is standard, well-documented Room/SQLite behavior, not a hidden risk — but per the proposal's own risk item, `BoardDaoTest.kt` (existing instrumented test, real in-memory Room DB) gets 2 new cases: explicit-PK insert survives + a later regular `insert()` still autoincrements past the imported max id without colliding.

## Data Flow

    BoardListScreen (bottom bar) ──onClick──→ nav(SETTINGS)
                                                    │
    SettingsScreen ──Exportar──→ SettingsViewModel.exportJson()
         │                            └─ repository.observeBoards().first() → BoardJsonCodec.encode → JSON
         │                                                                          │
         │                                                    Intent.ACTION_SEND (EXTRA_TEXT) ← Composable/LocalContext
         └──Importar──→ nav(IMPORT_BOARDS)

    ImportBoardsScreen (textarea + submit) ──onSubmit──→ ImportBoardsViewModel
         │                                                    ├─ BoardJsonCodec.decode(text)  [catch Exception → jsonError]
         │                                                    └─ repository.importBoards(list)
         │                                                            └─ BoardDao.getAllIds() + getAllIdentifiers() (prefetch)
         │                                                               → filter/dedup in Kotlin → BoardDao.insertAll(new)
         └── UiState.resultMessage ("N importados, M omitidos") → Snackbar

## File Changes

| File | Action | Description |
|---|---|---|
| `data/json/BoardJsonCodec.kt` | Create | `BoardExportDto` + `encode(List<BoardCard>): String` / `decode(String): List<BoardCard>` (Gson) |
| `data/local/dao/BoardDao.kt` | Modify | + `getAllIds()`, `getAllIdentifiers()`, `insertAll(List<BoardEntity>)` |
| `domain/repository/BoardRepository.kt` | Modify | + `ImportResult(imported, skipped)` data class, + `importBoards(List<BoardCard>): ImportResult` |
| `data/repository/RoomBoardRepository.kt` | Modify | `importBoards()` impl: prefetch + in-batch dedup + `insertAll` |
| `ui/boards/list/BoardListScreen.kt` | Modify | Add `onNavigateToSettings` param; wire the inert `IconButton(onClick = {})` |
| `ui/navigation/BingoRoute.kt` | Modify | + `SETTINGS`, `IMPORT_BOARDS` route constants |
| `ui/navigation/BingoNavHost.kt` | Modify | + 2 `composable(...)` destinations, thread `onNavigateToSettings` |
| `ui/boards/settings/SettingsScreen.kt` | Create | Zeroed-inset `Scaffold`/`TopAppBar` + 2 `ListItem`s (Exportar/Importar) |
| `ui/boards/settings/SettingsViewModel.kt` | Create | `exportJson(onReady: (String) -> Unit)` |
| `ui/boards/importexport/ImportBoardsUiState.kt` | Create | `jsonText`, `jsonError: String?`, `resultMessage: String?` |
| `ui/boards/importexport/ImportBoardsViewModel.kt` | Create | `onJsonTextChange`, `onSubmit`, `onResultMessageShown` |
| `ui/boards/importexport/ImportBoardsScreen.kt` | Create | Zeroed-inset `Scaffold`/`TopAppBar`, `OutlinedTextField(isError=...)`, submit `Button`, `Snackbar` |
| `gradle/libs.versions.toml` | Modify | + `gson` version + library entry |
| `app/build.gradle.kts` | Modify | + `implementation(libs.gson)` |
| `app/src/androidTest/.../BoardDaoTest.kt` | Modify | + explicit-PK-preserved test, + post-import-autoincrement test |

**Rough LOC estimate**: ~250 new (6 new files) + ~90 modified (9 files) ≈ **340 authored lines**. Under the 400-line budget but tight — `sdd-tasks` should still slice DAO/repository work from UI/screen work into separate reviewable units in case the estimate runs over during implementation (icon/import-string literals, doc comments).

## Interfaces / Contracts

```kotlin
// data/json/BoardJsonCodec.kt
data class BoardExportDto(val id: Long, val identifier: String, val numbers: List<Int>)
object BoardJsonCodec {
    fun encode(boards: List<BoardCard>): String
    fun decode(json: String): List<BoardCard>  // throws on any structural/type error
}

// domain/repository/BoardRepository.kt (additive)
data class ImportResult(val imported: Int, val skipped: Int)
interface BoardRepository {
    // ...existing methods unchanged...
    suspend fun importBoards(boards: List<BoardCard>): ImportResult
}

// data/local/dao/BoardDao.kt (additive)
interface BoardDao {
    // ...existing methods unchanged...
    @Query("SELECT id FROM board") suspend fun getAllIds(): List<Long>
    @Query("SELECT identifier FROM board") suspend fun getAllIdentifiers(): List<String>
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertAll(boards: List<BoardEntity>)
}
```

## Testing Strategy

| Layer | What to Test | Approach |
|---|---|---|
| Unit (JVM) | `BoardJsonCodec.encode`/`decode` round trip; malformed JSON throws; missing-field NPE path | Plain JUnit, no Android/Robolectric needed (pure-JVM Gson) |
| Unit (JVM) | `RoomBoardRepository.importBoards()` dedup logic (existing id, existing identifier, in-batch duplicate) | Fake `BoardDao` test double, mirrors `CreateBoardViewModelTest` convention |
| Unit (JVM) | `ImportBoardsViewModel` sets `jsonError` on bad JSON, clears text, builds result message | Mirrors `CreateBoardViewModelTest` |
| Instrumented | Explicit non-zero PK survives `insertAll`; later `insert()` autoincrements past imported max id | Extend existing `BoardDaoTest.kt` (real in-memory Room DB) |
| Manual (device) | Configuración → Exportar opens share sheet with valid JSON; Importar accepts/red-borders JSON; end-to-end export→import round trip | Per standing project convention — no Compose UI test infra exists |

## Threat Matrix

N/A — no shell, subprocess, VCS/PR automation, executable-file classification, or external process integration. The 2 new destinations are ordinary in-app Compose Navigation routes (same pattern as the existing 4), and `Intent.ACTION_SEND` targets the OS share sheet with a plain-text extra, not a file handoff.

## Migration / Rollout

No migration required — `BoardEntity`/schema unchanged, new DAO methods are additive. Rollback: revert `BoardDao.kt`/`BoardRepository.kt`/`RoomBoardRepository.kt`, delete `ui/boards/settings/`, `ui/boards/importexport/`, `data/json/`, remove the 2 routes, restore `onClick = {}`, drop the Gson dependency lines, revert `BoardDaoTest.kt`.

## Open Questions

- [ ] Confirm exact Gson patch version against Maven Central at apply time (baseline: `2.11.0`+, any current 2.x is fine — pure reflection API, no breaking changes expected).
- [ ] Confirm `Icons.Default.Share` and `Icons.Default.FileDownload` resolve from the already-pinned `material-icons-core` at compile time (both standard core-set icons; low risk, same category as the prior change's `Check`/`PlayArrow` confirmation).
