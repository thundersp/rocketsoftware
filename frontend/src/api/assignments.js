import { buildQuery, formatUtcParam, get, post, put, del } from './client';

export function getAssignments(params = {}) {
  return get(`/assignments${buildQuery({
    ...params,
    fromUtc: formatUtcParam(params.fromUtc),
    toUtc: formatUtcParam(params.toUtc),
  })}`);
}

export function getAssignment(assignmentId) {
  return get(`/assignments/${assignmentId}`);
}

export function createAssignment(data) {
  return post('/assignments', data);
}

export function updateAssignment(assignmentId, data) {
  return put(`/assignments/${assignmentId}`, data);
}

export function deleteAssignment(assignmentId) {
  return del(`/assignments/${assignmentId}`);
}

export function cancelAssignment(assignmentId, data = {}) {
  return post(`/assignments/${assignmentId}/cancel`, typeof data === 'string' ? { reason: data } : data);
}

export function overrideAssignment(assignmentId, data) {
  return post(`/assignments/${assignmentId}/override`, data);
}

// Room assignments
export function getRoomAssignments(params = {}) {
  return get(`/room-assignments${buildQuery({
    ...params,
    fromUtc: formatUtcParam(params.fromUtc),
    toUtc: formatUtcParam(params.toUtc),
  })}`);
}

export function getAssignmentRoomAssignments(assignmentId) {
  return get(`/assignments/${assignmentId}/room-assignments`);
}

export function addRoomToAssignment(assignmentId, data) {
  return post(`/assignments/${assignmentId}/room-assignments`, data);
}

export function updateRoomAssignment(meetingAssignmentId, data) {
  return put(`/room-assignments/${meetingAssignmentId}`, data);
}

export function removeRoomFromAssignment(assignmentId, meetingAssignmentId) {
  return del(`/assignments/${assignmentId}/room-assignments/${meetingAssignmentId}`);
}

// Video reservations
export function getVideoReservations(assignmentId) {
  return get(`/assignments/${assignmentId}/video-reservations`);
}

export function addVideoReservation(assignmentId, data) {
  return post(`/assignments/${assignmentId}/video-reservations`, data);
}

export function getVideoReservation(videoReservationId) {
  return get(`/video-reservations/${videoReservationId}`);
}

export function updateVideoReservation(videoReservationId, data) {
  return put(`/video-reservations/${videoReservationId}`, data);
}

export function deleteVideoReservation(videoReservationId) {
  return del(`/video-reservations/${videoReservationId}`);
}

export function getParticipants(assignmentId) {
  return get(`/assignments/${assignmentId}/participants`);
}

export function addParticipant(assignmentId, data) {
  return post(`/assignments/${assignmentId}/participants`, data);
}

export function removeParticipant(assignmentId, participantId) {
  return del(`/assignments/${assignmentId}/participants/${participantId}`);
}
