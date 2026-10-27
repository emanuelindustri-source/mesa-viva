import type { GameAction, GameId, GameView, MatchResult, Seat } from '../core/types';
import { Match } from '../core/match';
import { getGame } from '../core/registry';
import { BOT_AVATARS, BOT_NAMES } from '../core/constants';
import { randomSeed } from '../core/rng';
import { sanitizeName, uid } from '../core/utils';

export interface RoomPlayer {
  id: string;
  name: string;
  avatar: string;
  isHost: boolean;
  connected: boolean;
}

export interface ChatMessage {
  id: string;
  from: string;
  name: string;
  text: string;
  at: number;
}

export interface RoomInfo {
  code: string;
  hostId: string;
  players: RoomPlayer[];
  playlist: GameId[];
  scores: Record<string, number>;
  round: number;
  chat: ChatMessage[];
}

export interface RoomStartPayload {
  gameId: GameId;
  seats: Seat[];
  roundId: string;
  youId: string;
}

interface HostCallbacks {
  onReady: (code: string, youId: string) => void;
  onRoom: (room: RoomInfo) => void;
  onStart: (payload: RoomStartPayload) => void;
  onView: (view: GameView) => void;
  onError: (error: string) => void;
  onClosed: () => void;
}

interface GuestCallbacks {
  onRoom: (room: RoomInfo, youId: string) => void;
  onStart: (payload: RoomStartPayload) => void;
  onView: (view: GameView) => void;
  onLobby: () => void;
  onError: (error: string) => void;
  onClosed: () => void;
}

type WireMessage =
  | { t: 'welcome'; room: RoomInfo; youId: string }
  | { t: 'room'; room: RoomInfo }
  | { t: 'start'; gameId: GameId; seats: Seat[]; roundId: string; youId: string }
  | { t: 'view'; view: GameView }
  | { t: 'action'; action: GameAction }
  | { t: 'chat'; text: string }
  | { t: 'playlist'; gameId: GameId }
  | { t: 'removePlaylist'; gameId: GameId }
  | { t: 'lobby' }
  | { t: 'lobbyReq' }
  | { t: 'error'; key: string }
  | { t: 'closed' };

type ServerMessage =
  | { t: 'hosted'; code: string; hostId: string }
  | { t: 'joined'; code: string; guestId: string }
  | { t: 'guest'; guestId: string; name: string; avatar: string }
  | { t: 'guestLeft'; guestId: string }
  | { t: 'message'; from: string; payload: WireMessage }
  | { t: 'error'; key: string }
  | { t: 'closed' }
  | { t: 'pong' };

const CODE_ALPHABET = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';

const env = import.meta.env as Record<string, string | undefined>;

export function roomServerUrl(): string {
  if (typeof localStorage !== 'undefined') {
    const custom = localStorage.getItem('mesa-viva-room-server')?.trim();
    if (custom) return custom;
  }
  if (env.VITE_ROOM_WS_URL) return env.VITE_ROOM_WS_URL;
  if (env.DEV) return 'ws://localhost:9001';
  return '';
}

export function makeRoomCode(): string {
  const bytes = new Uint8Array(6);
  if (typeof crypto !== 'undefined') crypto.getRandomValues(bytes);
  else for (let i = 0; i < bytes.length; i += 1) bytes[i] = Math.floor(Math.random() * 256);
  return Array.from(bytes).map((b) => CODE_ALPHABET[b % CODE_ALPHABET.length]).join('');
}

export function normalizeCode(raw: string): string {
  return raw.toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, 6);
}

function sendSocket(ws: WebSocket | null, message: ServerMessage | { t: 'host'; code: string } | { t: 'join'; code: string; name: string; avatar: string } | { t: 'send'; to?: string; payload: unknown } | { t: 'ping' }): boolean {
  if (!ws || ws.readyState !== WebSocket.OPEN) return false;
  try {
    ws.send(JSON.stringify(message));
    return true;
  } catch {
    return false;
  }
}

export class RoomHost {
  readonly code: string;
  private ws: WebSocket | null = null;
  private room: RoomInfo;
  private callbacks: HostCallbacks;
  private match: Match | null = null;
  private seats: Seat[] = [];
  private roundId: string | null = null;
  private botTimer: number | null = null;
  private finishedRound = false;
  private closed = false;
  private ready = false;

