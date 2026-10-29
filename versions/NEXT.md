# Relevo del proyecto · Mesa Viva

> **No borrar.** Este archivo existe para que cualquier chat, persona o IA sepa dónde quedó el proyecto. Se actualiza al cerrar cada versión, pero no se elimina.

## Estado actual

- **Versión publicada:** 1.9.0.
- **Catálogo:** 30 juegos.
- **Juegos jugables:** 19, todos 100 % Jetpack Compose.
- **Juegos en WebView:** 0 (el WebView queda como respaldo de la PWA y de la sala remota).
- **Salas nativas:** Damas y Mancala (2), Dominó (2–4) y Serpientes (2–4), por LAN o relay online configurable.
- **Replays:** semilla, paso a paso, automático, enlace, archivo JSON, importación y visor web de Damas/Mancala.
- **Crónica PDF:** 19 mesas, con tarjeta de estadísticas.
- **Audio nativo:** música procedural por festividad y efectos en las 19 mesas.
- **Repositorio:** <https://github.com/emanuelindustri-source/mesa-viva>
- **Portada:** <https://emanuelindustri-source.github.io/mesa-viva/>
- **Juego web:** <https://emanuelindustri-source.github.io/mesa-viva/play/>
- **Replay web:** <https://emanuelindustri-source.github.io/mesa-viva/replay/>
- **Release:** <https://github.com/emanuelindustri-source/mesa-viva/releases/tag/v1.9.0>
- **VERSION:** `1.9.0`
- **Android:** `versionCode=21`, `versionName=1.9.0`, compileSdk/targetSdk 36.
- **Gradle:** 8.14.4.

### Calidad

- 30/30 pruebas de motores web.
- 101/101 pruebas Kotlin (juegos, salas, replays, Reversi, Ur, Brisca y Truco).
- **131 pruebas en total.**
- TypeScript sin errores.
- Web compilada con Vite 6.4.5 y sincronizada dentro del APK.
- APK debug, APK release firmado y AAB release firmado.
- Brisca y Truco probados en emulador Android 14.
- 19 juegos nativos en el catálogo; 11 pendientes.
- Teléfono físico `22101316G`: detectado por `adb`, pero MIUI rechaza la instalación USB.

### Checksums v1.9.0

- APK release firmado: `85847BE0911D1058798CC526EAAD25E5EFB61F03657B079DA5DA04A7C4EFFCD3` · 10.718.361 bytes.
- AAB release firmado: `E7F1E5B569F231486DDEE5C0507E443E058B228C5D3F4DDDE922D06C1DF690F5` · 10.343.829 bytes.
- Artefactos locales en `artifacts/`: `mesa-viva-1.9.0-release.apk`, `mesa-viva-1.9.0-release.aab`. La release de GitHub añade `mesa-viva-1.9.0-debug.apk`.

## Compose

- **Cáscara nativa completa:** inicio, catálogo, niveles, ajustes, sala y navegación.
- **19 juegos 100 % Compose:** Póker, Caída, Furia, Dominó, Dados, Ajedrez, Damas, Serpientes, Mancala, Go, Jungle, Ludo, Backgammon, Xiangqi, Damas Chinas, Reversi, Ur, **Brisca** y **Truco**.
- **Salas nativas:** Damas, Mancala, Dominó y Serpientes.
- **Replay automático** y **visor web** de Damas/Mancala.
- **Crónica PDF** en cada mesa.
- **Audio nativo** en las 19 mesas.
- **XP real** al terminar.
- **Accesibilidad:** cartas, fichas, casillas, puntos y tableros descritos.
- **Pendiente:** Ludo y Damas Chinas en salas, Brisca/Truco online y los 11 juegos restantes.

## Siguiente versión: 2.0.0

Ver [`v2.0.0.md`](./v2.0.0.md). Incluye:

1. Brisca y Truco en salas con manos privadas.
2. Ludo y Damas Chinas en salas.
3. Relay público y enlaces universales.
4. Replay web de Reversi, Ur, Póker, Caída y Furia.
5. Accesibilidad completa, teléfono físico y Baseline Profile.
6. Shogi, Janggi y el resto del catálogo.

## Dónde está cada cosa

| Ruta | Contenido |
|---|---|
| `src/games/` | 15 motores web |
| `src/ui/GameSurface.tsx` | Superficies web |
| `src/core/` | Tipos, registro, Match autoritativo, niveles |
| `src/net/room.ts` | Salas web, host/invitado y reconexión |
| `server/room-server.mjs` | Relay Node |
| `landing/replay/index.html` | Visor web de replays |
| `android/.../games/` | Motores Kotlin de los 19 juegos |
| `android/.../games/BriscaGame.kt` | Reglas de Brisca |
| `android/.../games/TrucoGame.kt` | Reglas de Truco |
| `android/.../shell/BriscaScreen.kt` | Mesa de Brisca |
| `android/.../shell/TrucoScreen.kt` | Mesa de Truco |
| `android/.../shell/NativeRoom.kt` | Sesión host/invitado, asientos, protocolo y bots |
| `android/.../shell/RoomCodec.kt` | Códec JSON con redacción de manos de Dominó |
| `android/.../shell/SnakesRoom.kt` | Reglas puras de Serpientes para 2–4 |
| `android/.../shell/ReplayExport.kt` | Enlaces nativos/web, archivo JSON e importación |
| `android/.../shell/RoomScreen.kt` | Lobby, sala web y mesas LAN/online |
| `android/.../shell/NativeAudio.kt` | Música y efectos procedurales |
| `android/.../shell/NativeChronicle.kt` | Crónica PDF |
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
6. No afirmar “100 % Compose” global hasta retirar el WebView. **Estado v1.9.0: los 19 juegos jugables son Compose; el WebView solo queda como respaldo de la PWA y la sala remota.**
