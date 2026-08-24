/**
 * Environment configuration for ServiceMate Angular Application (Development / Fallback)
 * Centralized API base URL pointing to local Spring Boot backend or deployed Render backend when hosted.
 */
export const environment = {
  production: false,
  apiUrl: (typeof window !== 'undefined' && (window as any)?.__env?.apiUrl)
    ? (window as any).__env.apiUrl
    : (typeof window !== 'undefined' && window.location.hostname !== 'localhost' && window.location.hostname !== '127.0.0.1')
      ? 'https://servicemate-ljd5.onrender.com/api'
      : 'http://localhost:8085/api'
};
