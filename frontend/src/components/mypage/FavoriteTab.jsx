import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { getFavorites } from "../../api/favorite";

/**
 * 마이페이지 - 관심지역 탭
 * 등록된 관심지역 목록을 우선순위 순으로 보여준다
 */
function FavoriteTab() {
  const [favorites, setFavorites] = useState([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchFavorites = async () => {
      try {
        const response = await getFavorites();
        setFavorites(response.data.data);
      } finally {
        setLoading(false);
      }
    };
    fetchFavorites();
  }, []);

  if (loading) {
    return (
      <p className="py-12 text-center text-sm text-stone-400">불러오는 중...</p>
    );
  }

  if (favorites.length === 0) {
    return (
      <div className="rounded-xl border border-dashed border-stone-300 py-12 text-center">
        <p className="text-sm text-stone-400">등록된 관심지역이 없습니다</p>
        <button
          onClick={() => navigate("/regions")}
          className="mt-4 rounded-lg bg-stone-900 px-5 py-2 text-xs font-medium text-white transition hover:bg-stone-700"
        >
          지역 선택하기
        </button>
      </div>
    );
  }

  return (
    <div>
      <ul className="space-y-2">
        {favorites.map((favorite) => (
          <li
            key={favorite.favoriteId}
            className="flex items-center gap-3 rounded-xl bg-white px-4 py-3.5 ring-1 ring-stone-200"
          >
            <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-stone-200 text-[11px] font-medium text-stone-600">
              {favorite.priority}
            </span>
            <span className="text-sm">
              <span className="font-medium">{favorite.dong}</span>
              <span className="ml-2 text-[11px] text-stone-400">
                {favorite.sido} {favorite.sigungu}
              </span>
            </span>
          </li>
        ))}
      </ul>

      <button
        onClick={() => navigate("/regions")}
        className="mt-6 w-full rounded-lg bg-white py-3 text-sm font-medium ring-1 ring-stone-300 transition hover:bg-stone-50"
      >
        관심지역 다시 선택
      </button>
    </div>
  );
}

export default FavoriteTab;
