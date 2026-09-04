import client from "./client";

/**
 * 관심지역 점수 조회
 * @param params { rentType, housingImportance, subwayImportance, hospitalImportance }
 */
export const getScores = (params) =>
  client.get("/api/favorites/scores", { params });
