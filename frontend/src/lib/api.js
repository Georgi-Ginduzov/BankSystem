import { getAccessToken } from './auth'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api'

export async function requestJson(path, options = {}) {
  const token = getAccessToken()
  const headers = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...(options.headers ?? {}),
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers,
    ...options,
  })
  const data = await response.json().catch(() => ({}))

  if (!response.ok) {
    const validationMessage =
      data && typeof data === 'object'
        ? Object.values(data).find((value) => typeof value === 'string')
        : ''
    throw new Error(data.message || data.error || validationMessage || 'Request failed.')
  }

  return data
}
