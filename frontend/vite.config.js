import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// Configuración de Vite para GeoLogix Dashboard
// - React plugin
// - Proxy de desarrollo: redirige /api y /ws al backend (Spring Boot en :8080)
//   para evitar errores de CORS durante el desarrollo.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/ws': {
        target: 'http://localhost:8080',
        ws: true,
        changeOrigin: true,
      },
    },
  },
})
