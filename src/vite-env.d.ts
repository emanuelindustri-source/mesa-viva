/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_ROOM_WS_URL?: string;
  readonly VITE_DIAGNOSTICS_ENDPOINT?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
