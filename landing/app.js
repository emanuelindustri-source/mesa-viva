/* Mesa Viva landing: sin dependencias, sin rastreo. */
(function () {
  const year = new Date().getFullYear();
  const note = document.querySelector('.footer-note');
  if (note) note.textContent = `© ${year} Mesa Viva · Código MIT · Arte y textos originales · Sin anuncios ni rastreo.`;

  // Copiar el SHA del APK firmado con un toque.
  document.querySelectorAll('.hash code').forEach((code) => {
    code.style.cursor = 'copy';
    code.title = 'Toca para copiar';
    code.addEventListener('click', async () => {
      try {
        await navigator.clipboard.writeText(code.textContent.trim());
        const original = code.textContent;
        code.textContent = 'Copiado ✓';
        setTimeout(() => { code.textContent = original; }, 1200);
      } catch (_) {
        // Sin permiso de portapapeles: no pasa nada.
      }
    });
  });
})();
