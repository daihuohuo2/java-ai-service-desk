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
