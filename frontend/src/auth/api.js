// Utilidades de autenticación: guarda el JWT y lo adjunta a cada petición API.

// En prod tras nginx: vacío (mismo origen). Con frontend separado (S3):
// VITE_API_URL=https://api.midominio.com
const API_BASE = (import.meta.env.VITE_API_URL || '').replace(/\/$/, '');

const TOKEN_KEY = 'geologix_token';
const USER_KEY = 'geologix_user';

export function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export function getUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY));
  } catch {
    return null;
  }
}

export function saveSession(token, username, role) {
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem(USER_KEY, JSON.stringify({ username, role }));
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}

export function isAdmin() {
  return getUser()?.role === 'ADMIN';
}

/** fetch con Authorization: Bearer <token> incluido automáticamente. */
export async function apiFetch(path, options = {}) {
  const headers = { ...(options.headers || {}) };
  const token = getToken();
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  if (options.body && !headers['Content-Type']) {
    headers['Content-Type'] = 'application/json';
  }
  const res = await fetch(`${API_BASE}${path}`, { ...options, headers });
  if (res.status === 401 && getToken()) {
    // Había sesión y expiró/falló → limpiar y volver al login.
    clearSession();
    window.location.reload();
    throw new Error('Sesión expirada');
  }
  return res;
}

export async function login(username, password) {
  const res = await fetch(`${API_BASE}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password })
  });
  if (!res.ok) {
    throw new Error(res.status === 401 ? 'Credenciales inválidas' : `Error ${res.status}`);
  }
  const data = await res.json();
  saveSession(data.token, data.username, data.role);
  return data;
}
