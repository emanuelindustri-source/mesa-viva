import { useEffect, useMemo, useState } from 'react';
import { motion } from 'framer-motion';
import type { CardView, GameAction, UIAction } from '../core/types';
import type { SurfaceProps } from './GameSurface';
import { useT } from '../i18n/useT';
import { Button } from './components';
import { cx } from '../core/utils';
import { sfx } from '../audio/engine';
import { haptic } from '../services/haptics';

interface MeldView {
  kind: 'chi' | 'pon' | 'kan';
  tiles: CardView[];
  from: string;
}

interface MahjongPlayerView {
  id: string;
  name: string;
  score: number;
  handCount: number;
  melds: MeldView[];
  discards: CardView[];
  flowers: CardView[];
  riichi: boolean;
}

interface MahjongExtra {
  variant: 'american' | 'riichi' | 'cards';
  variantLabel: string;
  round: number;
  dealerId: string;
  wallCount: number;
  canTsumo: boolean;
  canRiichi: boolean;
  players: MahjongPlayerView[];
}

const WINDS = ['東', '南', '西', '北'];
const DRAGONS = ['中', '發', '白'];
const CRACKS = ['一', '二', '三', '四', '五', '六', '七', '八', '九'];

function glyphFor(tile: CardView): string {
  switch (tile.suit) {
    case 'dots':
      return '●';
    case 'bams':
      return '❚';
    case 'cracks':
      return `${CRACKS[tile.rank - 1] ?? String(tile.rank)}萬`;
    case 'wind':
      return WINDS[tile.rank - 1] ?? String(tile.rank);
    case 'dragon':
      return DRAGONS[tile.rank - 1] ?? String(tile.rank);
    case 'flower':
      return '❀';
    case 'joker':
      return '★';
    default:
      return String(tile.rank);
  }
}

function suitClass(tile: CardView): string {
  return `suit-${tile.suit}`;
}

function tileLabel(t: (key: string, params?: Record<string, unknown>) => string, tile: CardView): string {
  return t('mahjong.tile', { suit: t(`mahjong.suit.${tile.suit}`), rank: tile.rank });
}

function MahjongTile({
  tile,
  selected,
  playable,
  small,
  onClick,
}: {
  tile: CardView;
  selected?: boolean;
  playable?: boolean;
  small?: boolean;
  onClick?: () => void;
}) {
  const t = useT();
  const glyph = glyphFor(tile);
  const showGlyph = tile.suit === 'cracks' || tile.suit === 'wind' || tile.suit === 'dragon' || tile.suit === 'flower' || tile.suit === 'joker';
  return (
    <motion.button
      type="button"
      layout
      whileHover={onClick ? { y: -7, scale: 1.03 } : undefined}
      whileTap={onClick ? { scale: 0.97 } : undefined}
      className={cx(
        'mj-tile',
        suitClass(tile),
        small && 'is-small',
        selected && 'is-selected',
        playable && 'is-playable',
        onClick && 'is-clickable',
      )}
      onClick={onClick}
      disabled={!onClick}
      aria-label={tileLabel(t, tile)}
      title={tileLabel(t, tile)}
    >
      {showGlyph ? (
        <span className="mj-glyph">{glyph}</span>
      ) : (
        <>
          <span className="mj-rank">{tile.rank}</span>
          <span className="mj-glyph">{glyph}</span>
        </>
      )}
      <span className="mj-corner">{tile.suit === 'joker' ? 'J' : tile.rank}</span>
    </motion.button>
  );
}

function DiscardRiver({ player }: { player: MahjongPlayerView }) {
  const t = useT();
  return (
    <div className="mj-river-column">
      <span className="mj-river-name">{player.name}</span>
      <div className="mj-river" role="list" aria-label={`${t('mahjong.discards')} ${player.name}`}>
        {player.discards.slice(-8).map((tile) => (
          <div key={tile.id} role="listitem">
            <MahjongTile tile={tile} small />
          </div>
        ))}
        {player.discards.length === 0 ? <span className="mj-empty">—</span> : null}
      </div>
    </div>
  );
}

function actionTone(action: UIAction): 'primary' | 'secondary' | 'danger' | 'ghost' {
  return action.tone ?? 'secondary';
}

