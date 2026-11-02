import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { ArrowUpRight, ShoppingBag } from "lucide-react";
import { api } from "../api";
import type { Order, PageResult } from "../types";
import {
  Badge,
  Empty,
  Loading,
  PageTitle,
  date,
  money,
  statusName,
} from "../components/ui";
import { TicketForm } from "../components/TicketForm";
import { Pagination } from "../components/Pagination";

export function Orders({
  onToast,
  go,
}: {
  onToast: (message: string) => void;
  go: (page: string) => void;
}) {
  const [params, setParams] = useSearchParams();
  const page = Math.max(1, Number(params.get("page")) || 1);
  const size = [1, 5, 10, 20, 50].includes(Number(params.get("size")))
    ? Number(params.get("size"))
    : 10;
  const status = params.get("status") || "";
  const query = params.get("query") || "";
  const [draft, setDraft] = useState(query);
  const [result, setResult] = useState<PageResult<Order> | null>(null);
  const [error, setError] = useState("");
  const [selected, setSelected] = useState<string | null>(null);
  function change(values: Record<string, string>) {
    const next = new URLSearchParams(params);
    for (const [key, value] of Object.entries(values))
      value ? next.set(key, value) : next.delete(key);
    setParams(next);
  }
  useEffect(() => {
    setDraft(query);
  }, [query]);
  useEffect(() => {
    const abort = new AbortController();
    setResult(null);
    setError("");
    const search = new URLSearchParams({
      page: String(page),
      size: String(size),
      status,
      query,
    });
    api<PageResult<Order>>("/orders/page?" + search, { signal: abort.signal })
      .then(setResult)
      .catch((failure) => {
        if (!abort.signal.aborted) setError(failure.message);
      });
    return () => abort.abort();
  }, [page, size, status, query]);
  return (
    <>
      <PageTitle
        eyebrow="MY ORDERS"
        title="我的订单"
        description="按状态和订单号查找记录，需要帮助时可直接发起服务申请。"
      />
      <form
        className="filter-bar"
        onSubmit={(event) => {
          event.preventDefault();
          change({ query: draft.trim(), page: "1" });
        }}
      >
        <label>
          订单状态
          <select
            aria-label="订单状态"
            value={status}
            onChange={(event) =>
              change({ status: event.target.value, page: "1" })
            }
          >
            <option value="">全部状态</option>
            {["PAID", "SHIPPED", "DELIVERED", "CANCELLED"].map((value) => (
              <option key={value} value={value}>
                {statusName[value]}
              </option>
            ))}
          </select>
        </label>
        <label>
          订单号
          <input
            aria-label="订单号关键词"
            value={draft}
            maxLength={40}
            onChange={(event) => setDraft(event.target.value)}
            placeholder="输入订单号关键词"
          />
        </label>
        <button className="primary" type="submit">
          查询订单
        </button>
        <button
          className="secondary"
          type="button"
          onClick={() => {
            setDraft("");
            setParams({});
          }}
        >
          重置
        </button>
      </form>
      {error ? (
        <div role="alert" className="error">
          {error}
        </div>
      ) : !result ? (
        <Loading />
      ) : (
        <>
          <div className="orders-grid">
            {result.items.length ? (
              result.items.map((order, index) => (
                <article className="order-card" key={order.id}>
                  <div className="order-card-top">
                    <span>{order.order_no}</span>
                    <Badge status={order.status} />
                  </div>
                  <div className="order-product">
                    <span className={"product-art art-" + index}>
                      <ShoppingBag size={37} />
                    </span>
                    <div>
                      <span className="eyebrow">{order.category}</span>
                      <h3>{order.product_name}</h3>
                      <p>
                        数量 {order.quantity} · {money(order.total_cents)}
                      </p>
                    </div>
                  </div>
                  <div className="order-meta">
                    <span>
                      下单时间<b>{date(order.created_at)}</b>
                    </span>
                    <span>
                      签收时间
                      <b>
                        {order.delivered_at
                          ? date(order.delivered_at)
                          : "尚未签收"}
                      </b>
                    </span>
                  </div>
                  <button
                    className="secondary full"
                    onClick={() => setSelected(order.id)}
                  >
                    申请售后 / 咨询
                    <ArrowUpRight size={16} />
                  </button>
                </article>
              ))
            ) : (
              <Empty text="没有符合条件的订单" />
            )}
          </div>
          <Pagination
            {...result}
            onPage={(next) => change({ page: String(next) })}
            onSize={(next) => change({ size: String(next), page: "1" })}
          />
          {selected && (
            <TicketForm
              orders={result.items}
              selected={selected}
              close={() => setSelected(null)}
              onDone={() => go("tickets")}
              onToast={onToast}
            />
          )}
        </>
      )}
    </>
  );
}
