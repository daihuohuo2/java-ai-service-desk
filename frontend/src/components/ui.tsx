import React, { useEffect, useState, useRef, FormEvent } from "react";
import { FileText, X, Loader2 } from "lucide-react";

export const statusName: Record<string, string> = {
  OPEN: "待受理",
  IN_PROGRESS: "处理中",
  WAITING_CUSTOMER: "待补充",
  RESOLVED: "已解决",
  CLOSED: "已关闭",
  DELIVERED: "已签收",
  SHIPPED: "运输中",
  PAID: "已付款",
  CANCELLED: "已取消",
};
export const kindName: Record<string, string> = {
  QUESTION: "咨询",
  REFUND: "退货退款",
  EXCHANGE: "换货",
  REPAIR: "维修",
};
export const money = (n: number) => "¥" + (n / 100).toFixed(2);
export const date = (s: string) =>
  new Date(s).toLocaleString("zh-CN", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  });

export function Badge({ status }: { status: string }) {
  return (
    <span className={"badge " + status.toLowerCase()}>
      <i />
      {statusName[status] || status}
    </span>
  );
}

export function Loading() {
  return (
    <div className="loading">
      <Loader2 className="spin" size={20} /> 正在加载…
    </div>
  );
}

export function Empty({ text }: { text: string }) {
  return (
    <div className="empty">
      <FileText size={32} />
      <p>{text}</p>
    </div>
  );
}

export function Modal({
  title,
  children,
  close,
}: {
  title: string;
  children: React.ReactNode;
  close: () => void;
}) {
  return (
    <div className="modal-shade" onClick={close}>
      <section
        role="dialog"
        aria-modal="true"
        aria-label={title}
        className="modal"
        onClick={(e) => e.stopPropagation()}
      >
        <header>
          <h2>{title}</h2>
          <button className="icon-btn" aria-label="关闭弹窗" onClick={close}>
            <X />
          </button>
        </header>
        {children}
      </section>
    </div>
  );
}

export function PageTitle({
  eyebrow,
  title,
  description,
  action,
}: {
  eyebrow: string;
  title: string;
  description: string;
  action?: React.ReactNode;
}) {
  return (
    <div className="page-title">
      <div>
        <div className="eyebrow">{eyebrow}</div>
        <h1>{title}</h1>
        <p>{description}</p>
      </div>
      {action}
    </div>
  );
}
