import { motion } from 'framer-motion';
import { useEffect, useRef, useState } from 'react';
import { useT } from '../i18n/useT';
import { cx } from '../core/utils';

const PIPS: Record<number, number[]> = {
  1: [4],
  2: [0, 8],
  3: [0, 4, 8],
  4: [0, 2, 6, 8],
  5: [0, 2, 4, 6, 8],
  6: [0, 2, 3, 5, 6, 8],
};

function DieFace({ value, size }: { value: number; size: number }) {
  const positions = PIPS[value] ?? PIPS[1] ?? [];
  return (
    <span className="die-face" style={{ width: size, height: size }}>
      {Array.from({ length: 9 }).map((_, i) => (
        <span key={i} className={cx('die-pip', positions.includes(i) && 'is-on')} />
      ))}
    </span>
  );
}

export function Die({
  value,
  size = 56,
  hidden,
  rolling,
  onClick,
  label,
  tone = 'ivory',
  index = 0,
}: {
  value: number;
  size?: number;
  hidden?: boolean;
  rolling?: boolean;
  onClick?: () => void;
  label?: string;
  tone?: 'ivory' | 'gold' | 'coral';
  index?: number;
}) {
  const t = useT();
  const [display, setDisplay] = useState(value);
  const [tumbling, setTumbling] = useState(false);
  const prev = useRef(value);

  useEffect(() => {
    if (rolling) {
      setTumbling(true);
      const start = Date.now();
      const interval = window.setInterval(() => {
        setDisplay(1 + Math.floor(Math.random() * 6));
        if (Date.now() - start > 600) {
          window.clearInterval(interval);
          setDisplay(value);
          setTumbling(false);
        }
      }, 70);
      return () => window.clearInterval(interval);
    }
    setDisplay(value);
    return undefined;
  }, [value, rolling]);

  useEffect(() => {
    if (prev.current !== value) {
      setDisplay(value);
      prev.current = value;
    }
  }, [value]);

  const Tag = onClick ? 'button' : 'div';
  return (
    <Tag
      type={onClick ? 'button' : undefined}
      className={cx('die', `die-${tone}`, tumbling && 'is-tumbling', hidden && 'is-hidden', onClick && 'is-clickable')}
      style={{ width: size, height: size, animationDelay: `${index * 40}ms` }}
      onClick={onClick}
      aria-label={hidden ? t('table.reveal') : `${label ?? t('table.roll')}: ${display}`}
    >
      {hidden ? (
        <span className="die-cup" aria-hidden="true">
          <span className="die-cup-lid" />
          <span className="die-cup-body">?</span>
        </span>
      ) : (
        <DieFace value={display} size={size * 0.72} />
      )}
      <span className="die-gloss" aria-hidden="true" />
    </Tag>
  );
}

export function DiceRow({
  values,
  hidden,
  size = 46,
  rolling,
  onRoll,
  label,
}: {
  values: number[];
  hidden?: boolean;
  size?: number;
  rolling?: boolean;
  onRoll?: () => void;
  label?: string;
}) {
  return (
    <div className="dice-row">
      {values.map((value, i) => (
        <Die
          key={i}
          value={value}
          size={size}
          hidden={hidden}
          rolling={rolling}
          index={i}
          onClick={onRoll}
          label={label}
        />
      ))}
    </div>
  );
}

export function DiceCup({ count, name, size = 36 }: { count: number; name: string; size?: number }) {
  return (
    <div className="dice-cup" title={`${name}: ${count}`} aria-label={`${name}: ${count}`}>
      <span className="dice-cup-icon" style={{ width: size, height: size }}>?</span>
      <span className="dice-cup-count">×{count}</span>
    </div>
  );
}

export function BigDie({ value, onRoll, rolling, hint }: { value: number; onRoll: () => void; rolling?: boolean; hint?: string }) {
  const t = useT();
  return (
    <div className="big-die-wrap">
      <motion.div
        animate={rolling ? { rotate: [0, 18, -12, 8, 0], y: [0, -12, 5, -4, 0] } : {}}
        transition={{ duration: 0.65 }}
      >
        <Die value={value} size={92} onClick={onRoll} rolling={rolling} tone="ivory" />
      </motion.div>
      <p className="die-hint">{hint ?? t('table.shakeHint')}</p>
    </div>
  );
}
