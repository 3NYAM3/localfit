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

// 401 응답 시 토큰을 지우고 로그인 페이지로 보낸다
client.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem("accessToken");
      localStorage.removeItem("refreshToken");
      window.location.href = "/login";
    }
    return Promise.reject(error);
  },
);

export default client;
