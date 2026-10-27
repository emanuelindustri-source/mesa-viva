import { APP_VERSION, CONTACT_EMAIL } from '../core/constants';
import { uid } from '../core/utils';

export type DiagLevel = 'error' | 'warn' | 'info';

export interface DiagEntry {
  id: string;
  at: number;
  level: DiagLevel;
  message: string;
  stack?: string;
  context?: string;
}

export interface DiagAction {
  at: number;
  name: string;
  detail?: string;
}

const STORAGE_KEY = 'mesa-viva-diag-v1';
const MAX_ERRORS = 80;
const MAX_ACTIONS = 60;

interface DiagStore {
  errors: DiagEntry[];
  actions: DiagAction[];
}

function read(): DiagStore {
  if (typeof localStorage === 'undefined') return { errors: [], actions: [] };
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return { errors: [], actions: [] };
    const parsed = JSON.parse(raw) as Partial<DiagStore>;
    return {
      errors: Array.isArray(parsed.errors) ? parsed.errors.slice(0, MAX_ERRORS) : [],
      actions: Array.isArray(parsed.actions) ? parsed.actions.slice(0, MAX_ACTIONS) : [],
    };
  } catch {
    return { errors: [], actions: [] };
  }
}

function write(store: DiagStore): void {
  if (typeof localStorage === 'undefined') return;
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(store));
  } catch {
    // El almacenamiento puede estar lleno; el diagnóstico no debe romper la app.
  }
}

let store = read();

export function diag(level: DiagLevel, message: string, stack?: string, context?: string): void {
  const entry: DiagEntry = {
    id: uid('diag'),
    at: Date.now(),
    level,
    message: message.slice(0, 2000),
    ...(stack ? { stack: stack.slice(0, 8000) } : {}),
    ...(context ? { context: context.slice(0, 1000) } : {}),
  };
  store = { ...store, errors: [entry, ...store.errors].slice(0, MAX_ERRORS) };
  write(store);
}

export function trackAction(name: string, detail?: string): void {
  store = {
    ...store,
    actions: [{ at: Date.now(), name: name.slice(0, 80), ...(detail ? { detail: detail.slice(0, 300) } : {}) }, ...store.actions].slice(0, MAX_ACTIONS),
  };
  write(store);
}

export function getErrors(): DiagEntry[] {
  return store.errors;
}

export function getActions(): DiagAction[] {
  return store.actions;
}

export function clearDiagnostics(): void {
  store = { errors: [], actions: [] };
  write(store);
}

export function installDiagnostics(): void {
  if (typeof window === 'undefined') return;
  window.addEventListener('error', (event) => {
    diag('error', event.message || 'Error de ventana', event.error instanceof Error ? event.error.stack : undefined, `${event.filename}:${event.lineno}:${event.colno}`);
  });
  window.addEventListener('unhandledrejection', (event) => {
    const reason = event.reason;
    diag(
      'error',
      reason instanceof Error ? reason.message : String(reason ?? 'Promesa rechazada'),
      reason instanceof Error ? reason.stack : undefined,
      'unhandledrejection',
    );
  });
}

export function getSystemInfo(): Record<string, string> {
  if (typeof window === 'undefined') return { version: APP_VERSION };
  const nav = navigator as Navigator & {
    deviceMemory?: number;
    connection?: { effectiveType?: string; downlink?: number };
  };
  const info: Record<string, string> = {
    version: APP_VERSION,
    platform: nav.platform || 'desconocida',
    userAgent: nav.userAgent,
    language: nav.language,
    languages: (nav.languages ?? []).join(', '),
    online: nav.onLine ? 'sí' : 'no',
    screen: `${window.screen.width}×${window.screen.height} @${window.devicePixelRatio}x`,
    viewport: `${window.innerWidth}×${window.innerHeight}`,
    cores: String(nav.hardwareConcurrency ?? '—'),
    memory: nav.deviceMemory ? `${nav.deviceMemory} GB` : '—',
    connection: nav.connection?.effectiveType ?? '—',
    timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
    standalone: window.matchMedia?.('(display-mode: standalone)').matches ? 'sí' : 'no',
    native: Boolean((window as unknown as { MesaViva?: unknown }).MesaViva) ? 'Android Mesa Viva' : 'web',
  };
  return info;
}

export async function getStorageInfo(): Promise<string> {
  try {
    if (navigator.storage?.estimate) {
      const estimate = await navigator.storage.estimate();
      const used = ((estimate.usage ?? 0) / 1024 / 1024).toFixed(1);
      const quota = ((estimate.quota ?? 0) / 1024 / 1024).toFixed(0);
      return `${used} MB de ${quota} MB`;
    }
  } catch {
    // Ignorar.
  }
  return '—';
}

export function buildReport(note = '', options: { errors?: boolean; actions?: boolean; match?: string } = {}): string {
  const info = getSystemInfo();
  const lines: string[] = [
    'Mesa Viva · Informe de diagnóstico',
    `Fecha: ${new Date().toISOString()}`,
    '',
    'Sistema:',
    ...Object.entries(info).map(([k, v]) => `- ${k}: ${v}`),
    '',
  ];
  if (note.trim()) {
    lines.push('Descripción del usuario:', note.trim(), '');
  }
  if (options.match) {
    lines.push('Contexto de partida:', options.match.slice(0, 1200), '');
  }
  if (options.errors !== false) {
    lines.push(`Errores recientes (${getErrors().length}):`);
    for (const e of getErrors().slice(0, 25)) {
      lines.push(`[${new Date(e.at).toISOString()}] ${e.level.toUpperCase()}: ${e.message}`);
      if (e.context) lines.push(`  contexto: ${e.context}`);
      if (e.stack) lines.push(`  ${e.stack.split('\n').slice(0, 6).join('\n  ')}`);
    }
    lines.push('');
  }
  if (options.actions !== false) {
    lines.push(`Actividad reciente (${getActions().length}):`);
    for (const a of getActions().slice(0, 30)) {
      lines.push(`[${new Date(a.at).toISOString()}] ${a.name}${a.detail ? ` · ${a.detail}` : ''}`);
    }
  }
  return lines.join('\n');
}

export function mailtoUrl(report: string, note = ''): string {
  const body = `${note ? `${note}\n\n` : ''}${report.slice(0, 6000)}`;
  return `mailto:${CONTACT_EMAIL}?subject=${encodeURIComponent('Informe de diagnóstico · Mesa Viva')}&body=${encodeURIComponent(body)}`;
}

export async function copyReport(report: string): Promise<boolean> {
  try {
    await navigator.clipboard.writeText(report);
    return true;
  } catch {
    return false;
  }
}

export function downloadReport(report: string): void {
  const blob = new Blob([report], { type: 'text/plain;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `mesa-viva-diagnostico-${new Date().toISOString().slice(0, 10)}.txt`;
  a.click();
  setTimeout(() => URL.revokeObjectURL(url), 2000);
}

export function testDiagnostic(): void {
  diag('info', 'Prueba de diagnóstico solicitada por el usuario.', undefined, 'diagnostics:test');
  trackAction('diagnostics:test', 'La mesa respondió correctamente');
}
