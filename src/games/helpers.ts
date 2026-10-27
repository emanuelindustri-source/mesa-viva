import type { GameAction, GameView, PromptView, UIAction } from '../core/types';

export type TFn = (key: string, params?: Record<string, unknown>) => string;

export function makeAction(
  id: string,
  labelKey: string,
  action: GameAction,
  options: Partial<Pick<UIAction, 'tone' | 'disabled' | 'icon' | 'labelParams'>> = {},
): UIAction {
  return {
    id,
    labelKey,
    action,
    tone: 'secondary',
    ...options,
  };
}

export function prompt(
  key: string,
  params?: Record<string, string | number>,
  tone: PromptView['tone'] = 'info',
): PromptView {
  return { key, params, tone };
}

export function emptyView(): GameView {
  return {
    gameId: 'caida',
    youId: '',
    phase: 'idle',
    turnId: null,
    players: [],
    hand: [],
    center: {},
    actions: [],
  };
}

export function actionType(action: GameAction): string {
  return String(action.type ?? '');
}
