import { Navigate, useLocation } from "react-router-dom";

/**
 * 인증이 필요한 라우트를 감싸는 컴포넌트
 * 토큰이 없으면 로그인 페이지로 보내되, 원래 가려던 경로를 함께 전달한다
 */
function PrivateRoute({ children }) {
  const location = useLocation();
  const token = localStorage.getItem("accessToken");

  if (!token) {
    return (
      <Navigate
        to="/login"
        state={{ from: location.pathname + location.search }}
        replace
      />
    );
  }

  return children;
}

export default PrivateRoute;
