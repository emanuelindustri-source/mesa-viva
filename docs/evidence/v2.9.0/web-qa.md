# QA web · Tableros · v2.9.0

- **Fecha:** 2026-10-31
- **Navegador:** Chromium integrado de OpenCode
- **URL base:** `http://localhost:5173/#/setup/`
- **Modo:** contra bots, dificultad normal

## Reversi

- Setup 2–2 jugadores y dificultad normal.
- Tablero 8×8 con 4 fichas iniciales y 4 jugadas legales.
- Se seleccionó una casilla, se volteó una ficha y el bot respondió.
- El historial mostró una sola entrada por movimiento; se corrigió la duplicación de eventos detectada en la primera pasada.
- Consola sin errores después de la corrección.

## Juego Real de Ur

- Pista de 20 casillas con rosetas 1, 5, 9, 14 y 17.
- Se lanzó el dado (valor 2), se iluminaron 4 fichas legales y se movió una ficha de inicio a la casilla 1.
- El turno pasó al bot. Consola sin errores.

## Luzhanqi

- Tablero 5×12 con 25 piezas por bando; rojas visibles y azules ocultas con `?`.
- Se seleccionó una pieza de la fila delantera y se iluminaron **12 jugadas** por vía.
- Se movió A8 → A7 y la partida continuó; se corrigió un fallo de IDs duplicados que cerraba la partida tras el primer movimiento.
- La prueba de regresión exige IDs únicos y jugadas para ambos bandos.

## Ajedrez para tres

- Tablero 12×12 con 48 piezas, 16 por ejército y tres marcadores de material (39).
- Se seleccionó un peón de Norte, se iluminaron 4 jugadas y se capturó B10.
- La partida continuó y el turno pasó a Oeste. Consola sin errores.

## Automático

- 50 pruebas web: 30 base, 8 de Mahjong y 12 de Reversi, Ur, Luzhanqi y tres.
- 153 pruebas Kotlin.
- TypeScript estricto y build de Vite correctos.
