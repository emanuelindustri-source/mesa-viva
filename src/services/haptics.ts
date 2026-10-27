export type HapticKind =
  | 'tap'
  | 'select'
  | 'card'
  | 'dice'
  | 'capture'
  | 'win'
  | 'error'
  | 'canto'
  | 'level';

const PATTERNS: Record<HapticKind, number | number[]> = {
  tap: 8,
  select: 14,
  card: [10, 25, 8],
  dice: [20, 40, 30, 40, 20],
  capture: [30, 20, 50],
  win: [40, 40, 60, 40, 90],
  error: [60, 40, 60],
  canto: [25, 30, 25, 30, 60],
  level: [20, 30, 20, 30, 20, 30, 80],
};

interface NativeBridge {
  vibrate?: (pattern: string) => void;
}

export function canVibrate(): boolean {
  return typeof navigator !== 'undefined' && typeof navigator.vibrate === 'function';
}

export function haptic(kind: HapticKind, enabled = true): void {
  if (!enabled || typeof window === 'undefined') return;
  const pattern = PATTERNS[kind];
  try {
    const bridge = (window as unknown as { MesaViva?: NativeBridge }).MesaViva;
    if (bridge?.vibrate) {
      bridge.vibrate(Array.isArray(pattern) ? pattern.join(',') : String(pattern));
      return;
    }
    navigator.vibrate?.(pattern);
  } catch {
    // La vibración es un extra, nunca debe romper la partida.
  }
}
