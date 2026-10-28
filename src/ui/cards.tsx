import { motion } from 'framer-motion';
import type { CSSProperties, MouseEvent, ReactNode } from 'react';
import { cardName } from '../i18n';
import { useT } from '../i18n/useT';
import type { CardView } from '../core/types';
import { cx } from '../core/utils';

const SUIT_COLORS: Record<string, string> = {
  oros: '#b07a22',
  copas: '#a92f38',
  espadas: '#2f4677',
  bastos: '#41643a',
};

const FURIA_BG: Record<string, { bg: string; ink: string; deep: string }> = {
  sol: { bg: '#f2b632', ink: '#2b1a04', deep: '#c98c11' },
  mar: { bg: '#2f7ec4', ink: '#f7fbff', deep: '#1a568f' },
  selva: { bg: '#3c9b62', ink: '#f7fff8', deep: '#257044' },
  magma: { bg: '#d94a3a', ink: '#fff8f5', deep: '#a32c22' },
  wild: { bg: '#1b1b2b', ink: '#fff8e8', deep: '#0b0b14' },
};

export function SuitIcon({ suit, size = 20, className }: { suit: string; size?: number; className?: string }) {
  const color = SUIT_COLORS[suit] ?? 'currentColor';
  const common = { width: size, height: size, viewBox: '0 0 24 24', fill: 'none', xmlns: 'http://www.w3.org/2000/svg' } as const;
  if (suit === 'oros') {
    return (
      <svg {...common} className={className} aria-hidden="true">
        <circle cx="12" cy="12" r="7.2" fill={color} opacity="0.16" />
        <circle cx="12" cy="12" r="6.2" stroke={color} strokeWidth="1.6" />
        <circle cx="12" cy="12" r="2.4" fill={color} />
        {[0, 45, 90, 135, 180, 225, 270, 315].map((angle) => (
          <path key={angle} d="M12 2.3v2.2" stroke={color} strokeWidth="1.5" strokeLinecap="round" transform={`rotate(${angle} 12 12)`} />
        ))}
      </svg>
    );
  }
  if (suit === 'copas') {
    return (
      <svg {...common} className={className} aria-hidden="true">
        <path d="M7 4.5h10c0 4.6-1.8 7.4-5 7.4s-5-2.8-5-7.4Z" fill={color} opacity="0.2" stroke={color} strokeWidth="1.5" strokeLinejoin="round" />
        <path d="M12 12v5.5M8.6 20h6.8M9.4 17.5h5.2" stroke={color} strokeWidth="1.7" strokeLinecap="round" />
        <path d="M9.2 6.5h5.6" stroke={color} strokeWidth="1.2" strokeLinecap="round" opacity="0.8" />
      </svg>
    );
  }
  if (suit === 'espadas') {
    return (
      <svg {...common} className={className} aria-hidden="true">
        <path d="M12 2.8 15 9.5 13 17h-2l-2-7.5L12 2.8Z" fill={color} opacity="0.18" stroke={color} strokeWidth="1.4" strokeLinejoin="round" />
        <path d="M7 15.6h10M12 17v4M9.5 21h5" stroke={color} strokeWidth="1.7" strokeLinecap="round" />
        <path d="m9.4 12.6 5.2 1.4M14.6 12.6l-5.2 1.4" stroke={color} strokeWidth="1" opacity="0.7" />
      </svg>
    );
  }
  if (suit === 'bastos') {
    return (
      <svg {...common} className={className} aria-hidden="true">
        <path d="M9.6 20.2 13.4 6.4c.4-1.5 2.4-2 3.4-.9 1 1 .6 2.7-.9 3.2L8.5 11.8" stroke={color} strokeWidth="2.2" strokeLinecap="round" fill="none" />
        <circle cx="8.6" cy="12" r="1.7" fill={color} />
        <circle cx="10.5" cy="8.2" r="1.4" fill={color} opacity="0.8" />
        <path d="M6.5 17.5c1.6-.2 2.8.6 3 2.2" stroke={color} strokeWidth="1.4" strokeLinecap="round" />
      </svg>
    );
  }
  return null;
}

