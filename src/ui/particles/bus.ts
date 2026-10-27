export type BurstKind = 'dust' | 'confetti' | 'spark' | 'snow' | 'ember' | 'petal' | 'star' | 'clover' | 'leaves';

export interface BurstEvent {
  kind: BurstKind;
  x: number;
  y: number;
  count: number;
  colors: string[];
  power: number;
}

type Listener = (event: BurstEvent) => void;

const listeners = new Set<Listener>();

export function burst(
  kind: BurstKind,
  x = window.innerWidth / 2,
  y = window.innerHeight / 2,
  options: { count?: number; colors?: string[]; power?: number } = {},
): void {
  if (typeof window === 'undefined') return;
  const event: BurstEvent = {
    kind,
    x,
    y,
    count: options.count ?? (kind === 'confetti' ? 42 : kind === 'dust' ? 14 : 24),
    colors: options.colors ?? defaultColors(),
    power: options.power ?? 1,
  };
  for (const listener of listeners) listener(event);
}

export function burstFromElement(element: Element | null, kind: BurstKind, options: { count?: number; colors?: string[]; power?: number } = {}): void {
  if (!element) {
    burst(kind, undefined, undefined, options);
    return;
  }
  const rect = element.getBoundingClientRect();
  burst(kind, rect.left + rect.width / 2, rect.top + rect.height / 2, options);
}

export function subscribeBurst(listener: Listener): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

function defaultColors(): string[] {
  if (typeof document === 'undefined') return ['#e0a63a', '#e4572e', '#fff4d6'];
  const style = getComputedStyle(document.documentElement);
  return [
    style.getPropertyValue('--accent').trim() || '#e0a63a',
    style.getPropertyValue('--accent-2').trim() || '#e4572e',
    '#fff4d6',
  ];
}
