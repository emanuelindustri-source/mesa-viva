# Evidencia visual · v1.9.0

Capturas del emulador `Pixel_7_API_34` (Android 14, API 34) con la APK de release firmada `mesa-viva-1.9.0-release.apk`.

| Archivo | Contenido |
|---|---|
| `home.png` | Inicio 1.9.0 con 19 juegos jugables de 30 |
| `settings.png` | Ajustes con la versión 1.9.0 |
| `brisca.png` | Brisca recién repartida: triunfo, mazo, baza del bot y mano |
| `brisca-play.png` | Baza resuelta, puntos capturados y robo de cartas |
| `truco.png` | Truco en la fase de envido, antes de la primera carta |
| `truco-envido.png` | Envido cantado por el usuario y rechazado por el bot |
| `truco-play.png` | Primera baza resuelta con las etiquetas de cada jugador |

## Pruebas

- **Brisca:** reparto de 3 cartas, muestra de triunfo, baza del bot, jugada del usuario, resolución y robo.
- **Truco:** tanto visible, envido rechazado (+1), mano jugada, truco aceptado (+2) y baza resuelta.
- **Catálogo:** 19 juegos jugables, Brisca y Truco marcados como `Nativo Compose`.
- **Tests:** 131 verdes (30 web + 101 Kotlin).
- **APK:** instalada y ejecutada en el emulador Android 14.

El teléfono físico `22101316G` responde a `adb`, pero MIUI rechaza la instalación USB con `INSTALL_FAILED_USER_RESTRICTED`; la verificación se hizo en emulador.

Los checksums y detalles completos están en [`../../versions/v1.9.0.md`](../../versions/v1.9.0.md).
