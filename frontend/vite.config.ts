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
});
