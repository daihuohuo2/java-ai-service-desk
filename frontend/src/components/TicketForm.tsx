import React, { useEffect, useState, useRef, FormEvent } from "react";
import { post, Row } from "../api";
import { kindName, Modal } from "../components/ui";
import { validateTicket } from "../validation";

export function TicketForm({
  orders,
  selected,
  onDone,
  onToast,
  close,
}: {
  orders: Row[];
  selected?: string;
  onDone: () => void;
  onToast: (s: string) => void;
  close: () => void;
}) {
  const [orderId, setOrder] = useState(selected || "");
  const [kind, setKind] = useState("QUESTION");
  const [subject, setSubject] = useState("");
  const [description, setDescription] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const key = useRef(crypto.randomUUID());
  return (
    <Modal title="创建服务工单" close={close}>
      <form
        onSubmit={async (e) => {
          e.preventDefault();
          const error = validateTicket({ orderId, kind, subject, description });
          setError(error);
          if (error) return;
          setBusy(true);
          try {
            await post(
              "/tickets",
              {
                orderId,
                kind,
                subject: subject.trim(),
                description: description.trim(),
              },
              key.current,
            );
            onToast("工单已提交");
            onDone();
            close();
          } catch (e) {
            onToast((e as Error).message);
          } finally {
            setBusy(false);
          }
        }}
      >
        {error && (
          <p role="alert" className="error">
            {error}
          </p>
        )}
        <label>
          关联订单
          <select value={orderId} onChange={(e) => setOrder(e.target.value)}>
            <option value="">不关联订单 · 普通咨询</option>
            {orders.map((o) => (
              <option key={o.id} value={o.id}>
                {o.product_name} / {o.order_no}
              </option>
            ))}
          </select>
        </label>
        <label>
          服务类型
          <select value={kind} onChange={(e) => setKind(e.target.value)}>
            {Object.entries(kindName).map(([v, l]) => (
              <option key={v} value={v}>
                {l}
              </option>
            ))}
          </select>
        </label>
        <label>
          工单标题
          <input
            required
            maxLength={120}
            value={subject}
            onChange={(e) => setSubject(e.target.value)}
            placeholder="简要说明需要什么帮助"
          />
        </label>
        <label>
          问题描述
          <textarea
            required
            maxLength={2000}
            rows={5}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="请说明问题、发生时间和您希望的处理方式"
          />
        </label>
        <p className="form-hint">
          提交工单表示发起申请，处理结果以客服审核为准。
        </p>
        <button className="primary full" disabled={busy}>
          {busy ? "正在提交…" : "确认提交工单"}
        </button>
      </form>
    </Modal>
  );
}
