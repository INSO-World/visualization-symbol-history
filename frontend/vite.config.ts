import { fileURLToPath, URL } from 'node:url'

import { defineConfig, UserConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import { configDotenv } from 'dotenv'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const envPath = new URL(`environments/${mode}.env`, import.meta.url)
  const env = configDotenv({ path: envPath }).parsed ?? {
    VITE_HOST: 'localhost',
    VITE_PORT: '5173',
  }

  return {
    plugins: [
      vue(),
      vueDevTools(),
    ],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      },
    },
    server: {
      host: env.VITE_HOST,
      port: Number.parseInt(env.VITE_PORT, 10),
    },
    define: {
      'process.env': {
        ...process.env,
        ...env,
      },
    },
  } satisfies UserConfig
})