export function MahjongSurface({ view, act, busy }: SurfaceProps) {
  const t = useT();
  const extra = (view.extra ?? {}) as unknown as MahjongExtra;
  const [selected, setSelected] = useState<string | null>(null);
  const me = extra.players?.find((player) => player.id === view.youId);
  const opponents = useMemo(
    () => (extra.players ?? []).filter((player) => player.id !== view.youId),
    [extra.players, view.youId],
  );
  const myTurn = view.turnId === view.youId && view.phase === 'play';
  const selectedTile = view.hand.find((tile) => tile.id === selected);

  useEffect(() => {
    if (selected && !view.hand.some((tile) => tile.id === selected)) setSelected(null);
  }, [view.hand, selected]);

  const discard = () => {
    if (!selectedTile || !myTurn) return;
    haptic('card', true);
    sfx('card');
    act({ type: 'discard', tileId: selectedTile.id });
    setSelected(null);
  };

  const riichi = () => {
    if (!selectedTile || !myTurn || !extra.canRiichi) return;
    haptic('card', true);
    sfx('card');
    act({ type: 'riichi', tileId: selectedTile.id });
    setSelected(null);
  };

  return (
    <div className="surface surface-mahjong">
      <div className="mj-meta">
        <span className="chip">{extra.variantLabel}</span>
        <span className="chip">{t('mahjong.round', { n: extra.round })}</span>
        <span className="chip">{t('mahjong.wallCount', { n: extra.wallCount })}</span>
        {me?.riichi ? <span className="chip chip-gold">RIICHI</span> : null}
      </div>

      <div className="mj-opponents">
        {opponents.map((player) => (
          <div key={player.id} className={cx('mj-opponent', view.turnId === player.id && 'is-turn')}>
            <div className="mj-opponent-head">
              <span className="mj-name">{player.name}</span>
              <span className="mj-score">{player.score}</span>
            </div>
            <div className="mj-backs" aria-label={`${player.handCount} ${t('mahjong.tiles')}`}>
              {Array.from({ length: Math.min(player.handCount, 14) }).map((_, index) => (
                <span key={index} className="mj-back" />
              ))}
            </div>
            <div className="mj-melds">
              {player.melds.map((meld, index) => (
                <span key={`${player.id}-meld-${index}`} className="mj-meld">
                  {meld.tiles.map((tile) => <MahjongTile key={tile.id} tile={tile} small />)}
                </span>
              ))}
              {player.riichi ? <span className="mj-riichi">RIICHI</span> : null}
            </div>
          </div>
        ))}
      </div>

      <div className="mj-river-board">
        {(extra.players ?? []).map((player) => <DiscardRiver key={player.id} player={player} />)}
      </div>

      <div className="mj-your-area">
        <div className="mj-your-head">
          <span>{t('mahjong.yourHand')}</span>
          <span className="mj-flowers">
            {me?.flowers.map((flower) => <span key={flower.id}>{glyphFor(flower)}</span>)}
          </span>
        </div>
        {me && me.melds.length > 0 ? (
          <div className="mj-your-melds">
            {me.melds.map((meld, index) => (
              <span key={`mine-${index}`} className="mj-meld">
                {meld.tiles.map((tile) => <MahjongTile key={tile.id} tile={tile} small />)}
              </span>
            ))}
          </div>
        ) : null}
        <div className={cx('mj-hand', view.hand.length > 10 && 'is-crowded')}>
          {view.hand.map((tile, index) => (
            <motion.div
              key={tile.id}
              initial={{ opacity: 0, y: 12, rotate: ((index % 5) - 2) * 0.4 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: Math.min(index * 0.012, 0.2) }}
            >
              <MahjongTile
                tile={tile}
                selected={selected === tile.id}
                playable={Boolean(tile.playable && myTurn && !busy)}
                onClick={tile.playable && myTurn && !busy ? () => {
                  setSelected((current) => (current === tile.id ? null : tile.id));
                  haptic('select');
                  sfx('select');
                } : undefined}
              />
            </motion.div>
          ))}
        </div>
        <div className="mj-actions">
          <Button variant="primary" onClick={discard} disabled={!selectedTile || !myTurn || Boolean(busy)}>
            {t('mahjong.discard')}
          </Button>
          {extra.variant === 'riichi' ? (
            <Button variant="secondary" onClick={riichi} disabled={!selectedTile || !myTurn || !extra.canRiichi || Boolean(busy)}>
              {t('mahjong.riichiAction')}
            </Button>
          ) : null}
          {extra.canTsumo ? (
            <Button variant="secondary" onClick={() => act({ type: 'tsumo' })} disabled={Boolean(busy)}>
              {t('mahjong.tsumo')}
            </Button>
          ) : null}
        </div>
        {view.actions.length > 0 ? (
          <div className="mj-actions mj-claims">
            {view.actions.map((action) => (
              <Button
                key={action.id}
                variant={actionTone(action)}
                onClick={() => act(action.action as GameAction)}
                disabled={Boolean(action.disabled || busy)}
              >
                {t(action.labelKey, action.labelParams)}
              </Button>
            ))}
          </div>
        ) : null}
        {view.prompt ? <p className={cx('mj-prompt', `tone-${view.prompt.tone ?? 'info'}`)}>{t(view.prompt.key, view.prompt.params)}</p> : null}
      </div>
    </div>
  );
}
