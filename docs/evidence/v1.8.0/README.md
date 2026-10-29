# Evidencia visual · v1.8.0

Capturas del emulador `Pixel_7_API_34` (Android 14, API 34) con la APK de release firmada `mesa-viva-1.8.0-release.apk`.

| Archivo | Contenido |
|---|---|
| `home.png` | Inicio 1.8.0 con 17 juegos jugables de 30 |
| `settings.png` | Ajustes con la versión 1.8.0 |
| `reversi.png` | Reversi al inicio, con jugadas legales |
| `reversi-move.png` | Reversi después de una jugada y la respuesta del bot |
| `ur.png` | Ur al inicio, con rosetas y dados |
| `ur-rosette.png` | Ur con una ficha en roseta y turno extra |

## Pruebas

- **Catálogo:** 30 ids únicos, 17 jugables. Se corrigió la ficha duplicada de Ur que provocaba un crash de `LazyColumn`.
- **Reversi:** apertura 2–2, cuatro jugadas legales, jugada del usuario y respuesta del bot (3–3).
- **Ur:** tirada de dado, entrada en la casilla 0 (roseta), turno extra y captura de historial.
- **Tests:** 123 verdes (30 web + 93 Kotlin).

El teléfono físico `23117RA68G` sigue bloqueado por MIUI para instalación USB; la verificación se hizo en emulador.

Los checksums y detalles completos están en [`../../versions/v1.8.0.md`](../../versions/v1.8.0.md).
