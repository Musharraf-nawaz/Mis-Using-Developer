import api from './axios';
import type {
  ApiResponse,
  Asset,
  AuthResponse,
  DashboardData,
  Interview,
  InterviewCalendar,
  InterviewRoundData,
  MediaType,
  Notification,
  PageResponse,
  Project,
  User,
} from '../types';

const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api';

export const fileUrl = (path?: string | null) => {
  if (!path) return '';
  if (path.startsWith('http')) return path;
  const origin = API_BASE.replace(/\/api$/, '');
  return path.startsWith('/') ? `${origin}${path}` : `${API_BASE}/${path}`;
};

export const authApi = {
  login: (email: string, password: string) =>
    api.post<ApiResponse<AuthResponse>>('/auth/login', { email, password }),
  logout: (refreshToken: string) =>
    api.post('/auth/logout', { refreshToken }),
};

export const dashboardApi = {
  get: () => api.get<ApiResponse<DashboardData>>('/dashboard'),
};

export const projectApi = {
  getAll: (params: Record<string, unknown>) =>
    api.get<ApiResponse<PageResponse<Project>>>('/projects', { params }),
  getById: (id: number) => api.get<ApiResponse<Project>>(`/projects/${id}`),
  create: (data: Partial<Project>) => api.post<ApiResponse<Project>>('/projects', data),
  update: (id: number, data: Partial<Project>) => api.put<ApiResponse<Project>>(`/projects/${id}`, data),
  delete: (id: number) => api.delete(`/projects/${id}`),
};

export const assetApi = {
  getAll: (params: Record<string, unknown>) =>
    api.get<ApiResponse<PageResponse<Asset>>>('/assets', { params }),
  getById: (id: number) => api.get<ApiResponse<Asset>>(`/assets/${id}`),
  create: (data: Partial<Asset>) => api.post<ApiResponse<Asset>>('/assets', data),
  update: (id: number, data: Partial<Asset>) => api.put<ApiResponse<Asset>>(`/assets/${id}`, data),
  delete: (id: number) => api.delete(`/assets/${id}`),
  uploadMedia: (id: number, file: File, type: MediaType) => {
    const form = new FormData();
    form.append('file', file);
    form.append('type', type);
    return api.post<ApiResponse<Asset>>(`/assets/${id}/media`, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
  updateOffboarded: (id: number, offboarded: boolean) =>
    api.patch<ApiResponse<Asset>>(`/assets/${id}/offboarded`, { offboarded }),
  assign: (data: Record<string, unknown>) => api.post('/assets/assign', data),
  returnAsset: (assignmentId: number, remarks?: string) =>
    api.post(`/assets/return/${assignmentId}`, { remarks }),
  getHistory: (id: number) => api.get(`/assets/${id}/history`),
  exportCsv: () => api.get('/assets/export/csv', { responseType: 'blob' }),
  exportExcel: () => api.get('/assets/export/excel', { responseType: 'blob' }),
};

export const interviewApi = {
  getAll: (params: Record<string, unknown>) =>
    api.get<ApiResponse<PageResponse<Interview>>>('/interviews', { params }),
  getById: (id: number) => api.get<ApiResponse<Interview>>(`/interviews/${id}`),
  create: (data: Partial<Interview>) => api.post<ApiResponse<Interview>>('/interviews', data),
  update: (id: number, data: Partial<Interview>) =>
    api.put<ApiResponse<Interview>>(`/interviews/${id}`, data),
  uploadCv: (id: number, file: File) => {
    const form = new FormData();
    form.append('file', file);
    return api.post<ApiResponse<Interview>>(`/interviews/${id}/cv`, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
  getRounds: (id: number) =>
    api.get<ApiResponse<InterviewRoundData[]>>(`/interviews/${id}/rounds`),
  initRounds: (id: number) =>
    api.post<ApiResponse<InterviewRoundData[]>>(`/interviews/${id}/rounds/init`),
  updateRound: (id: number, roundNumber: number, data: Record<string, unknown>) =>
    api.patch<ApiResponse<InterviewRoundData>>(`/interviews/${id}/rounds/${roundNumber}`, data),
  reschedule: (id: number, data: Partial<Interview>) =>
    api.patch<ApiResponse<Interview>>(`/interviews/${id}/reschedule`, data),
  cancel: (id: number, notes?: string) =>
    api.patch<ApiResponse<Interview>>(`/interviews/${id}/cancel`, { notes }),
  complete: (id: number, feedback?: string) =>
    api.patch<ApiResponse<Interview>>(`/interviews/${id}/complete`, { feedback }),
  delete: (id: number) => api.delete(`/interviews/${id}`),
  getCalendar: (start: string, end: string, status?: string) =>
    api.get<ApiResponse<InterviewCalendar[]>>('/interviews/calendar', { params: { start, end, status } }),
};

export const userApi = {
  getAll: (params: Record<string, unknown>) =>
    api.get<ApiResponse<PageResponse<User>>>('/users', { params }),
  getById: (id: number) => api.get<ApiResponse<User>>(`/users/${id}`),
  create: (data: Partial<User> & { password?: string }) =>
    api.post<ApiResponse<User>>('/users', data),
  update: (id: number, data: Partial<User>) => api.put<ApiResponse<User>>(`/users/${id}`, data),
  delete: (id: number) => api.delete(`/users/${id}`),
  activate: (id: number) => api.patch<ApiResponse<User>>(`/users/${id}/activate`),
  deactivate: (id: number) => api.patch<ApiResponse<User>>(`/users/${id}/deactivate`),
  resetPassword: (id: number, password: string) =>
    api.post(`/users/${id}/reset-password`, { password }),
};

export const notificationApi = {
  getAll: (page = 0, size = 20) =>
    api.get<ApiResponse<PageResponse<Notification>>>('/notifications', { params: { page, size } }),
  getUnreadCount: () => api.get<ApiResponse<{ count: number }>>('/notifications/unread-count'),
  markAsRead: (id: number) => api.patch(`/notifications/${id}/read`),
  markAllAsRead: () => api.patch('/notifications/read-all'),
};

export const wsBaseUrl = () => API_BASE.replace(/\/api$/, '');
