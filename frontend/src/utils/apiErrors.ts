import type { AxiosError } from 'axios';

type ApiErrorBody = {
  message?: string;
  data?: Record<string, string> | string;
};

export function extractApiError(error: unknown, fallback = 'Operation failed'): string {
  const axiosError = error as AxiosError<ApiErrorBody>;
  const body = axiosError.response?.data;

  if (body?.data && typeof body.data === 'object') {
    const details = Object.entries(body.data)
      .map(([field, msg]) => `${field}: ${msg}`)
      .join(', ');
    if (details) return `Validation failed — ${details}`;
  }

  if (body?.message) return body.message;

  if (axiosError.response?.status === 403) {
    return 'Access denied. Admin login is required for this action.';
  }

  if (axiosError.code === 'ECONNABORTED') {
    return 'Request timed out. The server may be waking up — please try again in a moment.';
  }

  return fallback;
}

export function sanitizeFormPayload<T extends Record<string, unknown>>(data: T): Partial<T> {
  const cleaned: Record<string, unknown> = {};

  for (const [key, value] of Object.entries(data)) {
    if (value === '' || value === null || value === undefined) continue;
    if (typeof value === 'string' && value.trim() === '') continue;
    cleaned[key] = value;
  }

  return cleaned as Partial<T>;
}

export function toNumber(value: unknown): number | undefined {
  if (value === '' || value === null || value === undefined) return undefined;
  const n = Number(value);
  return Number.isNaN(n) ? undefined : n;
}