function FaceFigure({ rank, suit }: { rank: number; suit: string }) {
  const color = SUIT_COLORS[suit] ?? '#b07a22';
  return (
    <svg viewBox="0 0 80 118" className="card-figure" aria-hidden="true">
      <defs>
        <linearGradient id={`robe-${suit}-${rank}`} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor={color} stopOpacity="0.95" />
          <stop offset="100%" stopColor={color} stopOpacity="0.55" />
        </linearGradient>
      </defs>
      <path d="M14 112c2-20 12-31 26-33 14 2 24 13 26 33H14Z" fill={`url(#robe-${suit}-${rank})`} />
      <path d="M27 86c0-6 5-10 13-10s13 4 13 10l-4 9H31l-4-9Z" fill="#f2d6ac" />
      <path d="M34 64h12v14c0 5-12 5-12 0V64Z" fill="#e8c397" />
      <ellipse cx="40" cy="48" rx="16" ry="19" fill="#f2d6ac" />
      <path d="M25 45c1-13 8-20 15-20 9 0 15 8 15 20-4-5-8-7-15-7s-11 2-15 7Z" fill="#3a2a22" opacity="0.85" />
      {rank === 12 ? (
        <>
          <path d="m23 27 6 5 4-9 7 8 7-8 4 9 6-5-3 13H26l-3-13Z" fill="#d9ad3f" stroke="#9a6a18" strokeWidth="1.2" strokeLinejoin="round" />
          <circle cx="40" cy="35" r="2" fill="#b0343c" />
          <path d="M32 63c3 2 5 3 8 3s5-1 8-3" stroke="#8c5a34" strokeWidth="1.6" fill="none" strokeLinecap="round" />
        </>
      ) : rank === 11 ? (
        <>
          <path d="M23 39c0-11 7-18 17-18s17 7 17 18l-5 3-3-10-9 5-9-5-3 10-5-3Z" fill="#cdd6e3" stroke="#75849b" strokeWidth="1.2" />
          <path d="M55 29c4-1 7 2 7 6s-3 7-7 6" stroke="#b0343c" strokeWidth="2.4" fill="none" strokeLinecap="round" />
          <path d="M29 44c3 5 7 7 11 7s8-2 11-7" stroke="#75849b" strokeWidth="1.2" fill="none" opacity="0.7" />
        </>
      ) : (
        <>
          <path d="M26 34c4-6 10-9 14-9s10 3 14 9c-1-9-6-14-14-14s-13 5-14 14Z" fill="#3a2a22" />
          <path d="M21 32c3-3 6-5 9-6l-1 5H21ZM59 32c-3-3-6-5-9-6l1 5h8Z" fill="#6a4a2c" />
          <circle cx="40" cy="27" r="3" fill="#d9ad3f" />
          <path d="M31 57c2 2 4 3 6 3M49 57c-2 2-4 3-6 3" stroke="#a76b45" strokeWidth="1.3" strokeLinecap="round" />
        </>
      )}
      <circle cx="34" cy="48" r="1.2" fill="#2b1c16" />
      <circle cx="46" cy="48" r="1.2" fill="#2b1c16" />
      <path d="M37 55c2 1.5 4 1.5 6 0" stroke="#a76b45" strokeWidth="1.2" fill="none" strokeLinecap="round" />
      <g transform="translate(31 91) scale(0.75)">
        <SuitIcon suit={suit} size={24} />
      </g>
    </svg>
  );
}

function PipGrid({ rank, suit }: { rank: number; suit: string }) {
  if (rank === 1) {
    return (
      <div className="card-pips card-pips-ace">
        <SuitIcon suit={suit} size={46} />
      </div>
    );
  }
  return (
    <div className={cx('card-pips', rank % 2 === 1 && 'is-odd')}>
      {Array.from({ length: rank }).map((_, i) => (
        <SuitIcon key={i} suit={suit} size={rank > 5 ? 15 : 19} />
      ))}
    </div>
  );
}

interface SpanishCardProps {
  card: CardView;
  faceDown?: boolean;
  playable?: boolean;
  selected?: boolean;
  small?: boolean;
  onClick?: (event: MouseEvent<HTMLButtonElement>) => void;
  className?: string;
  style?: CSSProperties;
  disabled?: boolean;
  index?: number;
}

