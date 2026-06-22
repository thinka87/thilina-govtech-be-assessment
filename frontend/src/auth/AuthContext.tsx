import React, { createContext, useContext, useEffect, useState } from 'react';
import type { AuthUser, LoginRequest, Role } from '../types/auth';
import { tokenStorage } from '../utils/tokenStorage';
import { authService } from './authService';

// ── Context shape ─────────────────────────────────────────────────────────────

interface AuthContextValue {
  user: AuthUser | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (credentials: LoginRequest) => Promise<void>;
  logout: () => void;
  clearMustChangePassword: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

// ── Provider ──────────────────────────────────────────────────────────────────

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser]       = useState<AuthUser | null>(null);
  const [token, setToken]     = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // Restore session from localStorage on initial mount
  useEffect(() => {
    const storedToken = tokenStorage.getToken();
    const storedUser  = tokenStorage.getUser();

    if (storedToken && storedUser) {
      try {
        setToken(storedToken);
        setUser(JSON.parse(storedUser) as AuthUser);
      } catch {
        tokenStorage.clear();
      }
    }
    setIsLoading(false);
  }, []);

  const login = async (credentials: LoginRequest): Promise<void> => {
    const response = await authService.login(credentials);

    const authUser: AuthUser = {
      username:           response.username,
      role:               response.role as Role,
      mustChangePassword: response.mustChangePassword,
      citizenReference:   response.citizenReference,
    };

    tokenStorage.setToken(response.accessToken);
    tokenStorage.setUser(authUser);
    setToken(response.accessToken);
    setUser(authUser);
  };

  const logout = (): void => {
    tokenStorage.clear();
    setToken(null);
    setUser(null);
    window.location.href = '/login';
  };

  const clearMustChangePassword = (): void => {
    setUser((prev) => {
      if (!prev) return prev;
      const updated = { ...prev, mustChangePassword: false };
      tokenStorage.setUser(updated);
      return updated;
    });
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isAuthenticated: !!token && !!user,
        isLoading,
        login,
        logout,
        clearMustChangePassword,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

// ── Hook ──────────────────────────────────────────────────────────────────────

export const useAuth = (): AuthContextValue => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within <AuthProvider>');
  return ctx;
};