  constructor(code: string, hostName: string, hostAvatar: string, callbacks: HostCallbacks) {
    this.code = normalizeCode(code);
    this.callbacks = callbacks;
    this.room = {
      code: this.code,
      hostId: '',
      players: [],
      playlist: ['caida', 'furia', 'domino', 'dados'],
      scores: {},
      round: 0,
      chat: [],
    };
    const url = roomServerUrl();
    if (!url) {
      callbacks.onError('sala.serverMissing');
      return;
    }
    const hostNameSafe = sanitizeName(hostName) || 'Anfitrión';
    this.ws = new WebSocket(url);
    this.ws.onopen = () => {
      if (this.closed) return;
      this.hostName = hostNameSafe;
      this.hostAvatar = hostAvatar || 'jaguar';
      sendSocket(this.ws, { t: 'host', code: this.code });
    };
    this.ws.onmessage = (event) => {
      let msg: ServerMessage | null = null;
      try {
        msg = JSON.parse(String(event.data)) as ServerMessage;
      } catch {
        return;
      }
      if (msg) this.receive(msg);
    };
    this.ws.onerror = () => {
      if (!this.closed) callbacks.onError('sala.needInternet');
    };
    this.ws.onclose = () => {
      if (!this.closed && this.ready) callbacks.onError('sala.connectionError');
    };
    this.botTimer = window.setInterval(() => this.stepBots(), 780);
  }

  private hostName = 'Anfitrión';
  private hostAvatar = 'jaguar';

  private receive(msg: ServerMessage): void {
    switch (msg.t) {
      case 'hosted': {
        if (this.ready) return;
        this.ready = true;
        this.room = {
          ...this.room,
          hostId: msg.hostId,
          players: [{
            id: msg.hostId,
            name: this.hostName,
            avatar: this.hostAvatar,
            isHost: true,
            connected: true,
          }],
          scores: { [msg.hostId]: 0 },
        };
        this.callbacks.onReady(this.code, msg.hostId);
        this.broadcastRoom();
        break;
      }
      case 'guest': {
        if (this.room.players.some((p) => p.id === msg.guestId)) return;
        const name = sanitizeName(msg.name) || 'Invitado';
        this.room = {
          ...this.room,
          players: [...this.room.players, {
            id: msg.guestId,
            name,
            avatar: msg.avatar || 'tucan',
            isHost: false,
            connected: true,
          }],
          scores: { ...this.room.scores, [msg.guestId]: 0 },
        };
        this.sendTo(msg.guestId, { t: 'welcome', room: this.room, youId: msg.guestId });
        this.broadcastRoom();
        break;
      }
      case 'guestLeft': {
        this.room = {
          ...this.room,
          players: this.room.players.map((p) => p.id === msg.guestId ? { ...p, connected: false } : p),
        };
        if (this.match) {
          this.addChat(msg.guestId, 'salió de la mesa');
          this.returnToLobby();
        } else {
          this.broadcastRoom();
        }
        break;
      }
      case 'message':
        this.receiveGuest(msg.from, msg.payload);
        break;
      case 'error':
        this.callbacks.onError(msg.key);
        break;
      case 'closed':
        this.callbacks.onClosed();
        break;
      default:
        break;
    }
  }

  private receiveGuest(from: string, payload: WireMessage): void {
    if (!this.room.players.some((p) => p.id === from)) return;
    switch (payload.t) {
      case 'action': {
        if (!this.match) {
          this.sendTo(from, { t: 'error', key: 'error.matchOver' });
          return;
        }
        if (!this.seats.some((s) => s.id === from)) {
          this.sendTo(from, { t: 'error', key: 'sala.spectator' });
          return;
        }
        const outcome = this.match.act(from, payload.action);
        if (!outcome.ok) {
          this.sendTo(from, { t: 'error', key: outcome.error ?? 'error.invalidAction' });
          return;
        }
        this.broadcastViews();
        break;
      }
      case 'chat':
        this.addChat(from, payload.text);
        break;
      case 'playlist':
        if (getGame(payload.gameId) && !this.room.playlist.includes(payload.gameId)) {
          this.room = { ...this.room, playlist: [...this.room.playlist, payload.gameId].slice(0, 8) };
          this.broadcastRoom();
        }
        break;
      case 'removePlaylist':
        this.room = { ...this.room, playlist: this.room.playlist.filter((id) => id !== payload.gameId) };
        this.broadcastRoom();
        break;
      case 'lobbyReq':
        this.returnToLobby();
        break;
      default:
        break;
    }
  }

