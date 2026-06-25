import { post, get } from './client';

export function login(email, password) {
  return post('/auth/login', { email, password });
}

export function signup(data) {
  return post('/auth/signup', data);
}

export function getMe() {
  return get('/auth/me');
}
