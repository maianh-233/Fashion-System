import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
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
      } : undefined,
    },
    build: {
      // Do not publish source maps with production assets.
      sourcemap: false,
    },
  }
})
