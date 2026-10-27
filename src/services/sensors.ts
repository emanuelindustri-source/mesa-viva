import { useCallback, useEffect, useRef, useState } from 'react';

type PermissionState = 'unknown' | 'granted' | 'denied' | 'unsupported';

interface ShakeOptions {
  enabled: boolean;
  onShake: (intensity: number) => void;
  threshold?: number;
  cooldownMs?: number;
}

interface MotionEventLike extends Event {
  accelerationIncludingGravity?: { x: number | null; y: number | null; z: number | null };
  acceleration?: { x: number | null; y: number | null; z: number | null };
}

export function useShakeDetector({ enabled, onShake, threshold = 20, cooldownMs = 1100 }: ShakeOptions) {
  const [supported, setSupported] = useState(false);
  const [permission, setPermission] = useState<PermissionState>('unknown');
  const lastShake = useRef(0);
  const lastMagnitude = useRef(0);
  const onShakeRef = useRef(onShake);
  onShakeRef.current = onShake;

  useEffect(() => {
    setSupported(
      typeof window !== 'undefined' &&
      ('DeviceMotionEvent' in window || Boolean((window as unknown as { MesaViva?: { vibrate?: unknown } }).MesaViva?.vibrate)),
    );
  }, []);

  const requestPermission = useCallback(async (): Promise<PermissionState> => {
    if (typeof window === 'undefined') return 'unsupported';
    const ctor = window.DeviceMotionEvent as unknown as { requestPermission?: () => Promise<PermissionState> };
    if (!('DeviceMotionEvent' in window)) {
      setPermission('unsupported');
      return 'unsupported';
    }
    if (typeof ctor.requestPermission !== 'function') {
      setPermission('granted');
      return 'granted';
    }
    try {
      const result = await ctor.requestPermission();
      setPermission(result);
      return result;
    } catch {
      setPermission('denied');
      return 'denied';
    }
  }, []);

  useEffect(() => {
    if (!enabled || typeof window === 'undefined') return;
    const nativeHandler = (event: Event) => {
      const detail = (event as CustomEvent<{ intensity?: number }>).detail;
      const now = Date.now();
      if (now - lastShake.current < cooldownMs) return;
      lastShake.current = now;
      const intensity = Math.min(1, Math.max(0.35, detail?.intensity ?? 0.8));
      onShakeRef.current(intensity);
    };
    window.addEventListener('mesaviva:shake', nativeHandler);
    if (!('DeviceMotionEvent' in window)) {
      return () => window.removeEventListener('mesaviva:shake', nativeHandler);
    }
    const handler = (event: Event) => {
      const motion = event as MotionEventLike;
      const acc = motion.accelerationIncludingGravity;
      const linear = motion.acceleration;
      if (!acc && !linear) return;
      const x = acc?.x ?? 0;
      const y = acc?.y ?? 0;
      const z = acc?.z ?? 0;
      const magnitude = Math.sqrt(x * x + y * y + z * z);
      const jerk = Math.abs(magnitude - lastMagnitude.current);
      lastMagnitude.current = magnitude;
      const now = Date.now();
      if (now - lastShake.current < cooldownMs) return;
      if (magnitude > threshold || jerk > threshold * 0.9) {
        lastShake.current = now;
        const intensity = Math.min(1, Math.max(0.35, magnitude / 45));
        onShakeRef.current(intensity);
      }
    };
    window.addEventListener('devicemotion', handler, { passive: true });
    return () => {
      window.removeEventListener('mesaviva:shake', nativeHandler);
      window.removeEventListener('devicemotion', handler);
    };
  }, [enabled, threshold, cooldownMs]);
  return { supported, permission, requestPermission };
}
