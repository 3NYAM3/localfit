import { useState, useEffect, useRef } from "react";
import { useNavigate, Link } from "react-router-dom";
import { signOut } from "../api/auth";

/**
 * 공통 헤더
 * 로그인 여부에 따라 프로필 드롭다운 내용이 달라진다
 */
function Header() {
  const [open, setOpen] = useState(false);
  const menuRef = useRef(null);
  const navigate = useNavigate();

  const isLoggedIn = Boolean(localStorage.getItem("accessToken"));

  // 드롭다운 바깥을 클릭하면 닫기
  useEffect(() => {
    const handleClickOutside = (e) => {
      if (menuRef.current && !menuRef.current.contains(e.target)) {
        setOpen(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const handleSignOut = async () => {
    await signOut();
    navigate("/");
  };

  return (
    <header className="sticky top-0 z-10 border-b border-stone-200 bg-stone-100/80 backdrop-blur">
      <div className="mx-auto flex h-16 max-w-5xl items-center justify-between px-6">
        <Link to="/" className="text-lg font-bold tracking-tight">
          LOCAL FIT
        </Link>

        <div className="relative" ref={menuRef}>
          <button
            onClick={() => setOpen(!open)}
            className={`flex h-9 w-9 items-center justify-center rounded-full text-sm font-medium transition ${
              isLoggedIn
                ? "bg-stone-900 text-white hover:bg-stone-700"
                : "bg-white text-stone-500 ring-1 ring-stone-300 hover:bg-stone-50"
            }`}
            aria-label="메뉴"
          >
            {isLoggedIn ? "나" : "···"}
          </button>

          {open && (
            <div className="absolute right-0 mt-2 w-40 overflow-hidden rounded-xl bg-white py-1 shadow-lg ring-1 ring-stone-200">
              {isLoggedIn ? (
                <>
                  <Link
                    to="/mypage"
                    onClick={() => setOpen(false)}
                    className="block px-4 py-2.5 text-sm transition hover:bg-stone-50"
                  >
                    마이페이지
                  </Link>
                  <button
                    onClick={handleSignOut}
                    className="block w-full px-4 py-2.5 text-left text-sm text-red-600 transition hover:bg-stone-50"
                  >
                    로그아웃
                  </button>
                </>
              ) : (
                <>
                  <Link
                    to="/login"
                    onClick={() => setOpen(false)}
                    className="block px-4 py-2.5 text-sm transition hover:bg-stone-50"
                  >
                    로그인
                  </Link>
                  <Link
                    to="/signup"
                    onClick={() => setOpen(false)}
                    className="block px-4 py-2.5 text-sm transition hover:bg-stone-50"
                  >
                    회원가입
                  </Link>
                </>
              )}
            </div>
          )}
        </div>
      </div>
    </header>
  );
}

export default Header;
