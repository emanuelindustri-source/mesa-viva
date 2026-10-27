# Seguridad

## Reportar una vulnerabilidad

No abras un issue público con datos sensibles. Escribe a **emanuel.industri@gmail.com** con:

- Versión de Mesa Viva y plataforma (web/Android).
- Pasos para reproducir.
- Impacto estimado y cualquier prueba de concepto.
- Si quieres, un informe de Diagnóstico exportado desde la app.

Objetivo de respuesta: 72 horas. No se promete recompensa económica por reportes.

## Modelo de seguridad

- **Local-first**: no hay cuentas, anuncios ni rastreo. El perfil y el historial viven en `localStorage`/almacenamiento del WebView.
- **Salas**: el anfitrión mantiene el estado y valida todas las acciones; el relay solo reenvía mensajes. Los invitados reciben vistas redactadas (nunca las manos ajenas ni los dados ocultos).
- **Relay**: límite de tamaño de mensaje (64 KB), rate limit por conexión, máximo de invitados/salas, limpieza al desconectar y caducidad de 12 h. No persiste partidas.
- **Web**: sin `dangerouslySetInnerHTML`; nombres y chat saneados; CSP en producción; dependencias auditables.
- **Android**: assets servidos con `WebViewAssetLoader` sobre origen HTTPS seguro; cámara solo con permiso explícito; PDF guardado vía MediaStore; sin permisos innecesarios.
- **Diagnóstico**: los errores se guardan localmente y solo salen del dispositivo si el usuario crea el correo. El informe no incluye cartas ni contraseñas.

## Limitaciones conocidas (v0.1.0)

- El relay público no está incluido: hay que desplegar `server/room-server.mjs` o apuntar Ajustes al servidor propio. Si no hay relay, las salas en línea no conectan; los modos locales sí funcionan.
- La sala LAN sin internet por doble QR está en el roadmap; requiere relay nativo en el anfitrión o WebRTC manual.
- Un APK de depuración es instalable, pero no sustituye a una firma de release ni a una revisión de tienda.
- El catálogo de 30 juegos incluye estados “próximamente” y “en investigación”; no todos están implementados.
- Las reglas de juegos tradicionales tienen variantes regionales. Mesa Viva documenta la variante usada y no pretende ser un reglamento oficial.

## Rotación de secretos

El token de GitHub compartido durante el desarrollo debe revocarse en <https://github.com/settings/tokens> y sustituirse por `gh auth login` o un token fine-grained con caducidad corta. Nunca se guarda en el repositorio. El archivo `.env.local` y `android/local.properties` están ignorados.
