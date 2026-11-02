import React, { useEffect, useState, useRef, FormEvent } from "react";
import {
  ArrowUpRight,
  Plus,
  FileText,
  RefreshCw,
  Upload,
  Loader2,
} from "lucide-react";
import { api, post, Row } from "../api";
import { Modal, PageTitle } from "../components/ui";
import { IndexJobs } from "../components/IndexJobs";

export function Knowledge({ onToast }: { onToast: (s: string) => void }) {
  const [docs, setDocs] = useState<Row[]>([]);
  const [products, setProducts] = useState<Row[]>([]);
  const [open, setOpen] = useState(false);
  const [title, setTitle] = useState("");
  const [product, setProduct] = useState("");
  const [content, setContent] = useState("");
  const [file, setFile] = useState<File | null>(null);
  const [busy, setBusy] = useState(false);
  const load = () => api("/knowledge").then(setDocs);
  useEffect(() => {
    load().catch((e) => onToast(e.message));
    api("/products").then(setProducts);
  }, []);
  return (
    <>
      <PageTitle
        eyebrow="KNOWLEDGE LIBRARY"
        title="让每一次回答，有据可查"
        description="发布商品说明和服务规则，为客服提供可引用的知识资料。"
        action={
          <div className="actions">
            <button
              className="secondary"
              disabled={busy}
              onClick={async () => {
                setBusy(true);
                try {
                  const r = await post("/knowledge/reindex");
                  onToast(
                    `向量索引：完成 ${r.indexed} / ${r.total} 个片段${r.complete ? "" : "，模型不可用，仍可关键词检索"}`,
                  );
                  load();
                } catch (e) {
                  onToast((e as Error).message);
                } finally {
                  setBusy(false);
                }
              }}
            >
              {busy ? (
                <Loader2 size={16} className="spin" />
              ) : (
                <RefreshCw size={16} />
              )}
              建立向量索引
            </button>
            <button className="primary" onClick={() => setOpen(true)}>
              <Plus size={17} />
              添加资料
            </button>
          </div>
        }
      />
      <IndexJobs
        onToast={onToast}
        onComplete={() => {
          load().catch((error) => onToast(error.message));
        }}
      />
      <div className="knowledge-grid">
        {docs.map((d) => (
          <article className="knowledge-card" key={d.id}>
            <div>
              <span className="doc-symbol">
                <FileText size={23} />
              </span>
              <span className={"badge " + (d.active ? "resolved" : "closed")}>
                {d.active ? "已发布" : "已停用"}
              </span>
            </div>
            <h3>{d.title}</h3>
            <p>
              {products.find((p) => p.id === d.product_id)?.name ||
                "通用服务资料"}
            </p>
            <div className="doc-stats">
              <span>{d.chunks} 个片段</span>
              <span>{d.indexed} 已向量化</span>
              <span>v{d.version}</span>
            </div>
            <button
              className="text-btn"
              disabled={busy}
              onClick={async () => {
                try {
                  await api("/knowledge/" + d.id, {
                    method: "PUT",
                    body: JSON.stringify({ active: !d.active }),
                  });
                  load();
                } catch (e) {
                  onToast((e as Error).message);
                }
              }}
            >
              {d.active ? "停用这份资料" : "重新启用"}
              <ArrowUpRight size={15} />
            </button>
          </article>
        ))}
      </div>
      <p className="form-hint">
        支持 UTF-8 TXT / Markdown；单文件 256 KB、正文 30000
        字符。停用后不再参与新问题检索，历史回复保留当时的引用快照。
      </p>
      {open && (
        <Modal title="添加知识资料" close={() => setOpen(false)}>
          <form
            onSubmit={async (e) => {
              e.preventDefault();
              setBusy(true);
              try {
                if (file) {
                  const fd = new FormData();
                  fd.append("file", file);
                  fd.append("title", title);
                  if (product) fd.append("productId", product);
                  await api("/knowledge/upload", { method: "POST", body: fd });
                } else
                  await post("/knowledge", {
                    title,
                    productId: product,
                    content,
                  });
                await load();
                setOpen(false);
                setTitle("");
                setContent("");
                setFile(null);
                onToast("资料已发布，可继续建立向量索引");
              } catch (e) {
                onToast((e as Error).message);
              } finally {
                setBusy(false);
              }
            }}
          >
            <label>
              资料标题
              <input
                required
                maxLength={120}
                value={title}
                onChange={(e) => setTitle(e.target.value)}
              />
            </label>
            <label>
              适用商品
              <select
                value={product}
                onChange={(e) => setProduct(e.target.value)}
              >
                <option value="">通用规则</option>
                {products.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.name}
                  </option>
                ))}
              </select>
            </label>
            <label>
              上传文件（可选）
              <input
                type="file"
                accept=".txt,.md"
                onChange={(e) => setFile(e.target.files?.[0] || null)}
              />
            </label>
            {!file && (
              <label>
                正文
                <textarea
                  rows={7}
                  required
                  maxLength={30000}
                  value={content}
                  onChange={(e) => setContent(e.target.value)}
                  placeholder="填写已经确认的商品信息或服务规则"
                />
              </label>
            )}
            <button className="primary full" disabled={busy}>
              <Upload size={17} />
              {busy ? "正在发布…" : "确认发布资料"}
            </button>
          </form>
        </Modal>
      )}
    </>
  );
}
