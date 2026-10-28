# QA de Caída · v0.7.1

Fecha: 2026-10-28 · Base: `572041c` · Web y Android 16.

## Criterios de aceptación

| Criterio | Resultado | Evidencia |
|---|---|---|
| Partida contra bots dura 8–15 min | **Aproximado** | Simulación de 4 bots a 100 puntos: 237–315 acciones. Con 1500 ms + 700 ms en eventos lentos y pausas de mano se estiman 8–10 min. Falta grabación real de una partida completa. |
| Ningún frame muestra la píldora sobre mesa vacía | **Corregido por lógica** | El efecto limpia `flash` cuando `lastCapture` es `null` o cambia `handNo`; la clave incluye `handNo`. No se detectó el estado en la prueba de mano 1; falta revisar la grabación de 60 s. |
| Cartas renderizadas == contador de TU MANO | **Sí en la prueba** | 3 cartas renderizadas y “3 cartas” en la etiqueta; se eliminó la animación de salida del abanico. |
| Ninguna carta toca los bordes | **Sí en 892×678** | Mano centrada, 3 cartas visibles; `hand` con padding lateral y sin `exit`. Falta verificar 720×1600 y 1080×2400 con captura. |
| Los 4 jugadores se ven completos | **Sí en 892×678** | Chips compactos de 116 px mínimos, flexibles y con scroll snap. |
| “Todavía no es tu turno” solo si no lo es | **Corregido** | `dispatch` valida `match.view(actorId).turnId` antes de enviar `play`/`canto`; si cambió, muestra “La mesa cambió de turno”. Falta prueba manual de 1 min. |
| Las 4 cartas de la entrega se ven | **Sí** | Overlay de entrega con 4 cartas, etiqueta y +puntos; se midió 486×108 px y termina antes de la mano. |
| Reglas coinciden con el motor | **Sí** | Reglas actualizadas con caída 1-7=1, 10=2, 11=3, 12=4; mesa +5; entrega 2/1; cantos 1–2/3/3/4/4/5/5/6; 10 cartas=1; meta 100; canto antes de la primera carta; cartas sobrantes al último que cayó. |
| `npx vitest run` | **Verde** | 25/25 pruebas. |
| `python tools/check_i18n.py` | **Verde en faltantes** | `missing 0`; quedan claves no usadas, no bloqueantes. |
| `npm run build` | **Verde** | Build de producción correcto. |
| Grabación de 60 s sin saltos | **Pendiente** | No se grabó en esta sesión; el teléfono quedó bloqueado tras la instalación. Se validó el layout por DOM y captura. |

## Cambios verificables en la UI

- Cabecera: 4 chips visibles, puntos grandes y cartas en pequeño.
- Mano: 3 cartas, 3 en el contador, sin cartas de salida.
- Entrega: 4 cartas visibles, “Entrega fallida/perfecta” y puntos.
- Acción: “Rendirse” visible; “Cantar” solo antes de la primera carta.
- Log: últimos movimientos con pluralización (“robó 1 carta” / “robó 3 cartas”).
- Pie: sin “Agita el teléfono” ni “Animaciones activadas” en Caída.

## Pendientes de validación manual

1. Grabar 60 s de una partida real a 30 fps y revisar frame a frame.
2. Probar en 720×1600 y 1080×2400.
3. Completar una partida de 100 puntos y medir el tiempo real.
4. Probar la sala LAN y la reconexión en el teléfono.
