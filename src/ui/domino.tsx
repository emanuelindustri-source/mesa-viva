import { motion } from 'framer-motion';
import type { TileView } from '../core/types';
import { cx } from '../core/utils';

function pipsFor(value: number): number[] {
  const layouts: Record<number, number[]> = {
    0: [],
    1: [4],
    2: [0, 8],
    3: [0, 4, 8],
    4: [0, 2, 6, 8],
    5: [0, 2, 4, 6, 8],
    6: [0, 2, 3, 5, 6, 8],
  };
  return layouts[value] ?? [];
}

function TileHalf({ value, vertical }: { value: number; vertical?: boolean }) {
  if (value > 6) {
    return <span className={cx('domino-numeral', vertical && 'is-vertical')}>{value}</span>;
  }
  const pips = pipsFor(value);
  return (
    <span className={cx('domino-half', vertical && 'is-vertical')}>
      {Array.from({ length: 9 }).map((_, i) => (
        <span key={i} className={cx('domino-pip', pips.includes(i) && 'is-on')} />
      ))}
    </span>
  );
}

export function DominoTile({
  tile,
  vertical,
  playable,
  selected,
  small,
  onClick,
  disabled,
}: {
  tile: TileView;
  vertical?: boolean;
  playable?: boolean;
  selected?: boolean;
  small?: boolean;
  onClick?: () => void;
  disabled?: boolean;
}) {
  const classes = cx('domino-tile', vertical && 'is-vertical', playable && 'is-playable', selected && 'is-selected', small && 'is-small', onClick && !disabled && 'is-clickable');
  const content = (
    <>
      <TileHalf value={tile.a} vertical={vertical} />
      <span className="domino-divider" />
      <TileHalf value={tile.b} vertical={vertical} />
    </>
  );
  if (!onClick || disabled) {
    return <div className={classes} aria-label={`Ficha ${tile.a} ${tile.b}`}>{content}</div>;
  }
  return (
    <motion.button
      type="button"
      className={classes}
      onClick={onClick}
      aria-label={`Ficha ${tile.a} ${tile.b}`}
      whileHover={{ y: -8, scale: 1.03 }}
      whileTap={{ scale: 0.97 }}
      layout
    >
      {content}
    </motion.button>
  );
}

export function DominoChain({ tiles }: { tiles: TileView[] }) {
  return (
    <div className="domino-chain" role="list" aria-label="Cadena de fichas">
      {tiles.map((tile, i) => (
        <div key={`${tile.id}-${i}`} role="listitem" className="domino-chain-item">
          <DominoTile tile={tile} vertical={tile.a === tile.b} small={tiles.length > 9} />
        </div>
      ))}
    </div>
  );
}
