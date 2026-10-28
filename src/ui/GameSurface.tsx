import { AnimatePresence, motion } from 'framer-motion';
import { useEffect, useMemo, useRef, useState } from 'react';
import { Dices, MousePointerClick, Sparkles } from 'lucide-react';
import type { CardView, GameAction, GameView, TileView } from '../core/types';
import { useT } from '../i18n/useT';
import { cx } from '../core/utils';
import { SpanishCard, FuriaCard, CardBack } from './cards';
import { DominoChain, DominoTile } from './domino';
import { BigDie, DiceRow, Die } from './dice';
import { Board } from './board';
import { Button, Modal } from './components';
import { burst, burstFromElement } from './particles/bus';
import { haptic } from '../services/haptics';
import { sfx } from '../audio/engine';

export interface SurfaceProps {
  view: GameView;
  act: (action: GameAction) => void;
  busy?: boolean;
}

type Extra = Record<string, unknown>;

function extra(view: GameView): Extra {
  return (view.extra ?? {}) as Extra;
}

function num(value: unknown, fallback = 0): number {
  return typeof value === 'number' ? value : fallback;
}

function str(value: unknown): string {
  return typeof value === 'string' ? value : '';
}

/* ------------------------------------------------------------------ Caída */

function CaidaSurface({ view, act, busy }: SurfaceProps) {
  const t = useT();
  const ex = extra(view);
  const table = view.center.cards ?? [];
  const deckCount = view.center.piles?.find((p) => p.id === 'deck')?.count ?? 0;
  const target = num(ex.target, 24);
  const handNo = num(ex.handNo, 1);
  const teams = Boolean(ex.teams);
  const captured = (ex.captured ?? {}) as Record<string, number>;
  const teamScores = (ex.teamScores ?? null) as Record<string, number> | null;
  const lastCapture = ex.lastCapture as { playerId: string; cards: CardView[]; points: number; mesa: boolean } | null;
  const [flash, setFlash] = useState<string | null>(null);
  const lastRef = useRef<string>('');

  useEffect(() => {
    if (!lastCapture) return;
    const key = `${lastCapture.playerId}-${lastCapture.points}-${lastCapture.cards.length}`;
    if (lastRef.current === key) return;
    lastRef.current = key;
    setFlash(`${t('table.caidaFlash', { points: lastCapture.points })} · ${lastCapture.cards.length} ${t('table.cards')}`);
    const timer = window.setTimeout(() => setFlash(null), 2000);
    return () => window.clearTimeout(timer);
  }, [lastCapture, t]);

  return (
    <div className="surface surface-caida">
      <div className="table-meta">
        <span>{t('table.handNo', { n: handNo })}</span>
        <span>·</span>
        <span>{t('table.target', { n: target })}</span>
        {teams && teamScores ? (
          <span className="team-mini">
            {Object.entries(teamScores).map(([team, score]) => (
              <span key={team} className="chip chip-gold">{team === '0' ? t('table.teamA') : t('table.teamB')} {score}</span>
            ))}
          </span>
        ) : null}
      </div>

      <div className="caida-table">
        <div className="deck-pile" aria-label={t('table.deck')}>
          <div className="deck-stack">
            <CardBack />
            <span className="deck-count">{deckCount}</span>
          </div>
          <span className="pile-label">{t('table.deck')}</span>
        </div>

        <div className="table-cards" ref={(el) => { if (el) el.dataset.drop = 'caida'; }}>
          <AnimatePresence initial={false}>
            {table.length === 0 ? (
              <motion.div key="empty" className="empty-table" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
                <Sparkles size={18} />
                <span>{t('table.logEmpty')}</span>
              </motion.div>
            ) : (
              table.map((card, i) => (
                <motion.div
                  key={card.id}
                  className="table-card"
                  initial={{ opacity: 0, y: 28, rotate: (i % 3) - 1 }}
                  animate={{ opacity: 1, y: 0, rotate: ((i * 7) % 11) - 5 }}
                  exit={{ opacity: 0, scale: 0.8 }}
                  style={{ marginLeft: i === 0 ? 0 : -34, zIndex: i }}
                >
                  <SpanishCard card={card} small />
                </motion.div>
              ))
            )}
          </AnimatePresence>
        </div>
      </div>

      <AnimatePresence>
        {flash ? (
          <motion.div className="capture-flash" initial={{ opacity: 0, scale: 0.8, y: 10 }} animate={{ opacity: 1, scale: 1, y: 0 }} exit={{ opacity: 0, y: -20 }}>
            {flash}
          </motion.div>
        ) : null}
      </AnimatePresence>

      <div className="captures-row">
        {view.players.map((p) => (
          <span key={p.id} className={cx('capture-chip', p.isTurn && 'is-turn')}>
            <span className="capture-name">{p.name}</span>
            <span>{captured[p.id] ?? 0} 🃏</span>
          </span>
        ))}
      </div>

      <div className="hand-area">
        <div className="hand-label">
          <span>{t('table.yourHand')}</span>
          <span>{view.hand.length} · {t('table.target', { n: target })}</span>
        </div>
        <div className={cx('hand', view.hand.length > 5 && 'is-crowded')}>
          <AnimatePresence initial={false}>
            {view.hand.map((card, i) => (
              <motion.div
                key={card.id}
                className="hand-card"
                initial={{ opacity: 0, y: 40, rotate: 0 }}
                animate={{ opacity: 1, y: 0, rotate: ((i - (view.hand.length - 1) / 2) * 3) }}
                exit={{ opacity: 0, y: -50, scale: 0.85 }}
                style={{ zIndex: i }}
              >
                <SpanishCard
                  card={card}
                  playable={card.playable && !busy}
                  onClick={card.playable && !busy ? (event) => {
                    burstFromElement(event.currentTarget, 'dust', { count: 10 });
                    haptic('card', true);
                    sfx('card');
                    act({ type: 'play', cardId: card.id });
                  } : undefined}
                />
              </motion.div>
            ))}
          </AnimatePresence>
        </div>
      </div>
    </div>
  );
}

