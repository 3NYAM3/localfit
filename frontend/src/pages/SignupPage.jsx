import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { signup } from "../api/auth";

/**
 * 회원가입 화면
 * 성공 시 로그인 페이지로 이동한다
 */
function SignupPage() {
  const [form, setForm] = useState({ email: "", password: "", nickname: "" });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm({ ...form, [name]: value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      await signup(form.email, form.password, form.nickname);
      navigate("/login");
    } catch (err) {
      setError(err.response?.data?.message ?? "회원가입에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center px-4 py-12">
      <div className="w-full max-w-sm">
        <div className="mb-10 text-center">
          <Link
            to="/"
            className="text-sm font-bold tracking-tight text-stone-400 transition hover:text-stone-600"
          >
            LOCAL FIT
          </Link>
          <h1 className="mt-4 text-4xl font-bold tracking-tight">회원가입</h1>
          <p className="mt-3 text-sm text-stone-500">
            LOCAL FIT과 함께 동네를 찾아보세요
          </p>
        </div>

        <div className="rounded-2xl bg-white p-8 shadow-sm ring-1 ring-stone-200">
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="mb-1.5 block text-sm font-medium text-stone-700">
                이메일
              </label>
              <input
                type="email"
                name="email"
                value={form.email}
                onChange={handleChange}
                placeholder="name@example.com"
                className="w-full rounded-lg border border-stone-300 px-3.5 py-2.5 text-sm outline-none transition focus:border-stone-900 focus:ring-1 focus:ring-stone-900"
              />
            </div>

            <div>
              <label className="mb-1.5 block text-sm font-medium text-stone-700">
                비밀번호
              </label>
              <input
                type="password"
                name="password"
                value={form.password}
                onChange={handleChange}
                placeholder="••••••••"
                className="w-full rounded-lg border border-stone-300 px-3.5 py-2.5 text-sm outline-none transition focus:border-stone-900 focus:ring-1 focus:ring-stone-900"
              />
              <p className="mt-1.5 text-xs text-stone-500">
                영문·숫자·특수문자 포함 8자 이상
              </p>
            </div>

            <div>
              <label className="mb-1.5 block text-sm font-medium text-stone-700">
                닉네임
              </label>
              <input
                type="text"
                name="nickname"
                value={form.nickname}
                onChange={handleChange}
                placeholder="사용할 닉네임"
                className="w-full rounded-lg border border-stone-300 px-3.5 py-2.5 text-sm outline-none transition focus:border-stone-900 focus:ring-1 focus:ring-stone-900"
              />
            </div>

            {error && (
              <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600">
                {error}
              </p>
            )}

            <button
              type="submit"
              disabled={loading}
              className="w-full rounded-lg bg-stone-900 py-2.5 text-sm font-medium text-white transition hover:bg-stone-800 disabled:opacity-50"
            >
              {loading ? "가입 중..." : "가입하기"}
            </button>
          </form>
        </div>
        <p className="mt-6 text-center text-sm text-stone-500">
          이미 계정이 있으신가요?{" "}
          <Link
            to="/login"
            className="font-medium text-stone-900 hover:underline"
          >
            로그인
          </Link>
        </p>
      </div>
    </div>
  );
}

export default SignupPage;
