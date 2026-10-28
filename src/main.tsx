import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import '@fontsource/fraunces/400.css';
import '@fontsource/fraunces/600.css';
import '@fontsource/fraunces/700.css';
import '@fontsource/outfit/400.css';
import '@fontsource/outfit/500.css';
import '@fontsource/outfit/600.css';
import '@fontsource/outfit/700.css';
import './styles/base.css';
import './styles/app.css';
import './styles/table.css';
import './styles/room.css';
import App from './App';
import { ErrorBoundary } from './ErrorBoundary';
import { installDiagnostics } from './services/diagnostics';
import { setLocale } from './i18n';
import { useAppStore } from './store/appStore';
import { initAudio } from './audio/engine';

installDiagnostics();
setLocale(useAppStore.getState().settings.language);

if (typeof localStorage !== 'undefined' && localStorage.getItem('mesa-viva-lan-host') === '1') {
  try {
    (window as unknown as { MesaViva?: { stopLanServer?: () => void } }).MesaViva?.stopLanServer?.();
  } catch {
    // Si no hay puente nativo, solo se limpian las claves.
  }
  localStorage.removeItem('mesa-viva-lan-host');
  localStorage.removeItem('mesa-viva-lan-url');
  localStorage.removeItem('mesa-viva-room-server');
}

if ('serviceWorker' in navigator && import.meta.env.PROD) {
  window.addEventListener('load', () => {
    void navigator.serviceWorker.register('./sw.js').catch(() => undefined);
  });
}

if (navigator.storage?.persist) {
  void navigator.storage.persist().catch(() => undefined);
}

window.addEventListener('pointerdown', () => {
  void initAudio();
}, { once: true });

const root = document.getElementById('root');
if (!root) throw new Error('No se encontró #root');

createRoot(root).render(
  <StrictMode>
    <ErrorBoundary>
      <App />
    </ErrorBoundary>
  </StrictMode>,
);
