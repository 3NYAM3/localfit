/**
 * 진행 단계 표시
 * 지역 선택 → 중요도 설정 → 결과
 */
function StepIndicator({ current }) {
  const steps = ["지역 선택", "중요도 설정", "결과"];

  return (
    <div className="flex items-center gap-2">
      {steps.map((label, index) => {
        const step = index + 1;
        const active = step === current;
        const done = step < current;

        return (
          <div key={label} className="flex items-center gap-2">
            <span
              className={`flex h-6 w-6 items-center justify-center rounded-full text-xs font-medium ${
                active || done
                  ? "bg-stone-900 text-white"
                  : "bg-stone-200 text-stone-500"
              }`}
            >
              {step}
            </span>
            <span
              className={`text-xs ${
                active ? "font-medium text-stone-900" : "text-stone-400"
              }`}
            >
              {label}
            </span>
            {step < steps.length && <span className="text-stone-300">—</span>}
          </div>
        );
      })}
    </div>
  );
}

export default StepIndicator;
