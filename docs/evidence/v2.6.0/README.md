# Evidencia visual · v2.6.0

Capturas del emulador `Pixel_7_API_34` (Android 14, API 34) con la build 2.6.0. Inicio y ajustes se capturaron con la APK de release firmada; la mesa de Aeroplane se capturó durante la prueba funcional con la build equivalente.

| Archivo | Contenido |
|---|---|
| `home.png` | Inicio 2.6.0 con 23 juegos |
| `settings.png` | Ajustes con la versión 2.6.0 |
| `aeroplane.png` | Mesa de Aeroplane Chess con 4 jugadores |

## Pruebas

- **Aeroplane Chess:** tablero de 4 jugadores, cuatro aviones por color, tirada de dado y botonera.
- **Reglas:** salto de cuatro casillas validado por prueba unitaria.
- **Catálogo:** 23 juegos jugables, Aeroplane Chess marcado como `Playable`.
- **Tests:** 144 verdes (30 web + 114 Kotlin).
- **APK:** instalada y ejecutada en el emulador Android 14.

El teléfono físico `22101316G` sigue bloqueado por MIUI para instalación USB; la verificación se hizo en emulador.

Los checksums y detalles completos están en [`../../versions/v2.6.0.md`](../../versions/v2.6.0.md).
