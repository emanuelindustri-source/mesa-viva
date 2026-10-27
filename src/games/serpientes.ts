import type {
  ActResult,
  BoardCell,
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
import { rolld6 } from '../core/rng';
import { event, nextIndex, playerViews } from './cards';
import { makeAction } from './helpers';

interface SnakeState {
  seats: Seat[];
  options: GameOptions;
  positions: Record<string, number>;
  turn: number;
  lastRoll?: number;
  lastFrom?: number;
  phase: 'play' | 'matchEnd';
  winnerIds?: string[];
  result?: MatchResult;
  rng: Rng;
}

export const LADDERS: Record<number, number> = {
  4: 14, 9: 31, 20: 38, 28: 84, 40: 59, 51: 67, 63: 81, 71: 91,
};
export const SNAKES: Record<number, number> = {
  17: 7, 54: 34, 62: 19, 64: 60, 87: 24, 93: 73, 95: 75, 99: 78,
};

function cellIndex(number: number): number {
  const z = number - 1;
  const rowFromBottom = Math.floor(z / 10);
  const col = rowFromBottom % 2 === 0 ? z % 10 : 9 - (z % 10);
  return (9 - rowFromBottom) * 10 + col;
}

function finish(state: SnakeState, winner: Seat): SnakeState {
  const result: MatchResult = {
    winnerIds: [winner.id],
    scores: Object.fromEntries(state.seats.map((s) => [s.id, state.positions[s.id] ?? 0])),
    reasonKey: 'result.reason.finishLine',
  };
  return { ...state, phase: 'matchEnd', winnerIds: [winner.id], result };
}

export const serpientesModule: GameModule<SnakeState> = {
  meta: {
    id: 'serpientes',
    nameKey: 'game.serpientes.name',
    descKey: 'game.serpientes.desc',
    rulesKey: 'game.serpientes.rules',
    icon: '🪜',
    category: 'tablero',
    minPlayers: 2,
    maxPlayers: 4,
    avgMinutes: 8,
    accent: '#3f8f6b',
    tagKeys: ['tag.familiar', 'tag.dados', 'tag.rapido'],
  },

  create(seats, options, rng) {
    return {
      seats: seats.map((s) => ({ ...s })),
      options,
      positions: Object.fromEntries(seats.map((s) => [s.id, 0])),
      turn: 0,
      phase: 'play',
      rng,
    };
  },

  view(state, viewerId): GameView {
    const turnSeat = state.seats[state.turn];
    const isTurn = turnSeat?.id === viewerId;
    const cells: BoardCell[] = [];
    const byCell = new Map<number, Seat[]>();
    for (const seat of state.seats) {
      const pos = state.positions[seat.id] ?? 0;
      if (pos > 0) {
        const arr = byCell.get(pos) ?? [];
        arr.push(seat);
        byCell.set(pos, arr);
      }
    }
    for (let number = 1; number <= 100; number += 1) {
      const occupants = byCell.get(number) ?? [];
      const isLadder = LADDERS[number];
      const isSnake = SNAKES[number];
      cells[cellIndex(number)] = {
        label: String(number),
        ...(occupants.length > 0
          ? { sub: occupants.map((s) => s.name.slice(0, 1).toUpperCase()).join('') }
          : isLadder
            ? { sub: '🪜' }
            : isSnake
              ? { sub: '🐍' }
              : {}),
        ...(occupants.some((s) => s.id === viewerId) ? { highlight: 'selected' as const } : {}),
        ...(isLadder ? { tone: 'ladder' } : isSnake ? { tone: 'snake' } : number % 2 === 0 ? { tone: 'alt' } : {}),
      };
    }
    const actions: UIAction[] = [];
    if (isTurn && state.phase === 'play') {
      actions.push(makeAction('roll', 'action.rollDice', { type: 'roll' }, { tone: 'primary', icon: 'dices' }));
    }
    const prompt = state.phase === 'matchEnd'
      ? { key: 'prompt.matchEnd', tone: 'success' as const }
      : isTurn
        ? { key: 'prompt.snakesRoll', tone: 'info' as const }
        : { key: 'prompt.waitingPlayer', params: { player: turnSeat?.name ?? '—' }, tone: 'info' as const };
    return {
      gameId: 'serpientes',
      youId: viewerId,
      phase: state.phase,
      turnId: turnSeat?.id ?? null,
      players: playerViews(state.seats, state.positions, turnSeat?.id ?? null, viewerId, () => ({ active: true })),
      hand: [],
      center: {
        board: { cols: 10, rows: 10, cells, orientation: 'white' },
        ...(state.lastRoll
          ? { dice: [{ id: 'last', value: state.lastRoll }] }
          : {}),
      },
      actions,
      prompt,
      extra: {
        ladders: LADDERS,
        snakes: SNAKES,
        lastRoll: state.lastRoll ?? null,
        lastFrom: state.lastFrom ?? null,
      },
    };
  },

  act(state, action, actorId, _rng): ActResult<SnakeState> {
    if (state.phase === 'matchEnd') return { state, events: [], error: 'error.matchOver' };
    const turnSeat = state.seats[state.turn];
    if (!turnSeat || turnSeat.id !== actorId) return { state, events: [], error: 'error.notYourTurn' };
    if (action.type !== 'roll') return { state, events: [], error: 'error.invalidAction' };
    const roll = rolld6(state.rng);
    const from = state.positions[actorId] ?? 0;
    const raw = from + roll > 100 ? from : from + roll;
    let landed = raw;
    const events = [event('log.roll', { p: turnSeat.name, n: roll }, 'info', actorId)];
    if (LADDERS[landed]) {
      landed = LADDERS[landed] ?? landed;
      events.push(event('log.ladder', { p: turnSeat.name, n: landed }, 'success', actorId));
    } else if (SNAKES[landed]) {
      landed = SNAKES[landed] ?? landed;
      events.push(event('log.snake', { p: turnSeat.name, n: landed }, 'warn', actorId));
    }
    const positions = { ...state.positions, [actorId]: landed };
    let next: SnakeState = {
      ...state,
      positions,
      lastRoll: roll,
      lastFrom: from,
      turn: nextIndex(state.turn, state.seats.length),
    };
    if (landed >= 100) {
      events.push(event('log.won', { p: turnSeat.name }, 'magic', actorId));
      next = finish(next, turnSeat);
    }
    return { state: next, events };
  },

  bot(state, botId, _difficulty: Difficulty, _rng: Rng): GameAction | null {
    if (state.phase === 'matchEnd') return null;
    return state.seats[state.turn]?.id === botId ? { type: 'roll' } : null;
  },

  result(state) {
    return state.result ?? null;
  },
};

export type { SnakeState };
