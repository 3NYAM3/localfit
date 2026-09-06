import { useState } from "react";
import { Eye, EyeOff } from "lucide-react";

/**
 * 비밀번호 입력 필드
 * 눈 아이콘을 눌러 입력값을 표시하거나 가릴 수 있다
 */
function PasswordInput({
  name,
  value,
  onChange,
  placeholder,
  autoFocus,
  className = "",
}) {
  const [visible, setVisible] = useState(false);

  return (
    <div className="relative">
      <input
        type={visible ? "text" : "password"}
        name={name}
        value={value}
        onChange={onChange}
        placeholder={placeholder}
        autoFocus={autoFocus}
        className={`w-full rounded-lg border border-stone-300 py-2 pl-3 pr-10 text-sm outline-none transition focus:border-stone-900 focus:ring-1 focus:ring-stone-900 ${className}`}
      />

      <button
        type="button"
        onClick={() => setVisible(!visible)}
        aria-label={visible ? "비밀번호 숨기기" : "비밀번호 표시"}
        className="absolute right-2 top-1/2 flex h-7 w-7 -translate-y-1/2 items-center justify-center rounded-md text-stone-400 transition hover:bg-stone-100 hover:text-stone-600"
      >
        {visible ? <EyeOff size={16} /> : <Eye size={16} />}
      </button>
    </div>
  );
}

export default PasswordInput;
