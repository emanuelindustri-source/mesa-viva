import type {
  ActResult,
  BoardCell,
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

type Color = 1 | 2;

interface GoState {
  seats: Seat[];
  options: GameOptions;
  size: number;
  board: number[];
  turn: Color;
  passes: number;
  ko: number | null;
  captures: Record<string, number>;
  moveNo: number;
  phase: 'play' | 'matchEnd';
  lastMove?: number;
  result?: MatchResult;
  rng: Rng;
}

interface Attempt {
  board: number[];
  captured: number[];
  ko: number | null;
}

const KOMI = 6.5;

function neighbors(index: number, size: number): number[] {
  const f = index % size;
  const r = Math.floor(index / size);
  const out: number[] = [];
  if (f > 0) out.push(index - 1);
  if (f < size - 1) out.push(index + 1);
  if (r > 0) out.push(index - size);
  if (r < size - 1) out.push(index + size);
  return out;
}

function group(board: number[], index: number, size: number): { stones: number[]; liberties: number[] } {
  const color = board[index] ?? 0;
  const stones: number[] = [];
  const liberties = new Set<number>();
  const seen = new Set<number>();
  const queue = [index];
  while (queue.length > 0) {
    const current = queue.pop() as number;
    if (seen.has(current)) continue;
    seen.add(current);
    if ((board[current] ?? 0) === 0) {
      liberties.add(current);
      continue;
    }
    if ((board[current] ?? 0) !== color) continue;
    stones.push(current);
    for (const n of neighbors(current, size)) {
      if ((board[n] ?? 0) === 0) liberties.add(n);
      else if ((board[n] ?? 0) === color && !seen.has(n)) queue.push(n);
    }
  }
  return { stones, liberties: [...liberties] };
}

function attempt(state: GoState, index: number, color: Color): Attempt | null {
  if (index < 0 || index >= state.board.length) return null;
  if ((state.board[index] ?? 0) !== 0) return null;
  if (state.ko === index) return null;
  const board = state.board.slice();
  board[index] = color;
  const captured: number[] = [];
  const opponent: Color = color === 1 ? 2 : 1;
  for (const n of neighbors(index, state.size)) {
    if ((board[n] ?? 0) !== opponent) continue;
    const g = group(board, n, state.size);
    if (g.liberties.length === 0) {
      for (const stone of g.stones) {
        board[stone] = 0;
        captured.push(stone);
      }
    }
  }
  const own = group(board, index, state.size);
  if (own.liberties.length === 0) return null;
  const ko = captured.length === 1 && own.stones.length === 1 && own.liberties.length === 1
    ? captured[0] ?? null
    : null;
  return { board, captured, ko };
}

function legalMoves(state: GoState, color: Color): number[] {
  const out: number[] = [];
  for (let i = 0; i < state.board.length; i += 1) {
    if ((state.board[i] ?? 0) !== 0) continue;
    if (attempt(state, i, color)) out.push(i);
  }
  return out;
}

function colorForSeat(state: GoState, seatId: string): Color | null {
  if (state.seats[0]?.id === seatId) return 1;
  if (state.seats[1]?.id === seatId) return 2;
  return null;
}

function scoreBoard(state: GoState): { black: number; white: number; blackTerritory: number; whiteTerritory: number } {
  const size = state.size;
  const visited = new Set<number>();
  let blackTerritory = 0;
  let whiteTerritory = 0;
  let blackStones = 0;
  let whiteStones = 0;
  for (let i = 0; i < state.board.length; i += 1) {
    if (state.board[i] === 1) blackStones += 1;
    if (state.board[i] === 2) whiteStones += 1;
    if (state.board[i] !== 0 || visited.has(i)) continue;
    const region: number[] = [];
    const borders = new Set<number>();
    const queue = [i];
    while (queue.length > 0) {
      const current = queue.pop() as number;
      if (visited.has(current)) continue;
      visited.add(current);
      region.push(current);
      for (const n of neighbors(current, size)) {
        const value = state.board[n] ?? 0;
        if (value === 0) queue.push(n);
        else borders.add(value);
      }
    }
    if (borders.size === 1) {
      if (borders.has(1)) blackTerritory += region.length;
      if (borders.has(2)) whiteTerritory += region.length;
    }
  }
  return {
    black: blackStones + blackTerritory,
    white: whiteStones + whiteTerritory + KOMI,
    blackTerritory,
    whiteTerritory,
  };
}

function finish(state: GoState, winner: Seat | null, reason: string, draw = false): GoState {
  const result: MatchResult = {
    winnerIds: winner ? [winner.id] : [],
    draw,
    scores: {
      [state.seats[0]?.id ?? 'b']: scoreBoard(state).black,
      [state.seats[1]?.id ?? 'w']: scoreBoard(state).white,
    },
    reasonKey: reason,
  };
  return { ...state, phase: 'matchEnd', result };
}

function finishScore(state: GoState): GoState {
  const score = scoreBoard(state);
  const blackSeat = state.seats[0];
  const whiteSeat = state.seats[1];
  const winner = score.black > score.white ? blackSeat : whiteSeat;
  if (!winner) return finish(state, null, 'result.reason.draw', true);
  return finish(state, winner, 'result.reason.goScore');
}

function pass(state: GoState): { state: GoState; events: ReturnType<typeof event>[]; error?: string } {
  if (state.phase !== 'play') return { state, events: [], error: 'error.matchOver' };
  const seat = state.seats[state.turn - 1];
  if (!seat) return { state, events: [], error: 'error.invalidAction' };
  const passes = state.passes + 1;
  const events = [event('log.goPass', { p: seat.name }, 'info', seat.id)];
  const next: GoState = {
    ...state,
    passes,
    ko: null,
    lastMove: undefined,
    turn: state.turn === 1 ? 2 : 1,
  };
  if (passes >= 2) {
    const finished = finishScore(next);
    events.push(event('log.goScore', {
      black: finished.result?.scores[state.seats[0]?.id ?? 'b'] ?? 0,
      white: finished.result?.scores[state.seats[1]?.id ?? 'w'] ?? 0,
    }, 'success'));
    const winner = finished.result?.winnerIds[0];
    if (winner) events.push(event('log.won', { p: state.seats.find((s) => s.id === winner)?.name ?? '—' }, 'magic'));
    return { state: finished, events };
  }
  return { state: next, events };
}

function coord(index: number, size: number): string {
  const f = index % size;
  const r = Math.floor(index / size);
  return `${String.fromCharCode(65 + f)}${size - r}`;
}

export const goModule: GameModule<GoState> = {
  meta: {
    id: 'go',
    nameKey: 'game.go.name',
    descKey: 'game.go.desc',
    rulesKey: 'game.go.rules',
    icon: '⚪',
    category: 'tablero',
    minPlayers: 2,
    maxPlayers: 2,
    avgMinutes: 20,
    accent: '#a88b5e',
    tagKeys: ['tag.estrategia', 'tag.ancestral', 'tag.duelo'],
  },

  create(seats, options, rng) {
    const size = options.boardSize === 13 ? 13 : 9;
    return {
      seats: seats.slice(0, 2).map((s) => ({ ...s })),
      options,
      size,
      board: Array.from({ length: size * size }, () => 0),
      turn: 1,
      passes: 0,
      ko: null,
      captures: Object.fromEntries(seats.slice(0, 2).map((s) => [s.id, 0])),
      moveNo: 0,
      phase: 'play',
      rng,
    };
  },

  view(state, viewerId): GameView {
    const turnSeat = state.seats[state.turn - 1];
    const isTurn = turnSeat?.id === viewerId;
    const legal = isTurn && state.phase === 'play' ? legalMoves(state, state.turn) : [];
    const cells: BoardCell[] = state.board.map((stone, i) => {
      const f = i % state.size;
      const r = Math.floor(i / state.size);
      let highlight: BoardCell['highlight'];
      if (state.lastMove === i) highlight = 'last';
      const star = state.size === 9
        ? ((f === 2 || f === 6) && (r === 2 || r === 6)) || (f === 4 && r === 4)
        : (f === 3 || f === 6 || f === 9) && (r === 3 || r === 6 || r === 9);
      return {
        glyph: stone === 1 ? '●' : stone === 2 ? '○' : undefined,
        tone: stone === 1 ? 'stone-b' : stone === 2 ? 'stone-w' : 'go',
        ...(highlight ? { highlight } : {}),
        ...(stone === 0 && star ? { sub: '·' } : {}),
        label: coord(i, state.size),
        pieceId: stone ? `stone-${stone}` : undefined,
      };
    });
    const blackStones = state.board.filter((v) => v === 1).length;
    const whiteStones = state.board.filter((v) => v === 2).length;
    const actions: UIAction[] = [];
    if (state.phase === 'play') {
      if (isTurn) actions.push(makeAction('pass', 'action.pass', { type: 'pass' }, { tone: 'secondary', icon: 'arrow-right' }));
      actions.push(makeAction('resign', 'action.resign', { type: 'resign' }, { tone: 'danger', icon: 'flag' }));
    }
    const prompt: PromptView = state.phase === 'matchEnd'
      ? { key: 'prompt.matchEnd', tone: 'success' }
      : isTurn
        ? { key: state.turn === 1 ? 'prompt.goTurnBlack' : 'prompt.goTurnWhite', tone: 'info' }
        : { key: 'prompt.waitingPlayer', params: { player: turnSeat?.name ?? '—' }, tone: 'info' };
    return {
      gameId: 'go',
      youId: viewerId,
      phase: state.phase,
      turnId: turnSeat?.id ?? null,
      players: playerViews(
        state.seats,
        { [state.seats[0]?.id ?? 'b']: blackStones, [state.seats[1]?.id ?? 'w']: whiteStones },
        turnSeat?.id ?? null,
        viewerId,
        () => ({ active: true }),
      ),
      hand: [],
      center: { board: { cols: state.size, rows: state.size, cells, orientation: 'white' } },
      actions,
      prompt,
      extra: {
        size: state.size,
        legalMoves: legal,
        lastMove: state.lastMove ?? null,
        ko: state.ko,
        captures: state.captures,
        komi: KOMI,
        passes: state.passes,
        blackStones,
        whiteStones,
        turnColor: state.turn,
      },
    };
  },

  act(state, action, actorId, _rng): ActResult<GoState> {
    if (state.phase === 'matchEnd') return { state, events: [], error: 'error.matchOver' };
    const color = colorForSeat(state, actorId);
    if (!color || color !== state.turn) return { state, events: [], error: 'error.notYourTurn' };
    if (action.type === 'resign') {
      const winner = state.seats[state.turn === 1 ? 1 : 0];
      if (!winner) return { state, events: [], error: 'error.invalidAction' };
      return {
        state: finish(state, winner, 'result.reason.resign'),
        events: [event('log.resign', { p: state.seats[state.turn - 1]?.name ?? '—' }, 'danger', actorId)],
      };
    }
    if (action.type === 'pass') {
      const res = pass(state);
      return { state: res.state, events: res.events, ...(res.error ? { error: res.error } : {}) };
    }
    if (action.type !== 'play') return { state, events: [], error: 'error.invalidAction' };
    const index = Number(action.index);
    const attemptResult = attempt(state, index, color);
    if (!attemptResult) return { state, events: [], error: 'error.invalidMove' };
    const seat = state.seats[state.turn - 1];
    if (!seat) return { state, events: [], error: 'error.invalidAction' };
    const captures = {
      ...state.captures,
      [seat.id]: (state.captures[seat.id] ?? 0) + attemptResult.captured.length,
    };
    const next: GoState = {
      ...state,
      board: attemptResult.board,
      ko: attemptResult.ko,
      passes: 0,
      captures,
      turn: color === 1 ? 2 : 1,
      lastMove: index,
      moveNo: state.moveNo + 1,
    };
    const events = [
      event('log.goPlay', { p: seat.name, at: coord(index, state.size) }, 'info', actorId),
    ];
    if (attemptResult.captured.length > 0) {
      events.push(event('log.goCapture', { p: seat.name, n: attemptResult.captured.length }, 'success', actorId));
    }
    return { state: next, events };
  },

  bot(state, botId, difficulty: Difficulty, rng: Rng): GameAction | null {
    if (state.phase === 'matchEnd') return null;
    const color = colorForSeat(state, botId);
    if (!color || color !== state.turn) return null;
    const legal = legalMoves(state, color);
    if (legal.length === 0) return { type: 'pass' };
    if (difficulty === 'easy') {
      return { type: 'play', index: legal[Math.floor(rng() * legal.length)] ?? legal[0] };
    }
    const center = (state.size - 1) / 2;
    let best = -Infinity;
    let pool: number[] = [];
    for (const index of legal) {
      const tried = attempt(state, index, color);
      if (!tried) continue;
      const own = group(tried.board, index, state.size);
      const f = index % state.size;
      const r = Math.floor(index / state.size);
      const centrality = 4 - (Math.abs(center - f) + Math.abs(center - r));
      const value = tried.captured.length * 16 + own.liberties.length * 2 + centrality;
      if (value > best + 0.01) {
        best = value;
        pool = [index];
      } else if (Math.abs(value - best) <= 0.01) {
        pool.push(index);
      }
    }
    const chosen = pool[Math.floor(rng() * pool.length)] ?? legal[0];
    return chosen !== undefined ? { type: 'play', index: chosen } : { type: 'pass' };
  },

  result(state) {
    return state.result ?? null;
  },
};

export type { GoState };
