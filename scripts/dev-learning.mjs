// A separate learning instance; never reads or overwrites the Docker deployment's .env/database.
import { spawn } from "node:child_process";
import { fileURLToPath } from "node:url";
import { mkdir } from "node:fs/promises";
import path from "node:path";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const children = [];
let closing = false;

function stop(code = 0) {
  if (closing) return;
  closing = true;
  for (const child of children) child.kill();
  process.exit(code);
}
process.on("SIGINT", () => stop());
process.on("SIGTERM", () => stop());

function run(command, args, environment, cwd = root) {
  const child = spawn(command, args, {
    cwd,
    env: { ...process.env, ...environment },
    stdio: "inherit",
    windowsHide: true,
  });
  children.push(child);
  child.on("error", (error) => { console.error(error.message); stop(1); });
  child.on("exit", (code) => { if (!closing) stop(code || 1); });
}

await mkdir(path.join(root, ".runtime/learning"), { recursive: true });
const ephemeral = process.argv.includes("--ephemeral");
run("java", ["-Dfile.encoding=UTF-8", "-jar", "backend/desk-app/target/service-desk-1.0.0.jar"], {
  PORT: "21984", BIND_ADDRESS: "127.0.0.1",
  DATABASE_URL: ephemeral
    ? "jdbc:h2:mem:learning;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
    : "jdbc:h2:file:./.runtime/learning/desk;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_ON_EXIT=FALSE",
  DATABASE_USER: "sa", DATABASE_PASSWORD: "", SPRING_PROFILES_ACTIVE: "",
  APP_DEMO: "true", AI_ENABLED: String(process.argv.includes("--ai")),
  INDEX_WORKER_ENABLED: "true", REDIS_ENABLED: "false", RABBITMQ_ENABLED: "false",
  ADMIN_PASSWORD: "DemoAdmin!2026", CUSTOMER_PASSWORD: "DemoCustomer!2026", AGENT_PASSWORD: "DemoAgent!2026",
  APP_ORIGIN: "http://127.0.0.1:21984,http://127.0.0.1:21985",
  DEPLOYMENT_MODE: "local", COOKIE_SECURE: "false",
});
let ready = false;
for (let attempt = 0; attempt < 180; attempt++) {
  try {
    const response = await fetch("http://127.0.0.1:21984/actuator/health", { signal: AbortSignal.timeout(1000) });
    if (response.ok) { ready = true; break; }
  } catch {}
  await new Promise((resolve) => setTimeout(resolve, 500));
}
if (!ready) { console.error("Learning API failed to become healthy."); stop(1); }
run(process.execPath, ["node_modules/vite/bin/vite.js", "--host", "127.0.0.1", "--port", "21985", "--strictPort"],
  { VITE_API_TARGET: "http://127.0.0.1:21984" }, path.join(root, "frontend"));
console.log("Learning UI: http://127.0.0.1:21985 — Ctrl+C stops this instance.");
