import React, { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { UserProfile, AuthResponse } from '../types/User';
import { loginApi, registerApi, getMeApi } from '../api/authApi';

export const TOKEN_KEY = 'auth_token';
const USER_KEY = 'auth_user';
export const VAULT_KEY_STORAGE = 'vault_key';

interface AuthContextType {
  user: UserProfile | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  vaultUnlocked: boolean;
  login: (username: string, password: string) => Promise<void>;
  register: (username: string, email: string, password: string) => Promise<void>;
  logout: () => void;
  refreshUser: () => Promise<void>;
  unlockVault: (derivedKey: string) => void;
  lockVault: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<UserProfile | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [vaultUnlocked, setVaultUnlocked] = useState(false);

  useEffect(() => {
    const storedVaultKey = sessionStorage.getItem(VAULT_KEY_STORAGE);
    if (storedVaultKey) {
      setVaultUnlocked(true);
    }
  }, []);

  useEffect(() => {
    const storedToken = localStorage.getItem(TOKEN_KEY);
    const storedUser = localStorage.getItem(USER_KEY);

    if (storedToken && storedUser) {
      try {
        setToken(storedToken);
        setUser(JSON.parse(storedUser));
        getMeApi()
          .then((freshUser) => {
            setUser(freshUser);
            localStorage.setItem(USER_KEY, JSON.stringify(freshUser));
          })
          .catch(() => {
            localStorage.removeItem(TOKEN_KEY);
            localStorage.removeItem(USER_KEY);
            sessionStorage.removeItem(VAULT_KEY_STORAGE);
            setToken(null);
            setUser(null);
            setVaultUnlocked(false);
          })
          .finally(() => setIsLoading(false));
      } catch {
        setIsLoading(false);
      }
    } else {
      setIsLoading(false);
    }
  }, []);

  const persistAuth = (authResponse: AuthResponse) => {
    localStorage.setItem(TOKEN_KEY, authResponse.token);
    localStorage.setItem(USER_KEY, JSON.stringify(authResponse.user));
    setToken(authResponse.token);
    setUser(authResponse.user);
  };

  const login = useCallback(async (username: string, password: string) => {
    const authResponse = await loginApi(username, password);
    persistAuth(authResponse);
  }, []);

  const register = useCallback(async (username: string, email: string, password: string) => {
    const authResponse = await registerApi(username, email, password);
    persistAuth(authResponse);
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    sessionStorage.removeItem(VAULT_KEY_STORAGE);
    setToken(null);
    setUser(null);
    setVaultUnlocked(false);
  }, []);

  const refreshUser = useCallback(async () => {
    const freshUser = await getMeApi();
    setUser(freshUser);
    localStorage.setItem(USER_KEY, JSON.stringify(freshUser));
  }, []);

  const unlockVault = useCallback((derivedKey: string) => {
    sessionStorage.setItem(VAULT_KEY_STORAGE, derivedKey);
    setVaultUnlocked(true);
  }, []);

  const lockVault = useCallback(() => {
    sessionStorage.removeItem(VAULT_KEY_STORAGE);
    setVaultUnlocked(false);
  }, []);

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isAuthenticated: !!token && !!user,
        isLoading,
        vaultUnlocked,
        login,
        register,
        logout,
        refreshUser,
        unlockVault,
        lockVault,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within an AuthProvider');
  return context;
};
