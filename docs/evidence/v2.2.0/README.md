# Evidencia visual · v2.2.0

Capturas del emulador `Pixel_7_API_34` (Android 14, API 34) con la build 2.2.0 y un invitado WebSocket real contra el relay LAN del anfitrión.

| Archivo | Contenido |
|---|---|
| `home.png` | Inicio 2.2.0 |
| `settings.png` | Ajustes con la versión 2.2.0 |
| `chino-room.png` | Damas Chinas en sala: anfitrión Android + invitado WebSocket, estrella de 121 hoyos |

## Pruebas de sala

- **Damas Chinas:** el invitado recibió el estado con 121 hoyos, dos jugadores y turno 0; la mesa inició y el tablero se dibujó con las diez fichas de cada triángulo.
- **Bots:** el mecanismo de asientos y bots de las versiones anteriores cubre los asientos vacíos o desconectados.
- **Autoridad:** el anfitrión valida turno, origen, destino y salto antes de aplicar el movimiento.

## Pruebas automáticas

- **30/30** motores web.
- **106/106** pruebas Kotlin, incluida la prueba nueva del códec de Damas Chinas.
- **136 pruebas en total.**

El teléfono físico `22101316G` sigue bloqueado por MIUI para instalación USB; la verificación se hizo en emulador y con un cliente WebSocket externo.

Los checksums y detalles completos están en [`../../versions/v2.2.0.md`](../../versions/v2.2.0.md).
