import type {
  ActResult,
  Difficulty,
  GameAction,
  GameModule,
  GameOptions,
  GameView,
  MatchResult,
  Seat,
  TileView,
  UIAction,
} from '../core/types';
import type { Rng } from '../core/types';
import { shuffle } from '../core/rng';
import { event, nextIndex, playerViews } from './cards';
import { makeAction } from './helpers';

type Phase = 'play' | 'matchEnd';
type End = 'left' | 'right';

interface Tile {
  id: string;
  a: number;
  b: number;
}

interface DominoState {
  seats: Seat[];
  options: GameOptions;
  scores: Record<string, number>;
  hands: Record<string, Tile[]>;
  boneyard: Tile[];
  chain: Tile[];
  left: number | null;
  right: number | null;
  turn: number;
  passes: number;
  phase: Phase;
  handNo: number;
  target: number;
  teams: boolean;
  starter: number;
  lastPlay?: { playerId: string; tile: Tile; end: End | 'start' };
  winnerIds?: string[];
  result?: MatchResult;
  rng: Rng;
}

function doubleSixDeck(maxPip = 6): Tile[] {
  const tiles: Tile[] = [];
  for (let a = 0; a <= maxPip; a += 1) {
    for (let b = a; b <= maxPip; b += 1) {
      tiles.push({ id: `${a}-${b}`, a, b });
    }
  }
  return tiles;
}

function pips(tile: Tile): number {
  return tile.a + tile.b;
}

function handPips(hand: Tile[]): number {
  return hand.reduce((total, t) => total + pips(t), 0);
}

function fits(tile: Tile, value: number | null): boolean {
  return value !== null && (tile.a === value || tile.b === value);
}

function canPlay(tile: Tile, state: DominoState): boolean {
  return fits(tile, state.left) || fits(tile, state.right);
}

function hasPlayable(hand: Tile[], state: DominoState): boolean {
  return hand.some((t) => canPlay(t, state));
}

function teamScores(state: DominoState): Record<string, number> {
  if (!state.teams) return { ...state.scores };
  const out: Record<string, number> = {};
  for (const team of [0, 1]) {
    const total = state.seats
      .filter((s) => (s.team ?? 0) === team)
      .reduce((max, s) => Math.max(max, state.scores[s.id] ?? 0), 0);
    for (const s of state.seats) if ((s.team ?? 0) === team) out[s.id] = total;
  }
  return out;
}

function teamPips(state: DominoState, team: number): number {
  return state.seats
    .filter((s) => (s.team ?? 0) === team)
    .reduce((total, s) => total + handPips(state.hands[s.id] ?? []), 0);
}

function award(state: DominoState, winners: Seat[], points: number): Record<string, number> {
  const scores = { ...state.scores };
  for (const seat of winners) scores[seat.id] = (scores[seat.id] ?? 0) + points;
  return scores;
}

function finish(state: DominoState, winners: Seat[], points: number, reason: string): DominoState {
  const scores = award(state, winners, points);
  const winnerIds = winners.map((s) => s.id);
  const result: MatchResult = {
    winnerIds,
    draw: winners.length === 0,
    scores: state.teams ? teamScores({ ...state, scores }) : scores,
    reasonKey: reason,
  };
  return { ...state, scores, phase: 'matchEnd', winnerIds, result };
}

function checkTarget(state: DominoState, reason: string): DominoState {
  const best = Math.max(...state.seats.map((s) => state.scores[s.id] ?? 0));
  if (best >= state.target) {
    const top = state.seats.filter((s) => (state.scores[s.id] ?? 0) === best);
    return finish({ ...state, scores: state.scores }, top, 0, reason);
  }
  return newHand(state);
}

