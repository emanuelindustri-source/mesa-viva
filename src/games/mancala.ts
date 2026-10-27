import type {
  ActResult,
  Difficulty,
  GameAction,
  GameModule,
  GameOptions,
  GameView,
  MatchResult,
  Seat,
  UIAction,
} from '../core/types';
import type { Rng } from '../core/types';
import { event, playerViews } from './cards';
import { makeAction } from './helpers';

type Side = 'w' | 'b';

interface MancalaState {
  seats: Seat[];
  options: GameOptions;
  pits: number[]; // 0-5 surco blanco, 6 almacén blanco, 7-12 surco negro, 13 almacén negro
  turn: Side;
  phase: 'play' | 'matchEnd';
  lastPit?: number;
  lastCapture?: { pit: number; seeds: number };
  winnerIds?: string[];
  result?: MatchResult;
  scores: Record<string, number>;
  rng: Rng;
}

const SIDE_PITS: Record<Side, number[]> = {
  w: [0, 1, 2, 3, 4, 5],
  b: [7, 8, 9, 10, 11, 12],
};
const STORE: Record<Side, number> = { w: 6, b: 13 };

function other(side: Side): Side {
  return side === 'w' ? 'b' : 'w';
}

function captureOpposite(pit: number): number {
  return 12 - pit;
}

function finish(state: MancalaState): MancalaState {
  const pits = [...state.pits];
  const stores = { w: pits[6] ?? 0, b: pits[13] ?? 0 };
  for (const p of SIDE_PITS.w) stores.w += pits[p] ?? 0;
  for (const p of SIDE_PITS.b) stores.b += pits[p] ?? 0;
  const scores = {
    [state.seats[0]?.id ?? 'w']: stores.w,
    [state.seats[1]?.id ?? 'b']: stores.b,
  };
  const draw = stores.w === stores.b;
  const winnerIds = draw ? [] : [state.seats[stores.w > stores.b ? 0 : 1]?.id ?? ''].filter(Boolean);
  const result: MatchResult = {
    winnerIds,
    draw,
    scores,
    reasonKey: draw ? 'result.reason.draw' : 'result.reason.moreSeeds',
  };
  return { ...state, pits, phase: 'matchEnd', winnerIds, result };
}

function checkEnd(state: MancalaState): MancalaState {
  const wEmpty = SIDE_PITS.w.every((p) => (state.pits[p] ?? 0) === 0);
  const bEmpty = SIDE_PITS.b.every((p) => (state.pits[p] ?? 0) === 0);
  return wEmpty || bEmpty ? finish(state) : state;
}

function sow(state: MancalaState, pit: number): { state: MancalaState; events: ReturnType<typeof event>[] } {
  const side = state.turn;
  const pits = [...state.pits];
  let seeds = pits[pit] ?? 0;
  if (seeds <= 0) return { state, events: [] };
  pits[pit] = 0;
  let cursor = pit;
  const opponentStore = STORE[other(side)];
  while (seeds > 0) {
    cursor = (cursor + 1) % 14;
    if (cursor === opponentStore) continue;
    pits[cursor] = (pits[cursor] ?? 0) + 1;
    seeds -= 1;
  }
  const events = [event('log.sowed', { p: state.seats[side === 'w' ? 0 : 1]?.name ?? '—', n: pit + 1 }, 'info')];
  const myStore = STORE[side];
  let extraTurn = false;
  let lastCapture: MancalaState['lastCapture'];
  if (cursor === myStore) {
    extraTurn = true;
    events.push(event('log.extraTurn', { p: state.seats[side === 'w' ? 0 : 1]?.name ?? '—' }, 'success'));
  } else if (SIDE_PITS[side].includes(cursor) && pits[cursor] === 1) {
    const opposite = captureOpposite(cursor);
    const captured = pits[opposite] ?? 0;
    if (captured > 0) {
      pits[opposite] = 0;
      pits[cursor] = 0;
      pits[myStore] = (pits[myStore] ?? 0) + captured + 1;
      lastCapture = { pit: cursor, seeds: captured + 1 };
      events.push(
        event('log.mancalaCapture', { p: state.seats[side === 'w' ? 0 : 1]?.name ?? '—', n: captured + 1 }, 'success'),
      );
    }
  }
  let next: MancalaState = {
    ...state,
    pits,
    lastPit: cursor,
    ...(lastCapture ? { lastCapture } : { lastCapture: undefined }),
    turn: extraTurn ? side : other(side),
    scores: {
      [state.seats[0]?.id ?? 'w']: pits[6] ?? 0,
      [state.seats[1]?.id ?? 'b']: pits[13] ?? 0,
    },
  };
  if (!extraTurn) next = checkEnd(next);
  return { state: next, events };
}

