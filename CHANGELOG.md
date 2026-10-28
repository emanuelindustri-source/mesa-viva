# Changelog

## 0.7.1 · 2026-10-28

Caída, mesa legible.

### Corregido

- Bots de Caída a 1500 ms + 700 ms tras caída, canto, entrega o fin de mano.
- Entrega visible con 4 cartas, resultado y puntos.
- Carta recién jugada resaltada; capturas con fantasma, flecha y flotante +N.
- Mano centrada sin cartas cortadas ni frames con cartas de más.
- Cabecera compacta para 4 jugadores.
- Píldora de captura con limpieza por mano; ya no queda congelada.
- Barra de acciones con altura reservada y toast desplazado.
- Canto solo antes de la primera carta; sin victoria instantánea.
- Reglas completas y meta por defecto 100.
- Pluralización y pistas de cartas corregidas.
- Partículas capadas y `reduceMotion` automático en equipos de baja potencia.
- 25 pruebas.

## 0.7.0 · 2026-10-28

La estrella.

### Añadido

- **Damas Chinas** con tablero estrella de 121 hoyos y coordenadas axiales.
- 2–6 jugadores, 10 fichas, saltos encadenados y meta en el triángulo opuesto.
- Bot por cercanía a la meta y saltos.
- 23 pruebas de motores.

### Cambiado

- Catálogo: 14 juegos jugables.
- Versión de app, service worker y Android a 0.7.0; `versionCode=8`.

### Pendiente

- Póker y Compose fase 1 pasan a v0.8.0.

## 0.6.0 · 2026-10-28

Compose, primera piedra.

### Añadido

- `MainActivity` migrada a `ComponentActivity` y `setContent` con `AndroidView`.
- Compose BOM 2024.12.01, activity-compose, ui, foundation y material3.
- Plugin de Compose para Kotlin 2.0.21.
- APK v0.6.0 probado e instalado en el teléfono real `23117RA68G`; inicio y Caída renderizan sin cierres.

### Notas

- La mesa sigue en WebView; **no es una migración completa a Compose**.
- Damas Chinas y Póker pasan a v0.7.0.
- El APK sube a 8.19 MB por el runtime de Compose.

## 0.5.0 · 2026-10-28

El general y el río.

### Añadido

- **Xiangqi**: tablero 9×10, palacio, río, elefante con ojo, caballo con pata, cañón con pantalla, soldados y generales enfrentados.
- Jaque, jaque mate, ahogado, rendición y bot alfa-beta.
- 22 pruebas de motores.
- Portada y versión 0.5.0.

### Pendiente

- Damas Chinas y Póker pasan a v0.6.0.
- Prueba de sala LAN en teléfono físico.

## 0.4.0 · 2026-10-28

Mesa resistente y Android 16.

### Añadido

- **Reconexión de invitados**: el invitado conserva su asiento y vuelve a la sala tras perder la conexión.
- Relay Node y relay LAN de Android aceptan `rejoin`, mantienen una ventana de gracia de 60 s y distinguen salida voluntaria de caída.
- Avisos de “reconectando” y “volviste a la mesa”.
- **Android 16**: edge-to-edge con insets de barras/teclado y back predictivo integrado con el WebView.
- **APK de release firmado** (RSA 4096) además del APK de depuración.
- **Portada web** de presentación en la raíz de Pages, con historial de versiones y descargas; el juego queda en `/play/`.
- Prueba de protocolo de reconexión con relay real.

### Cambiado

- Versión de app y service worker a 0.4.0; `versionCode=5`.
- Android se mantiene en `targetSdk=36` y sin librerías nativas `.so`.

### Pendiente

- Los juegos Damas Chinas, Xiangqi y Póker pasan a v0.5.0 para no sacrificar calidad.
- La prueba de la sala LAN sigue pendiente en teléfono físico.

## 0.3.0 · 2026-10-28

La mesa clásica.

### Añadido

- **Ludo** 2–4 jugadores: cuatro fichas, salida con 6, capturas, casillas seguras, tres seises, turno extra y meta exacta.
- **Backgammon**: 15 fichas, barra, capturas, dobles, bear-off y conteo de pips.
- Tableros interactivos propios para Ludo y Backgammon, con selección de fichas y destinos.
- 21 pruebas de motores (6 nuevas para Ludo y Backgammon).

### Cambiado

- Catálogo: Ludo y Backgammon pasan a jugables.
- Portada, README y contadores: de 10 a 12 juegos.
- Versión de app y service worker a 0.3.0.
- Android: `compileSdk`/`targetSdk` 36 y workflow con SDK 36.

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