/* ------------------------------------------------------------------ Furia */

const FURIA_COLORS = ['sol', 'mar', 'selva', 'magma'] as const;

function FuriaSurface({ view, act, busy }: SurfaceProps) {
  const t = useT();
  const ex = extra(view);
  const color = str(ex.color) || 'sol';
  const deckCount = view.center.piles?.find((p) => p.id === 'deck')?.count ?? 0;
  const top = view.center.cards?.[0];
  const [wildCard, setWildCard] = useState<CardView | null>(null);
  const [shout, setShout] = useState(false);
  const lastShout = ex.lastShout as { playerId: string } | null;
  const lastShoutRef = useRef<string>('');

  useEffect(() => {
    if (!lastShout) return;
    if (lastShoutRef.current === lastShout.playerId) return;
    lastShoutRef.current = lastShout.playerId;
    setShout(true);
    const timer = window.setTimeout(() => setShout(false), 2200);
    return () => window.clearTimeout(timer);
  }, [lastShout]);

  const playCard = (card: CardView) => {
    if (card.action === 'wild' || card.action === 'wild4') {
      setWildCard(card);
      return;
    }
    act({ type: 'play', cardId: card.id, color });
    haptic('card', true);
    sfx('card');
  };

  return (
    <div className="surface surface-furia">
      <AnimatePresence>
        {shout ? (
          <motion.div className="shout-banner" initial={{ scale: 0.4, opacity: 0, rotate: -6 }} animate={{ scale: 1, opacity: 1, rotate: -3 }} exit={{ scale: 1.4, opacity: 0 }}>
            ¡ÚLTIMA!
          </motion.div>
        ) : null}
      </AnimatePresence>

      <div className="furia-board">
        <div className="furia-deck">
          <div className="deck-stack">
            <CardBack />
            <span className="deck-count">{deckCount}</span>
          </div>
          <span className="pile-label">{t('table.deck')}</span>
        </div>

        <div className="furia-color-orb">
          <span className={`color-dot color-${color}`} />
          <span className="color-label">{t(`color.${color}`)}</span>
          <span className="color-hint">{t('table.wildColor')}</span>
        </div>

        <div className="furia-discard">
          <AnimatePresence mode="popLayout">
            {top ? (
              <motion.div key={top.id} initial={{ scale: 0.7, opacity: 0, rotate: -12 }} animate={{ scale: 1, opacity: 1, rotate: 0 }} exit={{ scale: 1.1, opacity: 0 }}>
                <FuriaCard card={top} />
              </motion.div>
            ) : (
              <div className="card-slot-empty" />
            )}
          </AnimatePresence>
          <span className="pile-label">{t('table.discard')}</span>
        </div>
      </div>

      <div className="hand-area">
        <div className="hand-label">
          <span>{t('table.yourHand')}</span>
          <span>{t('table.cardsLeft', { n: view.hand.length })}</span>
        </div>
        <div className={cx('hand', view.hand.length > 6 && 'is-crowded')}>
          <AnimatePresence initial={false}>
            {view.hand.map((card, i) => (
              <motion.div
                key={card.id}
                className="hand-card"
                initial={{ opacity: 0, y: 40 }}
                animate={{ opacity: 1, y: 0, rotate: ((i - (view.hand.length - 1) / 2) * 2.4) }}
                exit={{ opacity: 0, y: -60, scale: 0.8 }}
                style={{ zIndex: i }}
              >
                <FuriaCard
                  card={card}
                  playable={card.playable && !busy}
                  onClick={card.playable && !busy ? (event) => {
                    burstFromElement(event.currentTarget, 'spark', { count: 8 });
                    playCard(card);
                  } : undefined}
                />
              </motion.div>
            ))}
          </AnimatePresence>
        </div>
      </div>

      <Modal
        open={Boolean(wildCard)}
        onClose={() => setWildCard(null)}
        title={t('table.selectColor')}
        subtitle={wildCard ? t(`card.${wildCard.action}`) : undefined}
        variant="bottom"
        size="sm"
      >
        <div className="color-picker">
          {FURIA_COLORS.map((c) => (
            <button
              key={c}
              type="button"
              className={`color-choice color-${c}`}
              onClick={() => {
                if (!wildCard) return;
                act({ type: 'play', cardId: wildCard.id, color: c });
                setWildCard(null);
                haptic('canto', true);
                sfx('canto');
              }}
            >
              <span className="color-choice-dot" />
              {t(`color.${c}`)}
            </button>
          ))}
        </div>
      </Modal>
    </div>
  );
}