function evaluate(state: MancalaState, root: Side): number {
  const mine = root === 'w' ? (state.pits[6] ?? 0) : (state.pits[13] ?? 0);
  const theirs = root === 'w' ? (state.pits[13] ?? 0) : (state.pits[6] ?? 0);
  const myPits = SIDE_PITS[root].reduce((total, p) => total + (state.pits[p] ?? 0), 0);
  const theirPits = SIDE_PITS[other(root)].reduce((total, p) => total + (state.pits[p] ?? 0), 0);
  return (mine - theirs) * 10 + (myPits - theirPits) * 0.4;
}

function search(state: MancalaState, depth: number, alpha: number, beta: number, root: Side): number {
  if (state.phase === 'matchEnd') {
    const mine = root === 'w' ? (state.pits[6] ?? 0) : (state.pits[13] ?? 0);
    const theirs = root === 'w' ? (state.pits[13] ?? 0) : (state.pits[6] ?? 0);
    return (mine - theirs) * 1000 + (mine > theirs ? 500 : mine < theirs ? -500 : 0);
  }
  if (depth <= 0) return evaluate(state, root);
  const pits = SIDE_PITS[state.turn];
  const moves = pits.filter((p) => (state.pits[p] ?? 0) > 0);
  if (moves.length === 0) return evaluate(state, root);
  const maximizing = state.turn === root;
  let best = maximizing ? -Infinity : Infinity;
  for (const pit of moves) {
    const next = sow(state, pit).state;
    const value = search(next, depth - 1, alpha, beta, root);
    if (maximizing) {
      if (value > best) best = value;
      if (value > alpha) alpha = value;
    } else {
      if (value < best) best = value;
      if (value < beta) beta = value;
    }
    if (alpha >= beta) break;
  }
  return best;
}

