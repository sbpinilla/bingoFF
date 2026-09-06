# BingoFF

Aplicación Android nativa para gestionar múltiples cartones de bingo durante una partida real: registrás los cartones físicos, elegís la modalidad de juego, vas cargando los números que se cantan y la app te avisa automáticamente qué cartón ganó.

## Stack técnico

| Componente | Versión |
|---|---|
| Kotlin | 2.2.10 |
| Android Gradle Plugin | 9.4.0 |
| Jetpack Compose (BOM) | 2026.02.01 |
| Hilt (inyección de dependencias) | 2.60.1 |
| Room (persistencia local) | 2.7.2 |
| Navigation Compose | 2.9.5 |
| KSP | 2.2.10-2.0.2 |

> Nota de toolchain: AGP 9.4 trae su propio soporte de Kotlin embebido, así que el proyecto **no aplica el plugin explícito de Kotlin** (`org.jetbrains.kotlin.android`) — KSP se engancha directo sobre ese Kotlin embebido con el flag experimental `android.disallowKotlinSourceSets=false`. Es una decisión de diseño documentada, no un descuido.

## Arquitectura

MVVM sobre una única app module (`:app`), con capas separadas por responsabilidad:

```
com.sergiodev.bingo
├── domain/              # Reglas de negocio puras, sin Android ni Room
│   ├── model/           # BingoLetter, BoardCard, GridPosition, GameMode, WinPattern
│   ├── game/            # BingoWinChecker, GameSession, AnnouncedWin, WinAnnouncement
│   └── repository/      # BoardRepository (puerto/interfaz)
├── data/                # Implementación de persistencia
│   ├── local/           # Room: BoardEntity, BoardDao, BingoDatabase, converters
│   └── repository/      # RoomBoardRepository (adaptador del puerto de dominio)
├── di/                  # Módulos de Hilt (DatabaseModule, RepositoryModule)
├── ui/
│   ├── common/          # BingoNumberField y BingoFreeCell (componentes compartidos)
│   ├── boards/          # Pantallas de registro (create) y listado (list) de cartones
│   ├── game/            # Pantallas de configuración (setup) y juego (play)
│   ├── navigation/      # BingoNavHost + BingoRoute (grafo de navegación)
│   └── theme/           # Material3 theming
└── BingoApplication.kt  # @HiltAndroidApp
```

**Principios que se mantienen en todo el código:**

- Cada pantalla tiene su propio `@HiltViewModel` con un único `StateFlow<UiState>` inmutable — la UI solo lee, nunca muta directamente.
- `domain/` no importa nada de Room ni de Android — la detección de ganador es una función pura y testeable, independiente de cómo se persisten los datos.
- El estado "marcado" de una celda nunca se guarda: siempre se calcula como la intersección entre los números del cartón y los números cantados hasta el momento. Solo se trackean el historial de números cantados y qué combinaciones (cartón, patrón) ya fueron anunciadas como ganadoras, para no repetir un aviso.
- Los 5 modos de juego (ver abajo) se modelan como **un solo concepto**: cada modo es un conjunto de coordenadas sobre la grilla 5x5. Esto evita tener lógica separada por modo.

## Features implementadas

### 1. Registro y listado de cartones
- Alta de un cartón con un identificador (el número/etiqueta del cartón físico) y sus 24 números jugables (5x5 estándar: columnas B-I-N-G-O, con la casilla libre fija en el centro de la columna N).
- Los campos de número solo aceptan dígitos y máximo 2 caracteres (`BingoNumberField`, con una función pura `sanitizeNumberInput` totalmente testeada).
- Listado simple y scrolleable: cada cartón muestra su identificador y su grilla completa, sin buscador (se descartó a propósito, no hace falta por ahora).

### 2. Partida y modos de juego
Se elige un solo modo al iniciar la partida, y todos los cartones registrados participan:

| Modo | Condición de victoria |
|---|---|
| **Columna** | Se van cantando números hasta que cada una de las 5 columnas (B, I, N, G, O) tenga su ganador — pueden ganar cartones distintos, una columna a la vez |
| **O** | Marco/borde exterior de la grilla 5x5 |
| **L** | Columna B completa + fila inferior completa |
| **I** | Fila superior + columna central (N) + fila inferior |
| **Cartón completo** | Las 25 casillas marcadas (incluida la libre) |

Todos los modos son **acumulativos**: la partida sigue anunciando cada cartón nuevo que completa el patrón, sin repetir un aviso ya dado.

### 3. Carga de números en vivo
- Campo de número + selector de letra (B/I/N/G/O): la letra se autodetecta según el rango del número tipeado (B 1-15, I 16-30, N 31-45, G 46-60, O 61-75) pero se puede corregir a mano si el cantador se equivoca — si la corrección manual no coincide con el rango real del número, se bloquea el envío con un error.
- Debajo, una fila por letra muestra los números cantados hasta el momento, en orden.
- Cuando se detecta un ganador, aparece "¡Bingo!" junto al identificador del cartón ganador.
- Botón **"Terminar juego"** (destacado en rojo): pide confirmación antes de descartar la partida en curso y, al confirmar, vuelve a la lista de cartones dejando todo listo para una partida nueva.

## Cómo correr el proyecto

```bash
./gradlew clean build   # compila, corre lint y los tests unitarios
./gradlew test          # solo tests unitarios (JVM)
./gradlew connectedDebugAndroidTest   # tests instrumentados (requiere emulador/dispositivo)
```

## Estado del proyecto y deuda técnica conocida

- `fallbackToDestructiveMigration` sigue habilitado en Room — hay que sacarlo antes de cualquier release real con cartones ya cargados por usuarios (hoy borra la base ante cualquier cambio de schema).
- No hay lint/detekt que fuerce que `ui/` no importe nada de `data/` — hoy esa regla se verifica a mano (grep).
- No hay infraestructura de tests de UI en Compose (`createComposeRule`) — el flujo de pantallas se valida con pruebas manuales en dispositivo.
- Import/exportación de cartones, historial de partidas jugadas y edición/borrado de cartones quedaron fuera de alcance a propósito, pendientes para una futura iteración.

## Historial de desarrollo

Este proyecto se construyó siguiendo un flujo de Spec-Driven Development (SDD): cada feature pasó por exploración, propuesta, spec, diseño técnico, implementación con TDD, verificación y archivo, en tres iteraciones:

1. **Fundación MVVM** — ViewModel + StateFlow, Hilt y Room integrados sobre el template base de Android Studio.
2. **Bingo game tracker** — el modelo de dominio del juego completo: cartones, modos, detección de ganador y navegación entre pantallas.
3. **Pulido de inputs** — componentes de entrada de número compartidos y el flujo de "terminar juego".

## Licencia

MIT — ver [LICENSE](LICENSE).
