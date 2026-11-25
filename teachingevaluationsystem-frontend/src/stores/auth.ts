export type Side = 'admin' | 'student'

const TOKEN_KEY = 'token'
const SIDE_KEY = 'side'
const NAME_KEY = 'name'

export function setAuth(token: string, side: Side, name: string) {
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(SIDE_KEY, side)
  localStorage.setItem(NAME_KEY, name)
}

export function clearAuth() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(SIDE_KEY)
  localStorage.removeItem(NAME_KEY)
}

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function getSide(): Side | '' {
  return (localStorage.getItem(SIDE_KEY) as Side | null) || ''
}

export function getDisplayName() {
  return localStorage.getItem(NAME_KEY) || ''
}
