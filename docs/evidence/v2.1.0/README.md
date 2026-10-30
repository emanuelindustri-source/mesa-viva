# Evidencia visual · v2.1.0

Capturas del emulador `Pixel_7_API_34` (Android 14, API 34) con la build 2.1.0. Inicio y ajustes se capturaron con la APK de release firmada; la mesa de Ludo se capturó durante la prueba de sala con la build equivalente.

| Archivo | Contenido |
|---|---|
| `home.png` | Inicio 2.1.0 |
| `settings.png` | Ajustes con la versión 2.1.0 |
| `ludo-room.png` | Ludo en sala: anfitrión Android + invitado WebSocket, tablero y tirada |

## Pruebas de sala

- **Ludo:** el invitado recibió el estado con sus cuatro fichas, el dado y el turno. Se sincronizaron tiradas, salida con 6 y movimiento de ficha; el turno volvió al anfitrión.
- **Bot de reemplazo:** el mismo mecanismo de asientos y bots de v2.0.0 cubre Ludo.
- **Autoridad:** el anfitrión valida que el asiento y la fase sean correctos antes de aplicar la tirada o el movimiento.

## Pruebas automáticas

- **30/30** motores web.
- **105/105** pruebas Kotlin, incluida la prueba nueva del códec de Ludo.
- **135 pruebas en total.**

El teléfono físico `22101316G` sigue bloqueado por MIUI para instalación USB; la verificación se hizo en emulador y con un cliente WebSocket externo.

Los checksums y detalles completos están en [`../../versions/v2.1.0.md`](../../versions/v2.1.0.md).
