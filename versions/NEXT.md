# Relevo del proyecto · Mesa Viva

> **No borrar.** Este archivo existe para que cualquier chat, persona o IA sepa dónde quedó el proyecto. Se actualiza al cerrar cada versión, pero no se elimina.

## Estado actual

- **Versión publicada:** 1.6.0.
- **Juegos jugables:** 15.
- **Juegos 100 % Compose:** 15 (todos).
- **Juegos en WebView:** 0 (el WebView queda como respaldo de la PWA y de la sala remota).
- **Salas nativas:** Damas y Mancala (2 jugadores), Serpientes (2–4 jugadores), por LAN y con relay online configurable.
- **Replays:** semilla, paso a paso, enlace `mesaviva://replay`, archivo JSON e importación.
- **Crónica PDF:** 15 mesas, con tarjeta de estadísticas.
- **Audio nativo:** música procedural por festividad y efectos en las 15 mesas.
- **Repositorio:** <https://github.com/emanuelindustri-source/mesa-viva>
- **Portada:** <https://emanuelindustri-source.github.io/mesa-viva/>
- **Juego web:** <https://emanuelindustri-source.github.io/mesa-viva/play/>
- **Release:** <https://github.com/emanuelindustri-source/mesa-viva/releases/tag/v1.6.0>
- **VERSION:** `1.6.0`
- **Android:** `versionCode=18`, `versionName=1.6.0`, compileSdk/targetSdk 36.
- **Gradle:** 8.14.4.

### Calidad

- 30/30 pruebas de motores web.
- 78/78 pruebas Kotlin (15 juegos + 19 de salas, códec y replays).
- **108 pruebas en total.**
- TypeScript sin errores.
- Web compilada con Vite 6.4.5 y sincronizada dentro del APK.
- APK debug, APK release firmado y AAB release firmado.
- Serpientes probado con anfitrión real + 3 invitados WebSocket, y con invitado real + anfitrión WebSocket.
- Relay online probado con `server/room-server.mjs`.
- Replay guardado, compartido e importado por enlace profundo.
- Arranque en frío en emulador Android 14: 3.278 / 2.616 / 2.737 ms.
- Teléfono real `23117RA68G`: detectado por `adb`, pero MIUI bloquea la instalación por USB (“Instalación bloqueada de Mesa Viva vía USB”). Pendiente habilitar **Instalar vía USB** en Opciones de desarrollador.

### Checksums v1.6.0

- APK release firmado: `1F48903B944D661CA8FBD55BC551016B180FB0B7B21A29E40A8398AF46A746F1` · 10.554.521 bytes.
- AAB release firmado: `5B9A3EF4079897D4845BDC5357D4FADD06758EDDAAD230357836649BA0E1FF49` · 10.186.113 bytes.
- APK debug de CI: se registra al terminar el workflow.
- Artefactos locales en `artifacts/`: `mesa-viva-1.6.0-release.apk`, `mesa-viva-1.6.0-release.aab`. La release de GitHub añade `mesa-viva-1.6.0-debug.apk`.

## Compose

- **Cáscara nativa completa:** inicio, catálogo, niveles, ajustes, sala y navegación.
- **Quince juegos 100 % Compose.**
- **Salas nativas:** Damas, Mancala y Serpientes con asientos, QR, escáner, chat, reconexión y bots.
- **Relay online:** campo opcional al crear la sala.
- **Replay exportable:** enlace, archivo e importación.
- **Crónica PDF** en cada mesa: título, fecha, movimientos, estadísticas, paginación y compartición.
- **Audio nativo:** síntesis propia, sin samples; música por festividad y efectos en 15 mesas.
- **XP real:** victorias, derrotas y empates actualizan nivel y racha.
- **Accesibilidad:** cartas, fichas, casillas y turnos descritos; tamaño de fuente del sistema.
- **Pendiente:** salas de Dominó/Ludo/Damas Chinas, relay público, replay automático y accesibilidad completa.

## Siguiente versión: 1.7.0

