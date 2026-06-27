import type { AuthResponse } from '../types';

const ACCESS_TOKEN = 'accessToken';
const REFRESH_TOKEN = 'refreshToken';
const USER = 'user';

const AUTH_KEYS = [ACCESS_TOKEN, REFRESH_TOKEN, USER] as const;

export function clearAuthStorage() {
  for (const key of AUTH_KEYS) {
    localStorage.removeItem(key);
    sessionStorage.removeItem(key);
  }
}

/** Remove old persistent logins saved in localStorage before session-only auth. */
export function clearLegacyLocalAuth() {
  for (const key of AUTH_KEYS) {
    localStorage.removeItem(key);
  }
}

export function getAccessToken() {
  return sessionStorage.getItem(ACCESS_TOKEN);
}

export function getRefreshToken() {
  return sessionStorage.getItem(REFRESH_TOKEN);
}

export function getStoredUser(): AuthResponse | null {
  const token = sessionStorage.getItem(ACCESS_TOKEN);
  const raw = sessionStorage.getItem(USER);
  if (!token || !raw) return null;
  try {
    return JSON.parse(raw) as AuthResponse;
  } catch {
    return null;
  }
}

export function setAuthSession(auth: AuthResponse) {
  sessionStorage.setItem(ACCESS_TOKEN, auth.accessToken);
  sessionStorage.setItem(REFRESH_TOKEN, auth.refreshToken);
  sessionStorage.setItem(USER, JSON.stringify(auth));
}
