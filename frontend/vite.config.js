import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

import { fileURLToPath, URL } from "node:url";

// Backend seçimi:
//   npm run dev     -> monolith (ws/, http://localhost:8080)
//   npm run dev:ms  -> mikroservisler (api-gateway, http://localhost:8000)
// İki backend de aynı /api/v1 sözleşmesini sunduğu için frontend kodu değişmez.
const apiTarget = process.env.VITE_API_TARGET || 'http://localhost:8080'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': apiTarget,
      '/assets': apiTarget,
    }
  },
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url))
    }
  }
})
