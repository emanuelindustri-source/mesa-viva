import { useEffect, useMemo, useRef, useState } from 'react';
import { motion } from 'framer-motion';
import {
  Download,
  Home,
  RotateCcw,
  Share2,
  Trophy,
  Copy,
  Check,
  ChevronDown,
  Sparkles,
  Clock,
  UserRound,
} from 'lucide-react';
import type { GameView, LogEvent, XpAwardResult } from '../core/types';
import { LEVELS, xpProgress } from '../core/levels';
import { formatDuration, formatDate } from '../core/utils';
import { formatLog, localeMeta } from '../i18n';
import { useT, useLocale } from '../i18n/useT';
import { useAppStore } from '../store/appStore';
import { Avatar, Button, Chip, ProgressBar } from './components';
import { burst } from './particles/bus';
import { sfx } from '../audio/engine';
import { haptic } from '../services/haptics';
import { shareText as doShare, copyText } from '../services/pwa';
import { toast } from '../store/toasts';
import { trackAction, diag } from '../services/diagnostics';
import type { Festival } from '../theme/festivals';

export interface ResultScreenProps {
  view: GameView;
  award: XpAwardResult | null;
  gameName: string;
  festival: Festival;
  youId: string;
  onRematch: () => void;
  onHome: () => void;
  rematchLabel?: string;
  homeLabel?: string;
}

function toneClass(tone?: LogEvent['tone']): string {
  switch (tone) {
    case 'success':
      return 'log-success';
    case 'warn':
      return 'log-warn';
    case 'danger':
      return 'log-danger';
    case 'magic':
      return 'log-magic';
    default:
      return 'log-info';
  }
}