/* ------------------------------------------------------------------ Dominó */

function DominoSurface({ view, act, busy }: SurfaceProps) {
  const t = useT();
  const ex = extra(view);
  const left = typeof ex.left === 'number' ? ex.left : null;
  const right = typeof ex.right === 'number' ? ex.right : null;
  const boneyard = view.center.piles?.find((p) => p.id === 'boneyard')?.count ?? 0;
  const chain = (view.center.tiles ?? []) as TileView[];
  const hand = view.handTiles ?? [];
  const [selected, setSelected] = useState<string | null>(null);
  const selectedTile = hand.find((tile) => tile.id === selected);
  const leftFit = selectedTile && left !== null ? selectedTile.a === left || selectedTile.b === left : false;
  const rightFit = selectedTile && right !== null ? selectedTile.a === right || selectedTile.b === right : false;

  useEffect(() => {
    if (selected && !hand.some((tile) => tile.id === selected)) setSelected(null);
  }, [hand, selected]);

  const play = (end: 'left' | 'right') => {
    if (!selected) return;
    act({ type: 'play', tileId: selected, end });
    setSelected(null);
    sfx('card');
    haptic('card', true);
  };

  return (
    <div className="surface surface-domino">
      <div className="table-meta">
        <span>{t('table.boneyard')}: {boneyard}</span>
        <span>·</span>
        <span>{left ?? '—'} ↔ {right ?? '—'}</span>
      </div>

      <div className="domino-table-wrap">
        <div className="domino-end left">
          <span>{t('table.leftEnd')}</span>
          <strong>{left ?? '—'}</strong>
        </div>
        <div className="domino-chain-scroll">
          {chain.length > 0 ? <DominoChain tiles={chain} /> : <div className="empty-table">{t('table.noMovesYet')}</div>}
        </div>
        <div className="domino-end right">
          <span>{t('table.rightEnd')}</span>
          <strong>{right ?? '—'}</strong>
        </div>
      </div>

      <AnimatePresence>
        {selectedTile ? (
          <motion.div className="end-actions" initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: 8 }}>
            <Button variant="secondary" size="sm" disabled={!leftFit} onClick={() => play('left')}>
              ← {t('table.leftEnd')}
            </Button>
            <Button variant="secondary" size="sm" disabled={!rightFit} onClick={() => play('right')}>
              {t('table.rightEnd')} →
            </Button>
          </motion.div>
        ) : null}
      </AnimatePresence>

      <div className="hand-area">
        <div className="hand-label">
          <span>{t('table.yourHand')}</span>
          <span>{t('table.tilesLeft', { n: hand.length })}</span>
        </div>
        <div className="domino-hand">
          <AnimatePresence initial={false}>
            {hand.map((tile, i) => (
              <motion.div key={tile.id} initial={{ opacity: 0, y: 30 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -40 }} style={{ zIndex: i }}>
                <DominoTile
                  tile={tile}
                  small={hand.length > 7}
                  selected={selected === tile.id}
                  playable={tile.playable}
                  onClick={() => {
                    if (!tile.playable) return;
                    setSelected(tile.id === selected ? null : tile.id);
                    haptic('select', true);
                    sfx('tap');
                  }}
                  disabled={busy}
                />
              </motion.div>
            ))}
          </AnimatePresence>
        </div>
      </div>
    </div>
  );
}

