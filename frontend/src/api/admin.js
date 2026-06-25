import { buildQuery, get, post, put } from './client';

export function getAdminUsers(params = {}) {
  return get(`/admin/users${buildQuery(params)}`);
}

export function createAdminUser(data) {
  return post('/admin/users', data);
}

export function updateAdminUser(employeeId, data) {
  return put(`/admin/users/${employeeId}`, data);
}

export function deactivateAdminUser(employeeId) {
  return post(`/admin/users/${employeeId}/deactivate`);
}

export function assignUserRole(employeeId, data) {
  return post(`/admin/users/${employeeId}/roles`, data);
}

export function getEquipment(params = {}) {
  return get(`/admin/equipment${buildQuery(params)}`);
}

export function createEquipment(data) {
  return post('/admin/equipment', data);
}

export function updateEquipment(equipmentId, data) {
  return put(`/admin/equipment/${equipmentId}`, data);
}

export function assignEquipmentToRoom(equipmentId, data) {
  return post(`/admin/equipment/${equipmentId}/assign-room`, data);
}

export function retireEquipment(equipmentId, data = {}) {
  return post(`/admin/equipment/${equipmentId}/retire`, data);
}
