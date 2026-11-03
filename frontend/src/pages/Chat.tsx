import type {
  ChatThread,
  ChatMessage,
  TicketDetail,
  Proposal as ProposalData,
} from "../types";
import React, { useEffect, useState, useRef, FormEvent } from "react";
import {
  MessageSquare,
  Ticket,
  Package,
  BookOpen,
  ArrowUpRight,
  ArrowUp,
  Plus,
  ChevronRight,
  Check,
  Clock,
  FileText,
  ShieldCheck,
  RefreshCw,
  Headphones,
  Loader2,
} from "lucide-react";
import { api, post, askStream } from "../api";
import { kindName, money, date, Badge, PageTitle } from "../components/ui";

export function Chat({
  onToast,
  go,
}: {
  onToast: (s: string) => void;
  go: (s: string) => void;
}) {
  const [threads, setThreads] = useState<ChatThread[]>([]);
  const [current, setCurrent] = useState("");
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState("");
  const [busy, setBusy] = useState(false);
  const [phase, setPhase] = useState("");
  const [history, setHistory] = useState(false);
  const end = useRef<HTMLDivElement>(null);
  const loadThreads = () => api<ChatThread[]>("/chat/threads").then(setThreads);
  useEffect(() => {
    loadThreads().catch((e) => onToast(e.message));
  }, []);
  useEffect(() => {
    end.current?.scrollIntoView({ behavior: "smooth", block: "nearest" });
  }, [messages, phase]);
  async function send(value = input) {
    if (!value.trim() || busy) return;
    setBusy(true);
    setInput("");
    setPhase("正在准备咨询…");
    let id = current;
    try {
      if (!id) {
        id = (await post<ChatThread>("/chat/threads")).id;
        setCurrent(id);
      }
      setMessages((x) => [...x, { role: "user", content: value }]);
      const result = await askStream(id, value, setPhase, crypto.randomUUID());
      setMessages((x) => [
        ...x,
        { role: "assistant", content: result.answer, detail: result },
      ]);
      await loadThreads();
    } catch (e) {
      onToast((e as Error).message);
      if (id)
        api<ChatMessage[]>("/chat/threads/" + id + "/messages")
          .then(setMessages)
          .catch(() => {});
    } finally {
      setBusy(false);
      setPhase("");
    }
  }
  async function open(id: string) {
    try {
      setMessages(
        await api<ChatMessage[]>("/chat/threads/" + id + "/messages"),
      );
      setCurrent(id);
      setHistory(false);
    } catch (e) {
      onToast((e as Error).message);
    }
  }
  const questions = [
    "栖光阅读灯需要充电吗？",
    "签收后可以退货吗？",
    "查询订单 ZS20261030001",
    "我需要人工客服帮助",
  ];
  return (
    <>
      <PageTitle
        eyebrow="CUSTOMER CARE"
        title="每个问题，都值得认真回答"
        description="查询商品知识、跟进订单，或让我们帮您安排售后。"
        action={
          <button
            className="secondary"
            disabled={busy}
            onClick={() => {
              setCurrent("");
              setMessages([]);
            }}
          >
            <Plus size={17} />
            新的咨询
          </button>
        }
      />
      <div className="chat-grid">
        <section className="chat-card">
          <div className="chat-card-head">
            <div className="assistant-mark">
              <MessageSquare size={20} />
            </div>
            <div>
              <strong>知序服务助手</strong>
              <span>
                <i /> 知识问答 · 订单查询 · 售后协助
              </span>
            </div>
            <button
              className="text-btn"
              disabled={busy}
              onClick={() => setHistory(!history)}
            >
              <Clock size={15} />
              历史咨询
            </button>
          </div>
          {history && (
            <div className="thread-list">
              {threads.length ? (
                threads.map((t) => (
                  <button key={t.id} onClick={() => open(t.id)}>
                    <MessageSquare size={15} />
                    {t.title}
                    <small>{date(t.created_at)}</small>
                  </button>
                ))
              ) : (
                <p>还没有历史咨询</p>
              )}
            </div>
          )}
          <div className="messages" aria-live="polite">
            {!messages.length ? (
              <div className="chat-welcome">
                <div className="welcome-symbol">
                  <MessageSquare size={33} />
                  <span>✦</span>
                </div>
                <h2>您好，有什么可以帮您？</h2>
                <p>
                  告诉我您遇到的问题，
                  <br />
                  我会先查找资料，再帮您找到下一步。
                </p>
                <div className="suggestions">
                  {questions.map((q, i) => (
                    <button key={q} disabled={busy} onClick={() => send(q)}>
                      {i === 0 ? (
                        <BookOpen size={17} />
                      ) : i === 1 ? (
                        <RefreshCw size={17} />
                      ) : i === 2 ? (
                        <Package size={17} />
                      ) : (
                        <Headphones size={17} />
                      )}
                      <span>{q}</span>
                      <ArrowUpRight size={16} />
                    </button>
                  ))}
                </div>
              </div>
            ) : (
              messages.map((m, i) => (
                <div key={m.id || i} className={"message " + m.role}>
                  <span className="message-avatar">
                    {m.role === "user" ? "我" : <MessageSquare size={16} />}
                  </span>
                  <div>
                    <div className="message-label">
                      {m.role === "user" ? "您" : "知序服务助手"}
                      {m.detail && (
                        <small>
                          {m.detail.mode === "OLLAMA"
                            ? "AI 辅助 · 已校验"
                            : m.detail.mode === "RETRIEVAL_ONLY"
                              ? "资料检索模式"
                              : "模型暂不可用 · 资料检索"}
                        </small>
                      )}
                    </div>
                    <div className="message-body">{m.content}</div>
                    {m.detail && m.detail.citations.length > 0 && (
                      <div className="citations">
                        {m.detail.citations.map((c, j) => (
                          <details key={c.id}>
                            <summary>
                              <FileText size={13} />[{j + 1}] {c.title}
                              <small>第 {c.ordinal} 段</small>
                            </summary>
                            <p>{c.content}</p>
                          </details>
                        ))}
                      </div>
                    )}
                    {m.detail?.orders?.map((o) => (
                      <div className="inline-order" key={o.id}>
                        <div>
                          <strong>{o.product_name}</strong>
                          <Badge status={o.status} />
                        </div>
                        <small>{o.order_no}</small>
                        <b>{money(o.total_cents)}</b>
                      </div>
                    ))}
                    {m.detail?.proposal && (
                      <Proposal
                        data={m.detail.proposal}
                        onToast={onToast}
                        go={go}
                      />
                    )}
                  </div>
                </div>
              ))
            )}
            {busy && (
              <div className="thinking">
                <Loader2 size={16} className="spin" />
                {phase}
              </div>
            )}
            <div ref={end} />
          </div>
          <form
            className="composer"
            onSubmit={(e) => {
              e.preventDefault();
              send();
            }}
          >
            <textarea
              aria-label="输入咨询内容"
              placeholder="描述您的问题，或输入订单号…"
              maxLength={1000}
              value={input}
              onChange={(e) => setInput(e.target.value)}
              disabled={busy}
              onKeyDown={(e) => {
                if (
                  e.key === "Enter" &&
                  !e.shiftKey &&
                  !e.nativeEvent.isComposing
                ) {
                  e.preventDefault();
                  send();
                }
              }}
            />
            <button
              aria-label="发送咨询"
              className="send"
              disabled={busy || !input.trim()}
            >
              {busy ? (
                <Loader2 className="spin" size={20} />
              ) : (
                <ArrowUp size={21} />
              )}
            </button>
          </form>
          <div className="composer-note">
            <ShieldCheck size={12} /> 办理售后前会请您确认，AI
            不会直接批准退款。<span>Enter 发送</span>
          </div>
        </section>
        <aside className="chat-rail">
          <div className="rail-heading">您的服务，从这里开始</div>
          <button className="rail-link" onClick={() => go("orders")}>
            <span className="rail-icon">
              <Package size={22} />
            </span>
            <div>
              <strong>查看我的订单</strong>
              <p>商品、物流与签收状态</p>
            </div>
            <ChevronRight size={17} />
          </button>
          <button className="rail-link" onClick={() => go("tickets")}>
            <span className="rail-icon amber">
              <Ticket size={22} />
            </span>
            <div>
              <strong>跟进售后工单</strong>
              <p>处理进度与客服回复</p>
            </div>
            <ChevronRight size={17} />
          </button>
          <div className="service-promise">
            <span className="eyebrow">HOW IT WORKS</span>
            <h3>
              让办理过程
              <br />
              清楚一点。
            </h3>
            <ol>
              <li>
                <span>01</span>
                <div>
                  <strong>先了解问题</strong>
                  <p>结合资料与本人订单核对</p>
                </div>
              </li>
              <li>
                <span>02</span>
                <div>
                  <strong>由您确认申请</strong>
                  <p>检查草稿，确认后提交</p>
                </div>
              </li>
              <li>
                <span>03</span>
                <div>
                  <strong>人工继续处理</strong>
                  <p>进度、回复和结果都有记录</p>
                </div>
              </li>
            </ol>
          </div>
          <div className="rail-foot">
            <ShieldCheck size={16} />
            <span>只展示当前账号的订单信息</span>
          </div>
        </aside>
      </div>
    </>
  );
}

function Proposal({
  data,
  onToast,
  go,
}: {
  data: ProposalData;
  onToast: (s: string) => void;
  go: (s: string) => void;
}) {
  const [busy, setBusy] = useState(false);
  const [ticket, setTicket] = useState<TicketDetail | null>(null);
  return (
    <div className="proposal">
      <div>
        <Ticket size={18} />
        <strong>待确认 · {kindName[data.kind]}</strong>
      </div>
      <p>{data.description}</p>
      <small>有效期至 {date(data.expires_at)}</small>
      {ticket ? (
        <button className="primary" onClick={() => go("tickets")}>
          <Check size={16} />
          已创建 {ticket.ticket_no} · 查看
        </button>
      ) : (
        <button
          className="primary"
          disabled={busy}
          onClick={async () => {
            setBusy(true);
            try {
              setTicket(
                await post<TicketDetail>("/proposals/" + data.id + "/confirm"),
              );
              onToast("工单已创建，客服将继续跟进");
            } catch (e) {
              onToast((e as Error).message);
            } finally {
              setBusy(false);
            }
          }}
        >
          {busy ? <Loader2 className="spin" size={16} /> : <Check size={16} />}
          确认创建工单
        </button>
      )}
    </div>
  );
}
