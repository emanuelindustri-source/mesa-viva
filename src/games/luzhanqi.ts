import type {
  ActResult,
  Difficulty,
  GameAction,
  GameModule,
  GameView,
  LogEvent,
  MatchResult,
  Seat,
} from '../core/types';
import type { Rng } from '../core/types';
import { event, playerViews, shuffleWithRng } from './cards';
import { makeAction, prompt } from './helpers';

type Side = 0 | 1;
type Phase = 'play' | 'matchEnd';
type RankId =
  | 'commander' | 'general' | 'division' | 'brigade' | 'regiment'
  | 'battalion' | 'company' | 'platoon' | 'engineer'
  | 'bomb' | 'mine' | 'flag';

interface RankInfo {
  label: string;
  power: number;
}

const RANKS: Record<RankId, RankInfo> = {
  commander: { label: '司令', power: 10 },
  general: { label: '军长', power: 9 },
  division: { label: '师长', power: 8 },
  brigade: { label: '旅长', power: 7 },
  regiment: { label: '团长', power: 6 },
  battalion: { label: '营长', power: 5 },
  company: { label: '连长', power: 4 },
  platoon: { label: '排长', power: 3 },
  engineer: { label: '工兵', power: 2 },
  bomb: { label: '炸弹', power: 1 },
  mine: { label: '地雷', power: 0 },
  flag: { label: '军旗', power: -1 },
};

export interface LzPiece {
  id: string;
  side: Side;
  rank: RankId;
  cell: number;
  revealed: boolean;
}

export interface LzMove {
  from: number;
  to: number;
  side: Side;
  attacker: RankId;
  defender: RankId | null;
  battle: string | null;
  flagCaptured: boolean;
}

export interface LzState {
  seats: Seat[];
  pieces: LzPiece[];
  turn: Side;
  phase: Phase;
  winner?: Side;
  selected?: string;
  lastMove?: LzMove;
  history: LogEvent[];
  result?: MatchResult;
  rng: Rng;
}

const COLS = 5;
const ROWS = 12;
const CELLS = COLS * ROWS;

function cell(row: number, col: number): number {
  return row * COLS + col;
}

function row(index: number): number {
  return Math.floor(index / COLS);
}

function col(index: number): number {
  return index % COLS;
}

function other(side: Side): Side {
  return side === 0 ? 1 : 0;
}

function isCamp(index: number): boolean {
  const r = row(index);
  const c = col(index);
  if (r === 5 || r === 6) return c === 2;
  return [1, 3, 8, 10].includes(r) && (c === 1 || c === 3);
}

function isRailway(index: number): boolean {
  const r = row(index);
  const c = col(index);
  return [1, 5, 6, 10].includes(r) || c === 0 || c === 4;
}

function isHeadquarters(index: number): boolean {
  const r = row(index);
  const c = col(index);
  return (r === 0 || r === ROWS - 1) && (c === 1 || c === 3);
}

function pieceAt(state: LzState, target: number): LzPiece | undefined {
  return state.pieces.find((piece) => piece.cell === target);
}

function piecesOf(state: LzState, side: Side): LzPiece[] {
  return state.pieces.filter((piece) => piece.side === side);
}

function flagOf(state: LzState, side: Side): LzPiece | undefined {
  return state.pieces.find((piece) => piece.side === side && piece.rank === 'flag');
}

function setupSide(side: Side, rng: Rng): LzPiece[] {
  const rows = side === 0 ? [0, 1, 2, 3, 4] : [7, 8, 9, 10, 11];
  const available: number[] = [];
  for (const r of rows) for (let c = 0; c < COLS; c += 1) available.push(cell(r, c));
  const flagCell = side === 0 ? cell(0, 1) : cell(ROWS - 1, 1);
  const mineCells = side === 0 ? [cell(0, 0), cell(0, 2), cell(0, 3)] : [cell(ROWS - 1, 0), cell(ROWS - 1, 2), cell(ROWS - 1, 3)];
  const rest = available.filter((value) => value !== flagCell && !mineCells.includes(value));
  const shuffled = shuffleWithRng(rest, rng);
  const ranks: RankId[] = [
    'commander', 'general',
    'division', 'division', 'brigade', 'brigade',
    'regiment', 'regiment', 'battalion', 'battalion',
    'company', 'company', 'company',
    'platoon', 'platoon', 'platoon',
    'engineer', 'engineer', 'engineer',
    'bomb', 'bomb',
  ];
  const shuffledRanks = shuffleWithRng(ranks, rng);
  const pieces: LzPiece[] = [];
  let id = 0;
  const prefix = side === 0 ? 'r' : 'b';
  pieces.push({ id: `${prefix}${id++}`, side, rank: 'flag', cell: flagCell, revealed: false });
  for (const mine of mineCells) pieces.push({ id: `${prefix}${id++}`, side, rank: 'mine', cell: mine, revealed: false });
  shuffledRanks.forEach((rank, index) => {
    const target = shuffled[index];
    if (target === undefined) return;
    pieces.push({ id: `${prefix}${id++}`, side, rank, cell: target, revealed: false });
  });
  return pieces;
}

