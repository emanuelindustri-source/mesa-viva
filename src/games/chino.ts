import type {
  ActResult,
  Difficulty,
  GameAction,
  GameModule,
  GameOptions,
  GameView,
  MatchResult,
  PromptView,
  Rng,
  Seat,
  UIAction,
} from '../core/types';
import { event, playerViews } from './cards';
import { makeAction } from './helpers';

interface Cell {
  q: number;
  r: number;
  triangle: number | null;
  x: number;
  y: number;
}

interface ChinoState {
  seats: Seat[];
  options: GameOptions;
  pieces: Array<number | null>;
  turn: number;
  phase: 'play' | 'matchEnd';
  lastMove?: { from: number; to: number; jumps: number };
  result?: MatchResult;
  rng: Rng;
}

interface Move {
  from: number;
  to: number;
  jumps: number;
}

const COLORS = ['red', 'blue', 'green', 'yellow', 'purple', 'orange'] as const;
const COLOR_KEYS = ['chino.red', 'chino.blue', 'chino.green', 'chino.yellow', 'chino.purple', 'chino.orange'];
const AXIAL: Array<[number, number]> = [[1, 0], [1, -1], [0, -1], [-1, 0], [-1, 1], [0, 1]];

function buildBoard(): { cells: Cell[]; triangles: number[][]; neighbors: number[][]; index: Map<string, number> } {
  const all = new Map<string, Cell>();
  for (let q = -4; q <= 4; q += 1) {
    for (let r = -4; r <= 4; r += 1) {
      if (Math.abs(q) > 4 || Math.abs(r) > 4 || Math.abs(q + r) > 4) continue;
      all.set(`${q},${r}`, { q, r, triangle: null, x: q + r / 2, y: r * (Math.sqrt(3) / 2) });
    }
  }
  const triangles: number[][] = [];
  const rotate = ([q, r]: [number, number]): [number, number] => [-r, q + r];
  for (let t = 0; t < 6; t += 1) {
    for (let k = 1; k <= 4; k += 1) {
      for (let r = -4; r <= -k; r += 1) {
        let point: [number, number] = [4 + k, r];
        for (let turn = 0; turn < t; turn += 1) point = rotate(point);
        const key = `${point[0]},${point[1]}`;
        const cell = all.get(key);
        if (cell && cell.triangle === null) cell.triangle = t;
        else if (!cell) {
          all.set(key, {
            q: point[0],
            r: point[1],
            triangle: t,
            x: point[0] + point[1] / 2,
            y: point[1] * (Math.sqrt(3) / 2),
          });
        }
      }
    }
    triangles.push([]);
  }
  const cells = [...all.values()];
  const index = new Map<string, number>();
  cells.forEach((cell, i) => index.set(`${cell.q},${cell.r}`, i));
  const neighbors = cells.map((cell) =>
    AXIAL.map(([dq, dr]) => index.get(`${cell.q + dq},${cell.r + dr}`)).filter((v): v is number => v !== undefined),
  );
  for (let t = 0; t < 6; t += 1) {
    triangles[t] = cells.map((cell, i) => (cell.triangle === t ? i : -1)).filter((i) => i >= 0);
  }
  return { cells, triangles, neighbors, index };
}

const BOARD = buildBoard();
const CELLS = BOARD.cells;
const NEIGHBORS = BOARD.neighbors;
const TRIANGLES = BOARD.triangles;

function colorsForPlayers(count: number): number[] {
  if (count <= 2) return [0, 3];
  if (count === 3) return [0, 2, 4];
  if (count === 4) return [0, 1, 3, 4];
  return [0, 1, 2, 3, 4, 5].slice(0, Math.max(2, Math.min(6, count)));
}

function targetTriangle(seat: number, starts: number[]): number {
  return ((starts[seat] ?? 0) + 3) % 6;
}

function targetCells(seat: number, starts: number[]): number[] {
  return TRIANGLES[targetTriangle(seat, starts)] ?? [];
}

