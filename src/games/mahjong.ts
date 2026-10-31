import type {
  ActResult,
  CardView,
  Difficulty,
  GameAction,
  GameModule,
  GameOptions,
  GameView,
  LogEvent,
  MatchResult,
  Seat,
  UIAction,
} from '../core/types';
import type { Rng } from '../core/types';
import { event, playerViews, shuffleWithRng } from './cards';
import { makeAction, prompt } from './helpers';

export type MahjongVariant = 'american' | 'riichi' | 'cards';
type Phase = 'play' | 'handEnd' | 'matchEnd';
type ClaimKind = 'ron' | 'pon' | 'chi' | 'kan';
type MeldKind = 'chi' | 'pon' | 'kan';

export const MAHJONG_HANDS = 4;

interface Meld {
  kind: MeldKind;
  tiles: CardView[];
  from: string;
}

interface Claim {
  kind: ClaimKind;
  seatId: string;
  tiles: CardView[];
}

interface PlayerState {
  seat: Seat;
  hand: CardView[];
  melds: Meld[];
  discards: CardView[];
  flowers: CardView[];
  riichi: boolean;
  score: number;
}

export interface MahjongState {
  variant: MahjongVariant;
  seats: Seat[];
  players: Record<string, PlayerState>;
  wall: CardView[];
  turn: number;
  dealer: number;
  round: number;
  phase: Phase;
  drawn?: CardView;
  lastDiscard?: CardView;
  lastDiscardBy?: string;
  pending: Claim[];
  winnerId?: string;
  winnerTile?: CardView;
  winLabel?: string;
  result?: MatchResult;
  log: LogEvent[];
  rng: Rng;
}

const VARIANT_LABEL: Record<MahjongVariant, string> = {
  american: 'Mahjong americano',
  riichi: 'Riichi Mahjong',
  cards: 'Mahjong con naipes',
};

const SUIT_ORDER: Record<string, number> = {
  dots: 0,
  bams: 1,
  cracks: 2,
  wind: 3,
  dragon: 4,
  flower: 5,
  joker: 6,
};

function isFlower(tile: CardView): boolean {
  return tile.suit === 'flower';
}

function isJoker(tile: CardView): boolean {
  return tile.suit === 'joker';
}

function isHonor(tile: CardView): boolean {
  return tile.suit === 'wind' || tile.suit === 'dragon';
}

function isSuited(tile: CardView): boolean {
  return tile.suit === 'dots' || tile.suit === 'bams' || tile.suit === 'cracks';
}

function isTerminal(tile: CardView): boolean {
  return isSuited(tile) && (tile.rank === 1 || tile.rank === 9);
}

function sameTile(a: CardView, b: CardView): boolean {
  return a.suit === b.suit && a.rank === b.rank;
}

function sortTiles(tiles: CardView[]): CardView[] {
  return tiles.slice().sort((a, b) => {
    const sa = SUIT_ORDER[a.suit] ?? 99;
    const sb = SUIT_ORDER[b.suit] ?? 99;
    if (sa !== sb) return sa - sb;
    return a.rank - b.rank;
  });
}

function tile(id: string, suit: string, rank: number): CardView {
  return { id, suit, rank };
}

function buildDeck(variant: MahjongVariant): CardView[] {
  const deck: CardView[] = [];
  let id = 0;
  for (const suit of ['dots', 'bams', 'cracks']) {
    for (let rank = 1; rank <= 9; rank += 1) {
      for (let copy = 0; copy < 4; copy += 1) deck.push(tile(`t${id++}`, suit, rank));
    }
  }
  for (let rank = 1; rank <= 4; rank += 1) {
    for (let copy = 0; copy < 4; copy += 1) deck.push(tile(`t${id++}`, 'wind', rank));
  }
  for (let rank = 1; rank <= 3; rank += 1) {
    for (let copy = 0; copy < 4; copy += 1) deck.push(tile(`t${id++}`, 'dragon', rank));
  }
  if (variant !== 'riichi') {
    for (let rank = 1; rank <= 8; rank += 1) deck.push(tile(`t${id++}`, 'flower', rank));
    for (let rank = 1; rank <= 8; rank += 1) deck.push(tile(`t${id++}`, 'joker', rank));
  }
  return deck;
}

