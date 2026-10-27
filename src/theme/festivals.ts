export type FestivalId =
  | 'neutral'
  | 'halloween'
  | 'navidad'
  | 'reyes'
  | 'carnaval'
  | 'sanValentin'
  | 'sanPatricio'
  | 'semanaSanta'
  | 'independencia'
  | 'cruzDeMayo'
  | 'corpus'
  | 'diaDeMuertos';

import type { BurstKind } from '../ui/particles/bus';

export interface Festival {
  id: FestivalId;
  nameKey: string;
  greetingKey: string;
  descKey: string;
  emoji: string;
  accent: string;
  accent2: string;
  bg: string;
  particle: BurstKind;
  music: 'warm' | 'eerie' | 'party' | 'serene' | 'patriotic' | 'sacred' | 'neutral';
  characters: string[];
  motif: string[];
}

export const FESTIVALS: Record<FestivalId, Festival> = {
  neutral: {
    id: 'neutral', nameKey: 'festival.neutral', greetingKey: 'festival.greeting.neutral', descKey: 'festival.desc.neutral',
    emoji: '🪀', accent: '#e0a63a', accent2: '#e4572e', bg: '#08201c', particle: 'dust', music: 'neutral',
    characters: [], motif: ['✦', '◇', '●'],
  },
  halloween: {
    id: 'halloween', nameKey: 'festival.halloween', greetingKey: 'festival.greeting.halloween', descKey: 'festival.desc.halloween',
    emoji: '🎃', accent: '#f0a23a', accent2: '#a855f7', bg: '#120d1c', particle: 'ember', music: 'eerie',
    characters: ['festival.silbon', 'festival.sayona', 'festival.hachero', 'festival.chupacabras', 'festival.llorona'],
    motif: ['🕯️', '🌙', '🦇', '💀', '🌫️'],
  },
  navidad: {
    id: 'navidad', nameKey: 'festival.navidad', greetingKey: 'festival.greeting.navidad', descKey: 'festival.desc.navidad',
    emoji: '🎄', accent: '#d94f4f', accent2: '#e7c66a', bg: '#0c1f1a', particle: 'snow', music: 'warm',
    characters: ['🎅', '🤶', '⭐', '🦌'], motif: ['❄', '✧', '🎁', '🕯️'],
  },
  reyes: {
    id: 'reyes', nameKey: 'festival.reyes', greetingKey: 'festival.greeting.reyes', descKey: 'festival.desc.reyes',
    emoji: '👑', accent: '#d4a62a', accent2: '#6a4fb3', bg: '#101a2c', particle: 'star', music: 'warm',
    characters: ['👑', '🐪', '⭐'], motif: ['✦', '👑', '⭐'],
  },
  carnaval: {
    id: 'carnaval', nameKey: 'festival.carnaval', greetingKey: 'festival.greeting.carnaval', descKey: 'festival.desc.carnaval',
    emoji: '🎭', accent: '#ff5d8f', accent2: '#22c1c3', bg: '#1a0f2b', particle: 'confetti', music: 'party',
    characters: ['🎭', '🥁', '💃', '🪅'], motif: ['✦', '●', '◆', '✺'],
  },
  sanValentin: {
    id: 'sanValentin', nameKey: 'festival.sanValentin', greetingKey: 'festival.greeting.sanValentin', descKey: 'festival.desc.sanValentin',
    emoji: '💘', accent: '#ff6b8a', accent2: '#ffb3c1', bg: '#240e18', particle: 'petal', music: 'serene',
    characters: ['💌', '🌹', '💞'], motif: ['♥', '✧', '❀'],
  },
  sanPatricio: {
    id: 'sanPatricio', nameKey: 'festival.sanPatricio', greetingKey: 'festival.greeting.sanPatricio', descKey: 'festival.desc.sanPatricio',
    emoji: '☘️', accent: '#4caf7d', accent2: '#a7d129', bg: '#0a1f16', particle: 'leaves', music: 'party',
    characters: ['☘️', '🍀', '🌈'], motif: ['☘', '✦', '●'],
  },
  semanaSanta: {
    id: 'semanaSanta', nameKey: 'festival.semanaSanta', greetingKey: 'festival.greeting.semanaSanta', descKey: 'festival.desc.semanaSanta',
    emoji: '🕯️', accent: '#a78bfa', accent2: '#d6c7a1', bg: '#171326', particle: 'dust', music: 'sacred',
    characters: ['🕯️', '🌿', '✝️'], motif: ['✧', '◇', '🕯'],
  },
  independencia: {
    id: 'independencia', nameKey: 'festival.independencia', greetingKey: 'festival.greeting.independencia', descKey: 'festival.desc.independencia',
    emoji: '🇻🇪', accent: '#f4c542', accent2: '#2f6fdb', bg: '#0a1730', particle: 'confetti', music: 'patriotic',
    characters: ['🇻🇪', '⭐', '🎺'], motif: ['★', '✦', '●'],
  },
  cruzDeMayo: {
    id: 'cruzDeMayo', nameKey: 'festival.cruzDeMayo', greetingKey: 'festival.greeting.cruzDeMayo', descKey: 'festival.desc.cruzDeMayo',
    emoji: '🌺', accent: '#f0a6c8', accent2: '#8bc34a', bg: '#1b1420', particle: 'petal', music: 'serene',
    characters: ['🌺', '🕯️', '🎶'], motif: ['❀', '✧', '🌿'],
  },
  corpus: {
    id: 'corpus', nameKey: 'festival.corpus', greetingKey: 'festival.greeting.corpus', descKey: 'festival.desc.corpus',
    emoji: '🥁', accent: '#e25822', accent2: '#f2c14e', bg: '#1a0f0a', particle: 'ember', music: 'party',
    characters: ['👹', '🥁', '🔥'], motif: ['◆', '🔥', '✦'],
  },
  diaDeMuertos: {
    id: 'diaDeMuertos', nameKey: 'festival.diaDeMuertos', greetingKey: 'festival.greeting.diaDeMuertos', descKey: 'festival.desc.diaDeMuertos',
    emoji: '💀', accent: '#f28c28', accent2: '#b33fa1', bg: '#170f1c', particle: 'petal', music: 'serene',
    characters: ['💀', '🌼', '🕯️'], motif: ['✿', '💀', '✦'],
  },
};