/* ------------------------------------------------------------------ Dados */

function DadosSurface({ view, act, busy }: SurfaceProps) {
  const t = useT();
  const ex = extra(view);
  const bid = ex.bid as { qty: number; face: number } | null;
  const maxQty = num(ex.maxQty, 5);
  const reveal = ex.reveal as { dice: Record<string, number[]>; actual: number; bidTrue: boolean; loserId: string } | null;
  const isTurn = Boolean(ex.isTurn);
  const phase = view.phase;
  const [qty, setQty] = useState(bid?.qty ?? 1);
  const [face, setFace] = useState(bid?.face ?? 1);
  const diceForSeat = useMemo(() => {
    const map = new Map<string, number[]>();
    let cursor = 0;
    for (const player of view.players) {
      if (!player.active) continue;
      const count = player.diceLeft ?? 0;
      map.set(player.id, (view.center.dice ?? []).slice(cursor, cursor + count).map((d) => d.value));
      cursor += count;
    }
    return map;
  }, [view.players, view.center.dice]);

  useEffect(() => {
    if (bid) {
      setQty(bid.qty);
      setFace(Math.min(6, bid.face));
    }
  }, [bid?.qty, bid?.face, bid]);

  const validBid = bid ? qty > bid.qty || (qty === bid.qty && face > bid.face) : qty >= 1;
  const showBidPanel = isTurn && phase === 'bid';

  return (
    <div className="surface surface-dados">
      <div className="dados-table">
        <div className="dados-cups">
          {view.players.filter((p) => p.active).map((player) => (
            <div key={player.id} className={cx('cup-player', player.isTurn && 'is-turn')}>
              <span className="cup-name">{player.name}</span>
              {phase === 'reveal' ? (
                <DiceRow values={diceForSeat.get(player.id) ?? []} size={26} />
              ) : (
                <div className="cup-stack">
                  {Array.from({ length: player.diceLeft ?? 0 }).map((_, i) => (
                    <Die key={i} value={1} size={24} hidden index={i} />
                  ))}
                </div>
              )}
              <span className="cup-count">{player.diceLeft ?? 0} 🎲</span>
            </div>
          ))}
        </div>

        <div className="bid-display">
          {bid ? (
            <>
              <span className="bid-label">{t('table.bidPanel')}</span>
              <span className="bid-value">{bid.qty} × <span className="bid-face">{bid.face}</span></span>
            </>
          ) : (
            <span className="bid-empty">{t('prompt.dadosOpen')}</span>
          )}
        </div>

        {phase === 'reveal' && reveal ? (
          <div className={cx('reveal-panel', reveal.bidTrue ? 'is-true' : 'is-false')}>
            <span className="reveal-big">{reveal.actual}</span>
            <span>{t('table.actual', { n: reveal.actual })}</span>
            <span>{reveal.bidTrue ? '✓' : '✕'}</span>
          </div>
        ) : null}
      </div>

      {phase !== 'reveal' ? (
        <div className="dados-own">
          <div className="hand-label">
            <span>{t('table.yourDice')}</span>
            <span>{view.handDice?.length ?? 0} 🎲</span>
          </div>
          <DiceRow values={(view.handDice ?? []).map((d) => d.value)} size={48} />
        </div>
      ) : null}

      {showBidPanel ? (
        <div className="bid-panel">
          <div className="bid-control">
            <span>{t('table.quantity')}</span>
            <div className="stepper">
              <button type="button" onClick={() => setQty((q) => Math.max(1, q - 1))} disabled={qty <= 1}>−</button>
              <span>{qty}</span>
              <button type="button" onClick={() => setQty((q) => Math.min(maxQty, q + 1))} disabled={qty >= maxQty}>+</button>
            </div>
          </div>
          <div className="face-picker">
            <span>{t('table.face')}</span>
            <div className="face-buttons">
              {[1, 2, 3, 4, 5, 6].map((f) => (
                <button key={f} type="button" className={cx('face-btn', face === f && 'is-active')} onClick={() => setFace(f)}>
                  {f}
                </button>
              ))}
            </div>
          </div>
          <Button
            variant="primary"
            icon={<Dices size={17} />}
            disabled={!validBid || busy}
            onClick={() => {
              act({ type: 'bid', qty, face });
              haptic('dice', true);
              sfx('dice');
            }}
          >
            {t('table.bid')}
          </Button>
        </div>
      ) : null}
    </div>
  );
}