function endHand(state: DominoState, winnerId: string | null): DominoState {
  let winners: Seat[] = [];
  let points = 0;
  let reason = 'result.reason.dominoBlocked';
  if (winnerId) {
    const winner = state.seats.find((s) => s.id === winnerId);
    if (winner) {
      winners = state.teams ? state.seats.filter((s) => (s.team ?? 0) === (winner.team ?? 0)) : [winner];
      const losers = state.teams
        ? state.seats.filter((s) => (s.team ?? 0) !== (winner.team ?? 0))
        : state.seats.filter((s) => s.id !== winnerId);
      points = losers.reduce((total, s) => total + handPips(state.hands[s.id] ?? []), 0);
      reason = 'result.reason.dominoOut';
    }
  } else {
    // Tranca: gana quien tenga menos puntos en la mano.
    if (state.teams) {
      const totals = [0, 1].map((team) => teamPips(state, team));
      const min = Math.min(...totals);
      const winningTeams = totals.map((t, i) => (t === min ? i : -1)).filter((i) => i >= 0);
      if (winningTeams.length === 1) {
        const team = winningTeams[0] ?? 0;
        winners = state.seats.filter((s) => (s.team ?? 0) === team);
        points = state.seats
          .filter((s) => (s.team ?? 0) !== team)
          .reduce((total, s) => total + handPips(state.hands[s.id] ?? []), 0);
      }
    } else {
      const totals = state.seats.map((s) => handPips(state.hands[s.id] ?? []));
      const min = Math.min(...totals);
      const winningIndexes = totals.map((t, i) => (t === min ? i : -1)).filter((i) => i >= 0);
      if (winningIndexes.length === 1) {
        const idx = winningIndexes[0] ?? 0;
        const winner = state.seats[idx];
        if (winner) {
          winners = [winner];
          points = totals.reduce((total, v, i) => (i === idx ? total : total + v), 0);
        }
      }
    }
  }
  if (winners.length === 0) {
    const result: MatchResult = {
      winnerIds: [],
      draw: true,
      scores: state.teams ? teamScores(state) : state.scores,
      reasonKey: 'result.reason.draw',
    };
    return { ...state, phase: 'matchEnd', winnerIds: [], result };
  }
  const withPoints: DominoState = { ...state, scores: award(state, winners, points) };
  return checkTarget(withPoints, reason);
}

function newHand(state: DominoState): DominoState {
  const maxPip = state.options.doubleTwelve ? 12 : 6;
  const deck = shuffle(doubleSixDeck(maxPip), state.rng);
  const handSize = maxPip === 12
    ? state.seats.length >= 4 ? 10 : state.seats.length === 3 ? 12 : 15
    : state.seats.length >= 3 ? 7 : 10;
  const hands: Record<string, Tile[]> = Object.fromEntries(state.seats.map((s) => [s.id, []]));
  for (let i = 0; i < handSize; i += 1) {
    for (const seat of state.seats) {
      const t = deck.shift();
      if (t) hands[seat.id]?.push(t);
    }
  }
  // Busca el doble más alto en las manos; si no hay, la ficha de más puntos.
  let starter = 0;
  let bestTile: Tile | undefined;
  for (let d = maxPip; d >= 0; d -= 1) {
    for (const seat of state.seats) {
      const tile = (hands[seat.id] ?? []).find((t) => t.a === d && t.b === d);
      if (tile) {
        starter = state.seats.indexOf(seat);
        bestTile = tile;
        break;
      }
    }
    if (bestTile) break;
  }
  if (!bestTile) {
    let bestScore = -1;
    for (const seat of state.seats) {
      for (const tile of hands[seat.id] ?? []) {
        const score = pips(tile) * 10 + Math.max(tile.a, tile.b);
        if (score > bestScore) {
          bestScore = score;
          bestTile = tile;
          starter = state.seats.indexOf(seat);
        }
      }
    }
  }
  if (bestTile) {
    hands[state.seats[starter]?.id ?? ''] = (hands[state.seats[starter]?.id ?? ''] ?? []).filter(
      (t) => t.id !== bestTile?.id,
    );
  }
  const chain = bestTile ? [bestTile] : [];
  const left = bestTile ? bestTile.a : null;
  const right = bestTile ? bestTile.b : null;
  return {
    ...state,
    hands,
    boneyard: deck,
    chain,
    left,
    right,
    turn: bestTile ? nextIndex(starter, state.seats.length) : 0,
    passes: 0,
    phase: 'play',
    handNo: state.handNo + 1,
    starter,
    lastPlay: bestTile ? { playerId: state.seats[starter]?.id ?? '', tile: bestTile, end: 'start' } : undefined,
  };
}

