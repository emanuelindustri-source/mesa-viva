export const APP_NAME = 'Mesa Viva';
export const APP_VERSION = '0.4.0';
export const CONTACT_EMAIL = 'emanuel.industri@gmail.com';
export const GITHUB_URL = 'https://github.com/emanuelindustri-source/mesa-viva';
export const SITE_URL = 'https://emanuelindustri-source.github.io/mesa-viva/';
export const GAMES_COUNT = 12;
export const CATALOG_COUNT = 30;
export const IS_ANDROID = typeof document !== 'undefined' && Boolean((window as unknown as { MesaViva?: unknown }).MesaViva);

export const AVATARS = [
  { id: 'jaguar', emoji: '🐆', label: 'Jaguar' },
  { id: 'tucan', emoji: '🦜', label: 'Tucán' },
  { id: 'cardenal', emoji: '🐦', label: 'Cardenal' },
  { id: 'danta', emoji: '🦌', label: 'Danta' },
  { id: 'caiman', emoji: '🐊', label: 'Caimán' },
  { id: 'arana', emoji: '🕷️', label: 'Araña' },
  { id: 'silbon', emoji: '🎩', label: 'El Silbón' },
  { id: 'sayona', emoji: '🕯️', label: 'La Sayona' },
  { id: 'turpial', emoji: '🐤', label: 'Turpial' },
  { id: 'chiguiro', emoji: '🐹', label: 'Chigüiro' },
] as const;

export const BOT_NAMES = [
  'El Silbón', 'La Sayona', 'El Hachero', 'La Llorona', 'El Chupacabras',
  'Doña Rosa', 'Compadre José', 'La Comadre', 'El Primo', 'La Vecina',
] as const;

export const BOT_AVATARS = ['silbon', 'sayona', 'jaguar', 'arana', 'tucan', 'caiman', 'danta', 'cardenal'] as const;