export const mancalaModule: GameModule<MancalaState> = {
  meta: {
    id: 'mancala',
    nameKey: 'game.mancala.name',
    descKey: 'game.mancala.desc',
    rulesKey: 'game.mancala.rules',
    icon: '🫘',
    category: 'tablero',
    minPlayers: 2,
    maxPlayers: 2,
    avgMinutes: 10,
    accent: '#b7793c',
    tagKeys: ['tag.ancestral', 'tag.logica', 'tag.duelo'],
  },

  create(seats, options, rng) {
    return {
      seats: seats.slice(0, 2).map((s) => ({ ...s })),
      options,
      pits: [4, 4, 4, 4, 4, 4, 0, 4, 4, 4, 4, 4, 4, 0],
      turn: 'w',
      phase: 'play',
      scores: Object.fromEntries(seats.slice(0, 2).map((s) => [s.id, 0])),
      rng,
    };
  },

  view(state, viewerId): GameView {
    const turnSeat = state.seats[state.turn === 'w' ? 0 : 1];
    const isTurn = turnSeat?.id === viewerId;
    const movable = isTurn && state.phase === 'play'
      ? SIDE_PITS[state.turn].filter((p) => (state.pits[p] ?? 0) > 0)
      : [];
    const actions: UIAction[] = [];
    if (state.phase === 'play') {
      actions.push(makeAction('resign', 'action.resign', { type: 'resign' }, { tone: 'ghost', icon: 'flag' }));
    }
    const prompt = state.phase === 'matchEnd'
      ? { key: 'prompt.matchEnd', tone: 'success' as const }
      : isTurn
        ? { key: 'prompt.mancalaYourTurn', tone: 'info' as const }
        : { key: 'prompt.waitingPlayer', params: { player: turnSeat?.name ?? '—' }, tone: 'info' as const };
    return {
      gameId: 'mancala',
      youId: viewerId,
      phase: state.phase,
      turnId: turnSeat?.id ?? null,
      players: playerViews(
        state.seats,
        { [state.seats[0]?.id ?? 'w']: state.pits[6] ?? 0, [state.seats[1]?.id ?? 'b']: state.pits[13] ?? 0 },
        turnSeat?.id ?? null,
        viewerId,
        () => ({ active: true }),
      ),
      hand: [],
      center: {},
      actions,
      prompt,
      extra: {
        pits: state.pits,
        movable,
        stores: { w: state.pits[6] ?? 0, b: state.pits[13] ?? 0 },
        turn: state.turn,
        lastPit: state.lastPit ?? null,
        lastCapture: state.lastCapture ?? null,
      },
    };
  },

  act(state, action, actorId, _rng): ActResult<MancalaState> {
    if (state.phase === 'matchEnd') return { state, events: [], error: 'error.matchOver' };
    const turnSeat = state.seats[state.turn === 'w' ? 0 : 1];
    if (!turnSeat) return { state, events: [], error: 'error.invalidAction' };
    if (action.type === 'resign') {
      const winner = state.seats[state.turn === 'w' ? 1 : 0];
      if (!winner) return { state, events: [], error: 'error.invalidAction' };
      const result: MatchResult = {
        winnerIds: [winner.id],
        draw: false,
        scores: {
          [state.seats[0]?.id ?? 'w']: state.pits[6] ?? 0,
          [state.seats[1]?.id ?? 'b']: state.pits[13] ?? 0,
        },
        reasonKey: 'result.reason.resign',
      };
      return {
        state: { ...state, phase: 'matchEnd', winnerIds: [winner.id], result },
        events: [event('log.resign', { p: turnSeat.name }, 'danger', actorId)],
      };
    }
    if (turnSeat.id !== actorId) return { state, events: [], error: 'error.notYourTurn' };
    if (action.type !== 'sow') return { state, events: [], error: 'error.invalidAction' };
    const pit = Number(action.pit);
    if (!SIDE_PITS[state.turn].includes(pit) || (state.pits[pit] ?? 0) === 0) {
      return { state, events: [], error: 'error.invalidPit' };
    }
    const res = sow(state, pit);
    if (res.state.phase === 'matchEnd') {
      const winner = res.state.winnerIds?.[0];
      res.events.push(event('log.won', { p: state.seats.find((s) => s.id === winner)?.name ?? '—' }, 'magic'));
    }
    return { state: res.state, events: res.events };
  },

  bot(state, botId, difficulty: Difficulty, rng: Rng): GameAction | null {
    if (state.phase === 'matchEnd') return null;
    const turnSeat = state.seats[state.turn === 'w' ? 0 : 1];
    if (!turnSeat || turnSeat.id !== botId) return null;
    const moves = SIDE_PITS[state.turn].filter((p) => (state.pits[p] ?? 0) > 0);
    if (moves.length === 0) return null;
    if (difficulty === 'easy') {
      const pit = moves[Math.floor(rng() * moves.length)] ?? moves[0];
      return pit !== undefined ? { type: 'sow', pit } : null;
    }
    const depth = difficulty === 'hard' ? 7 : 4;
    let best = -Infinity;
    let pool: number[] = [];
    for (const pit of moves) {
      const value = search(sow(state, pit).state, depth - 1, -Infinity, Infinity, state.turn);
      if (value > best + 0.01) {
        best = value;
        pool = [pit];
      } else if (Math.abs(value - best) <= 0.01) {
        pool.push(pit);
      }
    }
    const pit = pool[Math.floor(rng() * pool.length)] ?? moves[0];
    return pit !== undefined ? { type: 'sow', pit } : null;
  },

  result(state) {
    return state.result ?? null;
  },
};

export type { MancalaState };
