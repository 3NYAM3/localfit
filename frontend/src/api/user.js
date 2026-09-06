import client from "./client";

/** 내 회원 정보 조회 */
export const getMyInfo = () => client.get("/api/users/me");

/** 닉네임 변경 */
export const updateNickname = (nickname) =>
  client.patch("/api/users/me/nickname", { nickname });

/** 비밀번호 변경 */
export const updatePassword = (currentPassword, newPassword) =>
  client.patch("/api/users/me/password", { currentPassword, newPassword });
