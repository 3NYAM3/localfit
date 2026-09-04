import { useState } from "react";
import { useNavigate } from "react-router-dom";
import Header from "../components/Header";
import { searchRegions } from "../api/region";
import { saveFavorites } from "../api/favorite";
import StepIndicator from "../components/StepIndicator";

const MAX_FAVORITES = 5;

/**
 * 지역 선택 화면
 * 검색으로 지역을 찾아 최대 5개까지 선택하고 우선순위를 지정한다
 */
function RegionSelectPage() {
  const [keyword, setKeyword] = useState("");
  const [results, setResults] = useState([]);
  const [selected, setSelected] = useState([]);
  const [searching, setSearching] = useState(false);
  const [error, setError] = useState("");
  const navigate = useNavigate();

  const handleSearch = async (e) => {
    e.preventDefault();
    if (!keyword.trim()) return;

    setSearching(true);
    setError("");

    try {
      const response = await searchRegions("", "", keyword);
      setResults(response.data.data.content);
    } catch {
      setError("검색에 실패했습니다.");
    } finally {
      setSearching(false);
    }
  };

  const isSelected = (region) => selected.some((s) => s.id === region.id);

  const toggleRegion = (region) => {
    if (isSelected(region)) {
      setSelected(selected.filter((s) => s.id !== region.id));
      return;
    }
    if (selected.length >= MAX_FAVORITES) return;
    setSelected([...selected, region]);
  };

  // 배열에서 항목의 위치를 한 칸 이동시킨다 (우선순위 변경)
  const moveRegion = (index, direction) => {
    const target = index + direction;
    if (target < 0 || target >= selected.length) return;

    const next = [...selected];
    [next[index], next[target]] = [next[target], next[index]];
    setSelected(next);
  };

  const handleNext = async () => {
    try {
      await saveFavorites(selected);
      navigate("/preferences");
    } catch (err) {
      setError(err.response?.data?.message ?? "저장에 실패했습니다.");
    }
  };

  return (
    <div className="min-h-screen">
      <Header />

      <main className="mx-auto max-w-2xl px-6 py-12">
        <StepIndicator current={1} />

        <h1 className="mt-8 text-2xl font-bold">관심 있는 동네를 골라주세요</h1>
        <p className="mt-2 break-keep text-sm text-stone-500">
          최대 {MAX_FAVORITES}개까지 선택할 수 있고, 순서는 나중에 바꿀 수
          있습니다.
        </p>

        {/* 검색 */}
        <form onSubmit={handleSearch} className="mt-8 flex gap-2">
          <input
            type="text"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            placeholder="동 이름으로 검색 (예: 잠실)"
            className="flex-1 rounded-lg border border-stone-300 bg-white px-4 py-2.5 text-sm outline-none transition focus:border-stone-900 focus:ring-1 focus:ring-stone-900"
          />
          <button
            type="submit"
            disabled={searching}
            className="rounded-lg bg-stone-900 px-5 text-sm font-medium text-white transition hover:bg-stone-700 disabled:opacity-50"
          >
            검색
          </button>
        </form>

        {error && (
          <p className="mt-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600">
            {error}
          </p>
        )}

        {/* 검색 결과 */}
        {results.length > 0 && (
          <div className="mt-6">
            <p className="mb-3 text-xs font-medium text-stone-500">검색 결과</p>
            <div className="max-h-72 overflow-y-auto rounded-xl bg-white ring-1 ring-stone-200">
              {results.map((region) => {
                const active = isSelected(region);
                const full = selected.length >= MAX_FAVORITES && !active;

                return (
                  <button
                    key={region.id}
                    onClick={() => toggleRegion(region)}
                    disabled={full}
                    className={`flex w-full items-center justify-between border-b border-stone-100 px-4 py-3 text-left text-sm transition last:border-0 ${
                      active ? "bg-stone-50" : "hover:bg-stone-50"
                    } ${full ? "cursor-not-allowed opacity-40" : ""}`}
                  >
                    <span>
                      <span className="text-stone-400">
                        {region.sido} {region.sigungu}
                      </span>{" "}
                      <span className="font-medium">{region.dong}</span>
                    </span>
                    {active && (
                      <span className="text-xs font-medium">선택됨</span>
                    )}
                  </button>
                );
              })}
            </div>
          </div>
        )}

        {/* 선택 목록 */}
        <div className="mt-8">
          <div className="mb-3 flex items-center justify-between">
            <p className="text-xs font-medium text-stone-500">선택한 지역</p>
            <p className="text-xs text-stone-400">
              {selected.length} / {MAX_FAVORITES}
            </p>
          </div>

          {selected.length === 0 ? (
            <div className="rounded-xl border border-dashed border-stone-300 py-10 text-center text-sm text-stone-400">
              위에서 검색해 지역을 선택해주세요
            </div>
          ) : (
            <ul className="space-y-2">
              {selected.map((region, index) => (
                <li
                  key={region.id}
                  className="flex items-center gap-3 rounded-xl bg-white px-4 py-3 ring-1 ring-stone-200"
                >
                  <span className="flex h-6 w-6 items-center justify-center rounded-full bg-stone-900 text-xs font-medium text-white">
                    {index + 1}
                  </span>

                  <span className="flex-1 text-sm">
                    <span className="text-stone-400">
                      {region.sido} {region.sigungu}
                    </span>{" "}
                    <span className="font-medium">{region.dong}</span>
                  </span>

                  <div className="flex items-center gap-1">
                    <IconButton
                      onClick={() => moveRegion(index, -1)}
                      disabled={index === 0}
                      label="위로"
                    >
                      ↑
                    </IconButton>
                    <IconButton
                      onClick={() => moveRegion(index, 1)}
                      disabled={index === selected.length - 1}
                      label="아래로"
                    >
                      ↓
                    </IconButton>
                    <IconButton
                      onClick={() => toggleRegion(region)}
                      label="삭제"
                    >
                      ×
                    </IconButton>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>

        <button
          onClick={handleNext}
          disabled={selected.length === 0}
          className="mt-10 w-full rounded-lg bg-stone-900 py-3 text-sm font-medium text-white transition hover:bg-stone-700 disabled:opacity-40"
        >
          다음
        </button>
      </main>
    </div>
  );
}

/** 작은 아이콘 버튼 (순서 변경, 삭제) */
function IconButton({ onClick, disabled, label, children }) {
  return (
    <button
      onClick={onClick}
      disabled={disabled}
      aria-label={label}
      className="flex h-7 w-7 items-center justify-center rounded-md text-stone-500 transition hover:bg-stone-100 disabled:opacity-30 disabled:hover:bg-transparent"
    >
      {children}
    </button>
  );
}

export default RegionSelectPage;