/* ------------------------------------------------------------------ Tableros */

function useBoardSelection(view: GameView) {
  const ex = extra(view);
  const legal = (ex.legalMoves ?? []) as Array<{ from: number; to: number; capture?: boolean; promo?: unknown }>;
  const [selected, setSelected] = useState<number | null>(null);
  useEffect(() => {
    if (selected !== null && !legal.some((m) => m.from === selected)) setSelected(null);
  }, [legal, selected]);
  return { legal, selected, setSelected, ex };
}

function ChessSurface({ view, act, busy }: SurfaceProps) {
  const t = useT();
  const { legal, selected, setSelected, ex } = useBoardSelection(view);
  const check = Boolean(ex.check);
  const last = ex.lastMove as { from: number; to: number } | null;
  const board = view.center.board;
  if (!board) return null;
  const cells = board.cells.map((cell) => ({ ...cell }));
  for (const move of legal) {
    if (cells[move.to]) {
      cells[move.to] = { ...cells[move.to], highlight: cells[move.to]?.glyph ? 'capture' : 'move' };
    }
  }
  if (selected !== null && cells[selected]) cells[selected] = { ...cells[selected], highlight: 'selected' };
  if (last) {
    for (const idx of [last.from, last.to]) {
      if (cells[idx] && cells[idx]?.highlight !== 'selected' && cells[idx]?.highlight !== 'capture' && cells[idx]?.highlight !== 'move') {
        cells[idx] = { ...cells[idx], highlight: 'last' };
      }
    }
  }
  if (check) {
    const king = num(ex.kingSquare, -1);
    if (king >= 0 && cells[king]) cells[king] = { ...cells[king], highlight: 'check' };
  }
  const onCell = (index: number) => {
    if (busy) return;
    if (selected !== null) {
      const move = legal.find((m) => m.from === selected && m.to === index);
      if (move) {
        act({ type: 'move', from: selected, to: index });
        setSelected(null);
        haptic('card', true);
        sfx('card');
        return;
      }
    }
    if (legal.some((m) => m.from === index)) {
      setSelected(index);
      haptic('select', true);
      sfx('tap');
    } else {
      setSelected(null);
    }
  };
  return (
    <div className="surface surface-board">
      <div className="board-frame">
        <Board board={{ ...board, cells }} onCell={onCell} interactive />
      </div>
      <p className="board-hint">{selected !== null ? t('table.selectDestination') : t('prompt.chessYourTurn')}</p>
    </div>
  );
}

