/**
 * Central runtime config. Values come from Vite env vars —
 * see .env.example and README.md → "Connecting your music service".
 */
export const config = {
  /** Base URL of the music service, e.g. https://api.example.com (no trailing slash). */
  apiUrl: (import.meta.env.VITE_MUSIC_API_URL as string | undefined)?.replace(/\/+$/, '') || '',
  appName: 'AUD',
  version: '1.0.0',
} as const

export const isApiConfigured = () => config.apiUrl.length > 0
