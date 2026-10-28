/** Tipos compartidos por todos los juegos de Mesa Viva. */

export type GameId =
  | 'caida'
  | 'furia'
  | 'domino'
  | 'dados'
  | 'ajedrez'
  | 'damas'
  | 'serpientes'
  | 'mancala'
  | 'go'
  | 'jungle'
  | 'ludo'
  | 'backgammon'
  | 'xiangqi';

export type Difficulty = 'easy' | 'normal' | 'hard';
export type GameCategory = 'cartas' | 'tablero' | 'dados';

export interface Seat {
  id: string;
  name: string;
  avatar: string;
  isBot: boolean;
  team?: number;
}

export interface GameMeta {
  id: GameId;
  nameKey: string;
  descKey: string;
  rulesKey: string;
  icon: string;
  category: GameCategory;
  minPlayers: number;
  maxPlayers: number;
  avgMinutes: number;
  accent: string;
  tagKeys: string[];
}

export interface GameOptions {
  target?: number;
  teams?: boolean;
  seed?: number;
  doubleTwelve?: boolean;
  boardSize?: number;
}

export interface Rng {
  (): number;
}

export interface CardView {
  id: string;
  suit: string;
  rank: number;
  action?: string;
  faceDown?: boolean;
  playable?: boolean;
  label?: string;
}

export interface TileView {
  id: string;
  a: number;
  b: number;
  playable?: boolean;
  vertical?: boolean;
}

export interface DieView {
  id: string;
  value: number;
  hidden?: boolean;
  held?: boolean;
  selected?: boolean;
}

export type Highlight = 'move' | 'capture' | 'selected' | 'last' | 'check' | 'home' | 'path' | 'dim';

export interface BoardCell {
  glyph?: string;
  label?: string;
  sub?: string;
  tone?: string;
  side?: 'r' | 'b' | 'w';
  highlight?: Highlight;
  pieceId?: string;
}

export interface BoardView {
  cols: number;
  rows: number;
  cells: BoardCell[];
  orientation?: 'white' | 'black';
}

export interface PileView {
  id: string;
  labelKey: string;
  count: number;
  top?: CardView;
}

export interface CenterView {
  cards?: CardView[];
  piles?: PileView[];
  tiles?: TileView[];
  dice?: DieView[];
  board?: BoardView;
}

export interface UIAction {
  id: string;
  labelKey: string;
  labelParams?: Record<string, string | number>;
  action: GameAction;
  tone?: 'primary' | 'secondary' | 'danger' | 'ghost';
  disabled?: boolean;
  icon?: string;
}

export type GameAction = { type: string } & Record<string, unknown>;

export interface PlayerView {
  id: string;
  name: string;
  avatar: string;
  isBot: boolean;
  team?: number;
  score: number;
  cardsLeft?: number;
  diceLeft?: number;
  active: boolean;
  isTurn: boolean;
  isYou: boolean;
}

export interface PromptView {
  key: string;
  params?: Record<string, string | number>;
  tone?: 'info' | 'warn' | 'success';
}

export interface GameView {
  gameId: GameId;
  youId: string;
  phase: string;
  turnId: string | null;
  players: PlayerView[];
  hand: CardView[];
  handTiles?: TileView[];
  handDice?: DieView[];
  center: CenterView;
  actions: UIAction[];
  prompt?: PromptView;
  extra?: Record<string, unknown>;
  log?: LogEvent[];
  result?: MatchResult | null;
  startedAt?: number;
}

export interface LogEvent {
  id: string;
  at: number;
  key: string;
  params?: Record<string, unknown>;
  tone?: 'info' | 'success' | 'warn' | 'danger' | 'magic';
  actorId?: string;
  visibility?: 'public' | 'private';
}

export interface MatchResult {
  winnerIds: string[];
  draw?: boolean;
  scores: Record<string, number>;
  reasonKey: string;
  stats?: Record<string, number>;
}

export interface XpAwardResult {
  xp: number;
  levelBefore: number;
  levelAfter: number;
  leveledUp: boolean;
}

export interface ActResult<S> {
  state: S;
  events: LogEvent[];
  error?: string;
}

export interface GameModule<S = unknown> {
  meta: GameMeta;
  create(seats: Seat[], options: GameOptions, rng: Rng): S;
  view(state: S, viewerId: string): GameView;
  act(state: S, action: GameAction, actorId: string, rng: Rng): ActResult<S>;
  bot(state: S, botId: string, difficulty: Difficulty, rng: Rng): GameAction | null;
  result(state: S): MatchResult | null;
}

export interface PlayerProfile {
  id: string;
  name: string;
  avatar: string;
  country: string;
  locale: string;
  xp: number;
  level: number;
  wins: number;
  losses: number;
  matches: number;
  streak: number;
  bestStreak: number;
  createdAt: number;
}

export interface LevelInfo {
  level: number;
  titleKey: string;
  minXp: number;
  maxXp: number;
  rewardKey: string;
  icon: string;
}