export function legalMoves(state: LzState, pieceId: string): number[] {
  if (state.phase === 'matchEnd') return [];
  const piece = state.pieces.find((value) => value.id === pieceId);
  if (!piece || piece.side !== state.turn) return [];
  if (piece.rank === 'flag' || piece.rank === 'mine') return [];
  const result = new Set<number>();

  const consider = (target: number) => {
    if (target < 0 || target >= CELLS) return;
    const occupant = pieceAt(state, target);
    if (occupant) {
      if (occupant.side !== piece.side && !isCamp(target)) result.add(target);
      return;
    }
    result.add(target);
  };

  consider(piece.cell - COLS);
  consider(piece.cell + COLS);
  consider(piece.cell - 1);
  consider(piece.cell + 1);

  if (isRailway(piece.cell)) {
    const queue: number[] = [piece.cell];
    const visited = new Set<number>([piece.cell]);
    while (queue.length > 0) {
      const current = queue.shift();
      if (current === undefined) break;
      const cr = row(current);
      const cc = col(current);
      const neighbours = [current - COLS, current + COLS, current - 1, current + 1];
      for (const target of neighbours) {
        if (target < 0 || target >= CELLS) continue;
        if (Math.abs(row(target) - cr) + Math.abs(col(target) - cc) !== 1) continue;
        if (!isRailway(target)) continue;
        const occupant = pieceAt(state, target);
        if (occupant) {
          if (occupant.side !== piece.side && !isCamp(target)) result.add(target);
          continue;
        }
        if (!visited.has(target)) {
          visited.add(target);
          result.add(target);
          queue.push(target);
        }
      }
    }
  }
  return [...result];
}

export function allMoves(state: LzState, side: Side): Array<{ pieceId: string; target: number }> {
  const temp = { ...state, turn: side };
  return piecesOf(state, side).flatMap((piece) => legalMoves(temp, piece.id).map((target) => ({ pieceId: piece.id, target })));
}

