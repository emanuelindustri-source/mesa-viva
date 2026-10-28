# Relevo del proyecto · Mesa Viva

> **No borrar.** Este archivo existe para que cualquier chat, persona o IA sepa dónde quedó el proyecto. Se actualiza al cerrar cada versión, pero no se elimina.

## Estado actual

- **Versión publicada:** 0.6.0
- **Juego:** 13 juegos jugables.
- **Repositorio:** <https://github.com/emanuelindustri-source/mesa-viva>
- **Portada:** <https://emanuelindustri-source.github.io/mesa-viva/>
- **Juego web:** <https://emanuelindustri-source.github.io/mesa-viva/play/>
- **Release:** <https://github.com/emanuelindustri-source/mesa-viva/releases/tag/v0.6.0>
- **VERSION:** `0.6.0`

### Calidad

- 22/22 pruebas de motores.
- TypeScript sin errores.
- Web, APK debug y APK firmado compilados.
- CI, Pages y Android verdes.
- APK firmado v0.6.0: `2C021D8FF5C7E31D1B3A51B61A5997CF13513285499419B2C5C18E860E2DA9B6`.
- APK firmado más grande (8.19 MB) por Compose.

### Android real

- Dispositivo: `23117RA68G` (`gaxoozj7q44ldeyp`).
- APK firmado instalado; `MainActivity` en foco.
- Inicio y mesa de **Caída** renderizan; sin `FATAL EXCEPTION`.
- Pendiente: partida completa, sala LAN y sensores.

## Compose

- **Fase 0 completada:** `MainActivity` es `ComponentActivity`, `setContent` + `AndroidView` hospeda la mesa WebView.
- **La mesa sigue en WebView.** No es una migración completa.
- Plan: `docs/COMPOSE_MIGRATION.md`.
- Próxima fase: inicio, ajustes, niveles y lobby nativos.

## Siguiente versión: 0.7.0

Ver [`v0.7.0.md`](./v0.7.0.md). Incluye:

1. Compose fase 1 (pantallas nativas).
2. Damas Chinas.
3. Póker sin dinero real.
4. Actualización de Kotlin/AGP.
5. Pruebas en teléfono real.

## Dónde está cada cosa

| Ruta | Contenido |
|---|---|
| `src/games/` | Motores de los 13 juegos |
| `src/ui/GameSurface.tsx` | Superficies de juego web |
| `src/core/` | Tipos, registro, Match autoritativo, niveles |
| `src/net/room.ts` | Salas, host/invitado y reconexión |
| `server/room-server.mjs` | Relay Node |
| `android/` | APK Kotlin, Compose host, WebView, relay LAN |
| `landing/` | Portada estática |
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
cd android && gradlew.bat assembleRelease
```

## Seguridad

- El token de GitHub compartido en el chat debe revocarse.
- El keystore y la contraseña viven en `C:\Users\Sr.Hidalgo\.mesa-viva\`; no se copian al repositorio.
- `android/keystore.properties` está ignorado por git.

## Reglas de continuidad

1. No borrar archivos de `versions/`.
2. Cada versión nueva crea su propio `vX.Y.Z.md`.
3. Mantener este `NEXT.md` actualizado, sin eliminarlo.
4. No guardar tokens, contraseñas ni keystores en el repositorio.
5. Antes de publicar: typecheck, tests, build web, APK, CI/Pages/Android.
