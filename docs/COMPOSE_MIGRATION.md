# Migración de Mesa Viva a Jetpack Compose

## Situación actual

Mesa Viva es una app Android **Kotlin nativa con WebView**. La lógica de juego, las pantallas y los 13 juegos están en React/TypeScript dentro de `android/app/src/main/assets/www`. La app usa Kotlin para cámara, sensores, vibración, PDF, Material You y el relay LAN.

Desde **v1.1.0**, `MainActivity` es una cáscara nativa completa en Compose (inicio, catálogo, niveles, ajustes, sala y navegación) y `GameWebActivity` hospeda la mesa compartida en WebView. **Doce juegos** son 100 % Compose: Serpientes y Escaleras, Dominó, Ajedrez, Ludo, Mancala, Backgammon, Damas, Dados Mentirosos, Go, Jungle, Xiangqi y Damas Chinas. Es la **fase 3 parcial**; quedan Caída, Furia de Colores y Póker en WebView.

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
- [ ] Salas online nativas para los seis juegos (v1.1.0).

- Motores compartidos entre web y Compose: extraer la lógica a un contrato/JSON o reimplementar en Kotlin con pruebas.
- Cada juego Compose se activa por bandera; el WebView sigue disponible.
- Comparar rendimiento, accesibilidad y tamaño.

### Fase 3 · Resto de juegos (v1.1.0, parcial)

- [x] Damas, Dados, Go, Jungle, Xiangqi y Damas Chinas nativos.
- [x] Animaciones con `Canvas`, `animate*AsState`, tableros táctiles y háptica.
- [x] Historial completo y últimas cinco jugadas en las nuevas mesas.
- [ ] Caída, Furia y Póker nativos (v1.2.0).
- [ ] Mesa compartida, chat y salas nativas (v1.2.0).

### Fase 4 · Retirada del WebView

- Eliminar assets web del APK cuando haya paridad.
- Mantener la PWA web como producto independiente.
- Reducir tamaño y mejorar arranque.
- Estado: 12 de 15 juegos fuera del WebView; no afirmar “100 % Compose” global todavía.

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

La fase 3 avanza en v1.1.0 con estrategia **híbrida**: cáscara Compose + WebView para la mesa compartida, portando un juego a la vez. Doce juegos demuestran la viabilidad. No se afirmará “100 % Compose” global hasta retirar el WebView de los 15 juegos.
