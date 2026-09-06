import { useState } from "react";
import Header from "../components/Header";
import AccountTab from "../components/mypage/AccountTab";
import FavoriteTab from "../components/mypage/FavoriteTab";

const TABS = [
  { key: "account", label: "계정" },
  { key: "favorite", label: "관심지역" },
];

/**
 * 마이페이지
 * 계정 정보와 관심지역을 탭으로 나눠 관리한다
 */
function MyPage() {
  const [activeTab, setActiveTab] = useState("account");

  return (
    <div className="min-h-screen">
      <Header />

      <main className="mx-auto max-w-2xl px-6 py-12">
        <h1 className="text-2xl font-bold">마이페이지</h1>

        {/* 탭 */}
        <div className="mt-8 flex gap-1 rounded-lg bg-stone-200 p-1">
          {TABS.map((tab) => (
            <button
              key={tab.key}
              onClick={() => setActiveTab(tab.key)}
              className={`flex-1 rounded-md py-2 text-xs font-medium transition ${
                activeTab === tab.key
                  ? "bg-white text-stone-900 shadow-sm"
                  : "text-stone-500 hover:text-stone-700"
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>

        <div className="mt-6">
          {activeTab === "account" ? <AccountTab /> : <FavoriteTab />}
        </div>
      </main>
    </div>
  );
}

export default MyPage;