function DamasSurface({ view, act, busy }: SurfaceProps) {
  const t = useT();
  const { legal, selected, setSelected, ex } = useBoardSelection(view);
  const board = view.center.board;
  if (!board) return null;
  const cells = board.cells.map((cell) => ({ ...cell }));
  for (const move of legal) {
    if (cells[move.to]) cells[move.to] = { ...cells[move.to], highlight: cells[move.to]?.glyph ? 'capture' : 'move' };
  }
  if (selected !== null && cells[selected]) cells[selected] = { ...cells[selected], highlight: 'selected' };
  const last = ex.lastMove as { from: number; to: number } | null;
  if (last) {
    for (const idx of [last.from, last.to]) {
      if (cells[idx] && !['selected', 'move', 'capture'].includes(cells[idx]?.highlight ?? '')) cells[idx] = { ...cells[idx], highlight: 'last' };
    }
  }
  const onCell = (index: number) => {
    if (busy) return;
    if (selected !== null) {
      const move = legal.find((m) => m.from === selected && m.to === index);
      if (move) {
        act({ type: 'move', from: selected, to: index });
        setSelected(null);
        haptic('card', true);
        sfx('card');
        return;
      }
    }
    if (legal.some((m) => m.from === index)) {
      setSelected(index);
      haptic('select', true);
      sfx('tap');
    } else {
      setSelected(null);
    }
  };
  return (
    <div className="surface surface-board">
      <div className="board-frame board-frame-checkers">
        <Board board={{ ...board, cells }} onCell={onCell} interactive />
      </div>
      <p className="board-hint">
        {num(ex.mustContinueFrom, -1) >= 0 ? t('prompt.damasChain') : selected !== null ? t('table.selectDestination') : t('prompt.damasYourTurn')}
      </p>
    </div>
  );
}

function SnakesSurface({ view, act, busy }: SurfaceProps) {
  const t = useT();
  const ex = extra(view);
  const board = view.center.board;
  const [rolling, setRolling] = useState(false);
  const lastRoll = num(ex.lastRoll, 0);
  const lastFrom = num(ex.lastFrom, 0);
  const roll = () => {
    if (busy || rolling) return;
    setRolling(true);
    haptic('dice', true);
    sfx('dice');
    act({ type: 'roll' });
    window.setTimeout(() => setRolling(false), 700);
    burst('confetti', window.innerWidth / 2, window.innerHeight * 0.35, { count: 10 });
  };
  if (!board) return null;
  return (
    <div className="surface surface-snakes">
      <div className="board-frame board-frame-snakes">
        <Board board={board} />
      </div>
      <div className="snake-roll">
        <BigDie value={lastRoll || 1} onRoll={roll} rolling={rolling} hint={t('table.shakeHint')} />
        <div className="snake-info">
          <span className="chip chip-gold">🪜 {t('table.ladders')}</span>
          <span className="chip chip-red">🐍 {t('table.snakes')}</span>
          {lastFrom > 0 ? <span className="snake-last">{t('table.yourScore')}: {lastFrom}</span> : null}
        </div>
      </div>
    </div>
  );
}

