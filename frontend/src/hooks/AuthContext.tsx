import {
  createContext,
  useContext,
  useEffect,
  useState,
  ReactNode,
} from "react";
import { api, post, refreshCsrf } from "../api";
import type { User } from "../types";

interface AuthState {
  user: User | null;
  ready: boolean;
  demo: boolean;
  setUser: (user: User) => void;
  logout: () => Promise<void>;
}
const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [ready, setReady] = useState(false);
  const [demo, setDemo] = useState(false);
  useEffect(() => {
    let current = true;
    Promise.all([
      api<{ demo: boolean }>("/meta")
        .then((result) => {
          if (current) setDemo(result.demo);
        })
        .catch(() => {}),
      api<User>("/auth/me")
        .then((result) => {
          if (current) setUser(result);
        })
        .catch(() => {}),
    ]).finally(() => {
      if (current) setReady(true);
    });
    return () => {
      current = false;
    };
  }, []);
  async function logout() {
    await refreshCsrf();
    await post("/auth/logout");
    setUser(null);
    await refreshCsrf();
  }
  return (
    <AuthContext.Provider value={{ user, ready, demo, setUser, logout }}>
      {children}
    </AuthContext.Provider>
  );
}
export function useAuth() {
  const value = useContext(AuthContext);
  if (!value) throw new Error("useAuth requires AuthProvider");
  return value;
}