export function ResultScreen({ view, award, gameName, festival, youId, onRematch, onHome, rematchLabel, homeLabel }: ResultScreenProps) {
  const t = useT();
  const locale = useLocale();
  const result = view.result;
  const log = view.log ?? [];
  const [showAll, setShowAll] = useState(false);
  const [copied, setCopied] = useState(false);
  const [pdfBusy, setPdfBusy] = useState(false);
  const fired = useRef(false);

  useEffect(() => {
    if (fired.current) return;
    fired.current = true;
    burst('confetti', window.innerWidth / 2, window.innerHeight * 0.3, { count: 70, power: 1.2 });
    sfx('win');
    haptic('win');
    trackAction('match:result', gameName);
  }, [gameName]);

  const winnerNames = useMemo(
    () => (result?.winnerIds ?? []).map((id) => view.players.find((p) => p.id === id)?.name).filter(Boolean) as string[],
    [result, view.players],
  );
  const isDraw = Boolean(result?.draw);
  const youWon = Boolean(result?.winnerIds.includes(youId));
  const sorted = useMemo(
    () => [...view.players].sort((a, b) => (result?.scores[b.id] ?? b.score) - (result?.scores[a.id] ?? a.score)),
    [view.players, result],
  );
  const headline = isDraw ? t('result.draw') : winnerNames.length > 1 ? t('result.winners', { names: winnerNames.join(' · ') }) : t('result.winner', { name: winnerNames[0] ?? '—' });
  const startedAt = view.startedAt ?? Date.now();
  const duration = Date.now() - startedAt;
  const profileXp = useAppStore((s) => s.profile.xp);
  const progress = xpProgress(profileXp);
  const levelInfo = award ? LEVELS.find((l) => l.level === award.levelAfter) : null;

  const logText = useMemo(
    () => log.map((e) => `[${new Date(e.at).toLocaleTimeString(localeMeta(locale).intl)}] ${formatLog(e, t)}`).join('\n'),
    [log, t, locale],
  );

  const share = async () => {
    const resultText = isDraw ? t('result.draw') : headline;
    const text = t('result.shareText', { game: gameName, result: resultText });
    const ok = await doShare('Mesa Viva', text, window.location.href);
    toast(ok ? t('notify.copied') : t('notify.error'), ok ? 'success' : 'danger');
  };

  const copyLog = async () => {
    const ok = await copyText(logText);
    setCopied(ok);
    toast(ok ? t('notify.logCopied') : t('notify.error'), ok ? 'success' : 'danger');
    if (ok) window.setTimeout(() => setCopied(false), 1600);
  };

  const downloadPdf = async () => {
    if (!result) return;
    setPdfBusy(true);
    try {
      const { generateMatchPdf } = await import('../services/pdf');
      await generateMatchPdf({
        view,
        result,
        log,
        gameName,
        startedAt,
        durationMs: duration,
        translate: t,
        accent: festival.accent,
        accent2: festival.accent2,
        locale: localeMeta(locale).intl,
      });
      toast(t('notify.pdfSaved'), 'success');
      trackAction('match:pdf', gameName);
    } catch (error) {
      diag('error', 'No se pudo generar el PDF', error instanceof Error ? error.stack : String(error), 'result:pdf');
      toast(t('result.pdfError'), 'danger');
    } finally {
      setPdfBusy(false);
    }
  };

  const visibleLog = showAll ? log : log.slice(-12);

  return (
    <div className="result-screen">
      <div className="result-confetti" aria-hidden="true" />
      <motion.div className="result-hero" initial={{ opacity: 0, y: 18 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.45 }}>
        <span className="result-season">{festival.emoji} {t(festival.nameKey)}</span>
        <div className="result-trophy">
          <Trophy size={30} />
        </div>
        <h1>{headline}</h1>
        <p className="result-game">{gameName} · {formatDuration(duration)}</p>
        <div className="result-chips">
          <Chip tone="gold">{t('result.reason')}: {t(result?.reasonKey ?? 'result.reason.draw')}</Chip>
          {award ? <Chip tone={youWon ? 'green' : isDraw ? 'blue' : 'red'}>{youWon ? t('result.youWon') : isDraw ? t('result.youDrew') : t('result.youLost')}</Chip> : null}
          <Chip tone="default">{formatDate(startedAt, localeMeta(locale).intl)}</Chip>
        </div>
      </motion.div>

      <div className="result-grid">
        <section className="result-card result-xp">
          <div className="result-card-head">
            <span className="eyebrow">{t('result.stats')}</span>
            <Sparkles size={18} />
          </div>
          {award ? (
            <>
              <div className="xp-big">+{award.xp} <span>XP</span></div>
              {award.leveledUp ? (
                <div className="levelup-banner">
                  <strong>{t('result.levelUp', { level: award.levelAfter })}</strong>
                  {levelInfo ? <span>{t('result.levelReward', { reward: t(levelInfo.rewardKey) })}</span> : null}
                </div>
              ) : (
                <p className="xp-next">{t('levels.xpToNext', { xp: Math.max(0, progress.needed - progress.current) })}</p>
              )}
              <div className="result-level-row">
                <span>Nivel {award.levelBefore}</span>
                <span>Nivel {award.levelAfter}</span>
              </div>
              <ProgressBar value={progress.ratio} />
              <p className="result-footnote">{t('result.xpCap')}: {award.xp} XP · {t('levels.xpRulesDesc')}</p>
            </>
          ) : (
            <p className="result-footnote">{t('table.spectating')}</p>
          )}
        </section>

        <section className="result-card">
          <div className="result-card-head">
            <span className="eyebrow">{t('result.finalScores')}</span>
            <UserRound size={18} />
          </div>
          <div className="score-list">
            {sorted.map((player, index) => {
              const score = result?.scores[player.id] ?? player.score;
              const winner = result?.winnerIds.includes(player.id);
              return (
                <motion.div
                  key={player.id}
                  className={`score-row ${winner ? 'is-winner' : ''}`}
                  initial={{ opacity: 0, x: -8 }}
                  animate={{ opacity: 1, x: 0 }}
                  transition={{ delay: 0.05 * index }}
                >
                  <span className="score-pos">{index + 1}</span>
                  <Avatar id={player.avatar} size={34} ring={winner} />
                  <span className="score-name">{player.name}{player.isBot ? <small>{t('lobby.botBadge')}</small> : null}</span>
                  {winner ? <Trophy size={15} className="score-crown" /> : null}
                  <strong className="score-value">{score}</strong>
                </motion.div>
              );
            })}
          </div>
          <div className="result-meta">
            <span><Clock size={14} /> {formatDuration(duration)}</span>
            <span><UserRound size={14} /> {view.players.length} {t('common.players')}</span>
          </div>
        </section>
      </div>

      <section className="result-card result-actions-card">
        <div className="result-actions">
          <Button variant="primary" icon={<RotateCcw size={18} />} onClick={onRematch}>{rematchLabel ?? t('result.rematch')}</Button>
          <Button variant="gold" icon={<Download size={18} />} onClick={() => void downloadPdf()} loading={pdfBusy}>{t('result.savePdf')}</Button>
          <Button variant="secondary" icon={<Share2 size={18} />} onClick={() => void share()}>{t('result.share')}</Button>
          <Button variant="ghost" icon={<Home size={18} />} onClick={onHome}>{homeLabel ?? t('result.home')}</Button>
        </div>
        <p className="result-rematch-hint">{t('result.rematchHint')}</p>
      </section>

      <section className="result-card result-log-card">
        <div className="result-card-head">
          <div>
            <span className="eyebrow">{t('result.moves')}</span>
            <h2>{t('result.logTitle')}</h2>
          </div>
          <div className="result-log-tools">
            <Button variant="ghost" size="sm" icon={copied ? <Check size={15} /> : <Copy size={15} />} onClick={() => void copyLog()}>
              {t('table.copyLog')}
            </Button>
            {log.length > 12 ? (
              <Button variant="ghost" size="sm" onClick={() => setShowAll((v) => !v)} trailing={<ChevronDown size={15} className={showAll ? 'rotate-180' : ''} />}>
                {showAll ? t('table.closeLog') : `${log.length} ${t('table.moves')}`}
              </Button>
            ) : null}
          </div>
        </div>
        <div className="result-log">
          {visibleLog.map((event, index) => (
            <div key={event.id} className={`log-line ${toneClass(event.tone)}`}>
              <span className="log-index">{showAll ? index + 1 : log.length - visibleLog.length + index + 1}</span>
              <span className="log-time">{new Date(event.at).toLocaleTimeString(localeMeta(locale).intl, { hour: '2-digit', minute: '2-digit' })}</span>
              <span className="log-text">{formatLog(event, t)}</span>
            </div>
          ))}
          {log.length === 0 ? <p className="result-footnote">{t('table.logEmpty')}</p> : null}
        </div>
      </section>

      <div className="result-footer">
        <span>{t('pdf.footer')}</span>
        <Button variant="ghost" size="sm" onClick={() => void downloadPdf()} icon={<Download size={15} />}>{t('result.downloadPdf')}</Button>
      </div>
    </div>
  );
}
