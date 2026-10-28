import { useEffect, useRef } from 'react';
import { subscribeBurst, type BurstEvent, type BurstKind } from './bus';

interface Particle {
  x: number;
  y: number;
  vx: number;
  vy: number;
  life: number;
  maxLife: number;
  size: number;
  color: string;
  kind: BurstKind;
  rotation: number;
  spin: number;
  gravity: number;
  drag: number;
}

const AMBIENT_SPAWN: Record<BurstKind, { rate: number; speed: number; size: number }> = {
  snow: { rate: 420, speed: 0.5, size: 3 },
  ember: { rate: 520, speed: 0.4, size: 2.6 },
  petal: { rate: 700, speed: 0.6, size: 4 },
  confetti: { rate: 900, speed: 0.5, size: 3 },
  star: { rate: 600, speed: 0.3, size: 2 },
  leaves: { rate: 800, speed: 0.5, size: 4 },
  dust: { rate: 0, speed: 0.25, size: 2 },
  spark: { rate: 0, speed: 1, size: 2 },
  clover: { rate: 900, speed: 0.4, size: 3 },
};

const PARTICLE_GRAVITY: Record<BurstKind, number> = {
  dust: 0.03,
  confetti: 0.09,
  spark: 0.02,
  snow: 0.006,
  ember: -0.025,
  petal: 0.018,
  star: -0.01,
  clover: 0.012,
  leaves: 0.012,
};

