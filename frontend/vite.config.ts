import { resolve } from 'node:path';
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Backend Spring Boot domyslnie chodzi na 8080 - proxy /api omija CORS w trybie dev.
// Inny port backendu: API_PROXY_TARGET=http://localhost:8081 npm run dev
const backendApiProxyTarget = process.env.API_PROXY_TARGET ?? 'http://localhost:8080';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': backendApiProxyTarget,
    },
  },
  build: {
    // "npm run build" wrzuca gotowy frontend do zasobow backendu - Spring Boot serwuje go razem z API
    // (FrontendSinglePageAppConfig). Stary build czysci skrypt "prebuild", a nie Vite: w static leza tez inne pliki.
    outDir: resolve(__dirname, '../src/main/resources/static'),
    emptyOutDir: false,
  },
});
