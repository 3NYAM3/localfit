import { Link } from "react-router-dom";

/**
 * 로그인·회원가입 화면용 간소화 헤더
 * 로고만 두어 인증 흐름에 집중하도록 한다
 */
function SimpleHeader() {
  return (
    <header className="border-b border-stone-200">
      <div className="mx-auto flex h-16 max-w-5xl items-center px-6">
        <Link
          to="/"
          className="text-lg font-bold tracking-tight transition hover:text-stone-600"
        >
          LOCAL FIT
        </Link>
      </div>
    </header>
  );
}

export default SimpleHeader;
