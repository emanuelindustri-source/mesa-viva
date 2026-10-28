import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { awardXp, levelForXp } from '../core/levels';
import type { Difficulty, GameId } from '../core/types';
import type { XpAwardResult } from '../core/types';
import { sanitizeName, uid } from '../core/utils';
import { detectCountry, detectLocale, setLocale, type Locale } from '../i18n';

export type ThemeMode = 'auto' | 'light' | 'dark';
export type AccentSource = 'season' | 'system' | 'custom' | 'wallpaper';
export type FestivalChoice = 'auto' | string;

export interface Settings {
  language: Locale;
  country: string;
  theme: ThemeMode;
  accentSource: AccentSource;
  accent: string | null;
  wallpaperColor: string | null;
  music: boolean;
  sfx: boolean;
  haptics: boolean;
  volume: number;
  reduceMotion: boolean;
  highContrast: boolean;
  festival: FestivalChoice;
  dynamicColor: boolean;
  tips: boolean;
  keepAwake: boolean;
}

export interface Profile {
  id: string;
  name: string;
  avatar: string;
  xp: number;
  level: number;
  wins: number;
  losses: number;
  draws: number;
  matches: number;
  streak: number;
  bestStreak: number;
  createdAt: number;
}

export interface HistoryEntry {
  id: string;
  gameId: GameId;
  date: number;
  result: 'win' | 'loss' | 'draw';
  players: string[];
  score: number;
  xp: number;
  durationMs: number;
}

interface GameStat {
  played: number;
  won: number;
}

export interface MatchRecordInput {
  gameId: GameId;
  won: boolean;
  draw: boolean;
  points: number;
  difficulty: Difficulty;
  humanPlayers: number;
  durationMs: number;
  players: string[];
}

interface AppState {
  onboarded: boolean;
  profile: Profile;
  settings: Settings;
  history: HistoryEntry[];
  gameStats: Partial<Record<GameId, GameStat>>;
  onboard: (input: { name: string; avatar: string; language: Locale; country: string }) => void;
  setSettings: (patch: Partial<Settings>) => void;
  setProfile: (patch: Partial<Pick<Profile, 'name' | 'avatar'>>) => void;
  recordMatch: (input: MatchRecordInput) => XpAwardResult;
  clearHistory: () => void;
  resetAll: () => void;
}

const initialLocale = detectLocale();

function defaultSettings(): Settings {
  const nav = typeof navigator !== 'undefined' ? navigator as Navigator & { deviceMemory?: number } : null;
  const lowPower = Boolean(nav && ((nav.deviceMemory ?? 8) <= 4 || nav.hardwareConcurrency <= 4));
  return {
    language: initialLocale,
    country: detectCountry(initialLocale),
    theme: 'auto',
    accentSource: 'season',
    accent: null,
    wallpaperColor: null,
    music: true,
    sfx: true,
    haptics: true,
    volume: 0.6,
    reduceMotion: lowPower,
    highContrast: false,
    festival: 'auto',
    dynamicColor: true,
    tips: true,
    keepAwake: true,
  };
}

function defaultProfile(): Profile {
  return {
    id: uid('player'),
    name: '',
    avatar: 'jaguar',
    xp: 0,
    level: 1,
    wins: 0,
    losses: 0,
    draws: 0,
    matches: 0,
    streak: 0,
    bestStreak: 0,
    createdAt: Date.now(),
  };
}

export const useAppStore = create<AppState>()(
  persist(
    (set, get) => ({
      onboarded: false,
      profile: defaultProfile(),
      settings: defaultSettings(),
      history: [],
      gameStats: {},

      onboard: ({ name, avatar, language, country }) => {
        setLocale(language);
        set((state) => ({
          onboarded: true,
          profile: {
            ...state.profile,
            name: sanitizeName(name) || 'Jugador',
            avatar,
          },
          settings: { ...state.settings, language, country },
        }));
      },

      setSettings: (patch) => {
        if (patch.language) setLocale(patch.language);
        set((state) => ({ settings: { ...state.settings, ...patch } }));
      },

      setProfile: (patch) => {
        set((state) => ({
          profile: {
            ...state.profile,
            ...(patch.name !== undefined ? { name: sanitizeName(patch.name) || state.profile.name } : {}),
            ...(patch.avatar !== undefined ? { avatar: patch.avatar } : {}),
          },
        }));
      },

      recordMatch: (input) => {
        const state = get();
        const levelBefore = state.profile.level;
        const xp = awardXp({
          won: input.won,
          draw: input.draw,
          points: input.points,
          difficulty: input.difficulty,
          humanPlayers: input.humanPlayers,
          streak: state.profile.streak,
        });
        const newXp = state.profile.xp + xp;
        const levelAfter = levelForXp(newXp).level;
        const streak = input.won ? state.profile.streak + 1 : 0;
        const entry: HistoryEntry = {
          id: uid('match'),
          gameId: input.gameId,
          date: Date.now(),
          result: input.draw ? 'draw' : input.won ? 'win' : 'loss',
          players: input.players.slice(0, 8),
          score: input.points,
          xp,
          durationMs: input.durationMs,
        };
        const stat = state.gameStats[input.gameId] ?? { played: 0, won: 0 };
        set({
          profile: {
            ...state.profile,
            xp: newXp,
            level: levelAfter,
            wins: state.profile.wins + (input.won ? 1 : 0),
            losses: state.profile.losses + (!input.won && !input.draw ? 1 : 0),
            draws: state.profile.draws + (input.draw ? 1 : 0),
            matches: state.profile.matches + 1,
            streak,
            bestStreak: Math.max(state.profile.bestStreak, streak),
          },
          history: [entry, ...state.history].slice(0, 60),
          gameStats: {
            ...state.gameStats,
            [input.gameId]: {
              played: stat.played + 1,
              won: stat.won + (input.won ? 1 : 0),
            },
          },
        });
        return { xp, levelBefore, levelAfter, leveledUp: levelAfter > levelBefore };
      },

      clearHistory: () => set({ history: [] }),

      resetAll: () => {
        const profile = defaultProfile();
        const settings = defaultSettings();
        setLocale(settings.language);
        set({ onboarded: false, profile, settings, history: [], gameStats: {} });
      },
    }),
    {
      name: 'mesa-viva-v1',
      version: 1,
      partialize: (state) => ({
        onboarded: state.onboarded,
        profile: state.profile,
        settings: state.settings,
        history: state.history,
        gameStats: state.gameStats,
      }),
      merge: (persisted, current) => {
        const p = (persisted ?? {}) as Partial<AppState>;
        const settings = { ...current.settings, ...(p.settings ?? {}) };
        const profile = { ...current.profile, ...(p.profile ?? {}) };
        return {
          ...current,
          ...p,
          profile,
          settings,
          history: Array.isArray(p.history) ? p.history : [],
          gameStats: p.gameStats ?? {},
        };
      },
    },
  ),
);

export function useProfile(): Profile {
  return useAppStore((s) => s.profile);
}

export function useSettings(): Settings {
  return useAppStore((s) => s.settings);
}
