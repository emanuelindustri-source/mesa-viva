import { useEffect, useRef, useState } from 'react';

interface BeforeInstallPromptEvent extends Event {
  prompt: () => Promise<void>;
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed' }>;
}

export function usePwa() {
  const [installEvent, setInstallEvent] = useState<BeforeInstallPromptEvent | null>(null);
  const [installed, setInstalled] = useState(false);
  const [online, setOnline] = useState(typeof navigator === 'undefined' ? true : navigator.onLine);

  useEffect(() => {
    const onPrompt = (event: Event) => {
      event.preventDefault();
      setInstallEvent(event as BeforeInstallPromptEvent);
    };
    const onInstalled = () => {
      setInstalled(true);
      setInstallEvent(null);
    };
    const onOnline = () => setOnline(true);
    const onOffline = () => setOnline(false);
    setInstalled(window.matchMedia('(display-mode: standalone)').matches);
    window.addEventListener('beforeinstallprompt', onPrompt);
    window.addEventListener('appinstalled', onInstalled);
    window.addEventListener('online', onOnline);
    window.addEventListener('offline', onOffline);
    return () => {
      window.removeEventListener('beforeinstallprompt', onPrompt);
      window.removeEventListener('appinstalled', onInstalled);
      window.removeEventListener('online', onOnline);
      window.removeEventListener('offline', onOffline);
    };
  }, []);

  const install = async (): Promise<boolean> => {
    if (!installEvent) return false;
    await installEvent.prompt();
    const choice = await installEvent.userChoice;
    if (choice.outcome === 'accepted') {
      setInstalled(true);
      setInstallEvent(null);
      return true;
    }
    return false;
  };

  return { canInstall: Boolean(installEvent), installed, install, online };
}

export async function shareText(title: string, text: string, url?: string): Promise<boolean> {
  if (typeof navigator === 'undefined') return false;
  const data = { title, text, ...(url ? { url } : {}) };
  if (navigator.share) {
    try {
      await navigator.share(data);
      return true;
    } catch {
      return false;
    }
  }
  try {
    await navigator.clipboard.writeText(url ? `${text}\n${url}` : text);
    return true;
  } catch {
    return false;
  }
}

export async function copyText(text: string): Promise<boolean> {
  try {
    await navigator.clipboard.writeText(text);
    return true;
  } catch {
    return false;
  }
}

export function useWakeLock(enabled: boolean) {
  const lock = useRef<{ release: () => Promise<void> } | null>(null);
  useEffect(() => {
    let cancelled = false;
    const request = async () => {
      if (!enabled || typeof navigator === 'undefined' || !('wakeLock' in navigator)) return;
      try {
        const sentinel = await (navigator as Navigator & {
          wakeLock: { request: (type: 'screen') => Promise<{ release: () => Promise<void> }> };
        }).wakeLock.request('screen');
        if (!cancelled) lock.current = sentinel;
      } catch {
        // No es crítico.
      }
    };
    const onVisibility = () => {
      if (document.visibilityState === 'visible') void request();
    };
    void request();
    document.addEventListener('visibilitychange', onVisibility);
    return () => {
      cancelled = true;
      document.removeEventListener('visibilitychange', onVisibility);
      void lock.current?.release().catch(() => undefined);
      lock.current = null;
    };
  }, [enabled]);
}