function standardTypes(): CardView[] {
  const types: CardView[] = [];
  let id = 0;
  for (const suit of ['dots', 'bams', 'cracks']) {
    for (let rank = 1; rank <= 9; rank += 1) types.push(tile(`type${id++}`, suit, rank));
  }
  for (let rank = 1; rank <= 4; rank += 1) types.push(tile(`type${id++}`, 'wind', rank));
  for (let rank = 1; rank <= 3; rank += 1) types.push(tile(`type${id++}`, 'dragon', rank));
  return types;
}

const STANDARD_TYPES = standardTypes();

function seatIndex(state: MahjongState, seatId: string): number {
  return state.seats.findIndex((seat) => seat.id === seatId);
}

function currentSeat(state: MahjongState): Seat | undefined {
  return state.seats[state.turn];
}

function playerOf(state: MahjongState, seatId: string): PlayerState | undefined {
  return state.players[seatId];
}

function drawOne(state: MahjongState, seatId: string): MahjongState {
  let wall = state.wall.slice();
  let player = playerOf(state, seatId);
  if (!player) return state;
  let drawn: CardView | undefined;
  while (wall.length > 0) {
    const next = wall.shift();
    if (!next) break;
    if (isFlower(next)) {
      player = { ...player, flowers: [...player.flowers, next] };
      continue;
    }
    drawn = next;
    break;
  }
  const players = { ...state.players, [seatId]: player };
  if (!drawn) {
    return {
      ...state,
      players,
      wall,
      phase: 'handEnd',
      winnerId: undefined,
      winLabel: 'empate',
      log: [...state.log, event('log.mahjongDraw', {}, 'warn')],
    };
  }
  players[seatId] = { ...player, hand: [...player.hand, drawn] };
  return { ...state, players, wall, drawn };
}

function advance(state: MahjongState): MahjongState {
  const next = (state.turn + 1) % MAHJONG_HANDS;
  const rotated: MahjongState = {
    ...state,
    turn: next,
    drawn: undefined,
    lastDiscard: undefined,
    lastDiscardBy: undefined,
    pending: [],
  };
  const seat = rotated.seats[next];
  return seat ? drawOne(rotated, seat.id) : rotated;
}

function advanceFrom(state: MahjongState, fromIndex: number): MahjongState {
  const next = (fromIndex + 1) % MAHJONG_HANDS;
  const rotated: MahjongState = {
    ...state,
    turn: next,
    drawn: undefined,
    lastDiscard: undefined,
    lastDiscardBy: undefined,
    pending: [],
  };
  const seat = rotated.seats[next];
  return seat ? drawOne(rotated, seat.id) : rotated;
}

function removeIndices(tiles: CardView[], indices: number[]): CardView[] {
  const remove = new Set(indices);
  return tiles.filter((_, index) => !remove.has(index));
}

function formSets(tiles: CardView[], need: number, sequences: boolean, jokers: boolean): boolean {
  if (need === 0) return tiles.length === 0;
  if (tiles.length === 0) return false;
  const first = tiles[0];
  if (!first) return false;
  if (isJoker(first)) {
    for (let i = 1; i < tiles.length; i += 1) {
      for (let j = i + 1; j < tiles.length; j += 1) {
        const a = tiles[i];
        const b = tiles[j];
        if (!a || !b) continue;
        if (sameTile(a, b) || (isJoker(a) && isJoker(b))) {
          if (formSets(removeIndices(tiles, [0, i, j]), need - 1, sequences, jokers)) return true;
        }
      }
    }
    if (sequences) {
      for (let i = 1; i < tiles.length; i += 1) {
        for (let j = i + 1; j < tiles.length; j += 1) {
          const a = tiles[i];
          const b = tiles[j];
          if (!a || !b) continue;
          if (isSuited(a) && a.suit === b.suit && !isJoker(b) && Math.abs(a.rank - b.rank) <= 2 && a.rank !== b.rank) {
            if (formSets(removeIndices(tiles, [0, i, j]), need - 1, sequences, jokers)) return true;
          }
        }
      }
    }
    return false;
  }

  const same = tiles
    .map((value, index) => ({ value, index }))
    .filter(({ value }) => sameTile(value, first))
    .map(({ index }) => index);
  if (same.length >= 3) {
    if (formSets(removeIndices(tiles, same.slice(0, 3)), need - 1, sequences, jokers)) return true;
  }
  if (jokers) {
    const jokerIndices = tiles
      .map((value, index) => ({ value, index }))
      .filter(({ value }) => isJoker(value))
      .map(({ index }) => index);
    if (same.length >= 2 && jokerIndices.length >= 1) {
      if (formSets(removeIndices(tiles, [...same.slice(0, 2), jokerIndices[0] ?? 0]), need - 1, sequences, jokers)) return true;
    }
    if (same.length >= 1 && jokerIndices.length >= 2) {
      if (formSets(removeIndices(tiles, [same[0] ?? 0, ...jokerIndices.slice(0, 2)]), need - 1, sequences, jokers)) return true;
    }
  }
  if (sequences && isSuited(first)) {
    const second = tiles.findIndex((tileValue) => tileValue.suit === first.suit && tileValue.rank === first.rank + 1 && !isJoker(tileValue));
    const third = tiles.findIndex((tileValue) => tileValue.suit === first.suit && tileValue.rank === first.rank + 2 && !isJoker(tileValue));
    if (second >= 0 && third >= 0) {
      if (formSets(removeIndices(tiles, [0, second, third]), need - 1, sequences, jokers)) return true;
    }
    if (jokers) {
      const jokerIndex = tiles.findIndex((tileValue) => isJoker(tileValue));
      if (jokerIndex > 0 && second >= 0) {
        if (formSets(removeIndices(tiles, [0, second, jokerIndex]), need - 1, sequences, jokers)) return true;
      }
      if (jokerIndex > 0 && third >= 0) {
        if (formSets(removeIndices(tiles, [0, third, jokerIndex]), need - 1, sequences, jokers)) return true;
      }
    }
  }
  return false;
}

