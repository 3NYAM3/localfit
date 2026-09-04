import { useState, useEffect, useRef } from "react";
import { useNavigate, Link } from "react-router-dom";
import { signOut } from "../api/auth";

/**
 * 공통 헤더
 * 로고와 프로필 드롭다운(마이페이지 / 로그아웃)을 제공한다
 */
function Header() {
  const [open, setOpen] = useState(false);
  const menuRef = useRef(null);
  const navigate = useNavigate();

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
        <Link to="/main" className="text-lg font-bold tracking-tight">
          LOCAL FIT
        </Link>

        <div className="relative" ref={menuRef}>
          <button
            onClick={() => setOpen(!open)}
            className="flex h-9 w-9 items-center justify-center rounded-full bg-stone-900 text-sm font-medium text-white transition hover:bg-stone-700"
          >
            나
          </button>

          {open && (
            <div className="absolute right-0 mt-2 w-40 overflow-hidden rounded-xl bg-white py-1 shadow-lg ring-1 ring-stone-200">
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
            </div>
          )}
        </div>
      </div>
    </header>
  );
}

export default Header;
