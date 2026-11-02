import React, { useEffect, useState, useRef, FormEvent } from "react";
import {
  MessageSquare,
  BookOpen,
  ArrowUpRight,
  ShieldCheck,
  Headphones,
  Loader2,
} from "lucide-react";
import { post, refreshCsrf, Row } from "../api";

export function Login({
  onLogin,
  demo,
}: {
  onLogin: (u: Row) => void;
  demo: boolean;
}) {
  const [username, setUsername] = useState(demo ? "customer" : "");
  const [password, setPassword] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError("");
    try {
      await refreshCsrf();
      const u = await post("/auth/login", { username, password });
      await refreshCsrf();
      onLogin(u);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  }
  return (
    <div className="login-page">
      <div className="login-story">
        <div className="brand">
          <span className="brand-icon">
            <MessageSquare size={24} />
          </span>
          知序<span className="brand-en">SERVICE DESK</span>
        </div>
        <div>
          <div className="eyebrow">好的服务，从一次理解开始</div>
          <h1>
            让每个问题，
            <br />
            都有清楚的回应。
          </h1>
          <p>
            连接知识、订单与售后服务。
            <br />
            从第一句咨询，到最后一次安心确认。
          </p>
          <div className="story-chips">
            <span>
              <BookOpen size={15} /> 有据可查
            </span>
            <span>
              <ShieldCheck size={15} /> 确认再办理
            </span>
            <span>
              <Headphones size={15} /> 人工接续
            </span>
          </div>
        </div>
        <span className="story-bottom">知序客服工作台 / 2026</span>
        <div className="orb one" />
        <div className="orb two" />
      </div>
      <div className="login-main">
        <div className="login-box">
          <span className="eyebrow green">WELCOME BACK</span>
          <h2>欢迎回来</h2>
          <p className="muted">登录后继续您的咨询与售后服务</p>
          {demo && (
            <div className="demo-switch">
              {[
                ["customer", "客户"],
                ["agent", "客服"],
                ["admin", "管理员"],
              ].map(([v, l]) => (
                <button
                  key={v}
                  className={username === v ? "selected" : ""}
                  onClick={() => {
                    setUsername(v);
                    setPassword(
                      v === "admin"
                        ? "DemoAdmin!2026"
                        : v === "agent"
                          ? "DemoAgent!2026"
                          : "DemoCustomer!2026",
                    );
                  }}
                >
                  {l}
                </button>
              ))}
            </div>
          )}
          <form onSubmit={submit}>
            <label>
              用户名
              <input
                required
                autoComplete="username"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
              />
            </label>
            <label>
              密码
              <input
                required
                type="password"
                autoComplete="current-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
            </label>
            {error && (
              <div role="alert" className="error">
                {error}
              </div>
            )}
            <button className="primary full" disabled={busy}>
              {busy ? <Loader2 className="spin" size={18} /> : null}登录工作台
              <ArrowUpRight size={18} />
            </button>
          </form>
          {demo && (
            <p className="demo-note">
              演示环境 · 数据均为虚构
              <br />
              点击上方角色可填入演示账号。
            </p>
          )}
        </div>
      </div>
    </div>
  );
}
