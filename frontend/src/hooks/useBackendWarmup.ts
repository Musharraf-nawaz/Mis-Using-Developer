import { useEffect } from 'react';

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'https://mis-using-developer.onrender.com/api';

/** Pings the backend on mount so Render free-tier instances wake before user actions. */
export function useBackendWarmup() {
  useEffect(() => {
    const controller = new AbortController();
    fetch(`${API_BASE.replace(/\/api$/, '')}/api/health`, {
      signal: controller.signal,
      cache: 'no-store',
    }).catch(() => {});
    return () => controller.abort();
  }, []);
}
