import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // En desarrollo el frontend llama a /api/v1/... y Vite lo reenvía al backend (evita URLs fijas y CORS).
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
