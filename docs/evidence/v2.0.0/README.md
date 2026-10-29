# Evidencia visual · v2.0.0

Capturas del emulador `Pixel_7_API_34` (Android 14, API 34) con la APK de release firmada `mesa-viva-2.0.0-release.apk` y un invitado WebSocket real contra el relay LAN del anfitrión.

| Archivo | Contenido |
|---|---|
| `home.png` | Inicio 2.0.0 |
| `settings.png` | Ajustes con la versión 2.0.0 |
| `brisca-room.png` | Brisca en sala: anfitrión Android + invitado real, mano privada y baza |
| `truco-room.png` | Truco en sala: envido resuelto, truco aceptado y baza |

## Pruebas de sala

- **Brisca:** el invitado recibió su mano real (`oros-11`, `bastos-7`, `bastos-12`) y los rivales como `h0-0`, `h0-1`, `h0-2`; el anfitrión vio 3 cartas y el invitado 2 después de jugar.
- **Truco:** el invitado recibió su mano real y el rival como `th0-*`; el envido se resolvió 6–11, el truco fue aceptado y la baza se sincronizó.
- **Desconexión:** al cerrar el invitado, el anfitrión activó el bot, que cantó Retruco y Vale Cuatro y jugó su carta.
- **Autoridad:** el anfitrión valida asiento, turno y canto; las jugadas fuera de turno no se aplican.

## Pruebas automáticas

- **30/30** motores web.
- **104/104** pruebas Kotlin, incluidas 3 pruebas nuevas del códec de cartas.
- **134 pruebas en total.**

El teléfono físico `22101316G` sigue bloqueado por MIUI para instalación USB; la verificación se hizo en emulador y con un cliente WebSocket externo.

Los checksums y detalles completos están en [`../../versions/v2.0.0.md`](../../versions/v2.0.0.md).