function distanceToTarget(): number[][] {
  return TRIANGLES.map((tri) => {
    const dist = Array.from({ length: CELLS.length }, () => Infinity);
    const queue: number[] = [];
    for (const cell of tri) {
      dist[cell] = 0;
      queue.push(cell);
    }
    for (let head = 0; head < queue.length; head += 1) {
      const current = queue[head];
      if (current === undefined) continue;
      for (const next of NEIGHBORS[current] ?? []) {
        if ((dist[next] ?? Infinity) > (dist[current] ?? Infinity) + 1) {
          dist[next] = (dist[current] ?? Infinity) + 1;
          queue.push(next);
        }
      }
    }
    return dist;
  });
}

const DIST = distanceToTarget();

function legalFor(state: ChinoState, seat: number): Move[] {
  const starts = colorsForPlayers(state.seats.length);
  const target = new Set(targetCells(seat, starts));
  const moves: Move[] = [];
  for (let from = 0; from < state.pieces.length; from += 1) {
    if (state.pieces[from] !== seat) continue;
    for (const next of NEIGHBORS[from] ?? []) {
      if (state.pieces[next] === null) moves.push({ from, to: next, jumps: 0 });
    }
    const seen = new Set<number>([from]);
    const queue: Array<{ cell: number; jumps: number }> = [{ cell: from, jumps: 0 }];
    while (queue.length > 0) {
      const current = queue.shift();
      if (!current) continue;
      for (const dir of AXIAL) {
        const midKey = `${CELLS[current.cell]?.q ?? 0},${CELLS[current.cell]?.r ?? 0}`;
        const key = midKey.split(',').map(Number);
        const mid = BOARD.index.get(`${(key[0] ?? 0) + dir[0]},${(key[1] ?? 0) + dir[1]}`);
        if (mid === undefined || state.pieces[mid] === null) continue;
        const cell = CELLS[mid];
        if (!cell) continue;
        const landing = BOARD.index.get(`${cell.q + dir[0]},${cell.r + dir[1]}`);
        if (landing === undefined || state.pieces[landing] !== null || seen.has(landing)) continue;
        seen.add(landing);
        queue.push({ cell: landing, jumps: current.jumps + 1 });
        if (!target.has(landing)) moves.push({ from, to: landing, jumps: current.jumps + 1 });
      }
    }
  }
  return moves;
}

function reachedTarget(state: ChinoState, seat: number): number {
  const starts = colorsForPlayers(state.seats.length);
  return targetCells(seat, starts).filter((cell) => state.pieces[cell] === seat).length;
}

function resultFor(state: ChinoState, winner: number): MatchResult {
  const seat = state.seats[winner];
  return {
    winnerIds: seat ? [seat.id] : [],
    scores: Object.fromEntries(state.seats.map((player, i) => [player.id, reachedTarget(state, i)])),
    reasonKey: 'result.reason.chinoHome',
  };
}

function promptFor(state: ChinoState, isTurn: boolean): PromptView {
  if (state.phase === 'matchEnd') return { key: 'prompt.matchEnd', tone: 'success' };
  if (!isTurn) {
    const seat = state.seats[state.turn];
    return { key: 'prompt.waitingPlayer', params: { player: seat?.name ?? '—' }, tone: 'info' };
  }
  return { key: 'prompt.chinoYourTurn', tone: 'info' };
}

