import type { AuditEntry } from "../types";
import React, { useEffect, useState, useRef, FormEvent } from "react";
import { api } from "../api";
import { date, Loading, PageTitle } from "../components/ui";

export function Audit({ onToast }: { onToast: (s: string) => void }) {
  const [rows, setRows] = useState<AuditEntry[] | null>(null);
  useEffect(() => {
    api<AuditEntry[]>("/admin/audit")
      .then(setRows)
      .catch((e) => onToast(e.message));
  }, []);
  return (
    <>
      <PageTitle
        eyebrow="ACTIVITY LOG"
        title="每一步处理，都有记录"
        description="最近 100 条关键操作，不记录密码和模型密钥。"
      />
      <section className="panel">
        {rows === null ? (
          <Loading />
        ) : (
          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>时间</th>
                  <th>操作者</th>
                  <th>操作</th>
                  <th>说明</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((r) => (
                  <tr key={r.id}>
                    <td>{date(r.created_at)}</td>
                    <td>{r.actor_name || "系统"}</td>
                    <td>
                      <code>{r.action}</code>
                    </td>
                    <td>{r.summary}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </>
  );
}
