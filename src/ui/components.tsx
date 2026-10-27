import { AnimatePresence, motion } from 'framer-motion';
import type { ButtonHTMLAttributes, ReactNode } from 'react';
import { useEffect, useId, useRef } from 'react';
import { createPortal } from 'react-dom';
import {
  AlertTriangle,
  Check,
  CheckCircle2,
  ChevronRight,
  Info,
  Sparkles,
  X,
} from 'lucide-react';
import { cx } from '../core/utils';
import { useToastStore } from '../store/toasts';
import { AVATARS } from '../core/constants';

/* ------------------------------------------------------------------ Button */

export type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'danger' | 'gold' | 'soft';
export type ButtonSize = 'sm' | 'md' | 'lg';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  size?: ButtonSize;
  icon?: ReactNode;
  trailing?: ReactNode;
  full?: boolean;
  loading?: boolean;
}

export function Button({
  variant = 'secondary',
  size = 'md',
  icon,
  trailing,
  full,
  loading,
  className,
  children,
  disabled,
  ...rest
}: ButtonProps) {
  return (
    <button
      type="button"
      className={cx('btn', `btn-${variant}`, `btn-${size}`, full && 'btn-full', className)}
      disabled={disabled || loading}
      {...rest}
    >
      {loading ? <span className="btn-spinner" aria-hidden /> : icon ? <span className="btn-icon">{icon}</span> : null}
      <span className="btn-label">{children}</span>
      {trailing ? <span className="btn-trailing">{trailing}</span> : null}
    </button>
  );
}

export function IconButton({
  label,
  children,
  className,
  ...rest
}: ButtonHTMLAttributes<HTMLButtonElement> & { label: string; children: ReactNode }) {
  return (
    <button type="button" aria-label={label} title={label} className={cx('icon-btn', className)} {...rest}>
      {children}
    </button>
  );
}

/* ------------------------------------------------------------------- Modal */

interface ModalProps {
  open: boolean;
  onClose: () => void;
  title?: ReactNode;
  subtitle?: ReactNode;
  children: ReactNode;
  footer?: ReactNode;
  size?: 'sm' | 'md' | 'lg';
  variant?: 'center' | 'bottom';
  dismissible?: boolean;
}

export function Modal({
  open,
  onClose,
  title,
  subtitle,
  children,
  footer,
  size = 'md',
  variant = 'center',
  dismissible = true,
}: ModalProps) {
  const titleId = useId();
  const panelRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    const previous = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && dismissible) onClose();
    };
    window.addEventListener('keydown', onKey);
    const timer = window.setTimeout(() => {
      panelRef.current?.querySelector<HTMLElement>('button, [href], input, select, textarea')?.focus();
    }, 80);
    return () => {
      document.body.style.overflow = previous;
      window.removeEventListener('keydown', onKey);
      window.clearTimeout(timer);
    };
  }, [open, dismissible, onClose]);

  if (typeof document === 'undefined') return null;

  return createPortal(
    <AnimatePresence>
      {open ? (
        <motion.div
          className={cx('modal-layer', `modal-${variant}`)}
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          transition={{ duration: 0.18 }}
        >
          <div className="modal-backdrop" onClick={() => dismissible && onClose()} />
          <motion.div
            ref={panelRef}
            className={cx('modal-panel', `modal-panel-${size}`)}
            role="dialog"
            aria-modal="true"
            aria-labelledby={title ? titleId : undefined}
            initial={{ y: variant === 'bottom' ? 40 : 16, scale: variant === 'bottom' ? 1 : 0.98, opacity: 0 }}
            animate={{ y: 0, scale: 1, opacity: 1 }}
            exit={{ y: variant === 'bottom' ? 30 : 8, scale: variant === 'bottom' ? 1 : 0.99, opacity: 0 }}
            transition={{ type: 'spring', damping: 28, stiffness: 320 }}
          >
            {variant === 'bottom' ? <div className="modal-handle" aria-hidden /> : null}
            {(title || dismissible) && (
              <header className="modal-head">
                <div>
                  {title ? <h2 id={titleId}>{title}</h2> : null}
                  {subtitle ? <p>{subtitle}</p> : null}
                </div>
                {dismissible ? (
                  <IconButton label="Cerrar" onClick={onClose} className="modal-close">
                    <X size={18} />
                  </IconButton>
                ) : null}
              </header>
            )}
            <div className="modal-body">{children}</div>
            {footer ? <footer className="modal-foot">{footer}</footer> : null}
          </motion.div>
        </motion.div>
      ) : null}
    </AnimatePresence>,
    document.body,
  );
}

/* ------------------------------------------------------------------ Toasts */

export function ToastHost() {
  const toasts = useToastStore((s) => s.toasts);
  const remove = useToastStore((s) => s.remove);
  return (
    <div className="toast-host" aria-live="polite" aria-atomic="false">
      <AnimatePresence initial={false}>
        {toasts.map((toast) => (
          <motion.div
            key={toast.id}
            className={cx('toast', `toast-${toast.tone}`)}
            initial={{ opacity: 0, y: 12, scale: 0.98 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 8, scale: 0.98 }}
            transition={{ duration: 0.2 }}
            onClick={() => remove(toast.id)}
            role="status"
          >
            <span className="toast-icon">
              {toast.tone === 'success' || toast.tone === 'magic' ? <CheckCircle2 size={17} /> : toast.tone === 'danger' || toast.tone === 'warn' ? <AlertTriangle size={17} /> : <Info size={17} />}
            </span>
            <span>{toast.message}</span>
          </motion.div>
        ))}
      </AnimatePresence>
    </div>
  );
}

