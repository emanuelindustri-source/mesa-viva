# Relevo del proyecto · Mesa Viva

> **No borrar.** Este archivo existe para que cualquier chat, persona o IA sepa dónde quedó el proyecto. Se actualiza al cerrar cada versión, pero no se elimina.

## Estado actual

- **Versión publicada:** 1.3.0.
- **Juegos jugables:** 15.
- **Juegos 100 % Compose:** 15 (todos) — Serpientes, Dominó, Ajedrez, Ludo, Mancala, Backgammon, Damas, Dados, Go, Jungle, Xiangqi, Damas Chinas, Caída, Furia de Colores y Póker.
- **Juegos en WebView:** 0 (el WebView queda como respaldo de la PWA y de la sala online).
- **Repositorio:** <https://github.com/emanuelindustri-source/mesa-viva>
- **Portada:** <https://emanuelindustri-source.github.io/mesa-viva/>
- **Juego web:** <https://emanuelindustri-source.github.io/mesa-viva/play/>
- **Release:** <https://github.com/emanuelindustri-source/mesa-viva/releases/tag/v1.3.0>
- **VERSION:** `1.3.0`
- **Android:** `versionCode=15`, `versionName=1.3.0`, compileSdk/targetSdk 36.

### Calidad

- 30/30 pruebas de motores web.
- 55/55 pruebas Kotlin (los quince juegos, incluidos Póker, Caída y Furia).
- **85 pruebas en total.**
- TypeScript sin errores.
- Web compilada con Vite 6.4.5 y sincronizada dentro del APK.
- APK debug, APK release firmado y AAB release firmado.
- Emulador Android 14: inicio, Póker (ciegas, call, flop, check, fold, showdown y siguiente mano), Caída, Furia, Damas, Go, Jungle, Xiangqi, Damas Chinas y Dados verificados.
- Teléfono real `23117RA68G`: detectado por `adb`, pero MIUI bloqueó la instalación por USB (“Instalación bloqueada de Mesa Viva vía USB”). Pendiente habilitar **Instalar vía USB** en Opciones de desarrollador.

### Checksums v1.3.0

- APK release firmado: `4F7768B325C01CEBA57E979CDA40382B91310FA2D1DF5D34ACFFE9EC35576735` · 10.330.905 bytes.
- AAB release firmado: `634145340F43ED7430831EA7D9BE7FFB540039E0620B283CEBFF5461B5BDC8BA` · 9.973.008 bytes.
- Artefactos locales en `artifacts/`: `mesa-viva-1.3.0-release.apk`, `mesa-viva-1.3.0-release.aab`. La release de GitHub añade `mesa-viva-1.3.0-debug.apk`.

## Compose

- **Cáscara nativa completa:** inicio, catálogo, niveles, ajustes, sala y navegación.
- **Quince juegos 100 % Compose:** todos los juegos jugables.
- **Póker:** Texas Hold'em con ciegas, calles, subidas, all-in, botes laterales, refund y showdown; evaluador de 5 entre 7 y bots.
- **Caída:** baraja española, entregas, cantos, caídas, mesa limpia, parejas y bots.
- **Furia:** 108 naipes, comodines, saltos, reversas, roba dos/cuatro, ¡Última! y bots.
- **Sala nativa:** crear/unirse, QR local ZXing, escaneo y selector de ronda.
- **Sincronización:** la cáscara pasa idioma/música/efectos/vibración/movimiento a la web; `ShellSync` devuelve el perfil.
- **Pendiente:** salas nativas, PDF/replays nativos, audio nativo y retirada del WebView de respaldo.

## Siguiente versión: 1.4.0

Ver [`v1.4.0.md`](./v1.4.0.md). Incluye:

1. Salas nativas host/invitado y reconexión.
2. Crónica nativa con PDF y replays con semilla.
3. Accesibilidad completa y audio nativo por juego.
4. Teléfono físico, LAN real y checklist de tienda.
5. Gradle 8.14.4+ y perfil de rendimiento.
6. Retirada del WebView de respaldo al desacoplar la sala online y la PWA.

## Dónde está cada cosa

| Ruta | Contenido |
|---|---|
| `src/games/` | 15 motores web, incluido `poker.ts` |
| `src/ui/GameSurface.tsx` | Superficies web, incluidas Caída, Furia y Póker |
| `src/core/` | Tipos, registro, Match autoritativo, niveles |
| `src/net/room.ts` | Salas, host/invitado y reconexión |
| `server/room-server.mjs` | Relay Node |
| `android/.../MainActivity.kt` | Cáscara Compose |
| `android/.../GameWebActivity.kt` | WebView de respaldo + puentes + params nativos |
| `android/.../games/` | Motores Kotlin de los quince juegos, incluido `PokerGame.kt` |
| `android/.../shell/` | Estado, textos, tema, pantallas, sala, QR y las quince mesas |
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
- El QR se genera localmente con ZXing; no se envía a un servicio.
- El diagnóstico se envía solo si el usuario abre el correo a `emanuel.industri@gmail.com`.

## Reglas de continuidad

1. No borrar archivos de `versions/`.
2. Cada versión nueva crea su propio `vX.Y.Z.md`.
3. Mantener este `NEXT.md` actualizado, sin eliminarlo.
4. No guardar tokens, contraseñas ni keystores en el repositorio.
5. Antes de publicar: typecheck, tests web, tests Kotlin, build web, APK, AAB, CI/Pages/Android.
6. No afirmar “100 % Compose” global hasta retirar el WebView de los 15 juegos. **Estado v1.3.0: los quince juegos jugables ya son Compose; el WebView solo queda como respaldo.**
