import React, { useEffect, useState, useRef, FormEvent } from "react";
import {
  Ticket,
  BookOpen,
  ArrowUpRight,
  Clock,
  Activity,
  CheckCircle2,
} from "lucide-react";
import { api, Row } from "../api";
import { date, Badge, Loading, Empty, PageTitle } from "../components/ui";

export function Overview({
  go,
  onToast,
}: {
  go: (s: string) => void;
  onToast: (s: string) => void;
}) {
  const [data, setData] = useState<Row | null>(null);
  const [rows, setRows] = useState<Row[]>([]);
  useEffect(() => {
    Promise.all([api("/admin/overview"), api("/tickets")])
      .then(([d, t]) => {
        setData(d);
        setRows(t);
      })
      .catch((e) => onToast(e.message));
  }, []);
  if (!data) return <Loading />;
  const count = (status: string) =>
    data.tickets.find((x: Row) => x.status === status)?.total || 0;
  return (
    <>
      <PageTitle
        eyebrow="SERVICE OVERVIEW"
        title="今天，也把服务做好"
        description="从待处理的需求开始，让每一位客户都能看到进展。"
        action={
          <button className="secondary" onClick={() => go("tickets")}>
            进入工单台
            <ArrowUpRight size={17} />
          </button>
        }
      />
      <div className="stats-grid">
        {[
          [count("OPEN"), "待受理工单", Ticket, "优先关注新的需求"],
          [count("IN_PROGRESS"), "正在处理", Clock, "保持沟通，持续跟进"],
          [
            count("RESOLVED") + count("CLOSED"),
            "已解决工单",
            CheckCircle2,
            "每一次解决，都有记录",
          ],
          [data.knowledge, "有效知识文档", BookOpen, "支持有依据的服务回复"],
        ].map(([num, label, Icon, desc], i) => {
          const I = Icon as typeof Ticket;
          return (
            <article key={i} className="stat-card">
              <div>
                <span>{label as string}</span>
                <I size={19} />
              </div>
              <strong>{num as number}</strong>
              <small>{desc as string}</small>
            </article>
          );
        })}
      </div>
      <div className="overview-grid">
        <section className="panel">
          <div className="panel-head">
            <h3>近期服务需求</h3>
            <button className="text-btn" onClick={() => go("tickets")}>
              查看全部
              <ArrowUpRight size={15} />
            </button>
          </div>
          {rows.slice(0, 6).map((t) => (
            <button
              className="recent-ticket"
              key={t.id}
              onClick={() => go("tickets")}
            >
              <span className="ticket-symbol">
                <Ticket size={19} />
              </span>
              <div>
                <strong>{t.subject}</strong>
                <small>
                  {t.customer_name} · {t.ticket_no}
                </small>
              </div>
              <Badge status={t.status} />
            </button>
          ))}
          {!rows.length && <Empty text="暂无待处理需求" />}
        </section>
        <section className="panel run-panel">
          <div className="panel-head">
            <h3>AI 服务记录</h3>
            <Activity size={18} />
          </div>
          <p className="run-total">
            <b>{data.modelRuns}</b> 次咨询请求
          </p>
          {data.recentRuns.map((r: Row) => (
            <div className="run-row" key={r.id}>
              <i className={r.state === "COMPLETED" ? "ok" : ""} />
              <div>
                <strong>
                  {r.model_mode === "OLLAMA"
                    ? "模型辅助处理"
                    : r.model_mode
                      ? "检索模式"
                      : "处理中 / 中断"}
                </strong>
                <small>{date(r.created_at)}</small>
              </div>
              <span>
                {r.latency_ms ? Math.round(r.latency_ms / 1000) + "s" : "—"}
              </span>
            </div>
          ))}
          <p className="form-hint">
            记录区分模型辅助和资料检索，不把接口成功等同于回答正确。
          </p>
        </section>
      </div>
    </>
  );
}
