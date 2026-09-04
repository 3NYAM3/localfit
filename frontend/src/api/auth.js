import client from "./client";

/** 회원가입 */
export const signup = (email, password, nickname) =>
  client.post("/api/auth/signup", { email, password, nickname });

/** 로그인 - 성공 시 토큰ㄴ을 localStorage에 저장 */
export const signIn = async (email, password) => {
  const response = await client.post("/api/auth/signin", { email, password });
  const { accessToken, refreshToken } = response.data.data;

  localStorage.setItem("accessToken", accessToken);
  localStorage.setItem("refreshToken", refreshToken);

  return response.data;
};

/** 로그아웃 */
export const signOut = async () => {
  try {
    await client.post("/api/auth/signout");
  } finally {
    localStorage.removeItem("accessToken");
    localStorage.removeItem("refreshToken");
  }
};
