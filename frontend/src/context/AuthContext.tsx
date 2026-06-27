import React, { createContext, useContext, useState, useCallback, useEffect } from 'react';
import type { AuthResponse, Role } from '../types';
import { authApi } from '../api/services';
import {
  clearAuthStorage,
  clearLegacyLocalAuth,
  getRefreshToken,
  getStoredUser,
  setAuthSession,
} from '../utils/authStorage';

interface AuthContextType {
  user: AuthResponse | null;
  isAuthenticated: boolean;
  authReady: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
  hasRole: (...roles: Role[]) => boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<AuthResponse | null>(null);
  const [authReady, setAuthReady] = useState(false);

  useEffect(() => {
    clearLegacyLocalAuth();
    setUser(getStoredUser());
    setAuthReady(true);
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    const { data } = await authApi.login(email, password);
    if (!data.success) throw new Error(data.message || 'Login failed');
    const auth = data.data;
    setAuthSession(auth);
    setUser(auth);
  }, []);

  const logout = useCallback(async () => {
    const refreshToken = getRefreshToken();
    if (refreshToken) {
      try {
        await authApi.logout(refreshToken);
      } catch {
        /* ignore */
      }
    }
    clearAuthStorage();
    setUser(null);
  }, []);

  const hasRole = useCallback(
    (...roles: Role[]) => (user ? roles.includes(user.role) : false),
    [user]
  );

  return (
    <AuthContext.Provider
      value={{ user, isAuthenticated: !!user, authReady, login, logout, hasRole }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
};
