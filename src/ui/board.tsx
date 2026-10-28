import { cx } from '../core/utils';
import type { BoardView } from '../core/types';

export function Board({
  board,
  onCell,
  interactive,
  className,
}: {
  board: BoardView;
  onCell?: (index: number) => void;
  interactive?: boolean;
  className?: string;
}) {
  const cells = Array.from({ length: board.cols * board.rows }, (_, i) => board.cells[i] ?? {});
  return (
    <div
      className={cx('board', interactive && 'is-interactive', className)}
      style={{
        gridTemplateColumns: `repeat(${board.cols}, minmax(0, 1fr))`,
        gridTemplateRows: `repeat(${board.rows}, minmax(0, 1fr))`,
        aspectRatio: `${board.cols} / ${board.rows}`,
      }}
      role={interactive ? 'grid' : 'img'}
      aria-label={interactive ? 'Tablero interactivo' : 'Tablero'}
    >
      {cells.map((cell, i) => {
        const Tag = interactive ? 'button' : 'div';
        return (
          <Tag
            key={i}
            type={interactive ? 'button' : undefined}
            className={cx(
              'board-cell',
              cell.tone && `tone-${cell.tone}`,
              cell.side && `side-${cell.side}`,
              cell.highlight && `hl-${cell.highlight}`,
              cell.glyph && 'has-glyph',
              interactive && 'is-cell-button',
            )}
            onClick={interactive && onCell ? () => onCell(i) : undefined}
            aria-label={cell.label ? `Casilla ${cell.label}` : `Casilla ${i + 1}`}
          >
            {cell.glyph ? <span className="cell-glyph">{cell.glyph}</span> : null}
            {cell.label ? <span className="cell-label">{cell.label}</span> : null}
            {cell.sub ? <span className="cell-sub">{cell.sub}</span> : null}
            {cell.highlight === 'move' ? <span className="cell-dot" /> : null}
            {cell.highlight === 'capture' ? <span className="cell-capture-ring" /> : null}
          </Tag>
        );
      })}
    </div>
  );
}
