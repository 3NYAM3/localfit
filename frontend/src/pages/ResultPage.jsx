import { useState, useEffect } from "react";
import { useSearchParams, useNavigate } from "react-router-dom";
import Header from "../components/Header";
import { getScores } from "../api/score";

/** 계층 탭 정의 - API 응답 필드명과 대응 */
const TIERS = [
  { key: "sigunguTier", label: "시군구 내" },
  { key: "sidoTier", label: "시도 내" },
  { key: "capitalTier", label: "수도권 전체" },
];

/** 지표 정의 - TierScore의 필드명, 가중치 필드명과 대응 */
const INDICATORS = [
  { key: "housingRanking", weightKey: "housing", label: "주거비" },
  { key: "subwayRanking", weightKey: "subway", label: "교통" },
  { key: "hospitalRanking", weightKey: "hospital", label: "의료" },
];

const RENT_TYPE_LABEL = { JEONSE: "전세", MONTHLY: "월세" };

/**
 * 결과 화면
 * URL 쿼리 파라미터로 전달된 조건으로 점수를 조회해 계층별로 보여준다
 */
function ResultPage() {
  const [searchParams] = useSearchParams();
  const [scores, setScores] = useState([]);
  const [activeTier, setActiveTier] = useState("sigunguTier");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const navigate = useNavigate();

  const rentType = searchParams.get("rentType") ?? "JEONSE";

  useEffect(() => {
    const fetchScores = async () => {
      setLoading(true);
      setError("");

      try {
        const params = Object.fromEntries(searchParams);
        const response = await getScores(params);
        setScores(response.data.data);
      } catch (err) {
        setError(err.response?.data?.message ?? "점수를 불러오지 못했습니다.");
      } finally {
        setLoading(false);
      }
    };

    fetchScores();
  }, [searchParams]);

  const weights = scores[0]?.weights;

  return (
    <div className="min-h-screen">
      <Header />

      <main className="mx-auto max-w-2xl px-6 py-12">
        <h1 className="text-2xl font-bold">분석 결과</h1>

        {/* 적용된 기준 */}
        {weights && (
          <div className="mt-4 flex flex-wrap items-center gap-x-3 gap-y-1 rounded-xl bg-white px-4 py-3 text-xs ring-1 ring-stone-200">
            <span className="font-medium">{RENT_TYPE_LABEL[rentType]}</span>
            <span className="text-stone-300">|</span>
            {INDICATORS.map((indicator) => (
              <span key={indicator.key} className="text-stone-500">
                {indicator.label}{" "}
                <span className="font-medium text-stone-900">
                  {weights[indicator.weightKey]}%
                </span>
              </span>
            ))}
            <button
              onClick={() => navigate("/preferences")}
              className="ml-auto font-medium text-stone-900 hover:underline"
            >
              조정
            </button>
          </div>
        )}

        {/* 계층 탭 */}
        <div className="mt-8 flex gap-1 rounded-lg bg-stone-200 p-1">
          {TIERS.map((tier) => (
            <button
              key={tier.key}
              onClick={() => setActiveTier(tier.key)}
              className={`flex-1 rounded-md py-2 text-xs font-medium transition ${
                activeTier === tier.key
                  ? "bg-white text-stone-900 shadow-sm"
                  : "text-stone-500 hover:text-stone-700"
              }`}
            >
              {tier.label}
            </button>
          ))}
        </div>

        <p className="mt-3 break-keep text-xs text-stone-400">
          {activeTier === "capitalTier"
            ? "수도권 744개 동을 같은 기준으로 비교한 결과입니다."
            : "각 지역이 속한 범위 안에서의 상대 평가입니다. 비교 대상이 달라 지역 간 점수를 직접 비교할 수는 없습니다."}
        </p>

        {loading && (
          <p className="mt-12 text-center text-sm text-stone-400">
            분석하는 중...
          </p>
        )}

        {error && (
          <p className="mt-6 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600">
            {error}
          </p>
        )}

        {/* 지역별 결과 카드 - 우선순위 순 */}
        {!loading && !error && (
          <div className="mt-6 space-y-3">
            {scores.map((score) => (
              <ScoreCard
                key={score.regionId}
                score={score}
                tierKey={activeTier}
              />
            ))}
          </div>
        )}

        {/* 상속 값 안내 */}
        {!loading && !error && scores.length > 0 && (
          <p className="mt-4 break-keep text-[11px] text-stone-400">
            * 표시는 데이터 특성상 상위 지역 기준으로 계산된 값입니다.
          </p>
        )}

        <div className="mt-10 flex gap-3">
          <button
            onClick={() => navigate("/regions")}
            className="flex-1 rounded-lg bg-white py-3 text-sm font-medium ring-1 ring-stone-300 transition hover:bg-stone-50"
          >
            지역 다시 선택
          </button>
          <button
            onClick={() => navigate("/main")}
            className="flex-1 rounded-lg bg-stone-900 py-3 text-sm font-medium text-white transition hover:bg-stone-700"
          >
            홈으로
          </button>
        </div>
      </main>
    </div>
  );
}

/** 관심지역 1건의 점수 카드 */
function ScoreCard({ score, tierKey }) {
  const tier = score[tierKey];

  return (
    <div className="rounded-2xl bg-white p-5 ring-1 ring-stone-200">
      <div className="flex items-start justify-between">
        <div className="flex items-start gap-2.5">
          <span className="mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-stone-200 text-[11px] font-medium text-stone-600">
            {score.priority}
          </span>

          <div>
            <h2 className="font-bold leading-tight">{score.dong}</h2>
            <p className="mt-0.5 text-[11px] text-stone-400">
              {score.sido} {score.sigungu}
            </p>
          </div>
        </div>

        <div className="text-right">
          <p className="text-2xl font-bold leading-none">{tier.totalScore}</p>
          <p className="mt-1 text-[11px] text-stone-400">점</p>
        </div>
      </div>

      {/* 지표별 순위 */}
      <div className="mt-5 space-y-2.5">
        {INDICATORS.map((indicator) => (
          <RankingBar
            key={indicator.key}
            label={indicator.label}
            ranking={tier[indicator.key]}
          />
        ))}
      </div>
    </div>
  );
}

/** 지표 하나의 순위를 막대로 표시 */
function RankingBar({ label, ranking }) {
  if (!ranking) {
    return (
      <div className="flex items-center gap-3 text-[11px]">
        <span className="w-10 shrink-0 text-stone-500">{label}</span>
        <span className="text-stone-300">데이터 없음</span>
      </div>
    );
  }

  // percentile은 낮을수록 좋으므로 뒤집어서 막대 길이로 사용
  const barWidth = 100 - ranking.percentile;

  return (
    <div className="flex items-center gap-3 text-[11px]">
      <span className="w-10 shrink-0 text-stone-500">{label}</span>

      <div className="h-1 flex-1 overflow-hidden rounded-full bg-stone-100">
        <div
          className="h-full rounded-full bg-stone-900 transition-all"
          style={{ width: `${barWidth}%` }}
        />
      </div>

      <span className="w-24 shrink-0 text-right text-stone-400">
        {ranking.totalCount}곳 중{" "}
        <span className="font-medium text-stone-700">{ranking.rank}위</span>
        {ranking.inherited && (
          <span className="ml-0.5" title="상위 지역 값을 참고했습니다">
            *
          </span>
        )}
      </span>
    </div>
  );
}

export default ResultPage;
