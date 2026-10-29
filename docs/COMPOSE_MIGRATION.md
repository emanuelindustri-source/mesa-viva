# Migración de Mesa Viva a Jetpack Compose

## Situación actual

Mesa Viva es una app Android **Kotlin nativa con una mesa WebView residual**. La lógica de Póker y la PWA siguen en React/TypeScript dentro de `android/app/src/main/assets/www`. La app usa Kotlin para cámara, sensores, vibración, PDF, Material You y el relay LAN.

Desde **v1.3.0**, `MainActivity` es una cáscara nativa completa en Compose (inicio, catálogo, niveles, ajustes, sala y navegación) y `GameWebActivity` queda como respaldo de la PWA y la sala online. **Los quince juegos** son 100 % Compose: Serpientes y Escaleras, Dominó, Ajedrez, Ludo, Mancala, Backgammon, Damas, Dados Mentirosos, Go, Jungle, Xiangqi, Damas Chinas, Caída, Furia de Colores y Póker.

“Compose por completo” significa reescribir en Kotlin/Compose:

- Inicio, onboarding, catálogo, preparación, mesa, resultados, ajustes, niveles, sala, diagnóstico y PDF.
- Las 13 superficies de juego y sus animaciones.
- El estado de sesión, perfiles, historial y persistencia.
- Las salas host/invitado y la reconexión.
- La música, partículas, háptica y temas.

Es un proyecto de varias versiones, no un commit.

## Estrategia recomendada: híbrida por fases

### Fase 0 · Base técnica

- [x] Fijar versiones estables: Kotlin 2.0.21, Compose BOM 2024.12.01, activity-compose 1.9.3.
- [x] Añadir `ComponentActivity` + `setContent` + `AndroidView`.
- [x] Mantener el WebView actual como respaldo.
- [x] Actualizar Kotlin/AGP en v0.8.0: Kotlin 2.4.21, AGP 8.13.2, Gradle 8.13, Compose BOM 2025.10.00.
- [ ] Pruebas instrumentadas mínimas y CI de compilación.

### Fase 1 · Cáscara Compose (completada en v0.8.0)

- [x] `ComponentActivity` + `setContent`.
- [x] Inicio, catálogo, niveles y ajustes en Compose.
- [x] Navegación y back handling.
- [x] Tema Material 3, acento de festividad y superficies dinámicas.
- [x] `GameWebActivity` dedicada para la mesa.
- [x] Primer juego 100 % Compose: Serpientes y Escaleras.
- [ ] Lobby nativo y QR (v0.9.0).
- [ ] Paridad completa de los 11 idiomas en la cáscara (hoy es/en).

### Fase 2 · Juegos piloto (completada en v0.9.0)

- [x] **Serpientes y Escaleras** (tablero y dado).
- [x] **Dominó** (fichas, parejas, pozo y tranca).
- [x] **Ajedrez** (reglas completas, promoción e IA alfa-beta).
- [x] **Ludo** (fichas, capturas, seguros y meta).
- [x] **Mancala** (siembra, captura y turno extra).
- [x] **Backgammon** (barra, dobles y bear-off).
- [ ] Salas online nativas para los juegos Compose (v1.3.0).

- Motores compartidos entre web y Compose: extraer la lógica a un contrato/JSON o reimplementar en Kotlin con pruebas.
- Cada juego Compose se activa por bandera; el WebView sigue disponible.
- Comparar rendimiento, accesibilidad y tamaño.

### Fase 3 · Resto de juegos (v1.1.0–v1.3.0, completada)

- [x] Damas, Dados, Go, Jungle, Xiangqi y Damas Chinas nativos.
- [x] Animaciones con `Canvas`, `animate*AsState`, tableros táctiles y háptica.
- [x] Historial completo y últimas cinco jugadas en las nuevas mesas.
- [x] Caída y Furia de Colores nativos con barajas dibujadas.
- [x] Póker nativo con ciegas, calles, botes laterales y showdown.
- [x] Mesa compartida, chat y salas nativas LAN para Damas y Mancala (v1.5.0).
- [x] Serpientes de 2–4 jugadores, relay online configurable y replays exportables (v1.6.0).
- [x] Dominó de 2–4 con manos privadas, replay automático y visor web (v1.7.0).
- [x] Catálogo de 30 juegos; Reversi y Ur nativos (v1.8.0).
- [x] Brisca y Truco nativos; 19 juegos Compose (v1.9.0).
- [x] Brisca y Truco en salas con manos privadas y bots (v2.0.0).
- [ ] Ludo y Damas Chinas en salas; relay público; replays web adicionales (v2.1.0).

### Fase 4 · Retirada del WebView

- Eliminar assets web del APK cuando haya paridad.
- Mantener la PWA web como producto independiente.
- Reducir tamaño y mejorar arranque.
- Estado: 15 de 15 juegos jugables fuera del WebView; el WebView queda como respaldo de la PWA y la sala remota.
- v1.4.0 añade crónica PDF nativa en las quince mesas y accesibilidad básica de cartas y turnos.
- v1.5.0 añade salas LAN nativas para Damas y Mancala, replays con semilla, audio procedural y PDF con estadísticas.
- v1.6.0 añade Serpientes de 2–4 jugadores, relay online configurable, replays exportables e importables y audio en las quince mesas.
- v1.7.0 añade Dominó de 2–4 con manos privadas, replay automático y visor web.
- v1.8.0 completa el catálogo de 30 juegos y suma Reversi y Ur nativos. La PWA y la sala web siguen en el WebView; el siguiente paso son Brisca/Truco, Ludo/Damas Chinas y el relay público.
- v1.9.0 suma Brisca y Truco nativos. Los 19 juegos jugables son Compose; el WebView solo queda como respaldo de la PWA y la sala remota. El siguiente paso son Ludo/Damas Chinas en salas, el relay público y los replays web adicionales.
- v2.0.0 lleva Brisca y Truco a las salas nativas con manos privadas, validación autoritativa y bots de reemplazo. El siguiente paso son Ludo/Damas Chinas en salas, el relay público y los replays web adicionales.

## Arquitectura propuesta

```
android/app/src/main/java/com/mesaviva/app/
  MainActivity.kt              # ComponentActivity + Compose
  compose/
    MesaVivaApp.kt             # navegación y tema
    screens/                   # Home, Catalog, Setup, Table, Settings...
    games/                     # superficies Compose por juego
    theme/                     # Material 3, Material You, festivales
    bridge/                    # puentes cámara, PDF, sensores, LAN
  legacy/                      # WebView de respaldo
```

## Riesgos

- **Doble mantenimiento** mientras convivan Compose y WebView.
- **Motores duplicados** si no se comparte la lógica; hay que definir un contrato.
- **Regresiones** en salas, reconexión, PDF y temas.
- **Tiempo**: la migración completa es comparable a reescribir la app.
- **Compose y WebView** pueden convivir, pero no se debe afirmar “100 % Compose” hasta retirar el WebView.

## Primer paso concreto

1. Crear `versions/v0.6.0.md` (ya existe) y este plan.
2. Actualizar Kotlin/AGP a versiones estables compatibles con API 36.
3. Añadir Compose BOM y `activity-compose`.
4. Convertir `MainActivity` a `ComponentActivity` manteniendo el WebView en un `AndroidView` como paso 0.
5. Portar **Inicio** y **Ajustes** a Compose.
6. Medir y decidir el primer juego piloto.

## Decisión

La fase 3 se cierra en v1.3.0: los quince juegos jugables corren en Compose. El WebView sigue como respaldo de la PWA y de la sala online hasta desacoplar esa capa en v1.4.0.
