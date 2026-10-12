import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { cwd } from 'node:process'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, cwd(), '')
  const backendUrl = env.BACKEND_URL?.replace(/\/$/, '')

  return {
    plugins: [
      react(),
      tailwindcss(),
    ],
    server: {
      proxy: backendUrl ? {
        '/api': {
          target: backendUrl,
          changeOrigin: true,
        },
        '/ws': {
          target: backendUrl,
          changeOrigin: true,
          ws: true,
        },
      } : undefined,
    },
    build: {
      // Do not publish source maps with production assets.
      sourcemap: false,
    },
  }
})