function isSevenPairs(tiles: CardView[]): boolean {
  if (tiles.length !== 14) return false;
  const counts = new Map<string, number>();
  for (const value of tiles) {
    const key = `${value.suit}-${value.rank}`;
    counts.set(key, (counts.get(key) ?? 0) + 1);
  }
  return counts.size === 7 && [...counts.values()].every((count) => count === 2);
}

export function canWinHand(hand: CardView[], melds: Meld[], variant: MahjongVariant): boolean {
  const concealed = hand.filter((value) => !isFlower(value));
  const needs = 4 - melds.length;
  if (needs < 0) return false;
  if (concealed.length !== needs * 3 + 2) return false;
  const allowJokers = variant !== 'riichi';
  const allowSequences = variant === 'riichi';
  if (!allowJokers && concealed.some((value) => isJoker(value))) return false;
  if (variant === 'riichi' && melds.length === 0 && isSevenPairs(concealed)) return true;
  const sorted = sortTiles(concealed);
  for (let i = 0; i < sorted.length; i += 1) {
    for (let j = i + 1; j < sorted.length; j += 1) {
      const a = sorted[i];
      const b = sorted[j];
      if (!a || !b) continue;
      const wildPair = allowJokers && (isJoker(a) || isJoker(b));
      if (!sameTile(a, b) && !wildPair) continue;
      if (formSets(removeIndices(sorted, [i, j]), needs, allowSequences, allowJokers)) return true;
    }
  }
  return false;
}

function isTenpai(hand: CardView[], melds: Meld[], variant: MahjongVariant): boolean {
  return STANDARD_TYPES.some((value) => canWinHand([...hand, tile('probe', value.suit, value.rank)], melds, variant));
}

function isFuriten(player: PlayerState, variant: MahjongVariant): boolean {
  return player.discards.some((discard) => canWinHand([...player.hand, discard], player.melds, variant));
}

function yakuFor(player: PlayerState, winTile: CardView, tsumo: boolean, state: MahjongState): string[] {
  const yaku: string[] = [];
  const hasWinTile = player.hand.some((value) => value.id === winTile.id);
  const concealed = hasWinTile ? player.hand : [...player.hand, winTile];
  const all = [...concealed, ...player.melds.flatMap((meld) => meld.tiles)];
  if (player.riichi) yaku.push('riichi');
  if (tsumo) yaku.push('tsumo');
  if (player.melds.length === 0 && isSevenPairs(concealed)) yaku.push('chiitoitsu');
  if (all.length > 0 && all.every((value) => !isTerminal(value) && !isHonor(value))) yaku.push('tanyao');
  const suits = new Set(all.filter(isSuited).map((value) => value.suit));
  if (suits.size === 1 && all.every((value) => isSuited(value))) yaku.push('chinitsu');
  else if (suits.size === 1 && all.some(isHonor)) yaku.push('honitsu');
  if (player.melds.length > 0 && player.melds.every((meld) => meld.kind !== 'chi')) yaku.push('toitoi');
  const groups = new Map<string, number>();
  for (const value of all) {
    const key = `${value.suit}-${value.rank}`;
    groups.set(key, (groups.get(key) ?? 0) + 1);
  }
  if ([...groups.entries()].some(([key, count]) => key.startsWith('dragon-') && count >= 3)) yaku.push('yakuhai');
  const seatWind = player.seat.id === state.seats[state.dealer]?.id ? 1 : ((state.seats.findIndex((seat) => seat.id === player.seat.id) - state.dealer + 4) % 4) + 1;
  if ([...groups.entries()].some(([key, count]) => key === `wind-${seatWind}` && count >= 3)) yaku.push('viento');
  return [...new Set(yaku)];
}