export function SpanishCard({
  card,
  faceDown,
  playable,
  selected,
  small,
  onClick,
  className,
  style,
  disabled,
}: SpanishCardProps) {
  const t = useT();
  const label = faceDown ? t('table.deck') : cardName(card, t);
  const interactive = Boolean(onClick) && !disabled;
  const content = faceDown ? (
    <CardBack />
  ) : (
    <>
      <span className="card-corner card-corner-tl">
        <span className="card-rank">{card.rank}</span>
        <SuitIcon suit={card.suit} size={11} />
      </span>
      <span className="card-inner">
        {card.rank >= 10 ? <FaceFigure rank={card.rank} suit={card.suit} /> : <PipGrid rank={card.rank} suit={card.suit} />}
      </span>
      <span className="card-corner card-corner-br">
        <span className="card-rank">{card.rank}</span>
        <SuitIcon suit={card.suit} size={11} />
      </span>
      <span className="card-suit-name">{t(`suit.${card.suit}`)}</span>
    </>
  );

  const classes = cx('playing-card', small && 'is-small', faceDown && 'is-back', playable && 'is-playable', selected && 'is-selected', className);
  if (!interactive) {
    return (
      <div className={classes} style={style} aria-label={label}>
        {content}
      </div>
    );
  }
  return (
    <motion.button
      type="button"
      className={classes}
      style={style}
      onClick={onClick}
      aria-label={label}
      aria-pressed={selected}
      whileHover={{ y: -10, rotate: 0, scale: 1.04 }}
      whileTap={{ scale: 0.96, y: -2 }}
      layout
    >
      {content}
    </motion.button>
  );
}

const POKER_SYMBOL: Record<string, string> = {
  clubs: '♣',
  diamonds: '♦',
  hearts: '♥',
  spades: '♠',
};

const POKER_NAMES: Record<string, string> = {
  clubs: 'tréboles',
  diamonds: 'diamantes',
  hearts: 'corazones',
  spades: 'picas',
};

export function pokerRankLabel(rank: number): string {
  if (rank === 14) return 'A';
  if (rank === 13) return 'K';
  if (rank === 12) return 'Q';
  if (rank === 11) return 'J';
  return String(rank);
}

export function PokerCard({
  card,
  faceDown,
  playable,
  selected,
  small,
  onClick,
  className,
  style,
  disabled,
}: SpanishCardProps) {
  const t = useT();
  const red = card.suit === 'hearts' || card.suit === 'diamonds';
  const symbol = POKER_SYMBOL[card.suit] ?? '♠';
  const rank = pokerRankLabel(card.rank);
  const label = faceDown ? t('table.deck') : `${rank} de ${POKER_NAMES[card.suit] ?? card.suit}`;
  const classes = cx(
    'poker-card',
    red && 'is-red',
    small && 'is-small',
    faceDown && 'is-back',
    playable && 'is-playable',
    selected && 'is-selected',
    className,
  );
  const content = faceDown ? (
    <CardBack />
  ) : (
    <>
      <span className="poker-corner poker-corner-tl">
        <b>{rank}</b>
        <i>{symbol}</i>
      </span>
      <span className="poker-card-center">
        <b>{rank}</b>
        <i>{symbol}</i>
      </span>
      <span className="poker-corner poker-corner-br">
        <b>{rank}</b>
        <i>{symbol}</i>
      </span>
    </>
  );
  if (!onClick || disabled) {
    return <div className={classes} style={style} aria-label={label}>{content}</div>;
  }
  return (
    <motion.button
      type="button"
      className={classes}
      style={style}
      onClick={onClick}
      aria-label={label}
      aria-pressed={selected}
      whileHover={{ y: -8, scale: 1.03 }}
      whileTap={{ scale: 0.97 }}
      layout
    >
      {content}
    </motion.button>
  );
}

