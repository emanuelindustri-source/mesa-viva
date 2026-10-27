import { create } from 'zustand';
import { uid } from '../core/utils';

export type ToastTone = 'info' | 'success' | 'warn' | 'danger' | 'magic';

export interface ToastItem {
  id: string;
  message: string;
  tone: ToastTone;
  duration: number;
}

interface ToastState {
  toasts: ToastItem[];
  push: (message: string, tone?: ToastTone, duration?: number) => void;
  remove: (id: string) => void;
}

export const useToastStore = create<ToastState>((set) => ({
  toasts: [],
  push: (message, tone = 'info', duration = 3200) => {
    const item: ToastItem = { id: uid('toast'), message, tone, duration };
    set((state) => ({ toasts: [...state.toasts.slice(-3), item] }));
    if (typeof window !== 'undefined') {
      window.setTimeout(() => {
        set((state) => ({ toasts: state.toasts.filter((t) => t.id !== item.id) }));
      }, duration);
    }
  },
  remove: (id) => set((state) => ({ toasts: state.toasts.filter((t) => t.id !== id) })),
}));

export function toast(message: string, tone: ToastTone = 'info', duration = 3200): void {
  useToastStore.getState().push(message, tone, duration);
}
