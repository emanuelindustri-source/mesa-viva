# Migración de Mesa Viva a Jetpack Compose

## Situación actual

Mesa Viva es una app Android **Kotlin nativa con WebView**. La lógica de juego, las pantallas y los 13 juegos están en React/TypeScript dentro de `android/app/src/main/assets/www`. La app usa Kotlin para cámara, sensores, vibración, PDF, Material You y el relay LAN.

**Compose no está en uso.** Cambiar `MainActivity` a Compose no migra la app: solo cambia el contenedor. “Compose por completo” significa reescribir en Kotlin/Compose:

- Inicio, onboarding, catálogo, preparación, mesa, resultados, ajustes, niveles, sala, diagnóstico y PDF.
- Las 13 superficies de juego y sus animaciones.
- El estado de sesión, perfiles, historial y persistencia.
- Las salas host/invitado y la reconexión.
- La música, partículas, háptica y temas.

Es un proyecto de varias versiones, no un commit.

## Estrategia recomendada: híbrida por fases

### Fase 0 · Base técnica

- Fijar versiones estables: Kotlin, AGP, Gradle, Compose BOM y activity-compose.
- Añadir un módulo/paquete `com.mesaviva.app.compose`.
- Mantener el WebView actual como respaldo.
- Pruebas instrumentadas mínimas y CI de compilación.

### Fase 1 · Cáscara Compose

- `ComponentActivity` + `setContent`.
- Splash, inicio, ajustes, “Acerca de”, niveles y lobby en Compose.
- El botón “Jugar” abre el WebView existente mediante `AndroidView` o una Activity dedicada.
- Tema Material 3, tipografía y colores.
- Paridad de idioma y Material You.

### Fase 2 · Juegos piloto

Elegir dos juegos con estado simple y mucha visibilidad:

1. **Serpientes y Escaleras** (tablero y dado).
2. **Damas** o **Backgammon** (tablero y fichas).

- Motores compartidos entre web y Compose: extraer la lógica a un contrato/JSON o reimplementar en Kotlin con pruebas.
- Cada juego Compose se activa por bandera; el WebView sigue disponible.
- Comparar rendimiento, accesibilidad y tamaño.

### Fase 3 · Resto de juegos

- Caída, Furia, Dominó, Dados, Ajedrez, Mancala, Go, Jungle, Ludo, Backgammon, Xiangqi.
- Animaciones nativas con `Canvas`, `animate*AsState`, partículas y háptica.
- Mesa compartida y chat.

### Fase 4 · Retirada del WebView

- Eliminar assets web del APK cuando haya paridad.
- Mantener la PWA web como producto independiente.
- Reducir tamaño y mejorar arranque.

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

## Decisión pendiente

No se debe empezar la fase 2 sin cerrar la fase 1. La opción de menor riesgo es **híbrida**: cáscara Compose + WebView para juegos, portando uno por uno. La opción de mayor riesgo es reescribir todo antes de publicar otra versión.