Ver [`v1.7.0.md`](./v1.7.0.md). Incluye:

1. Dominó, Ludo y Damas Chinas nativos de 3–8 jugadores.
2. Relay público con TLS, límites y enlaces universales.
3. Reproductor de replay automático y visor web.
4. Accesibilidad completa en las quince mesas.
5. Teléfono físico y checklist de Play Store.
6. Baseline Profile, macrobenchmark y optimización de arranque.

## Dónde está cada cosa

| Ruta | Contenido |
|---|---|
| `src/games/` | 15 motores web |
| `src/ui/GameSurface.tsx` | Superficies web |
| `src/core/` | Tipos, registro, Match autoritativo, niveles |
| `src/net/room.ts` | Salas web, host/invitado y reconexión |
| `server/room-server.mjs` | Relay Node |
| `android/.../MainActivity.kt` | Cáscara Compose y enlaces `mesaviva://join` y `mesaviva://replay` |
| `android/.../GameWebActivity.kt` | WebView de respaldo + puentes + params nativos |
| `android/.../LanRelayServer.kt` | Relay LAN del anfitrión |
| `android/.../games/` | Motores Kotlin de los quince juegos |
| `android/.../shell/NativeRoom.kt` | Sesión host/invitado, asientos, protocolo y bots |
| `android/.../shell/RoomCodec.kt` | Códec JSON de Damas, Mancala y Serpientes |
| `android/.../shell/SnakesRoom.kt` | Reglas puras de Serpientes para 2–4 |
| `android/.../shell/ReplayExport.kt` | Enlaces, archivo JSON e importación de replays |
| `android/.../shell/RoomScreen.kt` | Lobby, sala web y mesas LAN/online |
| `android/.../shell/NativeAudio.kt` | Música y efectos procedurales |
| `android/.../shell/NativeChronicle.kt` | Crónica PDF, MediaStore, FileProvider y compartición |
| `android/.../shell/CardUi.kt` | Barajas española, Furia y francesa, reversos y partículas |
| `landing/` | Portada estática con historial de versiones |
| `tools/build-pages.mjs` | Portada + juego en `dist-site/` |
| `versions/` | Evidencia inmutable por versión |

## Comandos

```bash
npm install
npm run dev                 # web en 5173
npm run typecheck
npm test
npm run test:relay          # requiere npm run rooms
npm run build
npm run build:pages         # portada + juego en dist-site/
npm run android:sync
cd android && gradlew.bat testDebugUnitTest
cd android && gradlew.bat assembleRelease
cd android && gradlew.bat bundleRelease
```

## Seguridad

- El token de GitHub compartido en el chat debe revocarse.
- El keystore y la contraseña viven en `C:\Users\Sr.Hidalgo\.mesa-viva\`; no se copian al repositorio.
- `android/keystore.properties` está ignorado por git.
- El QR nativo se genera localmente con ZXing; no se envía a un servicio.
- La sala nativa valida cada jugada en el anfitrión; el invitado nunca es autoridad.
- El replay es autocontenido y no incluye datos personales.
- El relay online es opcional y debe ser de confianza; se admite `ws://` y `wss://`.
- El diagnóstico se envía solo si el usuario abre el correo a `emanuel.industri@gmail.com`.
- El PDF se guarda en el dispositivo y solo se comparte si el usuario abre el selector de Android.
- El escáner QR usa la cámara solo con permiso explícito y no guarda imágenes.

## Reglas de continuidad

1. No borrar archivos de `versions/`.
2. Cada versión nueva crea su propio `vX.Y.Z.md`.
3. Mantener este `NEXT.md` actualizado, sin eliminarlo.
4. No guardar tokens, contraseñas ni keystores en el repositorio.
5. Antes de publicar: typecheck, tests web, tests Kotlin, build web, APK, AAB, CI/Pages/Android.
6. No afirmar “100 % Compose” global hasta retirar el WebView de la PWA y la sala remota. **Estado v1.6.0: los quince juegos jugables son Compose; el WebView solo queda como respaldo de la PWA y la sala remota.**
