import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { signIn } from "../api/auth";

/**
 * 로그인 화면
 * 성공 시 토큰을 저장하고 관심지역 페이지로 이동한다
 */

function LoginPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();

    try {
      await signIn(email, password);
      navigate("/favorites");
    } catch (err) {
      setError(err.response?.data?.message ?? "로그인에 실패했습니다.");
    }
  };

  return (
    <div style={{ maxWidth: 360, margin: "80px auto", padding: 20 }}>
      <h1>LOCAL FIT</h1>
      <p>공공데이터 기반 수도권 지역 분석·추천</p>

      <form onSubmit={handleSubmit}>
        <input
          type="email"
          placeholder="이메일"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          style={{ width: "100%", padding: 10, marginBottom: 10 }}
        />
        <input
          type="password"
          placeholder="비밀번호"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          style={{ width: "100%", padding: 10, marginBottom: 10 }}
        />

        {error && <p style={{ color: "red" }}>{error}</p>}

        <button type="submit" style={{ width: "100%", padding: 10 }}>
          로그인
        </button>
      </form>
    </div>
  );
}

export default LoginPage;
