import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";
import { defineConfig } from "vite";

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    proxy: {
      // /api, /internal로 시작하는 요청을 백엔드로 전달
      "/api": "http://localhost:8080",
      "/internal": "http://localhost:8080",
    },
  },
});
