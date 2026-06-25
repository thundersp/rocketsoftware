import { buildQuery, get } from './client';

export function getLocations() {
  return get('/locations');
}

export function getBuildings(locationId) {
  return get(`/buildings${buildQuery({ locationId })}`);
}

export function getRoomTypes() {
  return get('/room-types');
}

export function getTimeZones() {
  return get('/time-zones');
}

export function getEmployees(params = {}) {
  return get(`/employees${buildQuery(params)}`);
}