  private sendTo(guestId: string, payload: WireMessage): void {
    sendSocket(this.ws, { t: 'send', to: guestId, payload });
  }

  private broadcast(payload: WireMessage): void {
    for (const player of this.room.players) {
      if (!player.isHost && player.connected) this.sendTo(player.id, payload);
    }
  }

  addPlaylist(gameId: GameId): void {
    if (getGame(gameId) && !this.room.playlist.includes(gameId)) {
      this.room = { ...this.room, playlist: [...this.room.playlist, gameId].slice(0, 8) };
      this.broadcastRoom();
    }
  }

  removePlaylist(gameId: GameId): void {
    this.room = { ...this.room, playlist: this.room.playlist.filter((id) => id !== gameId) };
    this.broadcastRoom();
  }

  addChat(fromId: string, raw: string): void {
    const text = sanitizeName(raw, 180);
    if (!text) return;
    const from = this.room.players.find((p) => p.id === fromId);
    const message: ChatMessage = {
      id: uid('chat'),
      from: fromId,
      name: from?.name ?? 'Mesa',
      text,
      at: Date.now(),
    };
    this.room = { ...this.room, chat: [...this.room.chat.slice(-49), message] };
    this.broadcastRoom();
  }

  startRound(gameId: GameId): void {
    const game = getGame(gameId);
    if (!game) {
      this.callbacks.onError('error.invalidAction');
      return;
    }
    if (this.match) return;
    const meta = game.meta;
    const connected = this.room.players.filter((p) => p.connected);
    const humans = connected.slice(0, meta.maxPlayers);
    if (humans.length === 0) return;
    const seats: Seat[] = humans.map((p) => ({ id: p.id, name: p.name, avatar: p.avatar, isBot: false }));
    let botIndex = 1;
    while (seats.length < meta.minPlayers) {
      seats.push({
        id: `bot-${botIndex}`,
        name: BOT_NAMES[(botIndex - 1) % BOT_NAMES.length] ?? `Bot ${botIndex}`,
        avatar: BOT_AVATARS[(botIndex - 1) % BOT_AVATARS.length] ?? 'silbon',
        isBot: true,
      });
      botIndex += 1;
    }
    this.seats = seats;
    this.match = new Match({ gameId, seats, options: {}, difficulty: 'normal', seed: randomSeed() });
    this.roundId = uid('round');
    this.finishedRound = false;
    this.room = { ...this.room, round: this.room.round + 1 };
    for (const player of this.room.players) {
      const payload: RoomStartPayload = {
        gameId,
        seats,
        roundId: this.roundId,
        youId: seats.some((s) => s.id === player.id) ? player.id : 'spectator',
      };
      if (player.isHost) this.callbacks.onStart(payload);
      else if (player.connected) this.sendTo(player.id, { t: 'start', ...payload });
    }
    this.broadcastRoom();
    this.broadcastViews();
  }

  private broadcastViews(): void {
    if (!this.match) return;
    for (const player of this.room.players) {
      if (!player.connected) continue;
      const viewerId = this.seats.some((s) => s.id === player.id) ? player.id : 'spectator';
      let view: GameView;
      try {
        view = this.match.view(viewerId);
      } catch {
        continue;
      }
      if (player.isHost) this.callbacks.onView(view);
      else this.sendTo(player.id, { t: 'view', view });
    }
    const result: MatchResult | null = this.match.result;
    if (result && !this.finishedRound) {
      this.finishedRound = true;
      const scores = { ...this.room.scores };
      for (const [id, value] of Object.entries(result.scores)) {
        scores[id] = (scores[id] ?? 0) + value;
      }
      this.room = { ...this.room, scores };
      this.broadcastRoom();
    }
  }

  private stepBots(): void {
    if (!this.match || this.match.isOver) return;
    const step = this.match.botStep();
    if (step) this.broadcastViews();
  }

  private broadcastRoom(): void {
    this.callbacks.onRoom(this.room);
    this.broadcast({ t: 'room', room: this.room });
  }

  returnToLobby(): void {
    this.match = null;
    this.seats = [];
    this.roundId = null;
    this.finishedRound = false;
    this.broadcast({ t: 'lobby' });
    this.broadcastRoom();
  }

