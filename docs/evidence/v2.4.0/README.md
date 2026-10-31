# Evidencia visual · v2.4.0

Capturas del emulador `Pixel_7_API_34` (Android 14, API 34) con la build 2.4.0. Inicio y ajustes se capturaron con la APK de release firmada; la mesa de Shogi se capturó durante la prueba funcional con la build equivalente.

| Archivo | Contenido |
|---|---|
| `home.png` | Inicio 2.4.0 con 21 juegos |
| `settings.png` | Ajustes con la versión 2.4.0 |
| `shogi.png` | Tablero inicial de Shogi |
| `shogi-move.png` | Peón movido y respuesta del bot |

## Pruebas

- **Shogi:** tablero 9×9, 40 piezas iniciales, peón de a3 a a4, respuesta del bot de a9 a a8 e historial.
- **Catálogo:** 21 juegos jugables, Shogi marcado como `Playable`.
- **Tests:** 141 verdes (30 web + 111 Kotlin).
- **APK:** instalada y ejecutada en el emulador Android 14.

El teléfono físico `22101316G` sigue bloqueado por MIUI para instalación USB; la verificación se hizo en emulador.

Los checksums y detalles completos están en [`../../versions/v2.4.0.md`](../../versions/v2.4.0.md).
