# Relevo del proyecto · Mesa Viva

> **No borrar.** Este archivo existe para que cualquier chat, persona o IA sepa dónde quedó el proyecto. Se actualiza al cerrar cada versión, pero no se elimina.

## Estado actual

- **Versión publicada:** 1.2.0.
- **Juegos jugables:** 15.
- **Juegos 100 % Compose:** 14 (Serpientes, Dominó, Ajedrez, Ludo, Mancala, Backgammon, Damas, Dados, Go, Jungle, Xiangqi, Damas Chinas, Caída y Furia de Colores).
- **Juegos en WebView:** 1 (Póker).
- **Repositorio:** <https://github.com/emanuelindustri-source/mesa-viva>
- **Portada:** <https://emanuelindustri-source.github.io/mesa-viva/>
- **Juego web:** <https://emanuelindustri-source.github.io/mesa-viva/play/>
- **Release:** <https://github.com/emanuelindustri-source/mesa-viva/releases/tag/v1.2.0>
- **VERSION:** `1.2.0`
- **Android:** `versionCode=14`, `versionName=1.2.0`, compileSdk/targetSdk 36.

### Calidad

- 30/30 pruebas de motores web.
- 48/48 pruebas Kotlin (Dominó, Ajedrez, Ludo, Mancala, Backgammon, Damas, Dados, Go, Jungle, Xiangqi, Damas Chinas, Caída y Furia).
- **78 pruebas en total.**
- TypeScript sin errores.
- Web compilada con Vite 6.4.5 y sincronizada dentro del APK.
- APK debug, APK release firmado y AAB release firmado.
- Emulador Android 14: inicio, Caída (entrega, mano, table, jugadas, bots), Furia (mano, jugadas, robo, bots), Damas, Go, Jungle, Xiangqi, Damas Chinas y Dados verificados.
- Teléfono real `23117RA68G`: detectado por `adb`, pero MIUI bloqueó la instalación por USB (“Instalación bloqueada de Mesa Viva vía USB”). Pendiente habilitar **Instalar vía USB** en Opciones de desarrollador.

### Checksums v1.2.0

- APK release firmado: `0E4A075680A0D886FEF3DE9C6593FECCC5C8FE2B4D3562B50983B6DA7454FD37` · 10.265.373 bytes.
- AAB release firmado: `CAD939E0CA80122EF5597E7E0C2E9B68924D14F15AA51460EBE5BA4FC9213906` · 9.907.089 bytes.
- Artefactos locales en `artifacts/`: `mesa-viva-1.2.0-release.apk`, `mesa-viva-1.2.0-release.aab`.
- El APK de depuración de CI se añade al tag `v1.2.0`.

## Compose

- **Cáscara nativa completa:** inicio, catálogo, niveles, ajustes, sala y navegación.
- **Catorce juegos 100 % Compose:** los doce de v1.1.0 más Caída y Furia de Colores.
- **Caída:** baraja española dibujada, entrega, cantos, caídas, mesa limpia, parejas, meta y bots.
- **Furia:** 108 naipes, comodines, saltos, reversas, roba dos/cuatro, ¡Última! y bots.
- **Sala nativa:** crear/unirse, QR local ZXing, escaneo y selector de ronda.
- **Sincronización:** la cáscara pasa idioma/música/efectos/vibración/movimiento a la web; `ShellSync` devuelve el perfil.
- **Pendiente:** salas nativas, Póker nativo, PDF/replays nativos, audio nativo y retirada del WebView.

## Siguiente versión: 1.3.0

Ver [`v1.3.0.md`](./v1.3.0.md). Incluye:

1. Salas nativas host/invitado y reconexión.
2. Póker 100 % Compose para cerrar la paridad.
3. Crónica nativa con PDF y replays con semilla.
4. Accesibilidad completa y audio nativo por juego.
5. Teléfono físico, LAN real y checklist de tienda.
6. Gradle 8.14.4+ y perfil de rendimiento.
7. Retirada del WebView al cerrar la paridad total.

## Dónde está cada cosa

| Ruta | Contenido |
|---|---|
| `src/games/` | 15 motores web, incluido `poker.ts` |
| `src/ui/GameSurface.tsx` | Superficies web, incluidas Caída, Furia y Póker |
| `src/core/` | Tipos, registro, Match autoritativo, niveles |
| `src/net/room.ts` | Salas, host/invitado y reconexión |
| `server/room-server.mjs` | Relay Node |
| `android/.../MainActivity.kt` | Cáscara Compose |
| `android/.../GameWebActivity.kt` | Mesa compartida WebView + puentes + params nativos |
| `android/.../games/` | Motores Kotlin: Dominó, Ajedrez, Ludo, Mancala, Backgammon, Damas, Dados, Go, Jungle, Xiangqi, Damas Chinas, Caída y Furia |
| `android/.../shell/` | Estado, textos, tema, pantallas, sala, QR y las catorce mesas |
| `android/.../shell/CardUi.kt` | Barajas española y Furia, reversos y partículas |
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
6. No afirmar “100 % Compose” global hasta retirar el WebView de los 15 juegos.
