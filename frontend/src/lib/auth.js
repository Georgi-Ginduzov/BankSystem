const STORAGE_KEY = 'banksystem.session'

export function getStoredSession() {
  if (typeof window === 'undefined') {
    return null
  }

  try {
    const raw = window.localStorage.getItem(STORAGE_KEY)
    if (!raw) {
      return null
    }

    const parsed = JSON.parse(raw)
    if (!parsed?.token || !parsed?.email || !parsed?.role) {
      return null
    }

    return parsed
  } catch {
    return null
  }
}

export function storeSession(session) {
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(session))
}

export function clearStoredSession() {
  window.localStorage.removeItem(STORAGE_KEY)
}

export function getAccessToken() {
  return getStoredSession()?.token ?? ''
}
