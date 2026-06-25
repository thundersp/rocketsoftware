import { buildQuery, formatUtcParam, get, post, put, del } from './client';

export function getRooms(params = {}) {
  return get(`/rooms${buildQuery(params)}`);
}

export function getRoom(roomId) {
  return get(`/rooms/${roomId}`);
}

export function createRoom(data) {
  return post('/rooms', data);
}

export function updateRoom(roomId, data) {
  return put(`/rooms/${roomId}`, data);
}

export function deleteRoom(roomId) {
  return del(`/rooms/${roomId}`);
}

export function getRoomAvailability(roomId, startUtc, endUtc) {
  return get(`/rooms/${roomId}/availability${buildQuery({
    startUtc: formatUtcParam(startUtc),
    endUtc: formatUtcParam(endUtc),
  })}`);
}
