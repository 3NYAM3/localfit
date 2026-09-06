import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { getMyInfo, updateNickname, updatePassword } from "../../api/user";
import PasswordInput from "../PasswordInput";

/**
 * 마이페이지 - 계정 탭
 * 회원 정보 조회, 닉네임 변경, 비밀번호 변경
 */
function AccountTab() {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState(null); // "nickname" | "password" | null

  useEffect(() => {
    const fetchUser = async () => {
      try {
        const response = await getMyInfo();
        setUser(response.data.data);
      } finally {
        setLoading(false);
      }
    };
    fetchUser();
  }, []);

  if (loading) {
    return (
      <p className="py-12 text-center text-sm text-stone-400">불러오는 중...</p>
    );
  }

  return (
    <div className="space-y-3">
      {/* 이메일 */}
      <div className="rounded-xl bg-white p-5 ring-1 ring-stone-200">
        <p className="text-xs text-stone-500">이메일</p>
        <p className="mt-1 text-sm font-medium">{user?.email}</p>
      </div>

      {/* 닉네임 */}
      <div className="rounded-xl bg-white p-5 ring-1 ring-stone-200">
        {editing === "nickname" ? (
          <NicknameForm
            currentNickname={user.nickname}
            onSuccess={(updated) => {
              setUser(updated);
              setEditing(null);
            }}
            onCancel={() => setEditing(null)}
          />
        ) : (
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs text-stone-500">닉네임</p>
              <p className="mt-1 text-sm font-medium">{user?.nickname}</p>
            </div>
            <button
              onClick={() => setEditing("nickname")}
              className="rounded-lg px-3 py-1.5 text-xs font-medium ring-1 ring-stone-300 transition hover:bg-stone-50"
            >
              변경
            </button>
          </div>
        )}
      </div>

      {/* 비밀번호 */}
      <div className="rounded-xl bg-white p-5 ring-1 ring-stone-200">
        {editing === "password" ? (
          <PasswordForm onCancel={() => setEditing(null)} />
        ) : (
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs text-stone-500">비밀번호</p>
              <p className="mt-1 text-sm font-medium">••••••••</p>
            </div>
            <button
              onClick={() => setEditing("password")}
              className="rounded-lg px-3 py-1.5 text-xs font-medium ring-1 ring-stone-300 transition hover:bg-stone-50"
            >
              변경
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

/** 닉네임 변경 폼 */
function NicknameForm({ currentNickname, onSuccess, onCancel }) {
  const [nickname, setNickname] = useState(currentNickname);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setSaving(true);

    try {
      const response = await updateNickname(nickname);
      onSuccess(response.data.data);
    } catch (err) {
      setError(err.response?.data?.message ?? "변경에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      <p className="text-xs text-stone-500">닉네임</p>
      <input
        type="text"
        value={nickname}
        onChange={(e) => setNickname(e.target.value)}
        autoFocus
        className="mt-2 w-full rounded-lg border border-stone-300 px-3 py-2 text-sm outline-none transition focus:border-stone-900 focus:ring-1 focus:ring-stone-900"
      />

      {error && <p className="mt-2 text-xs text-red-600">{error}</p>}

      <div className="mt-3 flex gap-2">
        <button
          type="submit"
          disabled={saving}
          className="rounded-lg bg-stone-900 px-4 py-2 text-xs font-medium text-white transition hover:bg-stone-700 disabled:opacity-50"
        >
          저장
        </button>
        <button
          type="button"
          onClick={onCancel}
          className="rounded-lg px-4 py-2 text-xs font-medium text-stone-500 transition hover:bg-stone-50"
        >
          취소
        </button>
      </div>
    </form>
  );
}

/** 비밀번호 변경 폼 */
function PasswordForm({ onCancel }) {
  const [form, setForm] = useState({ currentPassword: "", newPassword: "" });
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const navigate = useNavigate();

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm({ ...form, [name]: value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setSaving(true);

    try {
      await updatePassword(form.currentPassword, form.newPassword);
      // 세션이 폐기되므로 토큰을 정리하고 로그인 화면으로 보낸다
      localStorage.removeItem("accessToken");
      localStorage.removeItem("refreshToken");
      navigate("/login");
    } catch (err) {
      setError(err.response?.data?.message ?? "변경에 실패했습니다.");
      setSaving(false);
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      <p className="text-xs text-stone-500">비밀번호 변경</p>

      <PasswordInput
        name="currentPassword"
        value={form.currentPassword}
        onChange={handleChange}
        placeholder="현재 비밀번호"
        autoFocus
        className="mt-2"
      />
      <PasswordInput
        name="newPassword"
        value={form.newPassword}
        onChange={handleChange}
        placeholder="새 비밀번호"
        className="mt-2"
      />
      <p className="mt-1.5 text-[11px] text-stone-400">
        영문·숫자·특수문자 포함 8자 이상
      </p>

      {error && <p className="mt-2 text-xs text-red-600">{error}</p>}

      <p className="mt-2 text-[11px] text-stone-400">
        변경 후 보안을 위해 다시 로그인해야 합니다.
      </p>

      <div className="mt-3 flex gap-2">
        <button
          type="submit"
          disabled={saving}
          className="rounded-lg bg-stone-900 px-4 py-2 text-xs font-medium text-white transition hover:bg-stone-700 disabled:opacity-50"
        >
          변경
        </button>
        <button
          type="button"
          onClick={onCancel}
          className="rounded-lg px-4 py-2 text-xs font-medium text-stone-500 transition hover:bg-stone-50"
        >
          취소
        </button>
      </div>
    </form>
  );
}

export default AccountTab;
