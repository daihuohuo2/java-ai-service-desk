export interface User {
  id: string;
  username: string;
  display_name: string;
  role: "CUSTOMER" | "AGENT" | "ADMIN";
}
export interface PageResult<T> {
  items: T[];
  total: number;
  page: number;
  size: number;
  pages: number;
}
export interface Order {
  id: string;
  order_no: string;
  product_id: string;
  product_name: string;
  category: string;
  quantity: number;
  total_cents: number;
  status: string;
  created_at: string;
  delivered_at: string | null;
}
export interface IndexJob {
  id: string;
  documentId: string | null;
  state: "PENDING" | "RUNNING" | "RETRY" | "SUCCEEDED" | "FAILED";
  attempts: number;
  indexed: number;
  total: number;
  error: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}
export type TicketKind = "QUESTION" | "REFUND" | "EXCHANGE" | "REPAIR";
export interface Product {
  id: string;
  name: string;
  category: string;
  price_cents: number;
  description: string;
  active: boolean;
}
export type ProductDraft = Omit<Product, "id"> & { id?: string };
export interface TicketSummary {
  id: string;
  ticket_no: string;
  subject: string;
  kind: TicketKind;
  status: string;
  updated_at: string;
  customer_name: string;
}
export interface TicketEvent {
  id: string;
  ticket_id: string;
  actor_id: string;
  actor_name: string;
  actor_role: string;
  event_type: string;
  content: string;
  created_at: string;
}
export interface TicketDetail extends TicketSummary {
  owner_id: string;
  order_id: string | null;
  order_no: string | null;
  product_name: string | null;
  description: string;
  assignee_id: string | null;
  assignee_name: string | null;
  version: number;
  created_at: string;
  events: TicketEvent[];
}
export interface Proposal {
  id: string;
  kind: TicketKind;
  order_id: string | null;
  subject: string;
  description: string;
  expires_at: string;
  ticket_id: string | null;
}
export interface ChatThread {
  id: string;
  title: string;
  created_at: string;
}
export interface KnowledgeHit {
  id: string;
  title: string;
  version: number;
  ordinal: number;
  content: string;
  score: number;
  retrieval: string;
}
export interface ChatAnswer {
  runId: string;
  mode: string;
  promptVersion: string;
  intent: string;
  tool?: string;
  answer: string;
  citations: KnowledgeHit[];
  orders?: Order[] | null;
  proposal?: Proposal | null;
  latencyMs: number;
}
export interface ChatMessage {
  id?: string;
  role: "user" | "assistant";
  content: string;
  detail?: ChatAnswer | null;
  created_at?: string;
}
export interface KnowledgeDocument {
  id: string;
  title: string;
  product_id: string | null;
  version: number;
  active: boolean;
  created_at: string;
  chunks: number;
  indexed: number;
}
export interface IndexResult {
  indexed: number;
  total: number;
  complete: boolean;
}
export interface RunSummary {
  id: string;
  state: string;
  model_mode: string | null;
  latency_ms: number | null;
  created_at: string;
}
export interface OverviewData {
  tickets: { status: string; total: number }[];
  orders: number;
  knowledge: number;
  modelRuns: number;
  recentRuns: RunSummary[];
}
export interface AuditEntry {
  id: string;
  actor_name: string | null;
  action: string;
  target_id: string | null;
  summary: string;
  created_at: string;
}