export function ParticleCanvas({ ambient, colors }: { ambient?: BurstKind; colors: string[] }) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const particles = useRef<Particle[]>([]);
  const raf = useRef<number | null>(null);
  const ambientRef = useRef<BurstKind | undefined>(ambient);
  const colorsRef = useRef(colors);
  const lastAmbient = useRef(0);
  const visible = useRef(true);

  ambientRef.current = ambient;
  colorsRef.current = colors;

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    const dpr = Math.min(1.5, window.devicePixelRatio || 1);
    const resize = () => {
      canvas.width = Math.floor(window.innerWidth * dpr);
      canvas.height = Math.floor(window.innerHeight * dpr);
      canvas.style.width = `${window.innerWidth}px`;
      canvas.style.height = `${window.innerHeight}px`;
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    };
    resize();
    window.addEventListener('resize', resize);
    const onVisibility = () => {
      visible.current = document.visibilityState === 'visible';
      if (visible.current) loop(performance.now());
    };
    document.addEventListener('visibilitychange', onVisibility);

    const spawn = (event: BurstEvent) => {
      const count = Math.min(40, event.count, Math.max(0, 40 - particles.current.length));
      for (let i = 0; i < count; i += 1) {
        const angle = Math.random() * Math.PI * 2;
        const speed = (event.kind === 'confetti' ? 3.5 : event.kind === 'spark' ? 4.5 : 1.6) * event.power * (0.4 + Math.random());
        particles.current.push({
          x: event.x + (Math.random() - 0.5) * 24,
          y: event.y + (Math.random() - 0.5) * 16,
          vx: Math.cos(angle) * speed,
          vy: Math.sin(angle) * speed - (event.kind === 'ember' ? 1.4 : 0.8),
          life: 0,
          maxLife: 500 + Math.random() * 700,
          size: (event.kind === 'confetti' ? 4 : event.kind === 'dust' ? 2.4 : 3) * (0.6 + Math.random()),
          color: event.colors[Math.floor(Math.random() * event.colors.length)] ?? '#e0a63a',
          kind: event.kind,
          rotation: Math.random() * Math.PI * 2,
          spin: (Math.random() - 0.5) * 0.25,
          gravity: PARTICLE_GRAVITY[event.kind] ?? 0.03,
          drag: event.kind === 'confetti' ? 0.985 : 0.972,
        });
      }
      if (raf.current === null) loop(performance.now());
    };
    const unsubscribe = subscribeBurst(spawn);
    let last = performance.now();
    loop(performance.now());

    function loop(now: number) {
      raf.current = null;
      if (!ctx) return;
      if (!visible.current) return;
      const dt = Math.min(32, now - last) / 16.67;
      last = now;

      // Ambiente sutil.
      const kind = ambientRef.current;
      if (kind && kind !== 'dust' && kind !== 'spark') {
        const config = AMBIENT_SPAWN[kind];
        if (config.rate > 0 && now - lastAmbient.current > config.rate && particles.current.length < 40) {
          lastAmbient.current = now;
          const x = Math.random() * window.innerWidth;
          const y = kind === 'ember' ? window.innerHeight + 10 : -10;
          particles.current.push({
            x,
            y,
            vx: (Math.random() - 0.5) * config.speed,
            vy: kind === 'ember' ? -config.speed * 2 : config.speed,
            life: 0,
            maxLife: 7000 + Math.random() * 6000,
            size: config.size * (0.6 + Math.random()),
            color: colorsRef.current[Math.floor(Math.random() * colorsRef.current.length)] ?? '#e0a63a',
            kind,
            rotation: Math.random() * Math.PI * 2,
            spin: (Math.random() - 0.5) * 0.05,
            gravity: kind === 'ember' ? -0.002 : 0.001,
            drag: 1,
          });
        }
      }

      ctx.clearRect(0, 0, window.innerWidth, window.innerHeight);
      const alive: Particle[] = [];
      for (const p of particles.current) {
        p.life += dt * 16.67;
        p.x += p.vx * dt;
        p.y += p.vy * dt;
        p.vy += p.gravity * dt;
        p.vx *= p.drag;
        p.vy *= p.drag;
        p.rotation += p.spin * dt;
        const t = p.life / p.maxLife;
        if (t >= 1 || p.y > window.innerHeight + 40 || p.x < -60 || p.x > window.innerWidth + 60) continue;
        alive.push(p);
        const alpha = Math.min(1, t < 0.1 ? t / 0.1 : 1 - (t - 0.1) / 0.9);
        ctx.save();
        ctx.globalAlpha = Math.max(0, alpha);
        ctx.translate(p.x, p.y);
        ctx.rotate(p.rotation);
        ctx.fillStyle = p.color;
        if (p.kind === 'confetti') {
          ctx.fillRect(-p.size / 2, -p.size / 2, p.size, p.size * 0.55);
        } else if (p.kind === 'spark') {
          ctx.fillRect(-p.size / 2, -0.7, p.size * 2.2, 1.4);
        } else if (p.kind === 'petal') {
          ctx.beginPath();
          ctx.ellipse(0, 0, p.size, p.size * 0.55, 0, 0, Math.PI * 2);
          ctx.fill();
        } else if (p.kind === 'star') {
          ctx.beginPath();
          for (let i = 0; i < 4; i += 1) {
            const a = (i / 4) * Math.PI * 2;
            ctx.lineTo(Math.cos(a) * p.size, Math.sin(a) * p.size);
            const b = a + Math.PI / 4;
            ctx.lineTo(Math.cos(b) * p.size * 0.35, Math.sin(b) * p.size * 0.35);
          }
          ctx.closePath();
          ctx.fill();
        } else {
          ctx.beginPath();
          ctx.arc(0, 0, p.size, 0, Math.PI * 2);
          ctx.fill();
        }
        ctx.restore();
      }
      particles.current = alive;
      const hasAmbient = Boolean(ambientRef.current && ambientRef.current !== 'dust' && ambientRef.current !== 'spark');
      if (alive.length > 0 || hasAmbient) {
        raf.current = requestAnimationFrame(loop);
      }
    }

    return () => {
      window.removeEventListener('resize', resize);
      document.removeEventListener('visibilitychange', onVisibility);
      unsubscribe();
      if (raf.current !== null) cancelAnimationFrame(raf.current);
      raf.current = null;
    };
  }, []);

  return <canvas ref={canvasRef} className="particle-canvas" aria-hidden="true" />;
}
