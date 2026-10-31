# Archivos de versión

Cada versión de Mesa Viva tiene su propio archivo en esta carpeta. **No se borran ni se sobrescriben** los archivos de versiones anteriores: son la evidencia histórica de lo que se construyó, probó y publicó.

## Índice

| Versión | Fecha | Estado | Archivo |
|---|---|---|---|
| 0.1.0 | 2026-10-27 | Publicada | [`v0.1.0.md`](./v0.1.0.md) |
| 0.1.1 | 2026-10-28 | Publicada | [`v0.1.1.md`](./v0.1.1.md) |
| 0.2.0 | 2026-10-28 | Publicada | [`v0.2.0.md`](./v0.2.0.md) |
| 0.3.0 | 2026-10-28 | Publicada | [`v0.3.0.md`](./v0.3.0.md) |
| 0.4.0 | 2026-10-28 | Publicada | [`v0.4.0.md`](./v0.4.0.md) |
| 0.5.0 | 2026-10-28 | Publicada | [`v0.5.0.md`](./v0.5.0.md) |
| 0.6.0 | 2026-10-28 | Publicada | [`v0.6.0.md`](./v0.6.0.md) |
| 0.7.0 | 2026-10-28 | Publicada | [`v0.7.0.md`](./v0.7.0.md) |
| 0.7.1 | 2026-10-28 | Publicada | [`v0.7.1.md`](./v0.7.1.md) |
| 0.8.0 | 2026-10-28 | Publicada | [`v0.8.0.md`](./v0.8.0.md) |
| 0.9.0 | 2026-10-28 | Publicada | [`v0.9.0.md`](./v0.9.0.md) |
| 1.0.0 | 2026-10-28 | Publicada | [`v1.0.0.md`](./v1.0.0.md) |
| 1.1.0 | 2026-10-28 | Publicada | [`v1.1.0.md`](./v1.1.0.md) |
| 1.2.0 | 2026-10-28 | Publicada | [`v1.2.0.md`](./v1.2.0.md) |
| 1.3.0 | 2026-10-29 | Publicada | [`v1.3.0.md`](./v1.3.0.md) |
| 1.4.0 | 2026-10-29 | Publicada | [`v1.4.0.md`](./v1.4.0.md) |
| 1.5.0 | 2026-10-29 | Publicada | [`v1.5.0.md`](./v1.5.0.md) |
| 1.6.0 | 2026-10-29 | Publicada | [`v1.6.0.md`](./v1.6.0.md) |
| 1.7.0 | 2026-10-29 | Publicada | [`v1.7.0.md`](./v1.7.0.md) |
| 1.8.0 | 2026-10-29 | Publicada | [`v1.8.0.md`](./v1.8.0.md) |
| 1.9.0 | 2026-10-29 | Publicada | [`v1.9.0.md`](./v1.9.0.md) |
| 2.0.0 | 2026-10-29 | Publicada | [`v2.0.0.md`](./v2.0.0.md) |
| 2.1.0 | 2026-10-29 | Publicada | [`v2.1.0.md`](./v2.1.0.md) |
| 2.2.0 | 2026-10-30 | Publicada | [`v2.2.0.md`](./v2.2.0.md) |
| 2.3.0 | 2026-10-30 | Publicada | [`v2.3.0.md`](./v2.3.0.md) |
| 2.4.0 | 2026-10-30 | Publicada | [`v2.4.0.md`](./v2.4.0.md) |
| 2.5.0 | 2026-10-30 | Publicada | [`v2.5.0.md`](./v2.5.0.md) |
| 2.6.0 | 2026-10-30 | Publicada | [`v2.6.0.md`](./v2.6.0.md) |
| 2.7.0 | 2026-10-30 | Publicada | [`v2.7.0.md`](./v2.7.0.md) |
| 2.7.1 | 2026-10-30 | Publicada | [`v2.7.1.md`](./v2.7.1.md) |
| 2.8.0 | 2026-10-30 | Pendiente | [`v2.8.0.md`](./v2.8.0.md) |

## Relevo

- [`NEXT.md`](./NEXT.md) — estado actual, pendientes y contexto para cualquier IA o persona. **No se borra.**
- [`../docs/COMPOSE_MIGRATION.md`](../docs/COMPOSE_MIGRATION.md) — plan de migración a Compose.

## Reglas

1. Cada release crea un archivo `vX.Y.Z.md` nuevo; los anteriores se conservan intactos.
2. El archivo incluye: objetivos, cambios, decisiones, pruebas ejecutadas, enlaces, checksums, problemas conocidos y pendientes.
3. Si una release se corrige, se crea una versión nueva (`v0.1.1`), no se edita el archivo de `v0.1.0`.
4. `CHANGELOG.md` es el resumen cronológico; `versions/` es la evidencia detallada.
5. El archivo `VERSION` de la raíz indica la versión publicada actual.
6. Nunca se incluyen tokens, contraseñas, keystores ni datos personales en estos archivos.

## Enlaces canónicos

- Repositorio: <https://github.com/emanuelindustri-source/mesa-viva>
- Web/PWA: <https://emanuelindustri-source.github.io/mesa-viva/>
- Releases: <https://github.com/emanuelindustri-source/mesa-viva/releases>
- Relay: `server/room-server.mjs` (despliegue en `docs/DEPLOY.md`)