function claimsFor(state: MahjongState, fromId: string, discard: CardView): Claim[] {
  const claims: Claim[] = [];
  for (const seat of state.seats) {
    if (seat.id === fromId) continue;
    const player = playerOf(state, seat.id);
    if (!player) continue;
    const winHand = [...player.hand, discard];
    const yakuOk = state.variant !== 'riichi' || yakuFor(player, discard, false, state).length > 0;
    if (canWinHand(winHand, player.melds, state.variant) && yakuOk && !isFuriten(player, state.variant)) {
      claims.push({ kind: 'ron', seatId: seat.id, tiles: [discard] });
    }
    const copies = player.hand.filter((value) => sameTile(value, discard));
    if (!player.riichi) {
      if (copies.length >= 3) claims.push({ kind: 'kan', seatId: seat.id, tiles: copies.slice(0, 3) });
      if (copies.length >= 2) claims.push({ kind: 'pon', seatId: seat.id, tiles: copies.slice(0, 2) });
    }
    if (state.variant === 'riichi' && !player.riichi && isSuited(discard)) {
      const nextSeat = state.seats[(state.turn + 1) % MAHJONG_HANDS];
      if (nextSeat?.id === seat.id) {
        const lower = player.hand.find((value) => value.suit === discard.suit && value.rank === discard.rank - 1);
        const lower2 = player.hand.find((value) => value.suit === discard.suit && value.rank === discard.rank - 2);
        const upper = player.hand.find((value) => value.suit === discard.suit && value.rank === discard.rank + 1);
        const upper2 = player.hand.find((value) => value.suit === discard.suit && value.rank === discard.rank + 2);
        if (lower && lower2) claims.push({ kind: 'chi', seatId: seat.id, tiles: [lower, lower2] });
        if (lower && upper) claims.push({ kind: 'chi', seatId: seat.id, tiles: [lower, upper] });
        if (upper && upper2) claims.push({ kind: 'chi', seatId: seat.id, tiles: [upper, upper2] });
      }
    }
  }
  const priority: Record<ClaimKind, number> = { ron: 0, kan: 1, pon: 2, chi: 3 };
  return claims.sort((a, b) => priority[a.kind] - priority[b.kind]);
}

function applyClaim(state: MahjongState, claim: Claim): MahjongState {
  const discard = state.lastDiscard;
  const fromId = state.lastDiscardBy;
  const player = playerOf(state, claim.seatId);
  if (!discard || !fromId || !player) return state;
  if (claim.kind === 'ron') return settle(state, claim.seatId, discard, false);
  const usedIds = new Set(claim.tiles.map((value) => value.id));
  const kind: MeldKind = claim.kind === 'chi' ? 'chi' : claim.kind === 'kan' ? 'kan' : 'pon';
  const meld: Meld = { kind, tiles: [...claim.tiles, discard], from: fromId };
  const players = { ...state.players };
  players[claim.seatId] = {
    ...player,
    hand: player.hand.filter((value) => !usedIds.has(value.id)),
    melds: [...player.melds, meld],
  };
  const discarder = players[fromId];
  if (discarder) {
    players[fromId] = { ...discarder, discards: discarder.discards.filter((value) => value.id !== discard.id) };
  }
  const claimed: MahjongState = {
    ...state,
    players,
    turn: seatIndex(state, claim.seatId),
    drawn: undefined,
    pending: [],
    lastDiscard: undefined,
    lastDiscardBy: undefined,
    log: [...state.log, event('log.mahjongClaim', { p: player.seat.name, kind, tile: discard }, 'success', claim.seatId)],
  };
  if (claim.kind === 'kan') return drawOne(claimed, claim.seatId);
  return claimed;
}

