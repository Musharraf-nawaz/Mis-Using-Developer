import axios from 'axios';
import {
  clearAuthStorage,
  getAccessToken,
  getRefreshToken,
  setAuthSession,
} from '../utils/authStorage';
import type { AuthResponse } from '../types';

const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL ||
  (import.meta.env.PROD ? 'https://mis-using-developer.onrender.com/api' : '/api');

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 45000,
  headers: { 'Content-Type': 'application/json' },
});

api.interceptors.request.use((config) => {
  const token = getAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      const refreshToken = getRefreshToken();
      if (refreshToken) {
        try {
          const { data } = await axios.post(`${API_BASE_URL}/auth/refresh`, { refreshToken });
          if (data.success) {
            const auth = data.data as AuthResponse;
            setAuthSession(auth);
            originalRequest.headers.Authorization = `Bearer ${auth.accessToken}`;
            return api(originalRequest);
          }
        } catch {
          clearAuthStorage();
          window.location.href = '/login';
        }
      }
    }
    return Promise.reject(error);
  }
);

export default api;
