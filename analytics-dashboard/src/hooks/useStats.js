import { useState, useEffect, useCallback } from 'react';
import axios from 'axios';

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
const POLL_INTERVAL_MS = 10_000; // refresh every 10 s


// useStats — fetches and auto-refreshes analytics data from GET /api/stats.
export function useStats() {
  const [data,      setData]      = useState(null);
  const [loading,   setLoading]   = useState(true);
  const [error,     setError]     = useState(null);
  const [lastFetch, setLastFetch] = useState(null);

  const fetchStats = useCallback(async (showLoading = false) => {
    if (showLoading) setLoading(true);
    setError(null);
    try {
      const { data: json } = await axios.get(`${API_BASE}/api/stats`, {
        timeout: 8000,
        headers: { Accept: 'application/json' },
      });
      setData(json);
      setLastFetch(new Date());
    } catch (err) {
      const msg = err.response?.data?.detail
        ?? err.message
        ?? 'Failed to reach log-collector service';
      setError(msg);
    } finally {
      setLoading(false);
    }
  }, []);

  // Initial fetch
  useEffect(() => { fetchStats(true); }, [fetchStats]);

  // Auto-refresh
  useEffect(() => {
    const id = setInterval(() => fetchStats(false), POLL_INTERVAL_MS);
    return () => clearInterval(id);
  }, [fetchStats]);

  return { data, loading, error, lastFetch, refresh: () => fetchStats(true) };
}
