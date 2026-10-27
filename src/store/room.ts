import { create } from 'zustand';
import { RoomGuest, RoomHost, makeRoomCode, type RoomInfo } from '../net/room';
import type { GameAction, GameId, GameView } from '../core/types';

type Role = 'host' | 'guest';
type Phase = 'idle' | 'lobby' | 'playing' | 'result';

interface RoomStore {
  role: Role | null;
  room: RoomInfo | null;
  youId: string | null;
  view: GameView | null;
  gameId: GameId | null;
  roundId: string | null;
  phase: Phase;
  connecting: boolean;
  error: string | null;
  create: (name: string, avatar: string) => void;
  join: (code: string, name: string, avatar: string) => void;
  start: (gameId: GameId) => void;
  addGame: (gameId: GameId) => void;
  removeGame: (gameId: GameId) => void;
  act: (action: GameAction) => void;
  sendChat: (text: string) => void;
  backToLobby: () => void;
  leave: () => void;
  clearError: () => void;
}

let host: RoomHost | null = null;
let guest: RoomGuest | null = null;

const initial = {
  role: null as Role | null,
  room: null as RoomInfo | null,
  youId: null as string | null,
  view: null as GameView | null,
  gameId: null as GameId | null,
  roundId: null as string | null,
  phase: 'idle' as Phase,
  connecting: false,
  error: null as string | null,
};

export const useRoom = create<RoomStore>((set, get) => {
  const reset = () => {
    host = null;
    guest = null;
    set({ ...initial });
  };

  return {
    ...initial,

    create: (name, avatar) => {
      if (host || guest) reset();
      set({ ...initial, role: 'host', connecting: true });
      const code = makeRoomCode();
      host = new RoomHost(code, name, avatar, {
        onReady: (_code, youId) => set((state) => ({ ...state, youId, phase: 'lobby', connecting: false })),
        onRoom: (room) => set((state) => ({
          ...state,
          room,
          youId: state.youId ?? room.hostId,
          phase: state.phase === 'idle' ? 'lobby' : state.phase,
        })),
        onStart: (payload) => set({ roundId: payload.roundId, gameId: payload.gameId, phase: 'playing', view: null }),
        onView: (view) => set((state) => ({
          ...state,
          view,
          gameId: view.gameId,
          youId: view.youId,
          phase: view.result ? 'result' : 'playing',
        })),
        onError: (error) => set({ error, connecting: false }),
        onClosed: reset,
      });
    },

    join: (code, name, avatar) => {
      if (host || guest) reset();
      set({ ...initial, role: 'guest', connecting: true });
      guest = new RoomGuest(code, name, avatar, {
        onRoom: (room, youId) => set({ room, youId, phase: 'lobby', connecting: false }),
        onStart: (payload) => set({ roundId: payload.roundId, gameId: payload.gameId, phase: 'playing', view: null }),
        onView: (view) => set((state) => ({
          ...state,
          view,
          gameId: view.gameId,
          youId: view.youId === 'spectator' ? 'spectator' : state.youId ?? view.youId,
          phase: view.result ? 'result' : 'playing',
        })),
        onLobby: () => set({ phase: 'lobby', view: null, roundId: null }),
        onError: (error) => set({ error, connecting: false }),
        onClosed: reset,
      });
    },

    start: (gameId) => {
      if (get().role !== 'host') return;
      host?.startRound(gameId);
    },

    addGame: (gameId) => {
      if (get().role === 'host') host?.addPlaylist(gameId);
      else guest?.addPlaylist(gameId);
    },

    removeGame: (gameId) => {
      if (get().role === 'host') host?.removePlaylist(gameId);
      else guest?.removePlaylist(gameId);
    },

    act: (action) => {
      const state = get();
      if (state.role === 'host' && state.youId) host?.act(state.youId, action);
      else guest?.act(action);
    },

    sendChat: (text) => {
      const state = get();
      const trimmed = text.trim().slice(0, 180);
      if (!trimmed) return;
      if (state.role === 'host' && state.youId) host?.addChat(state.youId, trimmed);
      else guest?.chat(trimmed);
    },

    backToLobby: () => {
      const state = get();
      if (state.role === 'host') {
        host?.returnToLobby();
        set({ phase: 'lobby', view: null, roundId: null });
      } else {
        guest?.requestLobby();
      }
    },

    leave: () => {
      if (host) host.close();
      else if (guest) guest.close();
      reset();
    },

    clearError: () => set({ error: null }),
  };
});