/* ------------------------------------------------------------------ Avatar */

export function Avatar({ id, size = 40, ring }: { id: string; size?: number; ring?: boolean }) {
  const avatar = AVATARS.find((a) => a.id === id);
  return (
    <span
      className={cx('avatar', ring && 'avatar-ring')}
      style={{ width: size, height: size, fontSize: Math.round(size * 0.5) }}
      aria-hidden="true"
    >
      {avatar?.emoji ?? id.slice(0, 1).toUpperCase()}
    </span>
  );
}

/* ------------------------------------------------------------------- misc */

export function Chip({ children, tone = 'default', className }: { children: ReactNode; tone?: 'default' | 'gold' | 'green' | 'red' | 'blue' | 'purple'; className?: string }) {
  return <span className={cx('chip', `chip-${tone}`, className)}>{children}</span>;
}

export function ProgressBar({ value, tone = 'gold', label }: { value: number; tone?: 'gold' | 'green' | 'blue'; label?: string }) {
  return (
    <div className={cx('progress', `progress-${tone}`)} role="progressbar" aria-valuenow={Math.round(value * 100)} aria-valuemin={0} aria-valuemax={100} aria-label={label}>
      <motion.span initial={{ width: 0 }} animate={{ width: `${Math.max(0, Math.min(1, value)) * 100}%` }} transition={{ duration: 0.7, ease: 'easeOut' }} />
    </div>
  );
}

export function SectionTitle({ eyebrow, title, action }: { eyebrow?: string; title: ReactNode; action?: ReactNode }) {
  return (
    <div className="section-title">
      <div>
        {eyebrow ? <span className="eyebrow">{eyebrow}</span> : null}
        <h2>{title}</h2>
      </div>
      {action}
    </div>
  );
}

export function EmptyState({ icon, title, text, action }: { icon?: ReactNode; title: string; text?: string; action?: ReactNode }) {
  return (
    <div className="empty-state">
      {icon ? <div className="empty-icon">{icon}</div> : <Sparkles size={22} />}
      <h3>{title}</h3>
      {text ? <p>{text}</p> : null}
      {action}
    </div>
  );
}

export function Segmented<T extends string>({
  value,
  options,
  onChange,
  label,
}: {
  value: T;
  options: Array<{ value: T; label: string; icon?: ReactNode; disabled?: boolean }>;
  onChange: (value: T) => void;
  label?: string;
}) {
  return (
    <div className="segmented" role="group" aria-label={label}>
      {options.map((option) => (
        <button
          key={option.value}
          type="button"
          className={cx('segment', value === option.value && 'is-active')}
          onClick={() => onChange(option.value)}
          aria-pressed={value === option.value}
          disabled={option.disabled}
        >
          {option.icon}
          {option.label}
        </button>
      ))}
    </div>
  );
}

export function Toggle({ checked, onChange, label, hint }: { checked: boolean; onChange: (value: boolean) => void; label: string; hint?: string }) {
  return (
    <label className="toggle-row">
      <span className="toggle-copy">
        <span className="toggle-label">{label}</span>
        {hint ? <span className="toggle-hint">{hint}</span> : null}
      </span>
      <button
        type="button"
        role="switch"
        aria-checked={checked}
        aria-label={label}
        className={cx('switch', checked && 'is-on')}
        onClick={() => onChange(!checked)}
      >
        <span className="switch-thumb">{checked ? <Check size={12} /> : null}</span>
      </button>
    </label>
  );
}

export function Stepper({ value, min, max, onChange, label }: { value: number; min: number; max: number; onChange: (value: number) => void; label: string }) {
  return (
    <div className="stepper" role="group" aria-label={label}>
      <button type="button" onClick={() => onChange(Math.max(min, value - 1))} disabled={value <= min} aria-label={`${label} −`}>−</button>
      <span>{value}</span>
      <button type="button" onClick={() => onChange(Math.min(max, value + 1))} disabled={value >= max} aria-label={`${label} +`}>+</button>
    </div>
  );
}

export function SettingRow({ icon, title, hint, children }: { icon?: ReactNode; title: string; hint?: string; children: ReactNode }) {
  return (
    <div className="setting-row">
      <div className="setting-copy">
        {icon ? <span className="setting-icon">{icon}</span> : null}
        <div>
          <div className="setting-title">{title}</div>
          {hint ? <div className="setting-hint">{hint}</div> : null}
        </div>
      </div>
      <div className="setting-control">{children}</div>
    </div>
  );
}

export function PageHeader({ title, subtitle, onBack, actions }: { title: ReactNode; subtitle?: ReactNode; onBack?: () => void; actions?: ReactNode }) {
  return (
    <header className="page-header">
      <div className="page-header-row">
        {onBack ? (
          <IconButton label="Volver" onClick={onBack} className="page-back">
            <ChevronRight size={20} className="flip-x" />
          </IconButton>
        ) : null}
        <div className="page-heading">
          <h1>{title}</h1>
          {subtitle ? <p>{subtitle}</p> : null}
        </div>
        {actions ? <div className="page-actions">{actions}</div> : null}
      </div>
    </header>
  );
}