function MancalaSurface({ view, act, busy }: SurfaceProps) {
  const t = useT();
  const ex = extra(view);
  const pits = (ex.pits ?? []) as number[];
  const movable = (ex.movable ?? []) as number[];
  const turn = str(ex.turn) || 'w';
  const lastCapture = ex.lastCapture as { pit: number; seeds: number } | null;
  const top = [12, 11, 10, 9, 8, 7];
  const bottom = [0, 1, 2, 3, 4, 5];
  const isWhite = turn === 'w';
  const whitePlayer = view.players[0];
  const blackPlayer = view.players[1];
  const whiteLabel = view.youId === whitePlayer?.id ? t('common.you') : (whitePlayer?.name.split(' ')[0] ?? 'B');
  const blackLabel = view.youId === blackPlayer?.id ? t('common.you') : (blackPlayer?.name.split(' ')[0] ?? 'N');

  const Pit = ({ index, side }: { index: number; side: 'w' | 'b' }) => {
    const seeds = pits[index] ?? 0;
    const canPlay = movable.includes(index);
    return (
      <button
        type="button"
        className={cx('mancala-pit', side === 'w' ? 'is-bottom' : 'is-top', canPlay && 'is-playable', lastCapture?.pit === index && 'is-captured')}
        onClick={() => {
          if (!canPlay || busy) return;
          act({ type: 'sow', pit: index });
          haptic('card', true);
          sfx('card');
        }}
        disabled={!canPlay || busy}
        aria-label={`Hoyo ${index + 1}: ${seeds} semillas`}
      >
        <span className="pit-seeds">
          {Array.from({ length: Math.min(seeds, 9) }).map((_, i) => (
            <span key={i} className="seed" style={{ transform: `rotate(${i * 40}deg) translateY(${i % 2 === 0 ? -2 : 2}px)` }} />
          ))}
        </span>
        <span className="pit-count">{seeds}</span>
      </button>
    );
  };

  return (
    <div className="surface surface-mancala">
      <div className={cx('mancala-board', isWhite ? 'turn-white' : 'turn-black')}>
        <div className="mancala-store store-black">
          <span className="store-label">{blackLabel.slice(0, 5)}</span>
          <strong>{pits[13] ?? 0}</strong>
        </div>
        <div className="mancala-pits">
          <div className="mancala-row">
            {top.map((index) => <Pit key={index} index={index} side="b" />)}
          </div>
          <div className="mancala-middle" aria-hidden="true">
            <span className="mancala-turn-dot" />
            <span>{t('prompt.mancalaYourTurn')}</span>
          </div>
          <div className="mancala-row">
            {bottom.map((index) => <Pit key={index} index={index} side="w" />)}
          </div>
        </div>
        <div className="mancala-store store-white">
          <span className="store-label">{whiteLabel.slice(0, 5)}</span>
          <strong>{pits[6] ?? 0}</strong>
        </div>
      </div>
      <p className="board-hint">{t('prompt.mancalaYourTurn')}</p>
    </div>
  );
}

/* ------------------------------------------------------------------ switch */
export function GameSurface({ view, act, busy }: SurfaceProps) {
  switch (view.gameId) {
    case 'caida':
      return <CaidaSurface view={view} act={act} busy={busy} />;
    case 'furia':
      return <FuriaSurface view={view} act={act} busy={busy} />;
    case 'domino':
      return <DominoSurface view={view} act={act} busy={busy} />;
    case 'dados':
      return <DadosSurface view={view} act={act} busy={busy} />;
    case 'ajedrez':
      return <ChessSurface view={view} act={act} busy={busy} />;
    case 'damas':
      return <DamasSurface view={view} act={act} busy={busy} />;
    case 'serpientes':
      return <SnakesSurface view={view} act={act} busy={busy} />;
    case 'mancala':
      return <MancalaSurface view={view} act={act} busy={busy} />;
    default:
      return (
        <div className="surface surface-fallback">
          <MousePointerClick size={24} />
          <p>Este juego todavía está en camino.</p>
        </div>
      );
  }
}