const INDEPENDENCE: Record<string, { month: number; day: number; key: string }> = {
  VE: { month: 7, day: 5, key: 'festival.independencia' },
  CO: { month: 7, day: 20, key: 'festival.independencia' },
  AR: { month: 7, day: 9, key: 'festival.independencia' },
  MX: { month: 9, day: 16, key: 'festival.independencia' },
  CL: { month: 9, day: 18, key: 'festival.independencia' },
  PE: { month: 7, day: 28, key: 'festival.independencia' },
  EC: { month: 8, day: 10, key: 'festival.independencia' },
  BO: { month: 8, day: 6, key: 'festival.independencia' },
  UY: { month: 8, day: 25, key: 'festival.independencia' },
  PY: { month: 5, day: 14, key: 'festival.independencia' },
  PA: { month: 11, day: 3, key: 'festival.independencia' },
  CR: { month: 9, day: 15, key: 'festival.independencia' },
  DO: { month: 2, day: 27, key: 'festival.independencia' },
  US: { month: 7, day: 4, key: 'festival.independencia' },
  BR: { month: 9, day: 7, key: 'festival.independencia' },
  PT: { month: 6, day: 10, key: 'festival.independencia' },
  DE: { month: 10, day: 3, key: 'festival.independencia' },
  FR: { month: 7, day: 14, key: 'festival.independencia' },
  IT: { month: 6, day: 2, key: 'festival.independencia' },
  RU: { month: 6, day: 12, key: 'festival.independencia' },
  CN: { month: 10, day: 1, key: 'festival.independencia' },
  TW: { month: 10, day: 10, key: 'festival.independencia' },
  JP: { month: 2, day: 11, key: 'festival.independencia' },
};

function dateAt(year: number, month: number, day: number): Date {
  return new Date(year, month - 1, day, 12, 0, 0, 0);
}

function addDays(date: Date, days: number): Date {
  const copy = new Date(date);
  copy.setDate(copy.getDate() + days);
  return copy;
}

function dayDiff(a: Date, b: Date): number {
  const ms = dateAt(a.getFullYear(), a.getMonth() + 1, a.getDate()).getTime() -
    dateAt(b.getFullYear(), b.getMonth() + 1, b.getDate()).getTime();
  return Math.round(ms / 86400000);
}

/** Algoritmo gregoriano de Pascua (Meeus/Jones/Butcher). */
export function easterSunday(year: number): Date {
  const a = year % 19;
  const b = Math.floor(year / 100);
  const c = year % 100;
  const d = Math.floor(b / 4);
  const e = b % 4;
  const f = Math.floor((b + 8) / 25);
  const g = Math.floor((b - f + 1) / 3);
  const h = (19 * a + b - d - g + 15) % 30;
  const i = Math.floor(c / 4);
  const k = c % 4;
  const l = (32 + 2 * e + 2 * i - h - k) % 7;
  const m = Math.floor((a + 11 * h + 22 * l) / 304);
  const month = Math.floor((h + l - 7 * m + 114) / 31);
  const day = ((h + l - 7 * m + 114) % 31) + 1;
  return dateAt(year, month, day);
}

