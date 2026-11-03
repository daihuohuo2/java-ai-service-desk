export type Row = Record<string, any>;
let csrf = "";
export async function refreshCsrf() {
  const r = await fetch("/api/auth/csrf", { credentials: "include" });
  if (!r.ok) throw new Error("无法连接服务");
  csrf = (await r.json()).data.token;
}
export async function api<T = any>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const method = options.method || "GET";
  if (method !== "GET" && !csrf) await refreshCsrf();
  const headers: Record<string, string> = {
    ...(options.headers as Record<string, string>),
  };
  if (method !== "GET") headers["X-CSRF-TOKEN"] = csrf;
  if (options.body && !(options.body instanceof FormData))
    headers["Content-Type"] = "application/json";
  const r = await fetch("/api" + path, {
    ...options,
    headers,
    credentials: "include",
  });
  if (r.status === 204) return undefined as T;
  const data = await r
    .json()
    .catch(() => ({ code: -1, message: "服务返回了无法识别的响应" }));
  if (!r.ok || data.code !== 0) throw new Error(data.message || "请求失败");
  return data.data as T;
}
export const post = (path: string, body: any = {}, key?: string) =>
  api(path, {
    method: "POST",
    body: JSON.stringify(body),
    headers: key ? { "Idempotency-Key": key } : {},
  });
export async function askStream(
  threadId: string,
  question: string,
  onPhase: (s: string) => void,
  key: string,
): Promise<Row> {
  if (!csrf) await refreshCsrf();
  const r = await fetch("/api/chat/stream", {
    method: "POST",
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      "X-CSRF-TOKEN": csrf,
      "Idempotency-Key": key,
    },
    body: JSON.stringify({ threadId, question }),
  });
  if (!r.ok) {
    const e = await r.json();
    throw new Error(e.message || "咨询失败");
  }
  const reader = r.body?.getReader();
  if (!reader) throw new Error("无法读取回复");
  let buffer = "",
    result: Row | undefined;
  const decoder = new TextDecoder();
  while (true) {
    const { value, done } = await reader.read();
    if (done) break;
    buffer += decoder.decode(value, { stream: true });
    buffer = buffer.replace(/\r\n/g, "\n");
    let n;
    while ((n = buffer.indexOf("\n\n")) >= 0) {
      const block = buffer.slice(0, n);
      buffer = buffer.slice(n + 2);
      const type = block
        .split("\n")
        .find((x) => x.startsWith("event:"))
        ?.slice(6)
        .trim();
      const raw = block
        .split("\n")
        .filter((x) => x.startsWith("data:"))
        .map((x) => x.slice(5))
        .join("\n");
      if (!raw) continue;
      const data = JSON.parse(raw);
      if (type === "phase") onPhase(data.message);
      if (type === "result") result = data;
      if (type === "error") throw new Error(data.error);
    }
  }
  if (!result) throw new Error("连接已中断，请检查会话记录后重试");
  return result;
}
