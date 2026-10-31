import type { SurfaceProps } from './GameSurface';
import { Board } from './board';
import { Button } from './components';
import { useT } from '../i18n/useT';

/**
 * Superficie compartida por los juegos de tablero cuyo motor expone un
 * `BoardView` y acepta `tap` con la casilla. Reversi, Luzhanqi y el ajedrez
 * para tres la usan para no duplicar la rejilla, el foco y los estados.
 */
export function BoardGameSurface({ view, act, busy }: SurfaceProps) {
  const t = useT();
  const board = view.center.board;
  if (!board) {
    return (
      <div className="surface surface-fallback">
        <p>{t('common.loading')}</p>
      </div>
    );
  }
  const active = view.phase === 'play' && view.turnId === view.youId;
  return (
    <div className="surface surface-boardgame">
      <div className="table-meta">
        {view.players.map((player) => (
          <span key={player.id} className={player.isTurn ? 'chip chip-gold' : 'chip'}>
            {player.name} · {player.score}
          </span>
        ))}
      </div>
      <div className="boardgame-frame">
        <Board
          board={board}
          interactive={active && !busy}
          onCell={(cell) => act({ type: 'tap', cell })}
        />
      </div>
      {view.actions.length > 0 ? (
        <div className="boardgame-actions">
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
      ) : null}
      {view.prompt ? (
        <p className={`board-hint tone-${view.prompt.tone ?? 'info'}`}>
          {t(view.prompt.key, view.prompt.params)}
        </p>
      ) : null}
    </div>
  );
}
