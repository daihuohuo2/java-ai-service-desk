import { useCallback, useEffect, useState } from "react";
import {
  Navigate,
  NavLink,
  Route,
  Routes,
  useLocation,
  useNavigate,
} from "react-router-dom";
import {
  Activity,
  AlertCircle,
  BookOpen,
  ChevronRight,
  LayoutDashboard,
  LogOut,
  Menu,
  MessageSquare,
  Package,
  ShieldCheck,
  ShoppingBag,
  Ticket,
  X,
} from "lucide-react";
import { useAuth } from "./hooks/AuthContext";
import type { User } from "./types";
import { Loading } from "./components/ui";
import { Login } from "./pages/Login";
import { Chat } from "./pages/Chat";
import { Orders } from "./pages/Orders";
import { Tickets } from "./pages/Tickets";
import { Overview } from "./pages/Overview";
import { Knowledge } from "./pages/Knowledge";
import { Products } from "./pages/Products";
import { Audit } from "./pages/Audit";

export function App() {
  const { user, ready, demo, setUser, logout } = useAuth();
  const [mobile, setMobile] = useState(false);
  const [toast, setToast] = useState("");
  const navigate = useNavigate();
  const location = useLocation();
  const notify = useCallback((message: string) => setToast(message), []);
  const go = useCallback(
    (page: string) => {
      navigate("/" + page);
      setMobile(false);
    },
    [navigate],
  );
  useEffect(() => {
    if (!toast) return;
    const timer = setTimeout(() => setToast(""), 6000);
    return () => clearTimeout(timer);
  }, [toast]);
  if (!ready) return <Loading />;
  if (!user)
    return <Login demo={demo} onLogin={(value) => setUser(value as User)} />;
  const staff = user.role !== "CUSTOMER";
  const admin = user.role === "ADMIN";
  const nav = [
    ...(staff
      ? [{ key: "overview", label: "服务概览", icon: LayoutDashboard }]
      : []),
    { key: "chat", label: "智能客服", icon: MessageSquare },
    { key: "orders", label: "我的订单", icon: Package },
    { key: "tickets", label: staff ? "工单工作台" : "我的工单", icon: Ticket },
    ...(admin
      ? [
          { key: "knowledge", label: "知识资料库", icon: BookOpen },
          { key: "products", label: "商品管理", icon: ShoppingBag },
          { key: "audit", label: "操作记录", icon: Activity },
        ]
      : []),
  ];
  const home = staff ? "/overview" : "/chat";
  return (
    <div className="app-shell">
      <aside className={"sidebar " + (mobile ? "is-open" : "")}>
        <div className="brand">
          <span className="brand-icon">
            <MessageSquare size={23} />
          </span>
          知序<span className="version">DESK</span>
        </div>
        <div className="workspace-name">
          客户服务中心 <span>工作空间</span>
        </div>
        <div className="nav-label">工作台</div>
        <nav>
          {nav.map((item) => (
            <NavLink
              key={item.key}
              to={"/" + item.key}
              onClick={() => setMobile(false)}
              className={({ isActive }) => (isActive ? "active" : "")}
            >
              <item.icon size={19} />
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-note">
          <span className="small-orb" />
          <h4>每一次回应，都有依据</h4>
          <p>
            知识有来源，处理有记录。
            <br />
            需要帮助时，人工客服随时接续。
          </p>
        </div>
        <div className="profile">
          <span className="avatar">{user.display_name.slice(0, 1)}</span>
          <div>
            <strong>{user.display_name}</strong>
            <small>
              {admin ? "系统管理员" : staff ? "客服专员" : "客户账号"}
            </small>
          </div>
          <button
            aria-label="退出登录"
            className="icon-btn"
            onClick={() => logout().catch((error) => notify(error.message))}
          >
            <LogOut size={17} />
          </button>
        </div>
      </aside>
      <div className="app-main">
        <header className="topbar">
          <div>
            <button
              className="mobile-menu icon-btn"
              aria-label="展开菜单"
              onClick={() => setMobile(!mobile)}
            >
              <Menu size={20} />
            </button>
            <span className="muted">工作台</span>
            <ChevronRight size={14} />
            <span>
              {nav.find((item) => "/" + item.key === location.pathname)?.label}
            </span>
          </div>
          <div className="topbar-right">
            {demo && <span className="demo-badge">演示环境</span>}
            <span className="secure-note">
              <ShieldCheck size={14} />
              私有会话
            </span>
          </div>
        </header>
        <main>
          <Routes key={user.id}>
            <Route path="/chat" element={<Chat onToast={notify} go={go} />} />
            <Route
              path="/orders"
              element={<Orders onToast={notify} go={go} />}
            />
            <Route
              path="/tickets"
              element={<Tickets staff={staff} onToast={notify} />}
            />
            <Route
              path="/overview"
              element={
                staff ? (
                  <Overview go={go} onToast={notify} />
                ) : (
                  <Navigate to={home} replace />
                )
              }
            />
            <Route
              path="/knowledge"
              element={
                admin ? (
                  <Knowledge onToast={notify} />
                ) : (
                  <Navigate to={home} replace />
                )
              }
            />
            <Route
              path="/products"
              element={
                admin ? (
                  <Products onToast={notify} />
                ) : (
                  <Navigate to={home} replace />
                )
              }
            />
            <Route
              path="/audit"
              element={
                admin ? (
                  <Audit onToast={notify} />
                ) : (
                  <Navigate to={home} replace />
                )
              }
            />
            <Route path="*" element={<Navigate to={home} replace />} />
          </Routes>
        </main>
        <footer className="app-footer">
          <span>知序 · 让服务有条理</span>
          <span>
            {demo ? "虚构商品与订单，仅供功能演示" : "客户服务工作台"}
          </span>
        </footer>
      </div>
      {toast && (
        <div role="status" className="toast">
          <AlertCircle size={17} />
          {toast}
          <button
            className="icon-btn"
            aria-label="关闭提示"
            onClick={() => setToast("")}
          >
            <X size={16} />
          </button>
        </div>
      )}
    </div>
  );
}
