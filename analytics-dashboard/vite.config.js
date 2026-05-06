import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // Proxy /api/* to the log-collector during local development
    // so the browser never hits a CORS preflight issue
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    // Produce a clean dist/ folder each time
    emptyOutDir: true,
    // Increase the default chunk-size warning threshold (Recharts is large)
    chunkSizeWarningLimit: 800,
  },
})