function settle(state: MahjongState, winnerId: string, winTile: CardView, tsumo: boolean): MahjongState {
  const winner = playerOf(state, winnerId);
  if (!winner) return state;
  const yaku = state.variant === 'riichi' ? yakuFor(winner, winTile, tsumo, state) : ['mano estándar'];
  let han = yaku.length + (tsumo ? 1 : 0) + (winner.riichi ? 1 : 0);
  const bonus = winner.flowers.length * 2;
  const points = state.variant === 'riichi'
    ? 8 * (1 << Math.min(han, 6)) + bonus
    : 40 + han * 10 + bonus;
  const payment = Math.max(1, points);
  const players = { ...state.players };
  for (const seat of state.seats) {
    const current = players[seat.id];
    if (!current) continue;
    players[seat.id] = {
      ...current,
      score: current.score + (seat.id === winnerId ? payment * (MAHJONG_HANDS - 1) : -payment),
    };
  }
  const label = yaku.length > 0 ? yaku.join(' · ') : 'mano estándar';
  return {
    ...state,
    players,
    phase: 'handEnd',
    winnerId,
    winnerTile: winTile,
    winLabel: label,
    pending: [],
    lastDiscard: undefined,
    log: [...state.log, event('log.mahjongWin', { p: winner.seat.name, tile: winTile, yaku: label, points: payment * 3 }, 'success', winnerId)],
  };
}

function newHand(state: MahjongState, dealer: number, round: number): MahjongState {
  const deck = shuffleWithRng(buildDeck(state.variant), state.rng);
  const wall = deck.slice();
  const players: Record<string, PlayerState> = {};
  for (const seat of state.seats) {
    const previous = state.players[seat.id];
    players[seat.id] = {
      seat,
      hand: [],
      melds: [],
      discards: [],
      flowers: [],
      riichi: false,
      score: previous?.score ?? 250,
    };
  }
  let next: MahjongState = {
    ...state,
    players,
    wall,
    turn: dealer,
    dealer,
    round,
    phase: 'play',
    drawn: undefined,
    lastDiscard: undefined,
    lastDiscardBy: undefined,
    pending: [],
    winnerId: undefined,
    winnerTile: undefined,
    winLabel: undefined,
    result: undefined,
    log: [...state.log, event('log.mahjongDeal', { n: round, dealer: state.seats[dealer]?.name ?? '—' }, 'info')],
  };
  for (const seat of state.seats) {
    for (let i = 0; i < 13; i += 1) {
      next = drawOneSilent(next, seat.id);
    }
  }
  const dealerSeat = state.seats[dealer];
  if (dealerSeat) next = drawOne(next, dealerSeat.id);
  return next;
}

function drawOneSilent(state: MahjongState, seatId: string): MahjongState {
  const wall = state.wall.slice();
  const player = playerOf(state, seatId);
  if (!player) return state;
  let drawn: CardView | undefined;
  let flowers = player.flowers;
  while (wall.length > 0) {
    const next = wall.shift();
    if (!next) break;
    if (isFlower(next)) {
      flowers = [...flowers, next];
      continue;
    }
    drawn = next;
    break;
  }
  if (!drawn) return state;
  return {
    ...state,
    wall,
    players: {
      ...state.players,
      [seatId]: { ...player, hand: [...player.hand, drawn], flowers },
    },
  };
}

function matchResult(state: MahjongState): MatchResult {
  const scores: Record<string, number> = {};
  for (const seat of state.seats) scores[seat.id] = state.players[seat.id]?.score ?? 0;
  const best = Math.max(...Object.values(scores));
  const winnerIds = state.seats.filter((seat) => scores[seat.id] === best).map((seat) => seat.id);
  return {
    winnerIds,
    draw: winnerIds.length > 1,
    scores,
    reasonKey: 'result.reason.mahjongScore',
  };
}

