import { create } from 'zustand';
import { Match, type MatchConfig } from '../core/match';
import type { GameAction } from '../core/types';
import { diag, trackAction } from '../services/diagnostics';

export type SessionMode = 'bots' | 'hotseat';

interface SessionState {
  match: Match | null;
  mode: SessionMode;
  version: number;
  error: string | null;
  start: (config: MatchConfig, mode: SessionMode) => void;
  act: (actorId: string, action: GameAction) => boolean;
  botStep: () => boolean;
  clear: () => void;
  setError: (error: string | null) => void;
}

export const useSession = create<SessionState>((set, get) => ({
  match: null,
  mode: 'bots',
  version: 0,
  error: null,

  start: (config, mode) => {
    try {
      const match = new Match(config);
      set({ match, mode, version: 0, error: null });
      trackAction('match:start', config.gameId);
    } catch (error) {
      diag('error', 'No se pudo iniciar la partida', error instanceof Error ? error.stack : String(error), 'session:start');
      set({ error: 'error.generic' });
    }
  },

  act: (actorId, action) => {
    const match = get().match;
    if (!match) return false;
    try {
      const outcome = match.act(actorId, action);
      if (!outcome.ok) {
        if (outcome.error && outcome.error !== 'error.invalidAction') {
          trackAction('match:error', `${action.type}:${outcome.error}`);
        }
        set({ error: outcome.error ?? 'error.invalidAction' });
        return false;
      }
      set({ version: get().version + 1, error: null });
      return true;
    } catch (error) {
      diag('error', 'Acción de partida fallida', error instanceof Error ? error.stack : String(error), `${match.config.gameId}:${action.type}`);
      set({ error: 'error.generic' });
      return false;
    }
  },

  botStep: () => {
    const match = get().match;
    if (!match || match.isOver) return false;
    try {
      const step = match.botStep();
      if (!step) return false;
      set({ version: get().version + 1, error: null });
      return true;
    } catch (error) {
      diag('error', 'Turno de bot fallido', error instanceof Error ? error.stack : String(error), match.config.gameId);
      return false;
    }
  },

  clear: () => set({ match: null, version: 0, error: null }),
  setError: (error) => set({ error }),
}));
