# Relevo del proyecto · Mesa Viva

> **No borrar.** Este archivo existe para que cualquier chat, persona o IA sepa dónde quedó el proyecto. Se actualiza al cerrar cada versión, pero no se elimina.

## Estado actual

- **Versión publicada:** 0.5.0
- **Juego:** 13 juegos jugables.
- **Repositorio:** <https://github.com/emanuelindustri-source/mesa-viva>
- **Portada:** <https://emanuelindustri-source.github.io/mesa-viva/>
- **Juego web:** <https://emanuelindustri-source.github.io/mesa-viva/play/>
- **Release:** <https://github.com/emanuelindustri-source/mesa-viva/releases/tag/v0.5.0>
- **Commit de release:** `289d27d`
- **Commit de evidencia:** `2df00cf`
- **VERSION:** `0.5.0`

### Calidad

- 22/22 pruebas de motores.
- TypeScript sin errores.
- Web, APK debug y APK firmado compilados.
- CI, Pages y Android verdes.
- APK firmado: `765E5BEEC0B3C4C7517677E03495A68EECF8A22D098FB7509F1F2FC4273C9068`.
- APK debug: `30461A709B33AA556B8C537E9E236E921DD77F8F2C277D6CC51FCF185D8A8D26`.

### Android real

- Dispositivo: `23117RA68G` (`gaxoozj7q44ldeyp`).
- APK firmado instalado y `MainActivity` abierta sin `FATAL EXCEPTION`; partida real de **Caída** visible en el teléfono.
- Pendiente: partida completa, sala LAN y sensores en el teléfono.

## Dónde está cada cosa

| Ruta | Contenido |
|---|---|
| `src/games/` | Motores de los 13 juegos |
| `src/ui/GameSurface.tsx` | Superficies de juego |
| `src/core/` | Tipos, registro, Match autoritativo, niveles |
| `src/net/room.ts` | Salas, host/invitado y reconexión |
| `server/room-server.mjs` | Relay Node |
| `android/` | APK Kotlin, WebView, relay LAN, cámara, sensores |
| `landing/` | Portada estática de presentación |
| `tools/build-pages.mjs` | Une portada + juego en `dist-site/` |
| `versions/` | Evidencia inmutable por versión |

## Siguiente versión: 0.6.0

Ver [`v0.6.0.md`](./v0.6.0.md). Incluye:

1. Damas Chinas.
2. Póker sin dinero real.
3. Migración a Compose por fases.
4. Prueba completa en teléfono real.
5. Actualización de Kotlin/AGP.

## Compose

**No se ha empezado código Compose.** La app es Kotlin + WebView. La migración completa es una reescritura de toda la interfaz y de los 13 juegos. El plan seguro es híbrido:

1. Cáscara Compose.
2. Juegos piloto Compose con WebView de respaldo.
3. Retirada progresiva del WebView.

Detalle en [`docs/COMPOSE_MIGRATION.md`](../docs/COMPOSE_MIGRATION.md).

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

APK firmado local: `android/app/build/outputs/apk/release/app-release.apk`.

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