function resolveMove(state: LzState, pieceId: string, target: number): ActResult<LzState> {
  if (state.phase === 'matchEnd') return { state, events: [], error: 'error.matchOver' };
  const piece = state.pieces.find((value) => value.id === pieceId);
  if (!piece || piece.side !== state.turn) return { state, events: [], error: 'error.notYourTurn' };
  if (!legalMoves(state, pieceId).includes(target)) return { state, events: [], error: 'error.invalidAction' };
  const before = state.history.length;
  const defender = pieceAt(state, target);
  let pieces = state.pieces.map((value) => ({ ...value }));
  let attackerSurvives = true;
  let defenderSurvives = false;
  let flagCaptured = false;
  let battle: string | null = null;
  const lines: string[] = [];
  if (!defender) {
    pieces = pieces.map((value) => (value.id === pieceId ? { ...value, cell: target } : value));
  } else {
    const ar = piece.rank;
    const dr = defender.rank;
    if (dr === 'flag') {
      flagCaptured = true;
      lines.push('¡Bandera capturada!');
    } else if (ar === 'bomb' || dr === 'bomb') {
      attackerSurvives = false;
      defenderSurvives = false;
      battle = 'bomba';
    } else if (dr === 'mine') {
      if (ar === 'engineer') {
        attackerSurvives = true;
        defenderSurvives = false;
        battle = 'ingeniero';
      } else {
        attackerSurvives = false;
        defenderSurvives = true;
        battle = 'mina';
      }
    } else if (RANKS[ar].power > RANKS[dr].power) {
      attackerSurvives = true;
      defenderSurvives = false;
      battle = 'ataque';
    } else if (RANKS[ar].power < RANKS[dr].power) {
      attackerSurvives = false;
      defenderSurvives = true;
      battle = 'defensa';
    } else {
      attackerSurvives = false;
      defenderSurvives = false;
      battle = 'empate';
    }
    pieces = pieces.map((value) => {
      if (value.id === pieceId) return { ...value, revealed: true, ...(attackerSurvives ? { cell: target } : {}) };
      if (value.id === defender.id) return { ...value, revealed: true };
      return value;
    });
    if (!defenderSurvives) pieces = pieces.filter((value) => value.id !== defender.id);
    if (!attackerSurvives) pieces = pieces.filter((value) => value.id !== pieceId);
  }
  const nextTurn = other(state.turn);
  let winner = flagCaptured ? piece.side : undefined;
  if (winner === undefined) {
    const redMoves = allMoves({ ...state, pieces }, 0);
    const blueMoves = allMoves({ ...state, pieces }, 1);
    if (redMoves.length === 0 && blueMoves.length > 0) winner = 1;
    else if (blueMoves.length === 0 && redMoves.length > 0) winner = 0;
  }
  const finished = winner !== undefined;
  const finishedCounts = {
    [state.seats[0]?.id ?? 'p0']: pieces.filter((value) => value.side === 0).length,
    [state.seats[1]?.id ?? 'p1']: pieces.filter((value) => value.side === 1).length,
  };
  const result: MatchResult | undefined = winner !== undefined
    ? {
      winnerIds: [state.seats[winner]?.id ?? ''],
      draw: false,
      scores: finishedCounts,
      reasonKey: flagCaptured ? 'result.reason.luzhanqiFlag' : 'result.reason.luzhanqiPieces',
    }
    : undefined;
  const next: LzState = {
    ...state,
    pieces,
    turn: finished ? state.turn : nextTurn,
    phase: finished ? 'matchEnd' : 'play',
    winner,
    selected: undefined,
    lastMove: {
      from: piece.cell,
      to: target,
      side: piece.side,
      attacker: piece.rank,
      defender: defender?.rank ?? null,
      battle,
      flagCaptured,
    },
    history: [...state.history, event('log.luzhanqiMove', {
      p: state.seats[state.turn]?.name ?? '—',
      from: `${String.fromCharCode(65 + col(piece.cell))}${ROWS - row(piece.cell)}`,
      to: `${String.fromCharCode(65 + col(target))}${ROWS - row(target)}`,
      battle: battle ?? '',
    }, battle ? 'success' : 'info', state.seats[state.turn]?.id)],
    result,
  };
  return { state: next, events: next.history.slice(before) };
}