export const dominoModule: GameModule<DominoState> = {
  meta: {
    id: 'domino',
    nameKey: 'game.domino.name',
    descKey: 'game.domino.desc',
    rulesKey: 'game.domino.rules',
    icon: '🁫',
    category: 'tablero',
    minPlayers: 2,
    maxPlayers: 4,
    avgMinutes: 20,
    accent: '#547aa5',
    tagKeys: ['tag.venezolano', 'tag.parejas', 'tag.clasico'],
  },

  create(seats, options, rng): DominoState {
    const teams = Boolean(options.teams) && seats.length === 4;
    const withTeams = seats.map((s, i) => (teams ? { ...s, team: i % 2 } : { ...s }));
    const base: DominoState = {
      seats: withTeams,
      options,
      scores: Object.fromEntries(withTeams.map((s) => [s.id, 0])),
      hands: Object.fromEntries(withTeams.map((s) => [s.id, []])),
      boneyard: [],
      chain: [],
      left: null,
      right: null,
      turn: 0,
      passes: 0,
      phase: 'play',
      handNo: 0,
      target: options.target ?? 100,
      teams,
      starter: 0,
      rng,
    };
    return newHand(base);
  },

  view(state, viewerId): GameView {
    const turnSeat = state.seats[state.turn];
    const isTurn = turnSeat?.id === viewerId;
    const hand = (state.hands[viewerId] ?? []).map<TileView>((tile) => ({
      ...tile,
      playable: isTurn && canPlay(tile, state),
    }));
    const handHasPlay = hasPlayable(state.hands[viewerId] ?? [], state);
    const actions: UIAction[] = [];
    if (isTurn && state.phase === 'play' && !handHasPlay) {
      if (state.boneyard.length > 0) {
        actions.push(makeAction('draw', 'action.drawTile', { type: 'draw' }, { tone: 'primary', icon: 'layers' }));
      } else {
        actions.push(makeAction('pass', 'action.pass', { type: 'pass' }, { tone: 'secondary', icon: 'arrow-right' }));
      }
    }
    let prompt: GameView['prompt'];
    if (state.phase === 'matchEnd') prompt = { key: 'prompt.matchEnd', tone: 'success' };
    else if (isTurn) {
      prompt = handHasPlay
        ? { key: 'prompt.dominoPlay', tone: 'info' }
        : state.boneyard.length > 0
          ? { key: 'prompt.dominoDraw', tone: 'warn' }
          : { key: 'prompt.dominoPass', tone: 'warn' };
    } else {
      prompt = { key: 'prompt.waitingPlayer', params: { player: turnSeat?.name ?? '—' }, tone: 'info' };
    }
    return {
      gameId: 'domino',
      youId: viewerId,
      phase: state.phase,
      turnId: turnSeat?.id ?? null,
      players: playerViews(state.seats, state.scores, turnSeat?.id ?? null, viewerId, (seat) => ({
        cardsLeft: state.hands[seat.id]?.length ?? 0,
        active: state.phase !== 'matchEnd',
      })),
      hand: [],
      handTiles: hand,
      center: {
        tiles: state.chain,
        piles: [{ id: 'boneyard', labelKey: 'table.boneyard', count: state.boneyard.length }],
      },
      actions,
      prompt,
      extra: {
        left: state.left,
        right: state.right,
        teamScores: state.teams ? teamScores(state) : null,
        teams: state.teams,
        target: state.target,
        handNo: state.handNo,
        lastPlay: state.lastPlay ?? null,
        boneyard: state.boneyard.length,
        passes: state.passes,
      },
    };
  },

  act(state, action, actorId, _rng): ActResult<DominoState> {
    if (state.phase === 'matchEnd') return { state, events: [], error: 'error.matchOver' };
    const seat = state.seats[state.turn];
    if (!seat || seat.id !== actorId) return { state, events: [], error: 'error.notYourTurn' };
    const hand = state.hands[actorId] ?? [];

    if (action.type === 'draw') {
      if (hasPlayable(hand, state)) return { state, events: [], error: 'error.mustPlay' };
      if (state.boneyard.length === 0) return { state, events: [], error: 'error.boneyardEmpty' };
      const boneyard = [...state.boneyard];
      const tile = boneyard.shift();
      if (!tile) return { state, events: [], error: 'error.boneyardEmpty' };
      const hands = { ...state.hands, [actorId]: [...hand, tile] };
      const events = [event('log.drewTile', { p: seat.name }, 'info', actorId)];
      const playable = canPlay(tile, state);
      if (!playable) {
        const nextState: DominoState = {
          ...state,
          boneyard,
          hands,
          turn: nextIndex(state.turn, state.seats.length),
          passes: state.passes + 1,
        };
        events.push(event('log.passed', { p: seat.name }, 'info', actorId));
        if (nextState.passes >= state.seats.length) {
          return { state: endHand({ ...nextState, boneyard: [] }, null), events };
        }
        return { state: nextState, events };
      }
      return { state: { ...state, boneyard, hands }, events };
    }

    if (action.type === 'pass') {
      if (hasPlayable(hand, state)) return { state, events: [], error: 'error.mustPlay' };
      if (state.boneyard.length > 0) return { state, events: [], error: 'error.mustDraw' };
      const nextState: DominoState = {
        ...state,
        turn: nextIndex(state.turn, state.seats.length),
        passes: state.passes + 1,
      };
      const events = [event('log.passed', { p: seat.name }, 'info', actorId)];
      if (nextState.passes >= state.seats.length) {
        return { state: endHand(nextState, null), events };
      }
      return { state: nextState, events };
    }

    if (action.type === 'play') {
      const tileId = String(action.tileId ?? '');
      const end = String(action.end ?? '') as End;
      const tile = hand.find((t) => t.id === tileId);
      if (!tile) return { state, events: [], error: 'error.invalidAction' };
      if (end !== 'left' && end !== 'right') return { state, events: [], error: 'error.chooseEnd' };
      if (!fits(tile, end === 'left' ? state.left : state.right)) {
        return { state, events: [], error: 'error.tileDoesNotFit' };
      }
      const touching = end === 'left' ? state.left : state.right;
      const other = tile.a === touching ? tile.b : tile.a;
      let placed: Tile;
      if (end === 'left') {
        placed = tile.a === touching ? { ...tile, a: tile.b, b: tile.a } : { ...tile };
      } else {
        placed = tile.b === touching ? { ...tile, a: tile.b, b: tile.a } : { ...tile };
      }
      const chain = end === 'left' ? [placed, ...state.chain] : [...state.chain, placed];
      const left = end === 'left' ? other : state.left;
      const right = end === 'right' ? other : state.right;
      const hands = { ...state.hands, [actorId]: hand.filter((t) => t.id !== tileId) };
      const events = [
        event('log.dominoPlay', { p: seat.name, tile: placed, end }, 'info', actorId),
      ];
      let nextState: DominoState = {
        ...state,
        hands,
        chain,
        left,
        right,
        passes: 0,
        lastPlay: { playerId: actorId, tile: placed, end },
      };
      if ((hands[actorId]?.length ?? 0) === 0) {
        events.push(event('log.dominoWin', { p: seat.name }, 'magic', actorId));
        nextState = endHand(nextState, actorId);
        if (nextState.phase === 'matchEnd') {
          events.push(event('log.matchEnd', { p: playerNameOf(nextState, nextState.winnerIds?.[0]) }, 'magic'));
        }
        return { state: nextState, events };
      }
      nextState = { ...nextState, turn: nextIndex(state.turn, state.seats.length) };
      return { state: nextState, events };
    }

    return { state, events: [], error: 'error.invalidAction' };
  },

  bot(state, botId, difficulty: Difficulty, rng: Rng): GameAction | null {
    if (state.phase === 'matchEnd') return null;
    const seat = state.seats[state.turn];
    if (!seat || seat.id !== botId) return null;
    const hand = state.hands[botId] ?? [];
    const playable = hand.filter((t) => canPlay(t, state));
    if (playable.length === 0) {
      return state.boneyard.length > 0 ? { type: 'draw' } : { type: 'pass' };
    }
    const options = playable.flatMap((tile) => {
      const ends: End[] = [];
      if (fits(tile, state.left)) ends.push('left');
      if (fits(tile, state.right)) ends.push('right');
      return ends.map((end) => ({ tile, end }));
    });
    const scored = options.map(({ tile, end }) => {
      const other = end === 'left'
        ? (tile.a === state.left ? tile.b : tile.a)
        : (tile.b === state.right ? tile.a : tile.b);
      const futureMatches = hand.filter((t) => t.id !== tile.id && (t.a === other || t.b === other)).length;
      const keepPoints = pips(tile);
      return { tile, end, value: keepPoints * 2 + futureMatches + (difficulty === 'hard' ? rng() * 0.5 : 0) };
    });
    scored.sort((a, b) => b.value - a.value);
    const chosen = scored[0] ?? (rng() < 0.5 ? options[0] : options[options.length - 1]);
    if (!chosen) return { type: 'pass' };
    return { type: 'play', tileId: chosen.tile.id, end: chosen.end };
  },

  result(state) {
    return state.result ?? null;
  },
};

function playerNameOf(state: DominoState, id?: string): string {
  return state.seats.find((s) => s.id === id)?.name ?? '—';
}

export type { DominoState, Tile };
