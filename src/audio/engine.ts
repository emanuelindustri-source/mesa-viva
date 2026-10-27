import type { Festival } from '../theme/festivals';

type Wave = OscillatorType;

interface AudioState {
  ctx: AudioContext | null;
  master: GainNode | null;
  music: GainNode | null;
  sfx: GainNode | null;
  noise: AudioBuffer | null;
  timer: number | null;
  beat: number;
  festival: Festival | null;
  gameId: string | null;
  musicEnabled: boolean;
  sfxEnabled: boolean;
  volume: number;
}

const state: AudioState = {
  ctx: null,
  master: null,
  music: null,
  sfx: null,
  noise: null,
  timer: null,
  beat: 0,
  festival: null,
  gameId: null,
  musicEnabled: true,
  sfxEnabled: true,
  volume: 0.6,
};

function midi(note: number): number {
  return 440 * 2 ** ((note - 69) / 12);
}

function makeNoise(ctx: AudioContext): AudioBuffer {
  const buffer = ctx.createBuffer(1, ctx.sampleRate * 0.6, ctx.sampleRate);
  const data = buffer.getChannelData(0);
  for (let i = 0; i < data.length; i += 1) {
    data[i] = (Math.random() * 2 - 1) * (1 - i / data.length);
  }
  return buffer;
}

export function audioReady(): boolean {
  return Boolean(state.ctx && state.ctx.state === 'running');
}

