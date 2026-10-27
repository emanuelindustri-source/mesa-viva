import type { LevelInfo } from './types';

/**
 * Curva de progresión de 10 niveles. Pensada para que un jugador casual
 * suba de nivel en varias sesiones y un jugador constante llegue a Leyenda.
 */
export const LEVELS: LevelInfo[] = [
  { level: 1, titleKey: 'level.1', minXp: 0, maxXp: 119, rewardKey: 'reward.tableClassic', icon: '🌱' },
  { level: 2, titleKey: 'level.2', minXp: 120, maxXp: 299, rewardKey: 'reward.cardBackSun', icon: '🃏' },
  { level: 3, titleKey: 'level.3', minXp: 300, maxXp: 599, rewardKey: 'reward.diceWood', icon: '🎲' },
  { level: 4, titleKey: 'level.4', minXp: 600, maxXp: 999, rewardKey: 'reward.avatarJaguar', icon: '🐆' },
  { level: 5, titleKey: 'level.5', minXp: 1000, maxXp: 1599, rewardKey: 'reward.tableEmerald', icon: '💚' },
  { level: 6, titleKey: 'level.6', minXp: 1600, maxXp: 2399, rewardKey: 'reward.effectConfetti', icon: '🎉' },
  { level: 7, titleKey: 'level.7', minXp: 2400, maxXp: 3499, rewardKey: 'reward.avatarSilbon', icon: '🎩' },
  { level: 8, titleKey: 'level.8', minXp: 3500, maxXp: 4999, rewardKey: 'reward.cardBackAraguaney', icon: '🌺' },
  { level: 9, titleKey: 'level.9', minXp: 5000, maxXp: 6999, rewardKey: 'reward.avatarSayona', icon: '🕯️' },
  { level: 10, titleKey: 'level.10', minXp: 7000, maxXp: Number.MAX_SAFE_INTEGER, rewardKey: 'reward.legend', icon: '👑' },
];

export function levelForXp(xp: number): LevelInfo {
  const safe = Math.max(0, Math.floor(xp));
  for (let i = LEVELS.length - 1; i >= 0; i -= 1) {
    const lvl = LEVELS[i];
    if (lvl && safe >= lvl.minXp) return lvl;
  }
  return LEVELS[0] as LevelInfo;
}

export function nextLevel(level: number): LevelInfo | null {
  return LEVELS.find((l) => l.level === level + 1) ?? null;
}

export function xpProgress(xp: number): { current: number; needed: number; ratio: number } {
  const lvl = levelForXp(xp);
  const next = nextLevel(lvl.level);
  if (!next) return { current: 1, needed: 1, ratio: 1 };
  const current = xp - lvl.minXp;
  const needed = next.minXp - lvl.minXp;
  return { current, needed, ratio: Math.max(0, Math.min(1, current / needed)) };
}

export interface XpAwardInput {
  won: boolean;
  draw?: boolean;
  points: number;
  difficulty: DifficultyLike;
  humanPlayers: number;
  streak: number;
  perfect?: boolean;
}

export type DifficultyLike = 'easy' | 'normal' | 'hard';

/** XP acotada por partida para evitar granjas de puntos. */
export function awardXp(input: XpAwardInput): number {
  let xp = input.won ? 60 : input.draw ? 35 : 22;
  xp += Math.min(40, Math.max(0, Math.round(input.points / 2)));
  xp += input.difficulty === 'hard' ? 20 : input.difficulty === 'normal' ? 10 : 0;
  xp += input.humanPlayers > 1 ? 15 : 0;
  xp += input.won ? Math.min(20, input.streak * 5) : 0;
  if (input.perfect) xp += 15;
  return Math.max(10, Math.min(170, xp));
}

export function levelUpMessage(fromLevel: number, toLevel: number): string | null {
  if (toLevel <= fromLevel) return null;
  return `level.${toLevel}`;
}
