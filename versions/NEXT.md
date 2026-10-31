# Relevo del proyecto · Mesa Viva

> **No borrar.** Este archivo existe para que cualquier chat, persona o IA sepa dónde quedó el proyecto. Se actualiza al cerrar cada versión, pero no se elimina.

## Estado actual

- **Versión publicada:** 2.9.1 (parche de integración sobre 2.9.0).
- **Catálogo:** 30 juegos.
- **Juegos jugables Android:** 30, todos 100 % Jetpack Compose.
- **Juegos jugables web:** 22 (los 15 anteriores más Mahjong americano, Riichi, Mahjong con naipes, Reversi, Ur, Luzhanqi y Ajedrez para tres).
- **Juegos en WebView:** 0 (el WebView queda como respaldo de la PWA y de la sala remota).
- **Salas nativas:** Damas y Mancala (2), Dominó (2–4), Serpientes (2–4), Brisca (2–4), Truco (2), Ludo (2–4) y Damas Chinas (2–6), por LAN o relay online configurable.
- **Manos privadas:** Dominó, Brisca y Truco.
- **Replays:** semilla, paso a paso, automático, enlace, archivo JSON, importación y visor web de Damas/Mancala.
- **Crónica PDF:** 30 mesas, con tarjeta de estadísticas.
- **Audio nativo:** música procedural por festividad y efectos en las 30 mesas.
- **Festividades nativas:** Halloween, Navidad, Reyes, Carnaval, San Valentín, San Patricio, Semana Santa, Corpus, Cruz de Mayo, Día de Muertos, San Juan e independencia por país.
- **Diagnóstico:** correo manual y envío automático opcional por `VITE_DIAGNOSTICS_ENDPOINT`.
- **Repositorio:** <https://github.com/emanuelindustri-source/mesa-viva>
- **Portada:** <https://emanuelindustri-source.github.io/mesa-viva/>
- **Juego web:** <https://emanuelindustri-source.github.io/mesa-viva/play/>
- **Replay web:** <https://emanuelindustri-source.github.io/mesa-viva/replay/>
- **Release:** <https://github.com/emanuelindustri-source/mesa-viva/releases/tag/v2.9.1>
- **VERSION:** `2.9.1`
- **Android:** `versionCode=33`, `versionName=2.9.1`, compileSdk/targetSdk 36.
- **Gradle:** 8.14.4.

### Calidad

- 50/50 pruebas de motores web (30 base + 8 de Mahjong + 12 de tableros).
- 153/153 pruebas Kotlin (146 base + 7 de festividades).
- **203 pruebas en total.**
- TypeScript sin errores.
- Web compilada con Vite 6.4.5 y sincronizada dentro del APK.
- APK debug, APK release firmado y AAB release firmado.
- Mahjong web probado en navegador: descarte humano, tres bots, robo y turno de vuelta sin errores de consola.
- Teléfono físico `22101316G`: detectado por `adb`, pero MIUI rechaza la instalación USB.

### Checksums v2.9.1

- APK release firmado: `4329AB713D41C92ACDE299D7C65B626CA0AB4D2A01279137ACBE1FBF7E4C278F` · 11.081.021 bytes.
- AAB release firmado: `6EEE12D42B8F909CA39D75E87F2B462D216B9553975542C2043FC05F59FB62A4` · 10.697.855 bytes.
- APK debug: `E67749AD64126B6F17E0FBAFB5348FA2A33A1816B36C324ADDB630D3D9772C28` · 14.929.990 bytes.
- Artefactos locales en `artifacts/`: `mesa-viva-2.9.1-release.apk`, `mesa-viva-2.9.1-release.aab` y `mesa-viva-2.9.1-debug.apk`.

## Web

- **Mahjong americano, Riichi y Mahjong con naipes** jugables en el navegador.
- Motor TypeScript con muro de 152/136, flores, comodines, pon, chi, kan, ron, tsumo, riichi, furiten y yaku.
- Mesa web propia con rivales, melds, río de descartes, flores y mano.
- Bots y modo pasar el teléfono.
- Reversi, Ur, Luzhanqi y Ajedrez para tres jugables en la web.
- 22 juegos jugables; faltan 8 para la paridad total.

## Compose

