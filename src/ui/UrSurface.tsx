import type { SurfaceProps } from './GameSurface';
import { Button } from './components';
import { useT } from '../i18n/useT';
import { cx } from '../core/utils';
import { sfx } from '../audio/engine';
import { haptic } from '../services/haptics';

interface UrPieceView {
  id: string;
  side: 0 | 1;
  position: number;
}

interface UrExtra {
  pieces: UrPieceView[];
  dice: number;
  rosettes: number[];
  track: number;
  finished: Record<string, number>;
  legal: string[];
  lastPiece: string | null;
  lastFrom: number;
  lastTo: number;
  captured: boolean;
  extraTurn: boolean;
}

export function UrSurface({ view, act, busy }: SurfaceProps) {
  const t = useT();
  const extra = (view.extra ?? {}) as unknown as UrExtra;
  const legal = new Set(extra.legal ?? []);
  const pieces = extra.pieces ?? [];
  const rosettes = new Set(extra.rosettes ?? []);
  const active = view.phase === 'play' && view.turnId === view.youId && !busy;
  const myPieces = pieces.filter((piece) => piece.id.startsWith('w'));
  const rivalPieces = pieces.filter((piece) => piece.id.startsWith('b'));
  const cellPieces = (index: number) => pieces.filter((piece) => piece.position === index);
  const move = (pieceId: string) => {
    if (!active || !legal.has(pieceId)) return;
    haptic('select');
    sfx('select');
    act({ type: 'move', pieceId });
  };

  return (
    <div className="surface surface-ur">
      <div className="ur-status">
        <div className={cx('ur-die', extra.dice > 0 && 'is-rolled')} aria-label={t('ur.dice')}>
          {extra.dice > 0 ? extra.dice : '—'}
        </div>
        <div className="ur-status-text">
          <strong>{t('ur.track')}</strong>
          <span>{view.prompt ? t(view.prompt.key, view.prompt.params) : t('ur.waiting')}</span>
          {extra.extraTurn ? <span className="ur-extra">{t('ur.extraTurn')}</span> : null}
          {extra.captured ? <span className="ur-capture">{t('ur.captured')}</span> : null}
        </div>
        <div className="ur-score">
          <span>{t('ur.white')} {extra.finished?.[view.players[0]?.id ?? ''] ?? 0}/4</span>
          <span>{t('ur.black')} {extra.finished?.[view.players[1]?.id ?? ''] ?? 0}/4</span>
        </div>
      </div>

      <div className="ur-starts">
        <div className="ur-start is-white">
          <span>{t('ur.startWhite')}</span>
          <div>
            {myPieces.filter((piece) => piece.position < 0).map((piece) => (
              <button
                key={piece.id}
                type="button"
                className={cx('ur-piece is-white', legal.has(piece.id) && 'is-playable')}
                onClick={() => move(piece.id)}
                disabled={!legal.has(piece.id)}
                aria-label={`${t('ur.white')} ${piece.id}`}
              />
            ))}
          </div>
        </div>
        <div className="ur-start is-black">
          <span>{t('ur.startBlack')}</span>
          <div>
            {rivalPieces.filter((piece) => piece.position < 0).map((piece) => (
              <span key={piece.id} className="ur-piece is-black" aria-label={`${t('ur.black')} ${piece.id}`} />
            ))}
          </div>
        </div>
      </div>

      <div className="ur-track" role="list" aria-label={t('ur.trackAria')}>
        {Array.from({ length: extra.track ?? 20 }, (_, index) => (
          <div
            key={index}
            role="listitem"
            className={cx('ur-cell', rosettes.has(index) && 'is-rosette', extra.lastTo === index && 'is-last')}
          >
            <span className="ur-cell-number">{index + 1}</span>
            {rosettes.has(index) ? <span className="ur-rosette">✦</span> : null}
            <div className="ur-cell-pieces">
              {cellPieces(index).map((piece) => (
                <button
                  key={piece.id}
                  type="button"
                  className={cx('ur-piece', piece.side === 0 ? 'is-white' : 'is-black', legal.has(piece.id) && 'is-playable')}
                  onClick={() => move(piece.id)}
                  disabled={!legal.has(piece.id)}
                  aria-label={`${piece.side === 0 ? t('ur.white') : t('ur.black')} ${piece.id}`}
                />
              ))}
            </div>
          </div>
        ))}
        <div className={cx('ur-cell is-goal', extra.finished && 'is-goal')}>
          <span className="ur-cell-number">20</span>
          <span className="ur-rosette">🏁</span>
        </div>
      </div>

      <div className="ur-actions">
        {view.actions.map((action) => (
          <Button
            key={action.id}
            variant={action.tone ?? 'secondary'}
            onClick={() => act(action.action)}
            disabled={action.disabled || busy}
          >
            {t(action.labelKey, action.labelParams)}
          </Button>
        ))}
      </div>
    </div>
  );
}
