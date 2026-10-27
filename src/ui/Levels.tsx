import { motion } from 'framer-motion';
import { Crown, Flame, Gamepad2, Lock, Star, Target, Trophy, Zap } from 'lucide-react';
import { LEVELS, levelForXp, xpProgress } from '../core/levels';
import { useAppStore } from '../store/appStore';
import { useT } from '../i18n/useT';
import { Avatar, Chip, PageHeader, ProgressBar, SectionTitle } from './components';
import { navigate } from '../app/router';

export function LevelsScreen() {
  const t = useT();
  const profile = useAppStore((s) => s.profile);
  const gameStats = useAppStore((s) => s.gameStats);
  const level = levelForXp(profile.xp);
  const progress = xpProgress(profile.xp);
  const played = Object.values(gameStats).reduce((total, stat) => total + (stat?.played ?? 0), 0);

  return (
    <div className="screen levels-screen">
      <PageHeader
        title={t('levels.title')}
        subtitle={t('levels.subtitle')}
        onBack={() => navigate('/')}
        actions={<Chip tone="gold">{profile.xp} XP</Chip>}
      />

      <section className="levels-hero">
        <div className="levels-hero-main">
          <Avatar id={profile.avatar} size={64} ring />
          <div>
            <span className="eyebrow">{t('levels.current')}</span>
            <h2>{t(level.titleKey)}</h2>
            <span className="levels-num">{level.icon} {t('levels.level', { n: level.level })} · {t('levels.reach', { xp: level.minXp })}</span>
          </div>
        </div>
        <div className="levels-progress">
          <div className="levels-progress-labels">
            <span>{t(level.titleKey)}</span>
            <span>{profile.xp} XP</span>
          </div>
          <ProgressBar value={progress.ratio} />
          <span className="levels-next">{t('levels.xpToNext', { xp: Math.max(0, progress.needed - progress.current) })}</span>
        </div>
        <div className="levels-stats">
          <div><Trophy size={17} /><strong>{profile.wins}</strong><span>{t('levels.wins')}</span></div>
          <div><Gamepad2 size={17} /><strong>{played}</strong><span>{t('levels.matches')}</span></div>
          <div><Flame size={17} /><strong>{profile.streak}</strong><span>{t('levels.streak')}</span></div>
          <div><Star size={17} /><strong>{profile.bestStreak}</strong><span>{t('levels.bestStreak')}</span></div>
        </div>
      </section>

      <section className="levels-path">
        <SectionTitle eyebrow={t('levels.rewards')} title={t('levels.rewardTitle')} />
        <p className="levels-desc">{t('levels.rewardDesc')}</p>
        <div className="level-timeline">
          {LEVELS.map((item, index) => {
            const unlocked = profile.xp >= item.minXp;
            const current = item.level === level.level;
            return (
              <motion.div
                key={item.level}
                className={`level-step ${unlocked ? 'is-unlocked' : 'is-locked'} ${current ? 'is-current' : ''}`}
                initial={{ opacity: 0, x: -10 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: index * 0.03 }}
              >
                <div className="level-step-marker">
                  <span>{unlocked ? item.icon : <Lock size={16} />}</span>
                  {index < LEVELS.length - 1 ? <i /> : null}
                </div>
                <div className="level-step-card">
                  <div className="level-step-head">
                    <span className="level-step-num">{t('levels.level', { n: item.level })}</span>
                    {current ? <Chip tone="gold">{t('levels.current')}</Chip> : unlocked ? <Chip tone="green">{t('levels.unlocked')}</Chip> : <Chip tone="default"><Lock size={11} /> {t('levels.locked')}</Chip>}
                  </div>
                  <h3>{t(item.titleKey)}</h3>
                  <div className="level-step-meta">
                    <span><Target size={13} /> {t('levels.reach', { xp: item.minXp })}</span>
                    <span><Zap size={13} /> {t(item.rewardKey)}</span>
                  </div>
                  {current ? <div className="level-step-bar"><ProgressBar value={progress.ratio} /></div> : null}
                </div>
              </motion.div>
            );
          })}
        </div>
      </section>

      <section className="levels-rules">
        <div className="levels-rules-icon"><Crown size={22} /></div>
        <div>
          <h3>{t('levels.xpRules')}</h3>
          <p>{t('levels.xpRulesDesc')}</p>
        </div>
      </section>
    </div>
  );
}