export const chinoModule: GameModule<ChinoState> = {
  meta: {
    id: 'chino',
    nameKey: 'game.chino.name',
    descKey: 'game.chino.desc',
    rulesKey: 'game.chino.rules',
    icon: '✡️',
    category: 'tablero',
    minPlayers: 2,
    maxPlayers: 6,
    avgMinutes: 25,
    accent: '#6f4bb5',
    tagKeys: ['tag.estrategia', 'tag.clasico', 'tag.familia'],
  },

  create(seats, options, rng) {
    const active = seats.slice(0, Math.min(6, seats.length));
    const starts = colorsForPlayers(active.length);
    const pieces: Array<number | null> = Array.from({ length: CELLS.length }, () => null);
    starts.forEach((triangle, seat) => {
      for (const cell of TRIANGLES[triangle] ?? []) pieces[cell] = seat;
    });
    return {
      seats: active.map((seat) => ({ ...seat })),
      options,
      pieces,
      turn: 0,
      phase: 'play',
      rng,
    };
  },

  view(state, viewerId): GameView {
    const seat = state.seats[state.turn];
    const isTurn = seat?.id === viewerId;
    const moves = isTurn && state.phase === 'play' ? legalFor(state, state.turn) : [];
    const starts = colorsForPlayers(state.seats.length);
    const actions: UIAction[] = [];
    if (state.phase === 'play') actions.push(makeAction('resign', 'action.resign', { type: 'resign' }, { tone: 'danger', icon: 'flag' }));
    const scores = Object.fromEntries(state.seats.map((player, i) => [player.id, reachedTarget(state, i)]));
    return {
      gameId: 'chino',
      youId: viewerId,
      phase: state.phase,
      turnId: seat?.id ?? null,
      players: playerViews(state.seats, scores, seat?.id ?? null, viewerId, () => ({ active: true })),
      hand: [],
      center: {},
      actions,
      prompt: promptFor(state, isTurn),
      extra: {
        cells: CELLS.map((cell, i) => ({
          index: i,
          triangle: cell.triangle,
          x: cell.x,
          y: cell.y,
          piece: state.pieces[i],
        })),
        triangles: TRIANGLES,
        starts,
        targets: starts.map((triangle) => (triangle + 3) % 6),
        pieces: state.pieces,
        legalMoves: moves,
        colors: COLORS,
        colorKeys: COLOR_KEYS,
        targetProgress: state.seats.map((_, i) => reachedTarget(state, i)),
        lastMove: state.lastMove ?? null,
      },
    };
  },

  act(state, action, actorId, _rng): ActResult<ChinoState> {
    if (state.phase === 'matchEnd') return { state, events: [], error: 'error.matchOver' };
    const turnSeat = state.seats[state.turn];
    if (!turnSeat || turnSeat.id !== actorId) return { state, events: [], error: 'error.notYourTurn' };
    if (action.type === 'resign') {
      const winner = (state.turn + 1) % state.seats.length;
      return {
        state: { ...state, phase: 'matchEnd', result: resultFor(state, winner) },
        events: [event('log.resign', { p: turnSeat.name }, 'danger', actorId)],
      };
    }
    if (action.type !== 'move') return { state, events: [], error: 'error.invalidAction' };
    const from = Number(action.from);
    const to = Number(action.to);
    const move = legalFor(state, state.turn).find((candidate) => candidate.from === from && candidate.to === to);
    if (!move) return { state, events: [], error: 'error.invalidMove' };
    const pieces = state.pieces.slice();
    pieces[from] = null;
    pieces[to] = state.turn;
    const next: ChinoState = {
      ...state,
      pieces,
      lastMove: move,
      turn: (state.turn + 1) % state.seats.length,
    };
    const starts = colorsForPlayers(state.seats.length);
    const target = targetCells(state.turn, starts);
    const won = target.every((cell) => pieces[cell] === state.turn);
    const events = [event('log.chinoMove', { p: turnSeat.name, n: move.jumps }, move.jumps > 0 ? 'success' : 'info', actorId)];
    if (won) {
      events.push(event('log.won', { p: turnSeat.name }, 'magic', actorId));
      return {
        state: { ...next, phase: 'matchEnd', result: resultFor({ ...next, pieces }, state.turn) },
        events,
      };
    }
    return { state: next, events };
  },

  bot(state, botId, difficulty: Difficulty, rng: Rng): GameAction | null {
    if (state.phase === 'matchEnd') return null;
    const turnSeat = state.seats[state.turn];
    if (!turnSeat || turnSeat.id !== botId) return null;
    const moves = legalFor(state, state.turn);
    if (moves.length === 0) return null;
    const starts = colorsForPlayers(state.seats.length);
    const target = targetTriangle(state.turn, starts);
    const dist = DIST[target] ?? [];
    let best = -Infinity;
    let pool: Move[] = [];
    for (const move of moves) {
      const value = -(dist[move.to] ?? 99) * 10 + move.jumps * 3 + (difficulty === 'easy' ? rng() * 8 : 0);
      if (value > best + 1) {
        best = value;
        pool = [move];
      } else if (Math.abs(value - best) <= 1) {
        pool.push(move);
      }
    }
    const chosen = pool[Math.floor(rng() * pool.length)] ?? moves[0];
    return chosen ? { type: 'move', from: chosen.from, to: chosen.to } : null;
  },

  result(state) {
    return state.result ?? null;
  },
};

export type { ChinoState, Cell as ChinoCell, Move as ChinoMove };