export const luzhanqiModule: GameModule<LzState> = {
  meta: {
    id: 'luzhanqi',
    nameKey: 'game.luzhanqi.name',
    descKey: 'game.luzhanqi.desc',
    rulesKey: 'game.luzhanqi.rules',
    icon: '🎖️',
    category: 'tablero',
    minPlayers: 2,
    maxPlayers: 2,
    avgMinutes: 30,
    accent: '#b53737',
    tagKeys: ['tag.estrategia', 'tag.cultural'],
  },

  create(seats, _options, rng): LzState {
    const pieces = [...setupSide(0, rng), ...setupSide(1, rng)];
    return {
      seats: seats.slice(0, 2),
      pieces,
      turn: 0,
      phase: 'play',
      history: [event('log.luzhanqiStart', {}, 'info')],
      rng,
    };
  },

  view(state, viewerId): GameView {
    const selected = state.selected;
    const legal = selected ? new Set(legalMoves(state, selected)) : new Set<number>();
    const isTurn = state.seats[state.turn]?.id === viewerId && state.phase === 'play';
    const cells = Array.from({ length: CELLS }, (_, index) => {
      const piece = pieceAt(state, index);
      const visible = piece && (piece.side === 0 || piece.revealed || state.phase === 'matchEnd');
      return {
        glyph: piece ? (visible ? RANKS[piece.rank].label : '?') : undefined,
        side: piece ? (piece.side === 0 ? 'r' as const : 'b' as const) : undefined,
        tone: isCamp(index) ? 'camp' : isRailway(index) ? 'rail' : isHeadquarters(index) ? 'hq' : undefined,
        highlight: legal.has(index) ? 'move' as const : selected === piece?.id ? 'selected' as const : state.lastMove?.to === index ? 'last' as const : undefined,
        label: `${String.fromCharCode(65 + col(index))}${ROWS - row(index)}`,
      };
    });
    const actions = state.phase === 'play'
      ? [makeAction('resign', 'action.resign', { type: 'resign' }, { tone: 'ghost' })]
      : [];
    return {
      gameId: 'luzhanqi',
      youId: viewerId,
      phase: state.phase,
      turnId: state.seats[state.turn]?.id ?? null,
      players: playerViews(state.seats, {
        [state.seats[0]?.id ?? 'p0']: piecesOf(state, 0).length,
        [state.seats[1]?.id ?? 'p1']: piecesOf(state, 1).length,
      }, state.seats[state.turn]?.id ?? null, viewerId, (seat) => ({
        cardsLeft: piecesOf(state, seat.id === state.seats[0]?.id ? 0 : 1).length,
        active: state.phase !== 'matchEnd',
      })),
      hand: [],
      center: { board: { cols: COLS, rows: ROWS, cells } },
      actions,
      prompt: state.phase === 'matchEnd'
        ? prompt('prompt.matchEnd', {}, 'success')
        : isTurn
          ? selected
            ? prompt('prompt.luzhanqiMove', {}, 'info')
            : prompt('prompt.luzhanqiSelect', {}, 'info')
          : prompt('prompt.waitingPlayer', { player: state.seats[state.turn]?.name ?? '—' }, 'info'),
      extra: {
        selected,
        legal: [...legal],
        camps: Array.from({ length: CELLS }, (_, index) => index).filter(isCamp),
        railways: Array.from({ length: CELLS }, (_, index) => index).filter(isRailway),
      },
    };
  },

  act(state, action, actorId): ActResult<LzState> {
    if (state.phase === 'matchEnd') return { state, events: [], error: 'error.matchOver' };
    if (state.seats[state.turn]?.id !== actorId) return { state, events: [], error: 'error.notYourTurn' };
    const before = state.history.length;
    if (action.type === 'move') {
      return resolveMove(state, String(action.pieceId ?? ''), Number(action.target));
    }
    if (action.type === 'resign') {
      const winner = other(state.turn);
      const counts = {
        [state.seats[0]?.id ?? 'p0']: piecesOf(state, 0).length,
        [state.seats[1]?.id ?? 'p1']: piecesOf(state, 1).length,
      };
      const next = {
        ...state,
        phase: 'matchEnd' as const,
        winner,
        result: { winnerIds: [state.seats[winner]?.id ?? ''], draw: false, scores: counts, reasonKey: 'result.reason.resign' },
        history: [...state.history, event('log.luzhanqiResign', { p: state.seats[state.turn]?.name ?? '—' }, 'warn', actorId)],
      };
      return { state: next, events: next.history.slice(before) };
    }
    if (action.type !== 'tap') return { state, events: [], error: 'error.invalidAction' };
    const target = Number(action.cell);
    if (!Number.isInteger(target) || target < 0 || target >= CELLS) return { state, events: [], error: 'error.invalidAction' };
    const piece = pieceAt(state, target);
    if (piece && piece.side === state.turn) {
      return { state: { ...state, selected: piece.id }, events: [] };
    }
    if (state.selected) {
      return resolveMove(state, state.selected, target);
    }
    return { state, events: [], error: 'error.invalidAction' };
  },

  bot(state, botId, _difficulty: Difficulty, rng): GameAction | null {
    if (state.phase === 'matchEnd') return null;
    if (state.seats[state.turn]?.id !== botId) return null;
    const options = allMoves(state, state.turn);
    if (options.length === 0) return null;
    const enemyFlag = flagOf(state, other(state.turn));
    let best = options[0];
    let bestScore = -Infinity;
    for (const option of options) {
      const piece = state.pieces.find((value) => value.id === option.pieceId);
      const defender = pieceAt(state, option.target);
      let score = rng();
      if (defender?.rank === 'flag') score += 10000;
      if (defender?.rank === 'mine' && piece?.rank === 'engineer') score += 500;
      if (piece?.rank === 'bomb' && defender) score += 300;
      if (defender) score += 100 + (RANKS[defender.rank].power - (piece ? RANKS[piece.rank].power : 0)) * 5;
      if (isCamp(option.target)) score += 8;
      if (enemyFlag) score -= Math.abs(row(option.target) - row(enemyFlag.cell)) * 2;
      if (score > bestScore) {
        bestScore = score;
        best = option;
      }
    }
    return best ? { type: 'move', pieceId: best.pieceId, target: best.target } : null;
  },

  result(state): MatchResult | null {
    return state.result ?? null;
  },
};
