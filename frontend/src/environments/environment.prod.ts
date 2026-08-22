/**
 * Environment configuration for ServiceMate Angular Application (Production)
 * Centralized API base URL pointing to the deployed Render backend.
 */
export const environment = {
  production: true,
  apiUrl: (typeof window !== 'undefined' && (window as any)?.__env?.apiUrl) || 'https://servicemate-jd5.onrender.com/api'
};