interface WindowRule {
  id: FestivalId;
  anchor: (year: number) => Date;
  before: number;
  after: number;
  countries?: string[];
  priority: number;
}

function independenceRule(country: string): WindowRule | null {
  const info = INDEPENDENCE[country];
  if (!info) return null;
  return {
    id: 'independencia',
    anchor: (year) => dateAt(year, info.month, info.day),
    before: 2,
    after: 2,
    priority: 10,
  };
}

function rulesFor(country: string, year: number): WindowRule[] {
  const easter = easterSunday(year);
  const rules: WindowRule[] = [
    { id: 'semanaSanta', anchor: () => addDays(easter, -7), before: 0, after: 7, priority: 100 },
    { id: 'carnaval', anchor: () => addDays(easter, -47), before: 3, after: 0, priority: 95 },
    { id: 'corpus', anchor: () => addDays(easter, 60), before: 1, after: 1, countries: ['VE'], priority: 70 },
    { id: 'reyes', anchor: (y) => dateAt(y, 1, 6), before: 1, after: 0, priority: 90 },
    { id: 'navidad', anchor: (y) => dateAt(y, 12, 25), before: 24, after: 5, priority: 80 },
    { id: 'halloween', anchor: (y) => dateAt(y, 10, 31), before: 6, after: 2, priority: 75 },
    { id: 'diaDeMuertos', anchor: (y) => dateAt(y, 11, 2), before: 1, after: 0, countries: ['MX'], priority: 78 },
    { id: 'sanValentin', anchor: (y) => dateAt(y, 2, 14), before: 7, after: 0, priority: 60 },
    { id: 'sanPatricio', anchor: (y) => dateAt(y, 3, 17), before: 7, after: 0, priority: 55 },
    { id: 'cruzDeMayo', anchor: (y) => dateAt(y, 5, 3), before: 2, after: 2, countries: ['VE'], priority: 50 },
  ];
  const independence = independenceRule(country);
  if (independence) rules.push(independence);
  const newYear: WindowRule = {
    id: 'navidad' as FestivalId,
    anchor: (y) => dateAt(y, 1, 1),
    before: 1,
    after: 0,
    priority: 85,
  };
  // Año Nuevo usa el mismo ambiente navideño, pero con prioridad propia.
  void newYear;
  return rules;
}

export function resolveFestival(
  date: Date = new Date(),
  country = 'VE',
  override: FestivalId | 'auto' = 'auto',
): Festival {
  if (override !== 'auto' && FESTIVALS[override]) return FESTIVALS[override];
  const year = date.getFullYear();
  const candidates: Array<{ id: FestivalId; priority: number }> = [];
  for (const rule of rulesFor(country, year)) {
    if (rule.countries && !rule.countries.includes(country)) continue;
    const anchor = rule.anchor(year);
    const diff = dayDiff(date, anchor);
    if (diff >= -rule.before && diff <= rule.after) {
      candidates.push({ id: rule.id, priority: rule.priority });
    }
  }
  // Año Nuevo: extiende la temporada navideña hasta el 1 de enero.
  const jan1 = dateAt(year, 1, 1);
  if (Math.abs(dayDiff(date, jan1)) <= 1) candidates.push({ id: 'navidad', priority: 86 });
  candidates.sort((a, b) => b.priority - a.priority);
  return candidates[0] ? FESTIVALS[candidates[0].id] : FESTIVALS.neutral;
}

export function isIndependenceDay(date: Date, country: string): boolean {
  const info = INDEPENDENCE[country];
  if (!info) return false;
  return date.getMonth() === info.month - 1 && Math.abs(date.getDate() - info.day) <= 2;
}

export function fetchSystemAccent(): string | null {
  if (typeof window === 'undefined') return null;
  const bridge = (window as unknown as {
    MesaViva?: { systemAccent?: string | (() => string) };
  }).MesaViva;
  let value: string | undefined;
  try {
    value = typeof bridge?.systemAccent === 'function' ? bridge.systemAccent() : bridge?.systemAccent;
  } catch {
    value = undefined;
  }
  return typeof value === 'string' && /^#[0-9a-f]{6}$/i.test(value) ? value : null;
}

export function hexToRgb(hex: string): { r: number; g: number; b: number } {
  const clean = hex.replace('#', '');
  const full = clean.length === 3 ? clean.split('').map((c) => c + c).join('') : clean;
  const n = Number.parseInt(full, 16);
  return { r: (n >> 16) & 255, g: (n >> 8) & 255, b: n & 255 };
}

export function readableInk(hex: string): string {
  const { r, g, b } = hexToRgb(hex);
  const lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255;
  return lum > 0.62 ? '#10231f' : '#fffaf0';
}
