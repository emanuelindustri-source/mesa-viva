# Contribuir a Mesa Viva

Gracias por querer sumarte a la mesa. Este proyecto busca ser hospitalario, original y técnicamente serio.

## Antes de abrir un issue

- Revisa el catálogo y el roadmap; puede que el juego ya esté planificado.
- Si es un fallo, adjunta el informe de **Ajustes → Diagnóstico** (sin datos personales) y los pasos para reproducirlo.
- Si es una idea de juego, indica reglas, número de jugadores, duración y una fuente cultural o de referencia.

## Flujo de trabajo

1. Crea una rama: `feat/nombre-corto`, `fix/nombre-corto` o `docs/nombre-corto`.
2. Instala: `npm install`.
3. Desarrollo: `npm run dev` y `npm run rooms` si tocas salas.
4. Antes del PR:
   ```bash
   npm run typecheck
   npm run test
   npm run build
   python tools/check_i18n.py
   ```
5. Abre un PR describiendo el cambio, capturas si es UI y cómo probarlo.

## Estilo

- TypeScript estricto. Evita `any`; prefiere tipos discriminados.
- Motores de juego puros y deterministas: reciben el RNG, devuelven estado nuevo y eventos; no tocan el DOM.
- Toda acción se valida en el motor. Nunca confíes en el cliente.
- Textos nuevos: español como fuente (`src/i18n/es.ts`) y al menos inglés. Si puedes, añade el resto; el fallback es inglés.
- Nada de arte, música, marcas o reglamentos propietarios. Todo debe ser original o tener licencia compatible.
- Respeta `prefers-reduced-motion`, el contraste alto y el tamaño táctil mínimo de 44 px.

## Seguridad

No incluyas secretos, tokens, keystores ni datos de jugadores. Si encuentras una vulnerabilidad, sigue `SECURITY.md`; no la publiques en un issue.

Al contribuir aceptas que tu código se publique bajo MIT y que el arte/textos originales se compartan bajo CC BY-SA 4.0.