function nextHand(state: MahjongState): MahjongState {
  if (state.phase !== 'handEnd') return state;
  if (state.round >= MAHJONG_HANDS) {
    const result = matchResult(state);
    return {
      ...state,
      phase: 'matchEnd',
      result,
      winnerId: result.winnerIds[0],
      log: [...state.log, event('log.mahjongMatch', { p: state.seats.find((seat) => seat.id === result.winnerIds[0])?.name ?? '—' }, 'success')],
    };
  }
  const dealerKeeps = state.winnerId === state.seats[state.dealer]?.id;
  const dealer = dealerKeeps ? state.dealer : (state.dealer + 1) % MAHJONG_HANDS;
  return newHand(state, dealer, state.round + 1);
}

function createState(variant: MahjongVariant, seats: Seat[], options: GameOptions, rng: Rng): MahjongState {
  const base: MahjongState = {
    variant,
    seats: seats.slice(0, MAHJONG_HANDS),
    players: {},
    wall: [],
    turn: 0,
    dealer: 0,
    round: 1,
    phase: 'play',
    pending: [],
    log: [],
    rng,
  };
  void options;
  return newHand(base, 0, 1);
}

function claimLabel(kind: ClaimKind): string {
  return kind === 'ron' ? 'mahjong.ron' : kind === 'pon' ? 'mahjong.pon' : kind === 'kan' ? 'mahjong.kan' : 'mahjong.chi';
}

function mahjongView(state: MahjongState, viewerId: string, id: 'mahjong' | 'riichi' | 'mahjong-cards'): GameView {
  const viewer = playerOf(state, viewerId);
  const turnSeat = currentSeat(state);
  const isTurn = turnSeat?.id === viewerId && state.phase === 'play' && state.pending.length === 0;
  const myClaims = state.pending.filter((claim) => claim.seatId === viewerId);
  const canTsumo = Boolean(isTurn && state.drawn && viewer && canWinHand(viewer.hand, viewer.melds, state.variant));
  const canRiichi = Boolean(
    isTurn
      && state.variant === 'riichi'
      && viewer
      && viewer.melds.length === 0
      && !viewer.riichi
      && viewer.score >= 10
      && isTenpai(viewer.hand.slice(0, -1), viewer.melds, state.variant),
  );
  const actions: UIAction[] = [];
  if (state.phase === 'handEnd') {
    actions.push(makeAction('nextHand', 'mahjong.nextHand', { type: 'nextHand' }, { tone: 'primary' }));
  }
  if (myClaims.length > 0) {
    for (const claim of myClaims) {
      actions.push(makeAction(`claim-${claim.kind}`, claimLabel(claim.kind), { type: 'claim', kind: claim.kind, seatId: viewerId }, {
        tone: claim.kind === 'ron' ? 'primary' : 'secondary',
      }));
    }
    actions.push(makeAction('pass', 'mahjong.pass', { type: 'pass' }, { tone: 'ghost' }));
  }
  if (canTsumo) actions.push(makeAction('tsumo', 'mahjong.tsumo', { type: 'tsumo' }, { tone: 'primary', icon: 'sparkles' }));
  const promptView = state.phase === 'matchEnd'
    ? prompt('mahjong.matchOver', {}, 'success')
    : state.phase === 'handEnd'
      ? prompt('mahjong.handOver', {}, 'success')
      : myClaims.length > 0
        ? prompt('mahjong.claimPrompt', {}, 'warn')
        : isTurn
          ? prompt('mahjong.yourTurn', {}, 'info')
          : prompt('mahjong.waiting', { p: turnSeat?.name ?? '—' }, 'info');
  const players = playerViews(state.seats, Object.fromEntries(state.seats.map((seat) => [seat.id, state.players[seat.id]?.score ?? 0])), turnSeat?.id ?? null, viewerId, (seat) => ({
    cardsLeft: playerOf(state, seat.id)?.hand.length ?? 0,
    active: state.phase !== 'matchEnd',
  }));
  return {
    gameId: id,
    youId: viewerId,
    phase: state.phase,
    turnId: turnSeat?.id ?? null,
    players,
    hand: viewer ? sortTiles(viewer.hand).map((value) => ({ ...value, playable: isTurn })) : [],
    center: {
      cards: state.lastDiscard ? [state.lastDiscard] : [],
      piles: [{ id: 'wall', labelKey: 'mahjong.wall', count: state.wall.length }],
    },
    actions,
    prompt: promptView,
    extra: {
      variant: state.variant,
      variantLabel: VARIANT_LABEL[state.variant],
      round: state.round,
      dealerId: state.seats[state.dealer]?.id ?? '',
      wallCount: state.wall.length,
      lastDiscard: state.lastDiscard ?? null,
      lastDiscardBy: state.lastDiscardBy ?? null,
      canTsumo,
      canRiichi,
      drawn: state.drawn ?? null,
      players: state.seats.map((seat) => {
        const player = playerOf(state, seat.id);
        return {
          id: seat.id,
          name: seat.name,
          score: player?.score ?? 0,
          handCount: player?.hand.length ?? 0,
          melds: player?.melds ?? [],
          discards: player?.discards ?? [],
          flowers: player?.flowers ?? [],
          riichi: player?.riichi ?? false,
        };
      }),
    },
  };
}

