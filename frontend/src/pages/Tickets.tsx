import React, { useEffect, useState, useRef, FormEvent } from "react";
import { Plus, ChevronRight, Search } from "lucide-react";
import { api, post, Row } from "../api";
import {
  statusName,
  kindName,
  date,
  Badge,
  Loading,
  Empty,
  Modal,
  PageTitle,
} from "../components/ui";

import { TicketForm } from "../components/TicketForm";
import { Pagination } from "../components/Pagination";
import type { PageResult } from "../types";
import { downloadTickets } from "../downloads";

export function Tickets({
  staff,
  onToast,
}: {
  staff: boolean;
  onToast: (s: string) => void;
}) {
  const [rows, setRows] = useState<Row[] | null>(null);
  const [filter, setFilter] = useState("ALL");
  const [query, setQuery] = useState("");
  const [selected, setSelected] = useState<Row | null>(null);
  const [create, setCreate] = useState(false);
  const [orders, setOrders] = useState<Row[]>([]);
  const [message, setMessage] = useState("");
  const [next, setNext] = useState("");
  const [busy, setBusy] = useState(false);
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(10);
  const [total, setTotal] = useState(0);
  const [pages, setPages] = useState(0);
  const [loadError, setLoadError] = useState("");
  const sequence = useRef(0);
  const load = async () => {
    const current = ++sequence.current;
    try {
      const search = new URLSearchParams({
        page: String(page),
        size: String(size),
        status: filter === "ALL" ? "" : filter,
        query,
      });
      const result = await api<PageResult<Row>>("/tickets/page?" + search);
      if (current !== sequence.current) return;
      setRows(result.items);
      setTotal(result.total);
      setPages(result.pages);
      setLoadError("");
    } catch (error) {
      if (current === sequence.current) {
        setLoadError((error as Error).message);
        setRows([]);
      }
    }
  };
  useEffect(() => {
    setRows(null);
    void load();
    return () => {
      sequence.current++;
    };
  }, [page, size, filter, query]);
  useEffect(() => {
    api("/orders")
      .then(setOrders)
      .catch(() => {});
  }, []);
  async function select(id: string) {
    try {
      const t = await api("/tickets/" + id);
      setSelected(t);
      setNext(t.status);
      setMessage("");
    } catch (e) {
      onToast((e as Error).message);
    }
  }
  const visible = rows;
  const transitions: Record<string, string[]> = {
    OPEN: ["IN_PROGRESS"],
    IN_PROGRESS: ["WAITING_CUSTOMER", "RESOLVED"],
    WAITING_CUSTOMER: ["IN_PROGRESS", "RESOLVED"],
    RESOLVED: ["IN_PROGRESS", "CLOSED"],
    CLOSED: [],
  };
  return (
    <>
      <PageTitle
        eyebrow={staff ? "SERVICE OPERATIONS" : "MY REQUESTS"}
        title={staff ? "让每件事都有回音" : "我的服务工单"}
        description={
          staff
            ? "接续客户需求，记录每一步处理与反馈。"
            : "申请、补充、解决。服务进度在这里清楚呈现。"
        }
        action={
          <div className="actions">
            <button
              className="secondary"
              onClick={() =>
                downloadTickets(filter === "ALL" ? "" : filter, query).catch(
                  (error) => onToast(error.message),
                )
              }
            >
              导出筛选结果
            </button>
            <button className="primary" onClick={() => setCreate(true)}>
              <Plus size={17} />
              创建工单
            </button>
          </div>
        }
      />
      <section className="panel">
        <div className="table-toolbar">
          <div className="tabs">
            {[
              ["ALL", "全部"],
              ["OPEN", "待受理"],
              ["IN_PROGRESS", "处理中"],
              ["WAITING_CUSTOMER", "待补充"],
              ["RESOLVED", "已解决"],
              ["CLOSED", "已关闭"],
            ].map(([v, l]) => (
              <button
                key={v}
                className={filter === v ? "active" : ""}
                onClick={() => {
                  setFilter(v);
                  setPage(1);
                }}
              >
                {l}
              </button>
            ))}
          </div>
          <div className="search">
            <Search size={15} />
            <input
              aria-label="搜索工单"
              value={query}
              maxLength={120}
              onChange={(e) => {
                setQuery(e.target.value);
                setPage(1);
              }}
              placeholder="搜索标题或编号"
            />
          </div>
        </div>
        {rows === null ? (
          <Loading />
        ) : !visible?.length ? (
          <Empty text="这里还没有工单" />
        ) : (
          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>工单 / 问题</th>
                  <th>服务类型</th>
                  {staff && <th>客户</th>}
                  <th>状态</th>
                  <th>最近更新</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {visible.map((t) => (
                  <tr key={t.id} onClick={() => select(t.id)}>
                    <td>
                      <button
                        className="row-title"
                        onClick={() => select(t.id)}
                      >
                        {t.subject}
                      </button>
                      <small>{t.ticket_no}</small>
                    </td>
                    <td>{kindName[t.kind]}</td>
                    {staff && <td>{t.customer_name}</td>}
                    <td>
                      <Badge status={t.status} />
                    </td>
                    <td className="muted">{date(t.updated_at)}</td>
                    <td>
                      <ChevronRight size={16} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
      {loadError && (
        <p role="alert" className="error">
          {loadError}
        </p>
      )}
      {rows && (
        <Pagination
          page={page}
          size={size}
          total={total}
          pages={pages}
          onPage={setPage}
          onSize={(next) => {
            setSize(next);
            setPage(1);
          }}
        />
      )}
      {create && (
        <TicketForm
          orders={orders}
          close={() => setCreate(false)}
          onDone={load}
          onToast={onToast}
        />
      )}
      {selected && (
        <Modal title="工单详情" close={() => setSelected(null)}>
          <div className="ticket-detail-title">
            <small>{selected.ticket_no}</small>
            <Badge status={selected.status} />
            <h3>{selected.subject}</h3>
            <p>
              {selected.product_name || "普通咨询"}{" "}
              {selected.order_no && "· " + selected.order_no}
            </p>
          </div>
          <div className="timeline">
            {selected.events.map((e: Row) => (
              <div
                key={e.id}
                className={"timeline-event " + e.event_type.toLowerCase()}
              >
                <span className="timeline-dot" />
                <div>
                  <strong>{e.actor_name}</strong>
                  <small>{date(e.created_at)}</small>
                  <p>
                    {e.event_type === "STATUS"
                      ? e.content
                          .split(" → ")
                          .map((x: string) => statusName[x] || x)
                          .join(" → ")
                      : e.content}
                  </p>
                </div>
              </div>
            ))}
          </div>
          {selected.status !== "CLOSED" && (
            <form
              className="ticket-reply"
              onSubmit={async (e) => {
                e.preventDefault();
                setBusy(true);
                try {
                  const t = await post("/tickets/" + selected.id + "/events", {
                    version: selected.version,
                    status: next,
                    message,
                  });
                  setSelected(t);
                  setNext(t.status);
                  setMessage("");
                  load();
                  onToast("工单已更新");
                } catch (e) {
                  onToast((e as Error).message);
                } finally {
                  setBusy(false);
                }
              }}
            >
              <label>
                {staff ? "回复客户" : "补充说明"}
                <textarea
                  aria-label="工单回复"
                  rows={3}
                  maxLength={2000}
                  value={message}
                  onChange={(e) => setMessage(e.target.value)}
                  placeholder="输入您的回复…"
                />
              </label>
              {staff ? (
                <label>
                  处理状态
                  <select
                    aria-label="处理状态"
                    value={next}
                    onChange={(e) => setNext(e.target.value)}
                  >
                    <option value={selected.status}>
                      {statusName[selected.status]}
                    </option>
                    {transitions[selected.status]?.map((x) => (
                      <option key={x} value={x}>
                        {statusName[x]}
                      </option>
                    ))}
                  </select>
                </label>
              ) : (
                selected.status === "RESOLVED" && (
                  <label className="check-label">
                    <input
                      type="checkbox"
                      checked={next === "CLOSED"}
                      onChange={(e) =>
                        setNext(e.target.checked ? "CLOSED" : selected.status)
                      }
                    />
                    问题已解决，确认关闭工单
                  </label>
                )
              )}
              <button className="primary full" disabled={busy}>
                {busy ? "提交中…" : "提交更新"}
              </button>
            </form>
          )}
        </Modal>
      )}
    </>
  );
}
