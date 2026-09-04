import { useState } from "react";
import { useNavigate } from "react-router-dom";
import Header from "../components/Header";
import StepIndicator from "../components/StepIndicator";

/** 임대 유형 선택지 */
const RENT_TYPES = [
  { value: "JEONSE", label: "전세" },
  { value: "MONTHLY", label: "월세" },
];

/** 중요도 등급 선택지 (백엔드 ImportanceLevel과 대응) */
const IMPORTANCE_LEVELS = [
  { value: "NOT_IMPORTANT", label: "상관없음" },
  { value: "NORMAL", label: "보통" },
  { value: "IMPORTANT", label: "중요" },
  { value: "VERY_IMPORTANT", label: "매우 중요" },
];

/** 평가 지표 */
const INDICATORS = [
  {
    key: "housingImportance",
    title: "주거비",
    description: "전월세 실거래가 기준 평균 비용",
  },
  {
    key: "subwayImportance",
    title: "교통",
    description: "지역 내 지하철역 수",
  },
  {
    key: "hospitalImportance",
    title: "의료",
    description: "종합병원 이상 의료기관 수",
  },
];

/**
 * 중요도 설정 화면
 * 선택한 값을 URL 쿼리 파라미터로 결과 화면에 전달한다
 */
function PreferencePage() {
  const [rentType, setRentType] = useState("JEONSE");
  const [importance, setImportance] = useState({
    housingImportance: "NORMAL",
    subwayImportance: "NORMAL",
    hospitalImportance: "NORMAL",
  });
  const navigate = useNavigate();

  const handleSelect = (key, value) => {
    setImportance({ ...importance, [key]: value });
  };

  const handleSubmit = () => {
    const params = new URLSearchParams({ rentType, ...importance });
    navigate(`/results?${params.toString()}`);
  };

  return (
    <div className="min-h-screen">
      <Header />

      <main className="mx-auto max-w-2xl px-6 py-12">
        <StepIndicator current={2} />

        <h1 className="mt-8 text-2xl font-bold">무엇을 중요하게 보시나요?</h1>
        <p className="mt-2 break-keep text-sm text-stone-500">
          선택한 중요도에 따라 지표별 가중치가 자동으로 계산됩니다.
        </p>

        {/* 임대 유형 */}
        <section className="mt-10">
          <h2 className="text-sm font-medium">임대 유형</h2>
          <p className="mt-1 text-xs text-stone-500">
            전세는 보증금, 월세는 월 임대료를 기준으로 비교합니다.
          </p>

          <div className="mt-3 flex gap-2">
            {RENT_TYPES.map((type) => (
              <button
                key={type.value}
                onClick={() => setRentType(type.value)}
                className={`flex-1 rounded-lg py-3 text-sm font-medium transition ${
                  rentType === type.value
                    ? "bg-stone-900 text-white"
                    : "bg-white text-stone-600 ring-1 ring-stone-200 hover:bg-stone-50"
                }`}
              >
                {type.label}
              </button>
            ))}
          </div>
        </section>

        {/* 지표별 중요도 */}
        <section className="mt-10 space-y-6">
          {INDICATORS.map((indicator) => (
            <div key={indicator.key}>
              <h2 className="text-sm font-medium">{indicator.title}</h2>
              <p className="mt-1 text-xs text-stone-500">
                {indicator.description}
              </p>

              <div className="mt-3 grid grid-cols-4 gap-2">
                {IMPORTANCE_LEVELS.map((level) => (
                  <button
                    key={level.value}
                    onClick={() => handleSelect(indicator.key, level.value)}
                    className={`rounded-lg py-2.5 text-xs font-medium transition ${
                      importance[indicator.key] === level.value
                        ? "bg-stone-900 text-white"
                        : "bg-white text-stone-600 ring-1 ring-stone-200 hover:bg-stone-50"
                    }`}
                  >
                    {level.label}
                  </button>
                ))}
              </div>
            </div>
          ))}
        </section>

        <button
          onClick={handleSubmit}
          className="mt-12 w-full rounded-lg bg-stone-900 py-3 text-sm font-medium text-white transition hover:bg-stone-700"
        >
          결과 보기
        </button>
      </main>
    </div>
  );
}

export default PreferencePage;