- **Cáscara nativa completa:** inicio, catálogo, niveles, ajustes, sala y navegación.
- **30 juegos 100 % Compose:** Póker, Caída, Furia, Dominó, Dados, Ajedrez, Damas, Serpientes, Mancala, Go, Jungle, Ludo, Backgammon, Xiangqi, Damas Chinas, Reversi, Ur, Brisca, Truco, Soroban, Shogi, Janggi, Aeroplane Chess, Mahjong americano, Riichi, Mahjong con naipes, Luzhanqi, Tren Mexicano, Chickenfoot y Ajedrez para tres.
- **Salas nativas:** Damas, Mancala, Dominó, Serpientes, Brisca, Truco, Ludo y Damas Chinas.
- **Replay automático** y **visor web** de Damas/Mancala.
- **Crónica PDF** en cada mesa.
- **Audio nativo** en las 30 mesas y en las festividades nuevas.
- **XP real** al terminar.
- **Accesibilidad:** cartas, fichas, casillas, puntos y tableros descritos.
- **Pendiente:** 11 idiomas nativos, accesibilidad completa, Baseline Profile, relay público y teléfono físico.

## Siguiente versión: 2.10.0

Ver [`v2.10.0.md`](./v2.10.0.md). Incluye:

1. Portar los 8 juegos web restantes hasta 30/30.
2. Relay público con TLS y moderación.
3. Replays web de todos los juegos.
4. 11 idiomas y festividades completas en la cáscara nativa.
5. Accesibilidad completa, Baseline Profile y pruebas instrumentadas.
6. Teléfono físico y checklist de tienda.

## Dónde está cada cosa

| Ruta | Contenido |
|---|---|
| `src/games/` | 22 motores web |
| `src/games/mahjong.ts` | Motor web de Mahjong (3 variantes) |
| `src/games/reversi.ts` | Motor web de Reversi |
| `src/games/ur.ts` | Motor web del Juego Real de Ur |
| `src/games/luzhanqi.ts` | Motor web de Luzhanqi |
| `src/games/threechess.ts` | Motor web del Ajedrez para tres |
| `src/ui/MahjongSurface.tsx` | Mesa web de Mahjong |
| `src/ui/BoardGameSurface.tsx` | Superficie web de tablero compartida |
| `src/styles/mahjong.css` | Estilos de la mesa web |
| `src/ui/GameSurface.tsx` | Superficies web |
| `src/core/` | Tipos, registro, Match autoritativo, niveles |
| `src/net/room.ts` | Salas web, host/invitado y reconexión |
| `server/room-server.mjs` | Relay Node |
| `landing/replay/index.html` | Visor web de replays |
| `android/.../games/` | Motores Kotlin de los 30 juegos |
| `android/.../games/MahjongGame.kt` | Mahjong americano, Riichi y naipes |
| `android/.../games/LuzhanqiGame.kt` | Luzhanqi con niebla, vías y campamentos |
| `android/.../games/TrainGames.kt` | Tren Mexicano y Chickenfoot |
| `android/.../games/ThreeChessGame.kt` | Ajedrez para tres |
| `android/.../shell/ShellState.kt` | Festividades por fecha y país |
| `android/.../shell/NativeAudio.kt` | Música procedural por festividad |
| `android/.../shell/ShellGames.kt` | Catálogo y mapa de pantallas/deep links |
| `android/.../shell/NativeRoom.kt` | Sesión host/invitado, asientos, protocolo y bots |
| `android/.../shell/RoomCodec.kt` | Códec JSON con redacción de manos |
| `android/.../shell/ReplayExport.kt` | Enlaces nativos/web, archivo e importación |
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
cd android && gradlew.bat assembleDebug
cd android && gradlew.bat assembleRelease
cd android && gradlew.bat bundleRelease
```

## Seguridad

- El token de GitHub compartido en el chat debe revocarse.
- El keystore y la contraseña viven en `C:\Users\Sr.Hidalgo\.mesa-viva\`.
- `android/keystore.properties` está ignorado por git.
- El QR se genera localmente con ZXing.
- La sala nativa valida cada jugada en el anfitrión.
- Las manos privadas se redactan por asiento; el pozo solo viaja como conteo.
- El replay es autocontenido y no incluye datos personales.
- El relay online es opcional y debe ser de confianza.
- El diagnóstico se envía solo si el usuario pulsa el botón; sin endpoint se abre el correo a `emanuel.industri@gmail.com`.
- El PDF se guarda en el dispositivo y solo se comparte si el usuario abre el selector.

## Reglas de continuidad

1. No borrar archivos de `versions/`.
2. Cada versión nueva crea su propio `vX.Y.Z.md`.
3. Mantener este `NEXT.md` actualizado, sin eliminarlo.
4. No guardar tokens, contraseñas ni keystores en el repositorio.
5. Antes de publicar: typecheck, tests web, tests Kotlin, build web, APK, AAB, CI/Pages/Android.
6. No afirmar “100 % Compose” global hasta retirar el WebView. **Estado v2.9.1: los 30 juegos jugables Android son Compose; el WebView solo queda como respaldo de la PWA y la sala remota.**
