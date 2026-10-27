import { useEffect, useMemo, useState } from 'react';
import { useAppStore } from '../store/appStore';
import {
  fetchSystemAccent,
  hexToRgb,
  readableInk,
  resolveFestival,
  type Festival,
  type FestivalId,
} from './festivals';

function useMediaDark(): boolean {
  const [dark, setDark] = useState(() =>
    typeof window !== 'undefined' ? window.matchMedia('(prefers-color-scheme: dark)').matches : true,
  );
  useEffect(() => {
    if (typeof window === 'undefined') return;
    const media = window.matchMedia('(prefers-color-scheme: dark)');
    const handler = (event: MediaQueryListEvent) => setDark(event.matches);
    media.addEventListener('change', handler);
    return () => media.removeEventListener('change', handler);
  }, []);
  return dark;
}

export interface ThemeState {
  festival: Festival;
  accent: string;
  isDark: boolean;
}

export function useFestivalTheme(): ThemeState {
  const settings = useAppStore((s) => s.settings);
  const prefersDark = useMediaDark();
  const [systemAccent, setSystemAccent] = useState<string | null>(null);
  const [date, setDate] = useState(() => new Date());

  useEffect(() => {
    const refresh = () => setSystemAccent(fetchSystemAccent());
    refresh();
    window.addEventListener('mesaviva:system-accent', refresh);
    const id = window.setTimeout(refresh, 900);
    return () => {
      window.removeEventListener('mesaviva:system-accent', refresh);
      window.clearTimeout(id);
    };
  }, []);

  useEffect(() => {
    const id = window.setInterval(() => setDate(new Date()), 30 * 60 * 1000);
    return () => window.clearInterval(id);
  }, []);

  const festival = useMemo(
    () =>
      resolveFestival(
        date,
        settings.country,
        settings.festival === 'auto' ? 'auto' : (settings.festival as FestivalId),
      ),
    [date, settings.country, settings.festival],
  );

  const accent = useMemo(() => {
    if (settings.accentSource === 'custom' && settings.accent) return settings.accent;
    if (settings.accentSource === 'wallpaper' && settings.wallpaperColor) return settings.wallpaperColor;
    if (settings.accentSource === 'system' && systemAccent) return systemAccent;
    return festival.accent;
  }, [settings.accentSource, settings.accent, settings.wallpaperColor, systemAccent, festival.accent]);

  const isDark = settings.theme === 'dark' || (settings.theme === 'auto' && prefersDark);

  useEffect(() => {
    if (typeof document === 'undefined') return;
    const root = document.documentElement;
    root.dataset.theme = isDark ? 'dark' : 'light';
    root.dataset.festival = festival.id;
    root.dataset.contrast = settings.highContrast ? 'high' : 'normal';
    root.dataset.motion = settings.reduceMotion ? 'reduced' : 'full';
    const rgb = hexToRgb(accent);
    root.style.setProperty('--accent', accent);
    root.style.setProperty('--accent-2', festival.accent2);
    root.style.setProperty('--accent-ink', readableInk(accent));
    root.style.setProperty('--accent-rgb', `${rgb.r}, ${rgb.g}, ${rgb.b}`);
    root.style.setProperty('--fest-bg', festival.bg);
    const meta = document.querySelector('meta[name="theme-color"]');
    if (meta) meta.setAttribute('content', isDark ? festival.bg : '#f4efe2');
  }, [accent, festival, isDark, settings.highContrast, settings.reduceMotion]);

  return { festival, accent, isDark };
}
