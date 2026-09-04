import client from "./client";

/**
 * 관심지역 목록 조회
 */
export const getFavorites = () => client.get("/api/favorites");

/**
 * 관심지역 일괄 등록 (기존 목록을 교체)
 * @param regions 선택한 지역 배열 (순서가 곧 우선순위)
 */
export const saveFavorites = (regions) =>
  client.put("/api/favorites", {
    favorites: regions.map((region, index) => ({
      regionId: region.id,
      priority: index + 1,
    })),
  });