export async function initAudio(): Promise<boolean> {
  if (typeof window === 'undefined') return false;
  const Ctor = window.AudioContext ?? (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext;
  if (!Ctor) return false;
  if (!state.ctx) {
    const ctx = new Ctor();
    const master = ctx.createGain();
    master.gain.value = 0.85;
    master.connect(ctx.destination);
    const music = ctx.createGain();
    music.gain.value = 0;
    music.connect(master);
    const sfx = ctx.createGain();
    sfx.gain.value = state.sfxEnabled ? 0.7 : 0;
    sfx.connect(master);
    state.ctx = ctx;
    state.master = master;
    state.music = music;
    state.sfx = sfx;
    state.noise = makeNoise(ctx);
  }
  try {
    if (state.ctx.state !== 'running') await state.ctx.resume();
    return state.ctx.state === 'running';
  } catch {
    return false;
  }
}

export function setAudioSettings(settings: { music: boolean; sfx: boolean; volume: number }): void {
  state.musicEnabled = settings.music;
  state.sfxEnabled = settings.sfx;
  state.volume = Math.max(0, Math.min(1, settings.volume));
  if (state.sfx) state.sfx.gain.value = settings.sfx ? 0.7 * state.volume : 0;
  if (state.music) state.music.gain.value = settings.music && state.timer ? 0.2 * state.volume : 0;
  if (!settings.music) stopMusic();
}

function tone(options: {
  freq: number;
  dur?: number;
  type?: Wave;
  gain?: number;
  when?: number;
  slideTo?: number;
  attack?: number;
  release?: number;
  destination?: GainNode | null;
  filter?: number;
  detune?: number;
}): void {
  const ctx = state.ctx;
  const dest = options.destination ?? state.music;
  if (!ctx || !dest) return;
  const when = options.when ?? ctx.currentTime;
  const dur = options.dur ?? 0.35;
  const osc = ctx.createOscillator();
  const gain = ctx.createGain();
  osc.type = options.type ?? 'sine';
  osc.frequency.setValueAtTime(options.freq, when);
  if (options.detune) osc.detune.value = options.detune;
  if (options.slideTo) osc.frequency.exponentialRampToValueAtTime(Math.max(20, options.slideTo), when + dur);
  const attack = options.attack ?? 0.02;
  const release = options.release ?? 0.18;
  const peak = Math.max(0.0001, options.gain ?? 0.2);
  gain.gain.setValueAtTime(0.0001, when);
  gain.gain.exponentialRampToValueAtTime(peak, when + attack);
  gain.gain.exponentialRampToValueAtTime(0.0001, when + dur + release);
  let node: AudioNode = osc;
  if (options.filter) {
    const filter = ctx.createBiquadFilter();
    filter.type = 'lowpass';
    filter.frequency.value = options.filter;
    osc.connect(filter);
    node = filter;
  }
  node.connect(gain);
  gain.connect(dest);
  osc.start(when);
  osc.stop(when + dur + release + 0.05);
}

function noiseBurst(options: { dur?: number; gain?: number; when?: number; freq?: number; q?: number; destination?: GainNode | null }): void {
  const ctx = state.ctx;
  const dest = options.destination ?? state.sfx;
  if (!ctx || !dest || !state.noise) return;
  const when = options.when ?? ctx.currentTime;
  const source = ctx.createBufferSource();
  source.buffer = state.noise;
  const filter = ctx.createBiquadFilter();
  filter.type = 'bandpass';
  filter.frequency.value = options.freq ?? 1800;
  filter.Q.value = options.q ?? 0.8;
  const gain = ctx.createGain();
  const dur = options.dur ?? 0.12;
  gain.gain.setValueAtTime(options.gain ?? 0.15, when);
  gain.gain.exponentialRampToValueAtTime(0.0001, when + dur);
  source.connect(filter);
  filter.connect(gain);
  gain.connect(dest);
  source.start(when);
  source.stop(when + dur + 0.02);
}

const SCALES: Record<string, number[]> = {
  eerie: [57, 60, 62, 64, 67, 68, 71, 72],
  warm: [60, 62, 64, 67, 69, 72, 74, 76],
  party: [55, 57, 59, 62, 64, 67, 69, 71],
  serene: [62, 64, 66, 69, 71, 74, 76, 78],
  patriotic: [60, 62, 64, 65, 67, 69, 71, 72],
  sacred: [52, 55, 57, 59, 62, 64, 67, 69],
  neutral: [57, 60, 62, 64, 67, 69, 72, 74],
};

const ROOTS = [45, 41, 48, 43];

function scheduleBeat(): void {
  const ctx = state.ctx;
  const fest = state.festival;
  if (!ctx || !fest || !state.music || state.musicEnabled === false) return;
  const scale = SCALES[fest.music] ?? SCALES['neutral'] ?? [57, 60, 62, 64, 67, 69, 72, 74];
  const beat = state.beat;
  const step = beat % 8;
  const bar = Math.floor(beat / 8) % 4;
  const root = ROOTS[bar] ?? 45;
  const tempo = state.gameId === 'furia' ? 0.38 : state.gameId === 'dados' ? 0.4 : state.gameId === 'ajedrez' ? 0.62 : 0.5;
  const now = ctx.currentTime + 0.06;

  if (!state.musicEnabled) return;
  // Bajo cada dos compases.
  if (step === 0) {
    tone({ freq: midi(root - 12), dur: tempo * 2.4, type: 'sine', gain: 0.09, when: now, destination: state.music, filter: 320 });
    tone({ freq: midi(root), dur: tempo * 1.8, type: 'triangle', gain: 0.035, when: now, destination: state.music, filter: 700 });
  }
  // Pad armónico suave.
  if (step === 0 && (fest.music === 'sacred' || fest.music === 'serene' || fest.music === 'warm')) {
    const chord = [0, 4, 7].map((n) => midi(root + n + 12));
    for (const f of chord) {
      tone({ freq: f, dur: tempo * 3.4, type: 'sine', gain: 0.018, when: now, destination: state.music, filter: 1200 });
    }
  }
  // Melodía.
  const melodyChance = fest.music === 'eerie' ? 0.46 : 0.66;
  if (Math.random() < melodyChance) {
    const note = scale[Math.floor(Math.random() * scale.length)] ?? 60;
    const octave = Math.random() < 0.22 ? 12 : 0;
    const wave: Wave = fest.music === 'eerie' ? 'sine' : fest.music === 'party' ? 'square' : 'triangle';
    tone({
      freq: midi(note + octave),
      dur: tempo * (Math.random() < 0.2 ? 1.4 : 0.65),
      type: wave,
      gain: fest.music === 'party' ? 0.035 : 0.05,
      when: now,
      destination: state.music,
      filter: 2600,
    });
  }
  // Percusión.
  if (fest.music === 'party' || fest.music === 'patriotic') {
    if (step === 0 || step === 3 || step === 6) {
      tone({ freq: 58, slideTo: 36, dur: 0.18, type: 'sine', gain: 0.18, when: now, destination: state.music });
    }
    if (step % 2 === 1) {
      noiseBurst({ dur: 0.05, gain: 0.05, freq: 5200, q: 0.6, when: now, destination: state.music });
    }
  }
  if (fest.music === 'eerie' && Math.random() < 0.08) {
    tone({ freq: Math.random() * 300 + 90, dur: 0.8, type: 'sawtooth', gain: 0.012, when: now, destination: state.music, filter: 500, slideTo: 60 });
  }
  state.beat += 1;
}

export function startMusic(festival: Festival, gameId: string | null): void {
  if (!state.ctx || !state.music || !state.musicEnabled) return;
  if (state.timer && state.festival?.id === festival.id && state.gameId === gameId) return;
  stopMusic();
  state.festival = festival;
  state.gameId = gameId;
  state.beat = 0;
  state.music.gain.cancelScheduledValues(state.ctx.currentTime);
  state.music.gain.setValueAtTime(0.0001, state.ctx.currentTime);
  state.music.gain.exponentialRampToValueAtTime(0.2 * state.volume, state.ctx.currentTime + 0.8);
  state.timer = window.setInterval(scheduleBeat, 500);
  scheduleBeat();
}

export function stopMusic(): void {
  if (state.timer !== null) {
    window.clearInterval(state.timer);
    state.timer = null;
  }
  state.festival = null;
  state.gameId = null;
  if (state.ctx && state.music) {
    try {
      state.music.gain.cancelScheduledValues(state.ctx.currentTime);
      state.music.gain.setTargetAtTime(0.0001, state.ctx.currentTime, 0.15);
    } catch {
      // Ignorar.
    }
  }
}

export type SfxName =
  | 'tap' | 'select' | 'card' | 'dice' | 'capture' | 'win' | 'error' | 'canto' | 'level' | 'deal' | 'reveal';

export function sfx(name: SfxName): void {
  if (!state.ctx || !state.sfxEnabled) return;
  const now = state.ctx.currentTime;
  switch (name) {
    case 'tap':
      tone({ freq: 780, dur: 0.05, type: 'sine', gain: 0.05, when: now, destination: state.sfx });
      break;
    case 'select':
      tone({ freq: 520, dur: 0.08, type: 'triangle', gain: 0.07, when: now, destination: state.sfx });
      break;
    case 'card':
      noiseBurst({ dur: 0.09, gain: 0.08, freq: 2100, q: 0.7 });
      tone({ freq: 280, dur: 0.08, type: 'sine', gain: 0.06, when: now, destination: state.sfx });
      break;
    case 'dice':
      for (let i = 0; i < 4; i += 1) {
        noiseBurst({ dur: 0.06, gain: 0.1 - i * 0.015, freq: 2400 + i * 300, q: 1.2, when: now + i * 0.09 });
      }
      break;
    case 'capture':
      [0, 4, 7, 12].forEach((n, i) => {
        tone({ freq: midi(60 + n), dur: 0.16, type: 'triangle', gain: 0.08, when: now + i * 0.055, destination: state.sfx });
      });
      break;
    case 'canto':
      [0, 5, 9, 12, 16].forEach((n, i) => {
        tone({ freq: midi(57 + n), dur: 0.22, type: 'sawtooth', gain: 0.055, when: now + i * 0.07, destination: state.sfx, filter: 1800 });
      });
      break;
    case 'win':
      [0, 4, 7, 12, 7, 12, 16].forEach((n, i) => {
        tone({ freq: midi(60 + n), dur: 0.3, type: 'triangle', gain: 0.09, when: now + i * 0.12, destination: state.sfx });
      });
      break;
    case 'level':
      [0, 7, 12, 19].forEach((n, i) => {
        tone({ freq: midi(67 + n), dur: 0.4, type: 'sine', gain: 0.08, when: now + i * 0.1, destination: state.sfx });
      });
      break;
    case 'error':
      tone({ freq: 180, slideTo: 90, dur: 0.25, type: 'square', gain: 0.05, when: now, destination: state.sfx });
      break;
    case 'deal':
      for (let i = 0; i < 5; i += 1) {
        noiseBurst({ dur: 0.05, gain: 0.06, freq: 1800 + i * 100, q: 0.9, when: now + i * 0.06 });
      }
      break;
    case 'reveal':
      noiseBurst({ dur: 0.15, gain: 0.07, freq: 900, q: 0.8, when: now });
      tone({ freq: 330, slideTo: 660, dur: 0.25, type: 'sine', gain: 0.06, when: now, destination: state.sfx });
      break;
  }
}

export function sfxForLog(key: string): SfxName | null {
  switch (key) {
    case 'log.played':
    case 'log.dominoPlay':
    case 'log.chessMove':
    case 'log.checkerMove':
      return 'card';
    case 'log.roll':
    case 'log.bid':
      return 'dice';
    case 'log.reveal':
      return 'reveal';
    case 'log.caida':
    case 'log.caidaMesa':
    case 'log.checkerCapture':
    case 'log.mancalaCapture':
      return 'capture';
    case 'log.canto':
    case 'log.cantoWin':
    case 'log.won':
    case 'log.chessMate':
    case 'log.checkerPromote':
    case 'log.lastCard':
      return 'canto';
    case 'log.matchEnd':
      return 'win';
    case 'log.snake':
    case 'log.eliminated':
    case 'log.resign':
      return 'error';
    case 'log.ladder':
    case 'log.extraTurn':
      return 'level';
    default:
      return null;
  }
}
