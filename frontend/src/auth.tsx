import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react';
import { api, setUnauthorizedHandler, tokenStore, type AuthResponse, type Me, type Permission } from './api';

interface AuthState {
  user: Me | null;
  loading: boolean;
  signIn: (res: AuthResponse) => void;
  signOut: () => void;
  /** RBAC: o usuário tem a permissão em algum dos seus perfis? */
  can: (permission: Permission) => boolean;
  /** Recarrega perfis e permissões (ex.: depois de alterar o próprio usuário). */
  refresh: () => void;
}

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<Me | null>(null);
  const [loading, setLoading] = useState(() => tokenStore.get() !== null);

  const signOut = useCallback(() => {
    tokenStore.clear();
    setUser(null);
  }, []);

  const signIn = useCallback((res: AuthResponse) => {
    tokenStore.set(res.token);
    setUser(res.user);
  }, []);

  useEffect(() => {
    setUnauthorizedHandler(signOut);
    if (!tokenStore.get()) return;
    api.me()
      .then(setUser)
      .catch(() => signOut())
      .finally(() => setLoading(false));
  }, [signOut]);

  const can = useCallback((p: Permission) => !!user?.permissions.includes(p), [user]);
  const refresh = useCallback(() => { api.me().then(setUser).catch(() => {}); }, []);
  return (
    <AuthContext.Provider
      value={{
        user,
        loading,
        signIn,
        signOut,
        can,
        refresh,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth precisa estar dentro de AuthProvider');
  return ctx;
}
