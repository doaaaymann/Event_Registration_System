import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { jwtDecode } from 'jwt-decode';
import api, { setTokenGetter } from '../lib/api';

const AuthContext = createContext(null);

function readStoredToken() {
  return localStorage.getItem('ers_token') || '';
}

function readStoredUser() {
  const raw = localStorage.getItem('ers_user');
  if (!raw) return null;

  try {
    return JSON.parse(raw);
  } catch {
    return null;
  }
}

function decodeFallbackUser(token) {
  try {
    const payload = jwtDecode(token);
    const roles = payload.roles || payload.authorities || [];
    return {
      id: payload.userId || payload.sub,
      fullName: payload.name || payload.sub,
      email: payload.sub,
      roles: Array.isArray(roles) ? roles : [roles].filter(Boolean),
    };
  } catch {
    return null;
  }
}

export function AuthProvider({ children }) {
  const [token, setToken] = useState(readStoredToken);
  const [user, setUser] = useState(readStoredUser);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setTokenGetter(() => token);
  }, [token]);

  useEffect(() => {
    async function hydrate() {
      if (!token) {
        setLoading(false);
        return;
      }

      try {
        const response = await api.get('/auth/me');
        const nextUser = response.data;
        setUser(nextUser);
        localStorage.setItem('ers_user', JSON.stringify(nextUser));
      } catch {
        const fallback = decodeFallbackUser(token);
        if (fallback) {
          setUser(fallback);
          localStorage.setItem('ers_user', JSON.stringify(fallback));
        } else {
          logout();
        }
      } finally {
        setLoading(false);
      }
    }

    hydrate();
  }, [token]);

  async function login(email, password) {
    const response = await api.post('/auth/login', { email, password });
    const nextToken = response.data.token || response.data.accessToken;
    const nextUser = response.data.user || decodeFallbackUser(nextToken);

    localStorage.setItem('ers_token', nextToken);
    localStorage.setItem('ers_user', JSON.stringify(nextUser));
    setToken(nextToken);
    setUser(nextUser);
  }

  async function register(payload) {
    return api.post('/auth/register', payload);
  }

  function logout() {
    localStorage.removeItem('ers_token');
    localStorage.removeItem('ers_user');
    setToken('');
    setUser(null);
  }

  const value = useMemo(
    () => ({
      token,
      user,
      loading,
      isAuthenticated: Boolean(token),
      login,
      register,
      logout,
    }),
    [loading, token, user],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used inside AuthProvider');
  }

  return context;
}