  act(actorId: string, action: GameAction): boolean {
    if (!this.match) return false;
    const outcome = this.match.act(actorId, action);
    if (!outcome.ok) {
      if (actorId === this.room.hostId) this.callbacks.onError(outcome.error ?? 'error.invalidAction');
      return false;
    }
    this.broadcastViews();
    return true;
  }

  close(): void {
    if (this.closed) return;
    this.closed = true;
    if (this.botTimer !== null) window.clearInterval(this.botTimer);
    try {
      this.ws?.close(1000, 'host closed');
    } catch {
      // Ignorar.
    }
    this.callbacks.onClosed();
  }
}

export class RoomGuest {
  readonly code: string;
  private ws: WebSocket | null = null;
  private callbacks: GuestCallbacks;
  private closed = false;
  private youId: string | null = null;
  private welcomeTimer: number | null = null;

  constructor(code: string, name: string, avatar: string, callbacks: GuestCallbacks) {
    this.code = normalizeCode(code);
    this.callbacks = callbacks;
    const url = roomServerUrl();
    if (!url) {
      callbacks.onError('sala.serverMissing');
      return;
    }
    const safeName = sanitizeName(name) || 'Invitado';
    this.ws = new WebSocket(url);
    this.ws.onopen = () => {
      if (this.closed) return;
      sendSocket(this.ws, { t: 'join', code: this.code, name: safeName, avatar: avatar || 'tucan' });
    };
    this.ws.onmessage = (event) => {
      let msg: ServerMessage | null = null;
      try {
        msg = JSON.parse(String(event.data)) as ServerMessage;
      } catch {
        return;
      }
      if (msg) this.receive(msg);
    };
    this.ws.onerror = () => {
      if (!this.closed) callbacks.onError('sala.needInternet');
    };
    this.ws.onclose = () => {
      if (!this.closed) callbacks.onError('sala.connectionError');
    };
    this.welcomeTimer = window.setTimeout(() => {
      if (!this.closed && !this.youId) callbacks.onError('sala.connectionError');
    }, 14000);
  }

  private receive(msg: ServerMessage): void {
    if (msg.t === 'joined') {
      this.youId = msg.guestId;
      return;
    }
    if (msg.t === 'closed') {
      this.callbacks.onError('sala.hostLeft');
      this.close();
      return;
    }
    if (msg.t === 'error') {
      this.callbacks.onError(msg.key);
      return;
    }
    if (msg.t !== 'message') return;
    const payload = msg.payload;
    if (!payload) return;
    switch (payload.t) {
      case 'welcome':
        if (this.welcomeTimer !== null) window.clearTimeout(this.welcomeTimer);
        this.welcomeTimer = null;
        this.youId = payload.youId;
        this.callbacks.onRoom(payload.room, payload.youId);
        break;
      case 'room':
        this.callbacks.onRoom(payload.room, this.youId ?? '');
        break;
      case 'start':
        this.callbacks.onStart({ gameId: payload.gameId, seats: payload.seats, roundId: payload.roundId, youId: payload.youId });
        break;
      case 'view':
        this.callbacks.onView(payload.view);
        break;      case 'lobby':
        this.callbacks.onLobby();
        break;
      case 'error':
        this.callbacks.onError(payload.key);
        break;
      case 'closed':
        this.callbacks.onError('sala.hostLeft');
        this.close();
        break;
      default:
        break;
    }
  }

  act(action: GameAction): void {
    this.send({ t: 'action', action });
  }

  addPlaylist(gameId: GameId): void {
    this.send({ t: 'playlist', gameId });
  }

  removePlaylist(gameId: GameId): void {
    this.send({ t: 'removePlaylist', gameId });
  }

  chat(text: string): void {
    this.send({ t: 'chat', text });
  }

  requestLobby(): void {
    this.send({ t: 'lobbyReq' });
  }

  private send(payload: WireMessage): void {
    if (!sendSocket(this.ws, { t: 'send', payload })) {
      this.callbacks.onError('sala.connectionError');
    }
  }

  close(): void {
    if (this.closed) return;
    this.closed = true;
    if (this.welcomeTimer !== null) window.clearTimeout(this.welcomeTimer);
    try {
      this.ws?.close(1000, 'guest left');
    } catch {
      // Ignorar.
    }
    this.callbacks.onClosed();
  }
}
