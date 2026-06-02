import api from './axios';
import type {
  ApiResponse,
  Asset,
  AuthResponse,
  DashboardData,
  Interview,
  Notification,
  PageResponse,
  User,
} from '../types';

export const authApi = {
  login: (email: string, password: string) =>
    api.post<ApiResponse<AuthResponse>>('/auth/login', { email, password }),
  logout: (refreshToken: string) =>
    api.post('/auth/logout', { refreshToken }),
};

export const dashboardApi = {
  get: () => api.get<ApiResponse<DashboardData>>('/dashboard'),
};

export const assetApi = {
  getAll: (params: Record<string, unknown>) =>
    api.get<ApiResponse<PageResponse<Asset>>>('/assets', { params }),
  getById: (id: number) => api.get<ApiResponse<Asset>>(`/assets/${id}`),
  create: (data: Partial<Asset>) => api.post<ApiResponse<Asset>>('/assets', data),
  update: (id: number, data: Partial<Asset>) => api.put<ApiResponse<Asset>>(`/assets/${id}`, data),
  delete: (id: number) => api.delete(`/assets/${id}`),
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
  reschedule: (id: number, data: Partial<Interview>) =>
    api.patch<ApiResponse<Interview>>(`/interviews/${id}/reschedule`, data),
  cancel: (id: number, notes?: string) =>
    api.patch<ApiResponse<Interview>>(`/interviews/${id}/cancel`, { notes }),
  complete: (id: number, feedback?: string) =>
    api.patch<ApiResponse<Interview>>(`/interviews/${id}/complete`, { feedback }),
  delete: (id: number) => api.delete(`/interviews/${id}`),
  getCalendar: (start: string, end: string, status?: string) =>
    api.get<ApiResponse<Interview[]>>('/interviews/calendar', { params: { start, end, status } }),
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
