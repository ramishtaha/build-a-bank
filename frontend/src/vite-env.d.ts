/// <reference types="vite/client" />

// Step 29 · type the one custom env var so `import.meta.env.VITE_API_BASE_URL` is typed (not `any`).
interface ImportMetaEnv {
  readonly VITE_API_BASE_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
