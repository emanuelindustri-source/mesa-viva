import { jsPDF } from 'jspdf';
import { formatLog } from '../i18n';
import { formatDuration } from '../core/utils';
import { APP_NAME, CONTACT_EMAIL, APP_VERSION } from '../core/constants';
import type { GameView, LogEvent, MatchResult, PlayerView } from '../core/types';

export interface PdfOptions {
  view: GameView;
  result: MatchResult;
  log: LogEvent[];
  gameName: string;
  startedAt: number;
  durationMs: number;
  translate: (key: string, params?: Record<string, unknown>) => string;
  accent: string;
  accent2: string;
  locale: string;
}

function hexRgb(hex: string): [number, number, number] {
  const clean = hex.replace('#', '');
  const full = clean.length === 3 ? clean.split('').map((c) => c + c).join('') : clean;
  const n = Number.parseInt(full, 16);
  return [(n >> 16) & 255, (n >> 8) & 255, n & 255];
}

function safe(text: string): string {
  // jsPDF con Helvetica cubre latín; sustituimos lo que no puede representar.
  return text.replace(/[^\u0000-\u00ff]/g, '·');
}

export async function generateMatchPdf(options: PdfOptions): Promise<void> {
  const { view, result, log, gameName, startedAt, durationMs, translate: t, accent, accent2 } = options;
  const doc = new jsPDF({ unit: 'mm', format: 'a4', compress: true });
  const pageW = 210;
  const pageH = 297;
  const margin = 16;
  const [ar, ag, ab] = hexRgb(accent);
  const [br, bg, bb] = hexRgb(accent2);

  // Fondo superior tipo infografía.
  doc.setFillColor(8, 26, 22);
  doc.rect(0, 0, pageW, 74, 'F');
  doc.setFillColor(ar, ag, ab);
  doc.circle(184, 16, 26, 'F');
  doc.setFillColor(br, bg, bb);
  doc.circle(196, 44, 10, 'F');
  doc.setFillColor(255, 244, 214);
  doc.roundedRect(margin, 16, 38, 6, 3, 3, 'F');

  doc.setFont('helvetica', 'bold');
  doc.setFontSize(22);
  doc.setTextColor(255, 250, 240);
  doc.text(safe(APP_NAME.toUpperCase()), margin, 38);
  doc.setFontSize(12);
  doc.setFont('helvetica', 'normal');
  doc.setTextColor(220, 230, 222);
  doc.text(safe(t('pdf.title')), margin, 48);

  doc.setFontSize(28);
  doc.setFont('helvetica', 'bold');
  doc.setTextColor(255, 250, 240);
  doc.text(safe(gameName), margin, 64);

  // Tarjeta de datos.
  let y = 88;
  doc.setFillColor(250, 245, 233);
  doc.roundedRect(margin, y, pageW - margin * 2, 38, 4, 4, 'F');
  doc.setTextColor(40, 50, 45);
  doc.setFontSize(9);
  doc.setFont('helvetica', 'bold');
  const cols = [
    [t('pdf.date'), new Date(startedAt).toLocaleString(options.locale)],
    [t('pdf.duration'), formatDuration(durationMs)],
    [t('pdf.players'), String(view.players.length)],
    [t('pdf.winner'), result.winnerIds.map((id) => view.players.find((p) => p.id === id)?.name ?? '—').join(', ') || t('result.draw')],
  ];
  cols.forEach(([label, value], i) => {
    const x = margin + 6 + (i % 2) * 88;
    const yy = y + 9 + Math.floor(i / 2) * 13;
    doc.setTextColor(115, 125, 118);
    doc.text(safe(String(label).toUpperCase()), x, yy);
    doc.setTextColor(35, 45, 40);
    doc.setFont('helvetica', 'normal');
    doc.text(safe(String(value)).slice(0, 46), x, yy + 6);
    doc.setFont('helvetica', 'bold');
  });

  // Marcador.
  y += 50;
  doc.setFontSize(12);
  doc.setTextColor(35, 45, 40);
  doc.text(safe(t('pdf.score')), margin, y);
  y += 6;
  const sorted: PlayerView[] = [...view.players].sort((a, b) => (result.scores[b.id] ?? b.score) - (result.scores[a.id] ?? a.score));
  const max = Math.max(1, ...sorted.map((p) => result.scores[p.id] ?? p.score));
  sorted.forEach((player, i) => {
    const score = result.scores[player.id] ?? player.score;
    const barW = Math.max(2, ((pageW - margin * 2 - 70) * score) / max);
    doc.setFontSize(10);
    doc.setTextColor(35, 45, 40);
    doc.setFont('helvetica', 'bold');
    doc.text(safe(player.name).slice(0, 24), margin, y + 5 + i * 9);
    doc.setFillColor(232, 226, 212);
    doc.roundedRect(margin + 48, y + i * 9, pageW - margin * 2 - 60, 5, 2, 2, 'F');
    doc.setFillColor(i === 0 ? ar : 160, i === 0 ? ag : 170, i === 0 ? ab : 160);
    doc.roundedRect(margin + 48, y + i * 9, barW, 5, 2, 2, 'F');
    doc.setFont('helvetica', 'normal');
    doc.setTextColor(70, 80, 74);
    doc.text(String(score), pageW - margin - 6, y + 5 + i * 9, { align: 'right' });
  });
  y += 20 + sorted.length * 9;

  // Crónica.
  doc.setDrawColor(225, 220, 208);
  doc.line(margin, y, pageW - margin, y);
  y += 8;
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(12);
  doc.setTextColor(35, 45, 40);
  doc.text(safe(t('pdf.moves')), margin, y);
  y += 7;
  doc.setFont('helvetica', 'normal');
  doc.setFontSize(8.4);
  const narrative = log.filter((e) => e.key !== 'log.matchStart');
  if (narrative.length === 0) {
    doc.setTextColor(110, 120, 112);
    doc.text(safe(t('pdf.noMoves')), margin, y);
  }
  for (const [i, event] of narrative.entries()) {
    if (y > pageH - 30) {
      doc.addPage();
      y = 22;
    }
    const line = `${String(i + 1).padStart(2, '0')}  ${new Date(event.at).toLocaleTimeString(options.locale, { hour: '2-digit', minute: '2-digit' })}  ${formatLog(event, t)}`;
    const wrapped = doc.splitTextToSize(safe(line), pageW - margin * 2 - 4) as string[];
    doc.setTextColor(55, 65, 58);
    doc.text(wrapped, margin, y);
    y += wrapped.length * 4.1 + 1.2;
  }

  // Pie en todas las páginas.
  const pages = doc.getNumberOfPages();
  for (let p = 1; p <= pages; p += 1) {
    doc.setPage(p);
    doc.setFontSize(7.5);
    doc.setTextColor(140, 148, 142);
    doc.text(safe(`${APP_NAME} · ${t('pdf.footer')} · v${APP_VERSION}`), margin, pageH - 10);
    doc.text(safe(`${t('pdf.page', { n: p })} / ${pages} · ${CONTACT_EMAIL}`), pageW - margin, pageH - 10, { align: 'right' });
  }

  const filename = `mesa-viva-${gameName.toLowerCase().replace(/[^a-z0-9]+/g, '-')}-${new Date().toISOString().slice(0, 10)}.pdf`;
  const bridge = (window as unknown as { MesaViva?: { savePdf?: (name: string, base64: string) => boolean } }).MesaViva;
  if (bridge?.savePdf) {
    try {
      const dataUri = doc.output('datauristring');
      const base64 = dataUri.slice(dataUri.indexOf(',') + 1);
      if (bridge.savePdf(filename, base64)) return;
    } catch {
      // Si el puente nativo falla, se intenta la descarga web.
    }
  }
  doc.save(filename);
}