function mahjongAct(state: MahjongState, action: GameAction, actorId: string): ActResult<MahjongState> {
  if (state.phase === 'matchEnd') return { state, events: [], error: 'error.matchOver' };
  const before = state.log.length;
  if (action.type === 'nextHand') {
    if (state.phase !== 'handEnd') return { state, events: [], error: 'error.invalidAction' };
    const next = nextHand(state);
    return { state: next, events: next.log.slice(before) };
  }
  const actor = playerOf(state, actorId);
  if (!actor) return { state, events: [], error: 'error.notYourTurn' };

  if (action.type === 'claim') {
    const kind = String(action.kind ?? '') as ClaimKind;
    const claim = state.pending.find((value) => value.seatId === actorId && value.kind === kind);
    if (!claim) return { state, events: [], error: 'error.invalidAction' };
    const next = applyClaim(state, claim);
    return { state: next, events: next.log.slice(before) };
  }

  if (action.type === 'pass') {
    if (state.pending.length === 0) return { state, events: [], error: 'error.invalidAction' };
    const remaining = state.pending.filter((claim) => claim.seatId !== actorId);
    if (remaining.length > 0) {
      const nextClaim = remaining[0];
      const nextSeat = nextClaim ? state.seats.find((seat) => seat.id === nextClaim.seatId) : undefined;
      if (nextClaim && nextSeat?.isBot) {
        const next = applyClaim(state, nextClaim);
        return { state: next, events: next.log.slice(before) };
      }
      if (nextClaim) {
        return {
          state: {
            ...state,
            pending: remaining,
            turn: seatIndex(state, nextClaim.seatId),
            log: [...state.log, event('log.mahjongPass', { p: actor.seat.name }, 'info', actorId)],
          },
          events: [event('log.mahjongPass', { p: actor.seat.name }, 'info', actorId)],
        };
      }
    }
    const fromIndex = state.lastDiscardBy ? seatIndex(state, state.lastDiscardBy) : state.turn;
    const next = advanceFrom({ ...state, pending: [], lastDiscard: undefined }, fromIndex);
    return { state: next, events: next.log.slice(before) };
  }

  if (state.pending.length > 0) return { state, events: [], error: 'error.notYourTurn' };
  const turnSeat = currentSeat(state);
  if (!turnSeat || turnSeat.id !== actorId) return { state, events: [], error: 'error.notYourTurn' };

  if (action.type === 'tsumo') {
    if (!state.drawn) return { state, events: [], error: 'error.invalidAction' };
    if (!canWinHand(actor.hand, actor.melds, state.variant)) return { state, events: [], error: 'error.invalidAction' };
    if (state.variant === 'riichi' && yakuFor(actor, state.drawn, true, state).length === 0) {
      return { state, events: [], error: 'error.invalidAction' };
    }
    const next = settle(state, actorId, state.drawn, true);
    return { state: next, events: next.log.slice(before) };
  }

  if (action.type === 'riichi') {
    if (state.variant !== 'riichi' || actor.riichi || actor.melds.length > 0) return { state, events: [], error: 'error.invalidAction' };
    const tileId = String(action.tileId ?? '');
    const tileValue = actor.hand.find((value) => value.id === tileId);
    if (!tileValue) return { state, events: [], error: 'error.invalidAction' };
    const rest = actor.hand.filter((value) => value.id !== tileId);
    if (!isTenpai(rest, actor.melds, state.variant)) return { state, events: [], error: 'error.invalidAction' };
    const players = { ...state.players, [actorId]: { ...actor, riichi: true, score: actor.score - 10 } };
    return mahjongAct({ ...state, players }, { type: 'discard', tileId }, actorId);
  }

  if (action.type === 'discard') {
    const tileId = String(action.tileId ?? '');
    const tileValue = actor.hand.find((value) => value.id === tileId);
    if (!tileValue) return { state, events: [], error: 'error.invalidAction' };
    const players = {
      ...state.players,
      [actorId]: { ...actor, hand: actor.hand.filter((value) => value.id !== tileId), discards: [...actor.discards, tileValue] },
    };
    const base: MahjongState = {
      ...state,
      players,
      drawn: undefined,
      lastDiscard: tileValue,
      lastDiscardBy: actorId,
      log: [...state.log, event('log.mahjongDiscard', { p: actor.seat.name, tile: tileValue }, 'info', actorId)],
    };
    const claims = claimsFor(base, actorId, tileValue);
    const ron = claims.find((claim) => claim.kind === 'ron');
    if (ron) {
      const next = settle(base, ron.seatId, tileValue, false);
      return { state: next, events: next.log.slice(before) };
    }
    if (claims.length === 0) {
      const next = advance(base);
      return { state: next, events: base.log.slice(before) };
    }
    const top = claims[0];
    if (top) {
      const topSeat = state.seats.find((seat) => seat.id === top.seatId);
      if (topSeat?.isBot) {
        const next = applyClaim({ ...base, pending: claims }, top);
        return { state: next, events: next.log.slice(before) };
      }
      return {
        state: {
          ...base,
          pending: claims,
          turn: seatIndex(state, top.seatId),
        },
        events: base.log.slice(before),
      };
    }
    const next = advance(base);
    return { state: next, events: base.log.slice(before) };
  }

  return { state, events: [], error: 'error.invalidAction' };
}

