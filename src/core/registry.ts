import type { GameId, GameMeta, GameModule } from '../core/types';
import { caidaModule } from '../games/caida';
import { furiaModule } from '../games/furia';
import { dominoModule } from '../games/domino';
import { dadosModule } from '../games/dados';
import { ajedrezModule } from '../games/ajedrez';
import { damasModule } from '../games/damas';
import { serpientesModule } from '../games/serpientes';
import { mancalaModule } from '../games/mancala';
import { goModule } from '../games/go';
import { jungleModule } from '../games/jungle';
import { ludoModule } from '../games/ludo';
import { backgammonModule } from '../games/backgammon';
import { xiangqiModule } from '../games/xiangqi';
import { chinoModule } from '../games/chino';
import { pokerModule } from '../games/poker';

export const GAME_MODULES: Record<GameId, GameModule<unknown>> = {
  caida: caidaModule as unknown as GameModule<unknown>,
  furia: furiaModule as unknown as GameModule<unknown>,
  domino: dominoModule as unknown as GameModule<unknown>,
  dados: dadosModule as unknown as GameModule<unknown>,
  ajedrez: ajedrezModule as unknown as GameModule<unknown>,
  damas: damasModule as unknown as GameModule<unknown>,
  serpientes: serpientesModule as unknown as GameModule<unknown>,
  mancala: mancalaModule as unknown as GameModule<unknown>,
  go: goModule as unknown as GameModule<unknown>,
  jungle: jungleModule as unknown as GameModule<unknown>,
  ludo: ludoModule as unknown as GameModule<unknown>,
  backgammon: backgammonModule as unknown as GameModule<unknown>,
  xiangqi: xiangqiModule as unknown as GameModule<unknown>,
  chino: chinoModule as unknown as GameModule<unknown>,
  poker: pokerModule as unknown as GameModule<unknown>,
};

export const GAMES: GameMeta[] = Object.values(GAME_MODULES).map((m) => m.meta);

export const GAME_BY_ID = Object.fromEntries(GAMES.map((g) => [g.id, g])) as Record<GameId, GameMeta>;

export function getGame(id: string): GameModule<unknown> | null {
  return (GAME_MODULES as Record<string, GameModule<unknown>>)[id] ?? null;
}

export interface CatalogEntry {
  id: string;
  name: string;
  category: 'cartas' | 'tablero' | 'dados' | 'herramienta';
  status: 'playable' | 'soon' | 'research';
  players: string;
  url?: string;
  note?: string;
  emoji: string;
}

/**
 * Catálogo completo de la visión del proyecto. Los juegos con estado
 * "playable" ya tienen motor e interfaz; "soon" están priorizados en el
 * roadmap y "research" requieren un motor especializado (mahjong, shogi...).
 */
