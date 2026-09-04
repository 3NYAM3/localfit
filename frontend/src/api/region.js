import client from "./client";

/**
 * 지역 검색
 * @param sido    시도 필터 (빈 값이면 전체)
 * @param sigungu 시군구 필터 (빈 값이면 전체)
 * @param keyword 동 이름 검색어
 * @param page    페이지 번호 (0부터 시작)
 */
export const searchRegions = (sido, sigungu, keyword, page = 0) =>
  client.get("/api/regions", {
    params: {
      sido: sido || undefined,
      sigungu: sigungu || undefined,
      keyword: keyword || undefined,
      page,
      size: 20,
    },
  });