function mahjongBot(state: MahjongState, botId: string): GameAction | null {
  if (state.phase !== 'play') return null;
  const claim = state.pending.find((value) => value.seatId === botId);
  if (claim) return { type: 'claim', kind: claim.kind, seatId: botId };
  const turnSeat = currentSeat(state);
  if (!turnSeat || turnSeat.id !== botId) return null;
  const player = playerOf(state, botId);
  if (!player) return null;
  if (state.drawn && canWinHand(player.hand, player.melds, state.variant)) return { type: 'tsumo' };
  const scored = player.hand.map((value) => {
    const copies = player.hand.filter((other) => sameTile(other, value)).length;
    const neighbors = player.hand.filter((other) => other.suit === value.suit && Math.abs(other.rank - value.rank) >= 1 && Math.abs(other.rank - value.rank) <= 2).length;
    const honors = isHonor(value) ? 1 : 0;
    const terminal = isTerminal(value) ? 0 : 1;
    return { id: value.id, score: copies * 4 + neighbors * 2 + honors + terminal };
  });
  const best = scored.slice().sort((a, b) => a.score - b.score)[0];
  return best ? { type: 'discard', tileId: best.id } : null;
}

function makeMahjongModule(
  variant: MahjongVariant,
  id: 'mahjong' | 'riichi' | 'mahjong-cards',
  nameKey: string,
  descKey: string,
  rulesKey: string,
  accent: string,
): GameModule<MahjongState> {
  return {
    meta: {
      id,
      nameKey,
      descKey,
      rulesKey,
      icon: '🀄',
      category: variant === 'cards' ? 'cartas' : 'tablero',
      minPlayers: 4,
      maxPlayers: 4,
      avgMinutes: 35,
      accent,
      tagKeys: ['tag.estrategia', 'tag.cultural'],
    },
    create(seats, options, rng): MahjongState {
      return createState(variant, seats, options, rng);
    },
    view(state, viewerId): GameView {
      return mahjongView(state, viewerId, id);
    },
    act(state, action, actorId): ActResult<MahjongState> {
      return mahjongAct(state, action, actorId);
    },
    bot(state, botId, _difficulty: Difficulty): GameAction | null {
      return mahjongBot(state, botId);
    },
    result(state): MatchResult | null {
      return state.result ?? null;
    },
  };
}

export const mahjongModule = makeMahjongModule('american', 'mahjong', 'game.mahjong.name', 'game.mahjong.desc', 'game.mahjong.rules', '#0f8b6c');
export const riichiModule = makeMahjongModule('riichi', 'riichi', 'game.riichi.name', 'game.riichi.desc', 'game.riichi.rules', '#b23a48');
export const mahjongCardsModule = makeMahjongModule('cards', 'mahjong-cards', 'game.mahjongCards.name', 'game.mahjongCards.desc', 'game.mahjongCards.rules', '#2f6fdb');
