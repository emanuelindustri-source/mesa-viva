# Changelog

## 0.2.0 · 2026-10-28

La mesa se amplía y se vuelve local.

### Añadido

- **Go** 9×9 y 13×13: capturas de grupos, ko, pases, rendición, komi 6.5 y conteo de territorio.
- **Jungle / Dou Shou Qi**: río, trampas, jerarquía animal, saltos de tigre y león, guaridas y bots con búsqueda alfa-beta.
- **Sala LAN sin internet en Android**: relay WebSocket nativo en el teléfono anfitrión, QR con la IP local y código de sala, unión automática del invitado y limpieza del servidor al salir.
- Preparación de Go con tamaño de tablero 9×9/13×13.
- 3 pruebas nuevas de motores (15 en total), incluidas las reglas básicas de Go y Jungle.

### Cambiado

- Catálogo: Go y Jungle pasan de “próximos” a jugables.
- Portada, README y contadores: de 8 a 10 juegos.
- Salas LAN: el invitado configura el relay desde el QR sin escribir direcciones.

### Corregido

- El cierre de una sala LAN detiene el relay nativo y limpia las claves locales.
- La app limpia un relay LAN huérfano al arrancar.

## 0.1.1 · 2026-10-28

Pulido de mesa y CI.

### Corregido

- Furia y Dominó muestran “7 cartas” / “6 fichas” en vez de “puntos” en la mano.
- Los tableros de ajedrez y damas ya no muestran coordenadas en cada casilla; las coordenadas quedan como etiquetas accesibles.
- Mancala identifica los almacenes con el nombre del jugador o “Tú”, no con B/W.
- El workflow Android usa el SDK preinstalado del runner (la acción antigua intentaba instalar un paquete `tools` eliminado).
- Onboarding: rejilla de avatares en 5 columnas para no recortarse.

## 0.1.0 · 2026-10-27

Primera mesa funcional.

### Añadido

- 8 juegos con bots: Caída, Furia de Colores, Dominó, Dados Mentirosos, Ajedrez, Damas, Serpientes y Escaleras, Mancala.
- Modo pasar el teléfono con pantalla de privacidad; mezcla de humanos y bots.
- Salas en línea con relay WebSocket, código de 6 caracteres, QR, chat, playlist y puntuación acumulada.
- Host autoritativo y vistas redactadas por jugador.
- Perfil local con 10 niveles, XP, rachas, victorias y recompensas.
- 11 idiomas de interfaz y temas por festividad.
- Semana Santa/Carnaval/Corpus calculados con Pascua; independencia según país.
- Color de acento por temporada, Material You o color dominante del fondo.
- Música generativa y SFX sintetizados; partículas; háptica; sensores para agitar el teléfono.
- Crónica de partida con últimos 5 movimientos, historial completo y PDF.
- Diagnóstico local con envío por correo y exportación.
- PWA instalable/offline y APK Android con `WebViewAssetLoader`.
- CI, Pages, build Android, Dependabot, SECURITY, privacidad, roadmap y arquitectura.

### Notas

- La sala LAN sin servidor por doble QR queda para v0.2; los modos locales sí funcionan offline.
- El catálogo muestra 30 juegos con su estado; los jugables son los 8 anteriores.
- El APK de CI es de depuración; la firma de release se documenta en `docs/DEPLOY.md`.
- Reglas de juegos tradicionales adaptadas y documentadas como variante Mesa Viva.
