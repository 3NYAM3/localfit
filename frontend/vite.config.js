import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      // /api, /internal로 시작하는 요청을 백엔드로 전달
      "/api": "http://localhost:8080",
      "/internal": "http://localhost:8080",
    },
  },
});
