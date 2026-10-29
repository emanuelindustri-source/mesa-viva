# Evidencia visual · v1.7.0

Capturas del emulador `Pixel_7_API_34` (Android 14, API 34) con la APK de release firmada `mesa-viva-1.7.0-release.apk`, y del visor web servido desde `dist-site/`.

| Archivo | Contenido |
|---|---|
| `home.png` | Inicio de Mesa Viva 1.7.0 |
| `settings.png` | Ajustes con la versión 1.7.0 |
| `domino-host.png` | Anfitrión de Dominó con tres invitados, mesa, pozo y mano privada |
| `domino-guest.png` | Invitado de Dominó con su mano y la mesa |
| `replay-web.jpg` | Visor web del replay de Damas |
| `replay-auto.png` | Replay nativo con botón Reproducir |

## Pruebas de Dominó

1. **Anfitrión real + 3 invitados WebSocket**: cada invitado recibió únicamente su mano (7 o 6 fichas) y los conteos públicos de los demás. El pozo viajó solo como número.
2. Los invitados jugaron fichas válidas y el anfitrión difundió la cadena y los turnos.
3. **Invitado real + anfitrión WebSocket**: el invitado recibió su mano, jugó una ficha y el estado volvió con la cadena actualizada.
4. La partida de 4 asientos mostró puntos, mano, pozo, meta y turno.

## Pruebas de replay

- El enlace `mesaviva://replay?game=damas&seed=123456&moves=40-33.17-26` abrió el replay con 2 jugadas.
- El visor web se probó en Chromium con el mismo enlace: tablero, lista de jugadas, navegación y reproducción automática sin errores de consola.
- El botón Reproducir aparece en el diálogo nativo.

El teléfono físico `23117RA68G` sigue bloqueado por MIUI para instalación USB; la verificación se hizo en emulador.

Los checksums y detalles completos están en [`../../versions/v1.7.0.md`](../../versions/v1.7.0.md).
