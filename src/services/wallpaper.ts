/**
 * Extrae el color dominante "vivo" de una imagen local usando un canvas.
 * No sube nada a ningún servidor.
 */
export async function extractWallpaperAccent(file: File): Promise<string> {
  const bitmap = await createImageBitmap(file);
  const size = 64;
  const canvas = document.createElement('canvas');
  canvas.width = size;
  canvas.height = size;
  const ctx = canvas.getContext('2d', { willReadFrequently: true });
  if (!ctx) throw new Error('Canvas no disponible');
  ctx.drawImage(bitmap, 0, 0, size, size);
  const { data } = ctx.getImageData(0, 0, size, size);
  const buckets = new Map<string, { score: number; r: number; g: number; b: number; count: number }>();
  for (let i = 0; i < data.length; i += 16) {
    const r = data[i] ?? 0;
    const g = data[i + 1] ?? 0;
    const b = data[i + 2] ?? 0;
    const max = Math.max(r, g, b);
    const min = Math.min(r, g, b);
    const sat = max === 0 ? 0 : (max - min) / max;
    const lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255;
    if (lum < 0.12 || lum > 0.92) continue;
    const qr = Math.round(r / 24) * 24;
    const qg = Math.round(g / 24) * 24;
    const qb = Math.round(b / 24) * 24;
    const key = `${qr},${qg},${qb}`;
    const current = buckets.get(key) ?? { score: 0, r: 0, g: 0, b: 0, count: 0 };
    current.score += 1 + sat * 5 + (1 - Math.abs(lum - 0.5)) * 2;
    current.r += r;
    current.g += g;
    current.b += b;
    current.count += 1;
    buckets.set(key, current);
  }
  let best: { score: number; r: number; g: number; b: number; count: number } | null = null;
  for (const bucket of buckets.values()) {
    if (!best || bucket.score > best.score) best = bucket;
  }
  if (!best) return '#e0a63a';
  const r = Math.round(best.r / best.count);
  const g = Math.round(best.g / best.count);
  const b = Math.round(best.b / best.count);
  return `#${[r, g, b].map((v) => Math.max(0, Math.min(255, v)).toString(16).padStart(2, '0')).join('')}`;
}