export function CardBack({ small }: { small?: boolean }) {
  return (
    <span className={cx('card-back-art', small && 'is-small')} aria-hidden="true">
      <svg viewBox="0 0 80 112" className="card-back-svg">
        <defs>
          <pattern id="back-diamond" width="16" height="16" patternUnits="userSpaceOnUse" patternTransform="rotate(45)">
            <rect width="16" height="16" fill="#7c1f2b" />
            <rect width="7" height="7" fill="#a83341" />
            <rect x="8" y="8" width="7" height="7" fill="#5e1620" />
          </pattern>
        </defs>
        <rect width="80" height="112" rx="6" fill="url(#back-diamond)" />
        <rect x="4" y="4" width="72" height="104" rx="4" fill="none" stroke="#e7c66a" strokeWidth="1.2" />
        <circle cx="40" cy="56" r="19" fill="#7c1f2b" stroke="#e7c66a" strokeWidth="1.4" />
        {[0, 45, 90, 135, 180, 225, 270, 315].map((a) => (
          <path key={a} d="M40 38v7" stroke="#e7c66a" strokeWidth="1.2" strokeLinecap="round" transform={`rotate(${a} 40 56)`} />
        ))}
        <circle cx="40" cy="56" r="7" fill="#e7c66a" />
        <circle cx="40" cy="56" r="3" fill="#7c1f2b" />
        {[22, 58].map((x) => (
          <g key={x} transform={`translate(${x} 18)`}>
            <path d="M0 6c4-5 8-5 12 0-4 5-8 5-12 0Z" fill="#e7c66a" opacity="0.85" />
          </g>
        ))}
        {[22, 58].map((x) => (
          <g key={`b${x}`} transform={`translate(${x} 88)`}>
            <path d="M0 6c4-5 8-5 12 0-4 5-8 5-12 0Z" fill="#e7c66a" opacity="0.85" />
          </g>
        ))}
      </svg>
    </span>
  );
}

const ACTION_GLYPH: Record<string, string> = {
  skip: '⊘',
  reverse: '⇄',
  draw2: '+2',
  wild: '✦',
  wild4: '+4',
};

export function FuriaCard({
  card,
  playable,
  selected,
  small,
  onClick,
  className,
  disabled,
  faceDown,
}: SpanishCardProps) {
  const t = useT();
  const meta = FURIA_BG[card.suit] ?? { bg: '#1b1b2b', ink: '#fff8e8', deep: '#0b0b14' };
  const label = faceDown ? t('table.deck') : cardName(card, t);
  const interactive = Boolean(onClick) && !disabled;
  const classes = cx(
    'furia-card',
    `furia-${card.suit}`,
    card.action && 'is-action',
    playable && 'is-playable',
    selected && 'is-selected',
    small && 'is-small',
    faceDown && 'is-back',
    className,
  );
  const content = faceDown ? (
    <CardBack />
  ) : (
    <>
      <span className="furia-corner furia-corner-tl">{card.action ? ACTION_GLYPH[card.action] ?? '' : card.rank}</span>
      <span className="furia-center">
        {card.action ? (
          <span className="furia-action">{ACTION_GLYPH[card.action] ?? '✦'}</span>
        ) : (
          <span className="furia-number">{card.rank}</span>
        )}
      </span>
      <span className="furia-corner furia-corner-br">{card.action ? ACTION_GLYPH[card.action] ?? '' : card.rank}</span>
      <span className="furia-color-name">
        {card.suit === 'wild' ? t('card.wild') : t(`color.${card.suit}`)}
      </span>
      <span className="furia-shine" />
    </>
  );
  const style = { '--furia-bg': meta.bg, '--furia-ink': meta.ink, '--furia-deep': meta.deep } as CSSProperties;
  if (!interactive) {
    return (
      <div className={classes} style={style} aria-label={label}>
        {content}
      </div>
    );
  }
  return (
    <motion.button
      type="button"
      className={classes}
      style={style}
      onClick={onClick}
      aria-label={label}
      aria-pressed={selected}
      whileHover={{ y: -10, scale: 1.04 }}
      whileTap={{ scale: 0.96, y: -2 }}
      layout
    >
      {content}
    </motion.button>
  );
}

export function CardSlot({ children, label }: { children?: ReactNode; label?: string }) {
  return (
    <div className="card-slot" aria-label={label}>
      {children ?? <span className="card-slot-empty" />}
    </div>
  );
}
