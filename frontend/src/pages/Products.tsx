import type { Product, ProductDraft } from "../types";
import React, { useEffect, useState, useRef, FormEvent } from "react";
import { Plus } from "lucide-react";
import { api } from "../api";
import { money, Modal, PageTitle } from "../components/ui";

export function Products({ onToast }: { onToast: (s: string) => void }) {
  const [rows, setRows] = useState<Product[]>([]);
  const [edit, setEdit] = useState<ProductDraft | null>(null);
  const [busy, setBusy] = useState(false);
  const load = () => api<Product[]>("/admin/products").then(setRows);
  useEffect(() => {
    load().catch((e) => onToast(e.message));
  }, []);
  return (
    <>
      <PageTitle
        eyebrow="PRODUCT CATALOG"
        title="商品管理"
        description="维护服务所需的商品资料；订单金额保留购买时的记录。"
        action={
          <button
            className="primary"
            onClick={() =>
              setEdit({
                name: "",
                category: "",
                price_cents: 0,
                description: "",
                active: true,
              })
            }
          >
            <Plus size={17} />
            新增商品
          </button>
        }
      />
      <section className="panel">
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>商品名称</th>
                <th>分类</th>
                <th>价格</th>
                <th>状态</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {rows.map((r) => (
                <tr key={r.id}>
                  <td>
                    <b>{r.name}</b>
                    <small>{r.description}</small>
                  </td>
                  <td>{r.category}</td>
                  <td>{money(r.price_cents)}</td>
                  <td>{r.active ? "上架" : "下架"}</td>
                  <td>
                    <button
                      className="text-btn"
                      onClick={() => setEdit({ ...r })}
                    >
                      编辑
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
      {edit && (
        <Modal
          title={edit.id ? "编辑商品" : "新增商品"}
          close={() => setEdit(null)}
        >
          <form
            onSubmit={async (e) => {
              e.preventDefault();
              setBusy(true);
              try {
                await api("/admin/products" + (edit.id ? "/" + edit.id : ""), {
                  method: edit.id ? "PUT" : "POST",
                  body: JSON.stringify({
                    id: edit.id,
                    name: edit.name,
                    category: edit.category,
                    priceCents: Math.round(Number(edit.price_cents)),
                    description: edit.description,
                    active: edit.active,
                  }),
                });
                setEdit(null);
                load();
                onToast("商品已保存");
              } catch (e) {
                onToast((e as Error).message);
              } finally {
                setBusy(false);
              }
            }}
          >
            {(
              [
                ["name", "商品名称"],
                ["category", "分类"],
                ["description", "商品描述"],
              ] as const
            ).map(([key, label]) => (
              <label key={key}>
                {label}
                <input
                  required
                  maxLength={
                    key === "description" ? 2000 : key === "name" ? 120 : 80
                  }
                  value={edit[key]}
                  onChange={(e) => setEdit({ ...edit, [key]: e.target.value })}
                />
              </label>
            ))}
            <label>
              价格（分）
              <input
                type="number"
                required
                min={0}
                max={100000000}
                step={1}
                value={edit.price_cents}
                onChange={(e) =>
                  setEdit({ ...edit, price_cents: Number(e.target.value) })
                }
              />
            </label>
            <label className="check-label">
              <input
                type="checkbox"
                checked={edit.active}
                onChange={(e) => setEdit({ ...edit, active: e.target.checked })}
              />
              上架商品
            </label>
            <button className="primary full" disabled={busy}>
              保存商品
            </button>
          </form>
        </Modal>
      )}
    </>
  );
}
