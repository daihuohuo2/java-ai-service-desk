import { useEffect, useState } from "react";
import { api, post } from "../api";
import type { IndexJob } from "../types";
import { date } from "./ui";

const labels: Record<IndexJob["state"], string> = {
  PENDING: "等待处理",
  RUNNING: "正在索引",
  RETRY: "等待重试",
  SUCCEEDED: "已完成",
  FAILED: "处理失败",
};
export function IndexJobs({
  onToast,
  onComplete,
}: {
  onToast: (message: string) => void;
  onComplete: () => void;
}) {
  const [jobs, setJobs] = useState<IndexJob[]>([]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  useEffect(() => {
    let active = true;
    async function refresh() {
      try {
        const result = await api<IndexJob[]>("/knowledge/jobs");
        if (active) {
          setJobs(result);
          setError("");
        }
      } catch (failure) {
        if (active) setError((failure as Error).message);
      }
    }
    void refresh();
    const timer = setInterval(refresh, 3000);
    return () => {
      active = false;
      clearInterval(timer);
    };
  }, []);
  async function submit() {
    setBusy(true);
    try {
      const job = await post<IndexJob>(
        "/knowledge/jobs",
        {},
        crypto.randomUUID(),
      );
      setJobs((old) => [job, ...old].slice(0, 50));
      onToast("索引任务已提交，可以继续使用其他页面");
    } catch (failure) {
      onToast((failure as Error).message);
    } finally {
      setBusy(false);
    }
  }
  return (
    <section className="panel index-jobs">
      <div className="panel-head">
        <h3>索引任务</h3>
        <div className="actions">
          <button className="secondary" onClick={onComplete}>
            刷新资料统计
          </button>
          <button className="primary" disabled={busy} onClick={submit}>
            后台建立向量索引
          </button>
        </div>
      </div>
      {error && (
        <p role="alert" className="error">
          {error}
        </p>
      )}
      {!jobs.length ? (
        <p className="muted">
          提交后可查看进度；模型暂不可用时会自动重试，失败后也可手动重试。
        </p>
      ) : (
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>提交时间</th>
                <th>状态</th>
                <th>最近一轮进度</th>
                <th>尝试次数</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {jobs.map((job) => (
                <tr key={job.id}>
                  <td>{date(job.createdAt)}</td>
                  <td>
                    {labels[job.state]}
                    {job.error && <small>{job.error}</small>}
                  </td>
                  <td>
                    {job.indexed} / {job.total}
                  </td>
                  <td>{job.attempts}</td>
                  <td>
                    {job.state === "FAILED" && (
                      <button
                        className="text-btn"
                        onClick={async () => {
                          try {
                            const updated = await post<IndexJob>(
                              "/knowledge/jobs/" + job.id + "/retry",
                            );
                            setJobs((old) =>
                              old.map((item) =>
                                item.id === updated.id ? updated : item,
                              ),
                            );
                          } catch (failure) {
                            onToast((failure as Error).message);
                          }
                        }}
                      >
                        重试任务
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
