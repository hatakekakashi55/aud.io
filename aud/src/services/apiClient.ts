/**
 * Minimal typed HTTP client for the music service.
 * Every request carries Accept: application/json and surfaces
 * non-2xx responses as ApiError with the HTTP status attached.
 */
import { config, isApiConfigured } from '../config'

export class ApiError extends Error {
  readonly status: number
  readonly url: string

  constructor(message: string, status: number, url: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.url = url
  }
}

export async function apiGet<T>(path: string, signal?: AbortSignal): Promise<T> {
  if (!isApiConfigured()) {
    throw new ApiError('Music service URL is not configured (VITE_MUSIC_API_URL)', 0, path)
  }
  const url = `${config.apiUrl}${path.startsWith('/') ? path : `/${path}`}`
  let response: Response
  try {
    response = await fetch(url, {
      method: 'GET',
      headers: { Accept: 'application/json' },
      signal,
    })
  } catch (err) {
    const reason = err instanceof Error ? err.message : 'Network request failed'
    throw new ApiError(reason, 0, url)
  }
  if (!response.ok) {
    throw new ApiError(`Request failed with HTTP ${response.status}`, response.status, url)
  }
  return (await response.json()) as T
}
