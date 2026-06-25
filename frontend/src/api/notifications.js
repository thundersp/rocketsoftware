import { buildQuery, get } from './client';

export function getNotifications(employeeId) {
  return get(`/notifications${buildQuery({ employeeId })}`);
}

export function getAuditLogs(params = {}) {
  return get(`/audit-logs${buildQuery(params)}`);
}
