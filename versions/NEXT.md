# Relevo del proyecto · Mesa Viva

> **No borrar.** Este archivo existe para que cualquier chat, persona o IA sepa dónde quedó el proyecto. Se actualiza al cerrar cada versión, pero no se elimina.

## Estado actual

- **Versión publicada:** 1.8.0.
- **Catálogo:** 30 juegos.
- **Juegos jugables:** 17, todos 100 % Jetpack Compose.
- **Juegos en WebView:** 0 (el WebView queda como respaldo de la PWA y de la sala remota).
- **Salas nativas:** Damas y Mancala (2), Dominó (2–4) y Serpientes (2–4), por LAN o relay online configurable.
- **Replays:** semilla, paso a paso, automático, enlace, archivo JSON, importación y visor web de Damas/Mancala.
- **Crónica PDF:** 17 mesas, con tarjeta de estadísticas.
- **Audio nativo:** música procedural por festividad y efectos en las 17 mesas.
- **Repositorio:** <https://github.com/emanuelindustri-source/mesa-viva>
- **Portada:** <https://emanuelindustri-source.github.io/mesa-viva/>
- **Juego web:** <https://emanuelindustri-source.github.io/mesa-viva/play/>
- **Replay web:** <https://emanuelindustri-source.github.io/mesa-viva/replay/>
- **Release:** <https://github.com/emanuelindustri-source/mesa-viva/releases/tag/v1.8.0>
- **VERSION:** `1.8.0`
- **Android:** `versionCode=20`, `versionName=1.8.0`, compileSdk/targetSdk 36.
- **Gradle:** 8.14.4.

### Calidad

- 30/30 pruebas de motores web.
- 93/93 pruebas Kotlin (juegos, códec de salas, replays, Reversi y Ur).
- **123 pruebas en total.**
- TypeScript sin errores.
- Web compilada con Vite 6.4.5 y sincronizada dentro del APK.
- APK debug, APK release firmado y AAB release firmado.
- Reversi y Ur probados en emulador Android 14.
- Dominó probado con anfitrión real + 3 invitados WebSocket; manos privadas y pozo oculto.
- Visor web probado en navegador real.
- Teléfono real `23117RA68G`: detectado por `adb`, pero MIUI bloquea la instalación por USB.

### Checksums v1.8.0

- APK release firmado: `0B184F5903FC328412E38F88EC9A39E30CE5CD2276AFF5699F0A1F0E429EF92D` · 10.636.441 bytes.
- AAB release firmado: `61455C1AD961E05E75240457A4A9FE46D0B43800813690A58861522A1308285F` · 10.259.356 bytes.
- APK debug de CI: se registra al terminar el workflow.
- Artefactos locales en `artifacts/`: `mesa-viva-1.8.0-release.apk`, `mesa-viva-1.8.0-release.aab`. La release de GitHub añade `mesa-viva-1.8.0-debug.apk`.

## Compose

- **Cáscara nativa completa:** inicio, catálogo, niveles, ajustes, sala y navegación.
- **17 juegos 100 % Compose.**
- **Salas nativas:** Damas, Mancala, Dominó y Serpientes.
- **Reversi y Ur:** nuevos motores con bots, historial y PDF.
- **Replay automático** y **visor web**.
- **Crónica PDF** en cada mesa.
- **Audio nativo** en las 17 mesas.
- **XP real** al terminar.
- **Accesibilidad:** cartas, fichas, casillas, puntos y tableros descritos.
- **Pendiente:** Brisca, Truco y los 11 juegos restantes del catálogo.

## Siguiente versión: 1.9.0

Ver [`v1.9.0.md`](./v1.9.0.md). Incluye:

1. Brisca y Truco nativos.
2. Ludo y Damas Chinas en salas.
3. Dominó por parejas y meta configurable.
4. Relay público y enlaces universales.
5. Replay web de más juegos.
6. Accesibilidad completa, teléfono físico y Baseline Profile.

## Dónde está cada cosa

| Ruta | Contenido |
|---|---|
| `src/games/` | 15 motores web |
| `src/ui/GameSurface.tsx` | Superficies web |
| `src/core/` | Tipos, registro, Match autoritativo, niveles |
| `src/net/room.ts` | Salas web, host/invitado y reconexión |
| `server/room-server.mjs` | Relay Node |
| `landing/replay/index.html` | Visor web de replays |
| `android/.../games/` | Motores Kotlin de los 17 juegos |
| `android/.../shell/NativeRoom.kt` | Sesión host/invitado, asientos, protocolo y bots |
| `android/.../shell/RoomCodec.kt` | Códec JSON con redacción de manos de Dominó |
| `android/.../shell/SnakesRoom.kt` | Reglas puras de Serpientes para 2–4 |
| `android/.../shell/ReplayExport.kt` | Enlaces nativos/web, archivo JSON e importación |
| `android/.../shell/RoomScreen.kt` | Lobby, sala web y mesas LAN/online |
| `android/.../shell/NativeAudio.kt` | Música y efectos procedurales |
| `android/.../shell/NativeChronicle.kt` | Crónica PDF |
| `android/.../shell/ReversiScreen.kt` | Mesa de Reversi |
| `android/.../shell/UrScreen.kt` | Mesa de Ur |
| `landing/` | Portada, versiones, descargas y replay |
| `tools/build-pages.mjs` | Portada + juego + replay en `dist-site/` |
| `versions/` | Evidencia inmutable por versión |

## Comandos

```bash
npm install
npm run dev                 # web en 5173
npm run typecheck
npm test
npm run test:relay          # requiere npm run rooms
npm run build
npm run build:pages         # portada + juego + replay en dist-site/
npm run android:sync
cd android && gradlew.bat testDebugUnitTest
cd android && gradlew.bat assembleRelease
cd android && gradlew.bat bundleRelease
```

## Seguridad

- El token de GitHub compartido en el chat debe revocarse.
- El keystore y la contraseña viven en `C:\Users\Sr.Hidalgo\.mesa-viva\`.
- `android/keystore.properties` está ignorado por git.
- El QR se genera localmente con ZXing.
- La sala nativa valida cada jugada en el anfitrión.
- Las manos de Dominó se redactan por asiento; el pozo solo viaja como conteo.
- El replay es autocontenido y no incluye datos personales.
- El relay online es opcional y debe ser de confianza.
- El diagnóstico se envía solo si el usuario abre el correo a `emanuel.industri@gmail.com`.
- El PDF se guarda en el dispositivo y solo se comparte si el usuario abre el selector.

## Reglas de continuidad

1. No borrar archivos de `versions/`.
2. Cada versión nueva crea su propio `vX.Y.Z.md`.
3. Mantener este `NEXT.md` actualizado, sin eliminarlo.
4. No guardar tokens, contraseñas ni keystores en el repositorio.
5. Antes de publicar: typecheck, tests web, tests Kotlin, build web, APK, AAB, CI/Pages/Android.
6. No afirmar “100 % Compose” global hasta retirar el WebView. **Estado v1.8.0: los 17 juegos jugables son Compose; el WebView solo queda como respaldo de la PWA y la sala remota.**
