# Evidencia visual · v1.5.0

Capturas del emulador `Pixel_7_API_34` (Android 14, API 34) con la APK de release firmada `mesa-viva-1.5.0-release.apk`.

| Archivo | Contenido |
|---|---|
| `home.png` | Inicio de Mesa Viva 1.5.0 |
| `room-native.png` | Sala nativa LAN con código, QR y selección de juego |
| `guest-game.png` | Invitado real jugando Damas contra un anfitrión WebSocket |
| `scanner.png` | Escáner QR integrado con el permiso de cámara |
| `replay.png` | Replay con semilla, paso a paso |
| `pdf-stats.png` | PDF con tarjeta de estadísticas renderizada |
| `cronica.pdf` | PDF real generado por la app |
| `settings.png` | Ajustes con la versión 1.5.0 |

La sala nativa se probó en los dos sentidos:

1. **Anfitrión real + invitado WebSocket** en el emulador: el invitado se unió, recibió `welcome`, `start` y `state`, y envió una jugada que el anfitrión validó y difundió.
2. **Invitado real + anfitrión WebSocket** en el PC: el invitado entró por enlace `mesaviva://join`, recibió el estado de Damas y jugó una pieza negra.

El escáner abrió `CaptureActivity` y solicitó cámara. El PDF se generó en `Descargas/Mesa Viva` y se renderizó con PyMuPDF. El teléfono físico `23117RA68G` sigue bloqueado por MIUI para instalación USB; la verificación se hizo en emulador.

Los checksums y detalles completos están en [`../../versions/v1.5.0.md`](../../versions/v1.5.0.md).
