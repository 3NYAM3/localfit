import axios from "axios";

/**
 * 백엔드 API 호출용 axios 인스턴스
 * Vite 프록시를 통해 http://localhost:8080으로 전달된다
 */

const client = axios.create({
  baseURL: "/",
  headers: {
    "Content-Type": "application/json",
  },
});

// 요청시 저장된 Access Token을 자동으로 헤더에 추가
client.interceptors.request.use((config) => {
  const token = localStorage.getItem("accessToken");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export default client;
