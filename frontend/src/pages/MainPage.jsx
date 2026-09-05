import { useNavigate } from "react-router-dom";
import Header from "../components/Header";

/**
 * 메인 화면
 * 지역 선택 플로우의 시작점
 */
function MainPage() {
  const navigate = useNavigate();

  const handleStart = () => {
    const isLoggedIn = Boolean(localStorage.getItem("accessToken"));

    if (isLoggedIn) {
      navigate("/regions");
      return;
    }

    // 로그인 후 원래 하려던 곳으로 보내기 위해 목적지를 함께 전달
    navigate("/login", { state: { from: "/regions" } });
  };

  return (
    <div className="min-h-screen">
      <Header />

      <main className="mx-auto flex max-w-5xl flex-col items-center px-6 py-24 text-center">
        <span className="rounded-full bg-white px-3.5 py-1.5 text-xs font-medium text-stone-600 ring-1 ring-stone-200">
          수도권 2,907개 동 분석
        </span>

        <h1 className="mt-8 text-5xl font-bold leading-tight tracking-tight">
          나에게 맞는 동네를
          <br />
          찾아보세요
        </h1>

        <p className="mt-6 max-w-md break-keep text-stone-500">
          주거비, 교통, 의료 접근성을 내가 정한 기준으로 평가합니다.
          <br />
          공공데이터를 기반으로 분석한 결과를 확인해보세요.
        </p>

        <button
          onClick={handleStart}
          className="mt-10 rounded-full bg-stone-900 px-8 py-3.5 font-medium text-white transition hover:bg-stone-700"
        >
          지역 선택하기
        </button>

        {/* 데이터 출처 */}
        <div className="mt-24 grid w-full max-w-3xl grid-cols-2 gap-4 sm:grid-cols-4">
          {[
            { label: "법정동", value: "2,907", unit: "개" },
            { label: "전월세 실거래", value: "223,781", unit: "건" },
            { label: "지하철역", value: "646", unit: "개" },
            { label: "병원", value: "750", unit: "개" },
          ].map((item) => (
            <div
              key={item.label}
              className="rounded-2xl bg-white p-5 ring-1 ring-stone-200"
            >
              <p className="text-xs text-stone-500">{item.label}</p>
              <p className="mt-1.5 text-xl font-bold">
                {item.value}
                <span className="ml-0.5 text-sm font-normal text-stone-500">
                  {item.unit}
                </span>
              </p>
            </div>
          ))}
        </div>
      </main>
    </div>
  );
}

export default MainPage;
