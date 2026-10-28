# Changelog

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
