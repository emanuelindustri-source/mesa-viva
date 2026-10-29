# Evidencia visual · v1.6.0

Capturas del emulador `Pixel_7_API_34` (Android 14, API 34) con la APK de release firmada `mesa-viva-1.6.0-release.apk` y compilaciones debug equivalentes para las pruebas de sala.

| Archivo | Contenido |
|---|---|
| `home.png` | Inicio de Mesa Viva 1.6.0 |
| `settings.png` | Ajustes con la versión 1.6.0 |
| `snakes-host.png` | Serpientes con anfitrión y tres invitados; cuatro posiciones y turnos |
| `snakes-guest.png` | Invitado real tras lanzar el dado, con evento de escalera |
| `snakes-online.png` | Sala nativa usando el relay online `ws://10.0.2.2:9022` |
| `replay-share.png` | Replay importado por enlace, con Compartir enlace y Guardar archivo |
| `replay-file.txt` | Archivo JSON real guardado en Descargas/Mesa Viva |
| `cold-start.txt` | Tres mediciones de arranque en frío |

## Pruebas de sala

1. **Serpientes con 3 invitados WebSocket**: los tres recibieron asientos 1, 2 y 3, lanzaron en orden y el anfitrión difundió posiciones y turnos. La captura muestra al invitado 2 en la casilla 22 tras una escalera.
2. **Invitado real con anfitrión WebSocket**: el invitado recibió su asiento, lanzó el dado y el estado volvió con turno del anfitrión.
3. **Relay online**: el anfitrión se conectó a `server/room-server.mjs` en el PC (`ws://10.0.2.2:9022`), creó la sala y recibió al invitado por el mismo relay.
4. **Replay**: el enlace `mesaviva://replay?game=damas&seed=123456&moves=40-33.17-26` abrió el diálogo con dos jugadas; Guardar archivo escribió el JSON adjunto; Compartir abrió el selector de Android.

El teléfono físico `23117RA68G` sigue bloqueado por MIUI para instalación USB; la verificación se hizo en emulador.

Los checksums y detalles completos están en [`../../versions/v1.6.0.md`](../../versions/v1.6.0.md).