export const CATALOG: CatalogEntry[] = [
  { id: 'caida', name: 'Caída', category: 'cartas', status: 'playable', players: '2–4', emoji: '🃏', url: 'https://steemit.com/spanish/@joriangel/juego-de-cartas-caida-explicado-venezuela', note: 'Reglas venezolanas de cantos y caídas.' },
  { id: 'furia', name: 'Furia de Colores', category: 'cartas', status: 'playable', players: '2–4', emoji: '🎨', note: 'Nuestro homenaje original a los juegos de combinar colores.' },
  { id: 'domino', name: 'Dominó', category: 'tablero', status: 'playable', players: '2–4', emoji: '🁫', url: 'https://www.ymimports.com/pages/double-12-dominoes', note: 'Doble 6 y doble 12, parejas y tranca.' },
  { id: 'dados', name: 'Dados Mentirosos', category: 'dados', status: 'playable', players: '2–6', emoji: '🎲', url: 'https://www.ymimports.com/pages/how-to-play-liars-dice' },
  { id: 'ajedrez', name: 'Ajedrez', category: 'tablero', status: 'playable', players: '2', emoji: '♞', url: 'https://www.ymimports.com/pages/how-to-play-chess' },
  { id: 'damas', name: 'Damas', category: 'tablero', status: 'playable', players: '2', emoji: '⚫', url: 'https://www.ymimports.com/pages/how-to-play-checkers-draughts' },
  { id: 'serpientes', name: 'Serpientes y Escaleras', category: 'tablero', status: 'playable', players: '2–4', emoji: '🪜', url: 'https://www.ymimports.com/pages/how-to-play-snakes-and-ladders' },
  { id: 'mancala', name: 'Mancala', category: 'tablero', status: 'playable', players: '2', emoji: '🫘', url: 'https://www.ymimports.com/pages/how-to-play-mancala' },
  { id: 'ludo', name: 'Ludo', category: 'tablero', status: 'playable', players: '2–4', emoji: '🎲', url: 'https://www.ymimports.com/pages/how-to-play-ludo', note: 'Cuatro fichas, capturas, casillas seguras y meta exacta.' },
  { id: 'backgammon', name: 'Backgammon', category: 'tablero', status: 'playable', players: '2', emoji: '🎯', url: 'https://www.ymimports.com/pages/how-to-play-backgammon', note: 'Barra, capturas, dobles y bear-off.' },
  { id: 'chino', name: 'Damas Chinas', category: 'tablero', status: 'playable', players: '2–6', emoji: '✡️', url: 'https://www.ymimports.com/pages/how-to-play-chinese-checkers', note: 'Estrella de 121 hoyos, saltos encadenados y meta opuesta.' },
  { id: 'go', name: 'Go', category: 'tablero', status: 'playable', players: '2', emoji: '⚪', url: 'https://www.ymimports.com/pages/how-to-play-go', note: '9×9 y 13×13, capturas, ko y komi 6.5.' },
  { id: 'xiangqi', name: 'Xiangqi', category: 'tablero', status: 'playable', players: '2', emoji: '🀄', url: 'https://www.ymimports.com/pages/how-to-play-xiangqi-chinese-chess', note: 'Palacio, río, cañón y generales enfrentados.' },
  { id: 'shogi', name: 'Shogi', category: 'tablero', status: 'soon', players: '2', emoji: '☗', url: 'https://www.ymimports.com/pages/how-to-play-shogi' },
  { id: 'janggi', name: 'Janggi', category: 'tablero', status: 'research', players: '2', emoji: '🀄', url: 'https://www.ymimports.com/pages/how-to-play-janggi' },
  { id: 'jungle', name: 'Jungle', category: 'tablero', status: 'playable', players: '2', emoji: '🐯', url: 'https://www.ymimports.com/pages/how-to-play-jungle', note: 'Dou Shou Qi: río, trampas y saltos de tigre y león.' },
  { id: 'luzhanqi', name: 'Luzhanqi', category: 'tablero', status: 'research', players: '2–4', emoji: '🎖️', url: 'https://www.ymimports.com/pages/how-to-play-luzhanqi' },
  { id: 'ur', name: 'Juego Real de Ur', category: 'tablero', status: 'soon', players: '2', emoji: '🏺', url: 'https://www.ymimports.com/pages/how-to-play-royal-game-of-ur' },
  { id: 'mexican-train', name: 'Tren Mexicano', category: 'tablero', status: 'soon', players: '2–8', emoji: '🚂', url: 'https://www.ymimports.com/pages/how-to-play-mexican-train-dominos' },
  { id: 'chickenfoot', name: 'Chickenfoot', category: 'tablero', status: 'soon', players: '2–8', emoji: '🐔', url: 'https://www.ymimports.com/pages/how-to-play-chickenfoot-dominoes' },
  { id: 'aeroplane', name: 'Aeroplane Chess', category: 'tablero', status: 'soon', players: '2–4', emoji: '✈️', url: 'https://www.ymimports.com/pages/how-to-play-aeroplane-chess-fei-xing-qi' },
  { id: 'poker', name: 'Póker', category: 'cartas', status: 'playable', players: '2–6', emoji: '♠️', url: 'https://www.nhfournier.es/como-jugar/poker/', note: 'Texas Hold\'em sin dinero real: fichas de práctica, ciegas, all-in y botes laterales.' },
  { id: 'mahjong', name: 'Mahjong', category: 'tablero', status: 'research', players: '4', emoji: '🀄', url: 'https://www.ymimports.com/pages/how-to-play-american-mahjong' },
  { id: 'riichi', name: 'Riichi Mahjong', category: 'tablero', status: 'research', players: '4', emoji: '🀄', url: 'https://www.ymimports.com/pages/how-to-play-japanese-riichi-mahjong' },
  { id: 'mahjong-cards', name: 'Mahjong con naipes', category: 'cartas', status: 'research', players: '4', emoji: '🃏', url: 'https://www.ymimports.com/pages/american-mahjong-playing-cards-edition' },
  { id: 'three-chess', name: 'Ajedrez para tres', category: 'tablero', status: 'research', players: '3', emoji: '♞', url: 'https://www.ymimports.com/pages/how-to-play-three-player-chess' },
  { id: 'soroban', name: 'Soroban', category: 'herramienta', status: 'soon', players: '1', emoji: '🧮', url: 'https://www.ymimports.com/pages/how-to-use-a-soroban-abacus' },
];
