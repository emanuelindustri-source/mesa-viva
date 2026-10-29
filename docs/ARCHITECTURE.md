# Arquitectura de Mesa Viva

## Visión

Un único motor de juego determinista alimenta la web y el APK. La mesa se puede jugar sin conexión; las salas en línea solo cambian el transporte y la autoridad.

```
UI React ──► Match (host) ──► GameModule (reglas puras)
   │             │                    │
   │             └── view(player) ◄────┘  (redacción por jugador)
   │
   ├── local: sesión en memoria
   ├── sala: relay WebSocket ──► invitados
   └── Android: cáscara Compose + GameWebActivity (mesa compartida)
```

## Capas

### `src/core`
- `types.ts`: contratos de `Seat`, `GameView`, `LogEvent`, `GameModule`.
- `match.ts`: anfitrión autoritativo. Crea el estado, encola eventos, valida acciones, ejecuta bots y detecta el final.
- `rng.ts`: mulberry32 con semilla; auditoría y replays.
- `levels.ts`: 10 niveles y fórmula de XP limitada por partida.
- `registry.ts`: módulos jugables y catálogo de 30 juegos.

### `src/games`
Cada juego implementa:

```ts
create(seats, options, rng) => State
view(state, viewerId) => GameView   // nunca revela información privada ajena
act(state, action, actorId, rng) => { state, events }
bot(state, botId, difficulty, rng) => Action
result(state) => MatchResult
```

Los motores son funciones puras sobre el estado (con el RNG explícito). Las vistas para red incluyen solo la mano, los dados o las piezas del jugador; por ejemplo, los dados ajenos se envían como `{hidden: true, value: 0}`.

### `src/ui`
- `Table`: mesa local con hotseat, privacidad al pasar el teléfono, bots y sonido.
- `RemoteTable`: mesa para salas; recibe vistas del host y envía acciones.
- `GameSurface`: superficies por tipo de juego (cartas, fichas, dados, tablero, mancala y póker).
- `Result`: crónica, XP, PDF y revancha.
- `ParticleCanvas`: partículas con Canvas 2D; respeta movimiento reducido.

### `src/net`
- Protocolo de mensajes y relay WebSocket.
- El host crea `Match` y difunde una vista por jugador. El relay no conoce las reglas.
- El servidor (`server/room-server.mjs`) solo enruta, aplica rate limit y limpia salas.

### `src/theme` y `src/i18n`
- `festivals.ts`: Pascua, Carnaval, Semana Santa, Corpus, fechas patrias y paletas; los personajes venezolanos se usan como referencias culturales originales.
- Material You: el APK expone `systemAccent()`; el navegador puede extraer el color dominante de una imagen local.
- `i18n`: español como base, inglés completo, 9 locales adicionales con fallback en inglés.

### `android`
Cáscara nativa en **Jetpack Compose** (`MainActivity`, `shell/`): inicio, catálogo, niveles, ajustes, sala, navegación, tema Material 3 y catorce juegos nativos (Serpientes, Dominó, Ajedrez, Ludo, Mancala, Backgammon, Damas, Dados, Go, Jungle, Xiangqi, Damas Chinas, Caída y Furia) con motores Kotlin en `games/`. `RoomScreen` genera QR local con ZXing. `GameWebActivity` hospeda la mesa compartida con `WebViewAssetLoader`, desde un origen HTTPS seguro, e inyecta `MesaViva` para vibración, sensores, color del sistema y PDF; gestiona cámara y selector de archivos. `ShellSync` importa el perfil de `localStorage` al volver de la mesa y la URL de la mesa recibe idioma/preferencias de la cáscara.

### `server`
Relay Node + `ws`. En producción debe ir detrás de TLS (`wss`). El estado de la partida no se guarda: si el host se desconecta, la sala se cierra.

## Decisiones

- **Host autoritativo**: evita trampas y estados divergentes. Los invitados no ejecutan reglas.
- **Relay en vez de P2P para v0.1**: WebRTC falla en redes móviles restrictivas y requiere STUN/TURN. Para juegos por turnos, un relay de baja latencia es más simple y fiable. La sala LAN sin servidor queda en el roadmap.
- **Offline real**: todos los assets y fuentes se empaquetan. Bots y hotseat no tocan la red.
- **Originalidad**: arte CSS/SVG y música sintetizada propia; sin assets de terceros.
- **Accesibilidad**: contraste alto, movimiento reducido, tamaños táctiles ≥44 px y textos localizados.

## Riesgos y mitigaciones

| Riesgo | Mitigación |
|---|---|
| Relay caído | Los modos locales siguen; la sala muestra error claro y permite reintentar |
| Invitado desconectado | El host vuelve a la sala; los bots pueden llenar asientos en la siguiente ronda |
| Trampas | Validación en el motor y host autoritativo |
| Información filtrada | `view()` redacta manos/dados ajenos; pruebas unitarias lo verifican |
| WebView insegura | AssetLoader HTTPS, permisos explícitos, sin file:// |
| Doble mantenimiento Compose/WebView | Cáscara nativa + motores compartidos; portar juegos uno por uno con pruebas |
| PDF en Android | Puente nativo a MediaStore; fallback web |
| Token/secretos | `.env.local` y `local.properties` ignorados; SECURITY.md |

## Cómo añadir un juego

1. Crea `src/games/miJuego.ts` implementando `GameModule`.
2. Añade el id a `GameId` y al registro.
3. Añade metadatos, textos `game.miJuego.*` y una superficie si el layout no encaja en las existentes.
4. Añade pruebas de creación, legalidad, redacción y final.
5. Actualiza catálogo y roadmap.
