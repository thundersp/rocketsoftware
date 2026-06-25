const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

const KEY_ALIASES = {
  Action: 'action',
  AssignmentId: 'assignmentId',
  AuditLogId: 'auditLogId',
  BuildingId: 'buildingId',
  BuildingName: 'buildingName',
  CancelledBy: 'cancelledBy',
  Capacity: 'capacity',
  Category: 'category',
  ChangedBy: 'changedBy',
  City: 'city',
  ConnectionLink: 'connectionLink',
  Country: 'country',
  CreatedAt: 'createdAt',
  CredentialActive: 'credentialActive',
  CredentialId: 'credentialId',
  Description: 'description',
  DialInInfo: 'dialInInfo',
  Email: 'email',
  EmployeeId: 'employeeId',
  EmployeeName: 'employeeName',
  EndUTC: 'endUtc',
  EntityId: 'entityId',
  EntityType: 'entityType',
  EquipmentId: 'equipmentId',
  EquipmentName: 'equipmentName',
  FirstName: 'firstName',
  Floor: 'floor',
  InviteSentAt: 'inviteSentAt',
  IsActive: 'isActive',
  IsPrimaryLocation: 'isPrimaryLocation',
  IsPrimaryRoom: 'isPrimaryRoom',
  IsRecurring: 'isRecurring',
  IsVideoEnabled: 'isVideoEnabled',
  IsVideoRoom: 'isVideoRoom',
  LastName: 'lastName',
  LocationId: 'locationId',
  MeetingAssignmentId: 'meetingAssignmentId',
  MeetingTitle: 'meetingTitle',
  Message: 'message',
  NewValues: 'newValues',
  NotificationId: 'notificationId',
  Notes: 'notes',
  OldValues: 'oldValues',
  OrganizerId: 'organizerId',
  OverriddenBy: 'overriddenBy',
  ParticipantId: 'participantId',
  PreviousAssignmentId: 'previousAssignmentId',
  Priority: 'priority',
  Quantity: 'quantity',
  RecurrencePattern: 'recurrencePattern',
  ResponseAt: 'responseAt',
  ResponseStatus: 'responseStatus',
  Responsibility: 'responsibility',
  RoleName: 'roleName',
  RoomCode: 'roomCode',
  RoomId: 'roomId',
  RoomName: 'roomName',
  RoomTypeId: 'roomTypeId',
  RoomTypeName: 'roomTypeName',
  SentAt: 'sentAt',
  StartUTC: 'startUtc',
  Status: 'status',
  TimeZoneId: 'timeZoneId',
  Title: 'title',
  Type: 'type',
  UpdatedAt: 'updatedAt',
  VideoReservationId: 'videoReservationId',
  VideoTitle: 'videoTitle',
};

function getToken() {
  return localStorage.getItem('buzzmeet_token');
}

export async function apiRequest(endpoint, options = {}) {
  const token = getToken();
  const headers = { ...options.headers };
  if (options.body !== undefined && !(options.body instanceof FormData)) {
    headers['Content-Type'] = headers['Content-Type'] || 'application/json';
  }
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(`${BASE_URL}${endpoint}`, {
    ...options,
    headers,
  });

  if (response.status === 401) {
    localStorage.removeItem('buzzmeet_token');
    localStorage.removeItem('buzzmeet_user');
    window.location.href = '/login';
    throw new Error('Unauthorized');
  }

  if (!response.ok) {
    const error = await readResponseBody(response);
    throw new Error(error?.message || error?.error || response.statusText || `Request failed with status ${response.status}`);
  }

  if (response.status === 204) return null;
  return readResponseBody(response);
}

export function get(endpoint) {
  return apiRequest(endpoint, { method: 'GET' });
}

export function post(endpoint, body = undefined) {
  return apiRequest(endpoint, { method: 'POST', body: body === undefined ? undefined : JSON.stringify(body) });
}

export function put(endpoint, body) {
  return apiRequest(endpoint, { method: 'PUT', body: JSON.stringify(body) });
}

export function del(endpoint) {
  return apiRequest(endpoint, { method: 'DELETE' });
}

export function buildQuery(params = {}) {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      query.append(key, value);
    }
  });
  const qs = query.toString();
  return qs ? `?${qs}` : '';
}

export function formatUtcParam(value) {
  if (!value) return value;
  const date = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toISOString().slice(0, 19).replace('T', ' ');
}

async function readResponseBody(response) {
  const text = await response.text();
  if (!text) return null;
  const contentType = response.headers.get('content-type') || '';
  if (!contentType.includes('application/json')) return text;
  return normalizeKeys(JSON.parse(text));
}

function normalizeKeys(value) {
  if (Array.isArray(value)) return value.map(normalizeKeys);
  if (!value || typeof value !== 'object') return value;
  return Object.fromEntries(
    Object.entries(value).map(([key, item]) => [KEY_ALIASES[key] || key, normalizeKeys(item)])
  );
}
