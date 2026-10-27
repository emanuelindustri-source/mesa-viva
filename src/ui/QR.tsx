import { useEffect, useRef, useState } from 'react';
import QRCode from 'qrcode';
import jsQR from 'jsqr';
import { Camera, Loader2, X } from 'lucide-react';
import { useT } from '../i18n/useT';
import { Button, IconButton } from './components';

export function QRCodeView({ value, size = 220, label }: { value: string; size?: number; label?: string }) {
  const [src, setSrc] = useState<string | null>(null);
  const [error, setError] = useState(false);
  useEffect(() => {
    let alive = true;
    setSrc(null);
    setError(false);
    QRCode.toDataURL(value, {
      width: size * 2,
      margin: 2,
      errorCorrectionLevel: 'M',
      color: { dark: '#0d2b26', light: '#fffaf0' },
    })
      .then((url) => {
        if (alive) setSrc(url);
      })
      .catch(() => {
        if (alive) setError(true);
      });
    return () => {
      alive = false;
    };
  }, [value, size]);
  if (error) return <div className="qr-fallback">{value}</div>;
  if (!src) return <div className="qr-loading" style={{ width: size, height: size }}><Loader2 size={20} className="spin" /></div>;
  return <img className="qr-image" src={src} width={size} height={size} alt={label ?? 'QR'} />;
}

export function QRScanner({ onResult, onClose }: { onResult: (value: string) => void; onClose: () => void }) {
  const t = useT();
  const videoRef = useRef<HTMLVideoElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [error, setError] = useState<string | null>(null);
  const [starting, setStarting] = useState(true);
  const doneRef = useRef(false);
  const onResultRef = useRef(onResult);
  onResultRef.current = onResult;

  useEffect(() => {
    let stream: MediaStream | null = null;
    let raf = 0;
    let cancelled = false;

    const start = async () => {
      try {
        if (!navigator.mediaDevices?.getUserMedia) throw new Error('unsupported');
        stream = await navigator.mediaDevices.getUserMedia({
          video: { facingMode: { ideal: 'environment' }, width: { ideal: 1280 }, height: { ideal: 720 } },
          audio: false,
        });
        if (cancelled) {
          stream.getTracks().forEach((track) => track.stop());
          return;
        }
        const video = videoRef.current;
        if (!video) return;
        video.srcObject = stream;
        await video.play();
        setStarting(false);
        const canvas = canvasRef.current;
        const ctx = canvas?.getContext('2d', { willReadFrequently: true });
        const scan = () => {
          if (cancelled || doneRef.current) return;
          if (video.readyState === video.HAVE_ENOUGH_DATA && canvas && ctx) {
            canvas.width = video.videoWidth;
            canvas.height = video.videoHeight;
            ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
            const data = ctx.getImageData(0, 0, canvas.width, canvas.height);
            const code = jsQR(data.data, data.width, data.height, { inversionAttempts: 'dontInvert' });
            if (code?.data) {
              doneRef.current = true;
              onResultRef.current(code.data);
              return;
            }
          }
          raf = requestAnimationFrame(scan);
        };
        raf = requestAnimationFrame(scan);
      } catch (err) {
        const name = err instanceof Error ? err.name : '';
        setError(name === 'NotAllowedError' ? t('sala.cameraDenied') : t('sala.scanError'));
        setStarting(false);
      }
    };
    void start();
    return () => {
      cancelled = true;
      cancelAnimationFrame(raf);
      stream?.getTracks().forEach((track) => track.stop());
    };
  }, [t]);

  return (
    <div className="qr-scanner">
      <video ref={videoRef} playsInline muted aria-label={t('sala.scanTitle')} />
      <canvas ref={canvasRef} className="hidden" />
      <div className="qr-frame" aria-hidden="true"><span /><span /><span /><span /></div>
      <div className="qr-scanner-bar">
        <div>
          <Camera size={17} />
          <span>{starting ? t('sala.connecting') : error ?? t('sala.scanGuest')}</span>
        </div>
        <IconButton label={t('common.close')} onClick={onClose}><X size={18} /></IconButton>
      </div>
      {error ? <div className="qr-error"><p>{error}</p><Button variant="secondary" size="sm" onClick={onClose}>{t('common.close')}</Button></div> : null}
    </div>
  );
}
