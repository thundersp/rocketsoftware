# BuzzMeet API Endpoints

Base URL: `http://localhost:8080`

All protected endpoints require the header:
```
Authorization: Bearer <JWT access token>
```

JWT tokens are obtained from `/api/auth/login` or `/api/auth/signup`.

---

## Table of Contents
  
1. [Authentication](#authentication)
2. [Admin — Users](#admin--users)
3. [Admin — Equipment](#admin--equipment)
4. [Assignments](#assignments)
5. [Room Assignments](#room-assignments)
6. [Video Reservations](#video-reservations)
7. [Participants](#participants)
8. [Rooms](#rooms)
9. [Lookup / Reference Data](#lookup--reference-data)
10. [Notifications](#notifications)
11. [Audit Logs](#audit-logs)

---

## Authentication

> Public endpoints — no token required.

### `POST /api/auth/login`

Authenticates a user and returns a JWT access token.

**Request Body**
```json
{
  "email": "user@example.com",    // required, valid email
  "password": "secret123"         // required, non-blank
}
```

**Response `200 OK`**
```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "employeeId": 42,
  "email": "user@example.com",
  "roles": ["EMPLOYEE"]
}
```

---

### `POST /api/auth/signup`

Registers a new user account and returns a JWT access token.

**Request Body**
```json
{
  "firstName": "Jane",            // required, max 50 chars
  "lastName":  "Doe",             // required, max 50 chars
  "email":     "jane@example.com",// required, valid email, max 50 chars
  "password":  "password123",     // required, 8–128 chars
  "locationId": 1,                // required, integer
  "role":      "EMPLOYEE",        // required, non-blank
  "title":     "SWE",             // optional, max 11 chars
  "country":   "USA",             // optional, max 50 chars
  "city":      "Boston"           // optional, max 50 chars
}
```

**Response `200 OK`** — same shape as `/login`.

---

### `GET /api/auth/me`

Returns the profile of the currently authenticated user.

**Auth required:** JWT Bearer token (`meeting:view` authority)

**Response `200 OK`**
```json
{
  "employeeId": 42,
  "email":      "jane@example.com",
  "firstName":  "Jane",
  "lastName":   "Doe",
  "title":      "SWE",
  "roles":      ["EMPLOYEE"]
}
```

---

## Admin — Users

> All endpoints require `user:manage` authority (typically `ADMIN` role).

### `GET /api/admin/users`

Retrieves a list of all users, optionally filtered.

**Query Params**
| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `activeOnly` | boolean | No | If `true`, returns only active users |
| `roleName` | string | No | Filter by role name (e.g. `MANAGER`) |

**Response `200 OK`** — Array of user objects.

---

### `POST /api/admin/users`

Creates a new user/employee record.

**Request Body** (`Map<String, Object>`) — user fields (name, email, locationId, role, etc.)

**Response `200 OK`**
```json
{ "employeeId": 99 }
```

---

### `PUT /api/admin/users/{employeeId}`

Updates an existing user's details.

**Path Params**
| Param | Type | Description |
|-------|------|-------------|
| `employeeId` | integer | ID of the employee to update |

**Request Body** (`Map<String, Object>`) — fields to update.

**Response `200 OK`** (empty body)

---

### `POST /api/admin/users/{employeeId}/deactivate`

Deactivates a user account.

**Path Params**
| Param | Type | Description |
|-------|------|-------------|
| `employeeId` | integer | ID of the employee to deactivate |

**Response `200 OK`** (empty body)

---

### `POST /api/admin/users/{employeeId}/roles`

Assigns a role to a user.

**Path Params**
| Param | Type | Description |
|-------|------|-------------|
| `employeeId` | integer | ID of the employee |

**Request Body** (`Map<String, Object>`) — e.g. `{ "roleName": "MANAGER" }`

**Response `200 OK`** (empty body)

---

## Admin — Equipment

> All endpoints require `equipment:manage` authority.

### `GET /api/admin/equipment`

Retrieves all equipment records, optionally filtered by status.

**Query Params**
| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `status` | string | No | Filter by equipment status (e.g. `ACTIVE`, `RETIRED`) |

**Response `200 OK`** — Array of equipment objects.

---

### `POST /api/admin/equipment`

Creates a new equipment record.

**Request Body** (`Map<String, Object>`) — equipment details (name, type, serialNumber, etc.)

**Response `200 OK`**
```json
{ "equipmentId": 7 }
```

---

### `PUT /api/admin/equipment/{equipmentId}`

Updates an existing equipment record.

**Path Params**
| Param | Type | Description |
|-------|------|-------------|
| `equipmentId` | integer | ID of the equipment |

**Request Body** (`Map<String, Object>`) — fields to update.

**Response `200 OK`** (empty body)

---

### `POST /api/admin/equipment/{equipmentId}/assign-room`

Assigns a piece of equipment to a room.

**Path Params**
| Param | Type | Description |
|-------|------|-------------|
| `equipmentId` | integer | ID of the equipment |

**Request Body** (`Map<String, Object>`) — e.g. `{ "roomId": 3 }`

**Response `200 OK`** (empty body)

---

### `POST /api/admin/equipment/{equipmentId}/retire`

Marks a piece of equipment as retired.

**Path Params**
| Param | Type | Description |
|-------|------|-------------|
| `equipmentId` | integer | ID of the equipment |

**Request Body** (`Map<String, Object>`, optional) — optional retirement notes/reason.

**Response `200 OK`** (empty body)

---

## Assignments

> Assignments represent meeting/event records.

### `POST /api/assignments`

**Auth:** `meeting:create`

Creates a new assignment (meeting/event).

**Request Body** (`Map<String, Object>`) — meeting details: title, organizerId, locationId, startUtc, endUtc, priority, etc.

**Response `200 OK`** (empty body)

---

### `GET /api/assignments`

**Auth:** `meeting:view`

Retrieves a list of assignments with optional filters.

**Query Params**
| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `organizerId` | integer | No | Filter by organizer employee ID |
| `participantEmployeeId` | integer | No | Filter by participant employee ID |
| `status` | string | No | Assignment status (e.g. `PENDING`, `CONFIRMED`, `CANCELLED`) |
| `locationId` | integer | No | Filter by location |
| `roomId` | integer | No | Filter by room |
| `fromUtc` | string | No | Start of date range (ISO-8601 UTC) |
| `toUtc` | string | No | End of date range (ISO-8601 UTC) |
| `priority` | string | No | Filter by priority level |

**Response `200 OK`** — Array of assignment objects.

---

### `GET /api/assignments/{assignmentId}`

**Auth:** `meeting:view`

Retrieves full details of a single assignment.

**Path Params**
| Param | Type | Description |
|-------|------|-------------|
| `assignmentId` | integer | ID of the assignment |

**Response `200 OK`** — Assignment detail object.

---

### `PUT /api/assignments/{assignmentId}`

**Auth:** `meeting:create`

Updates an existing assignment.

**Path Params**
| Param | Type | Description |
|-------|------|-------------|
| `assignmentId` | integer | ID of the assignment |

**Request Body** (`Map<String, Object>`) — fields to update.

**Response `200 OK`** (empty body)

---

### `DELETE /api/assignments/{assignmentId}`

**Auth:** `meeting:create`

Deletes an assignment.

**Path Params**
| Param | Type | Description |
|-------|------|-------------|
| `assignmentId` | integer | ID of the assignment |

**Response `200 OK`** (empty body)

---

### `POST /api/assignments/{assignmentId}/cancel`

**Auth:** `meeting:create`

Cancels an assignment.

**Path Params**
| Param | Type | Description |
|-------|------|-------------|
| `assignmentId` | integer | ID of the assignment |

**Request Body** (`Map<String, Object>`) — e.g. `{ "reason": "Rescheduled" }`

**Response `200 OK`** (empty body)

---

### `POST /api/assignments/{assignmentId}/override`

**Auth:** `meeting:override`

Overrides constraints on an assignment (e.g. forces booking despite conflicts).

**Path Params**
| Param | Type | Description |
|-------|------|-------------|
| `assignmentId` | integer | ID of the assignment |

**Request Body** (`Map<String, Object>`) — override justification/details.

**Response `200 OK`** (empty body)

---

## Room Assignments

> Room assignments link a meeting assignment to a physical room.

### `GET /api/room-assignments`

**Auth:** `meeting:view`

Retrieves room assignments with optional filters.

**Query Params**
| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `roomId` | integer | No | Filter by room |
| `locationId` | integer | No | Filter by location |
| `status` | string | No | Filter by status |
| `fromUtc` | string | No | Start of date range (ISO-8601 UTC) |
| `toUtc` | string | No | End of date range (ISO-8601 UTC) |

**Response `200 OK`** — Array of room assignment objects.

---

### `GET /api/assignments/{assignmentId}/room-assignments`

**Auth:** `meeting:view`

Retrieves all room assignments for a specific assignment.

**Path Params**
| Param | Type | Description |
|-------|------|-------------|
| `assignmentId` | integer | ID of the parent assignment |

**Response `200 OK`** — Array of room assignment objects.

---

### `POST /api/assignments/{assignmentId}/room-assignments`

**Auth:** `meeting:book`

Adds a room assignment to an existing assignment (books a room for the meeting).

**Path Params**
| Param | Type | Description |
|-------|------|-------------|
| `assignmentId` | integer | ID of the parent assignment |

**Request Body** (`Map<String, Object>`) — e.g. `{ "roomId": 5, "startUtc": "...", "endUtc": "..." }`

**Response `200 OK`** (empty body)

---

### `PUT /api/room-assignments/{meetingAssignmentId}`

**Auth:** `meeting:book`

Updates a specific room assignment.

**Path Params**
| Param | Type | Description |
|-------|------|-------------|
| `meetingAssignmentId` | integer | ID of the room assignment |

**Request Body** (`Map<String, Object>`) — fields to update.

**Response `200 OK`** (empty body)

---

### `DELETE /api/assignments/{assignmentId}/room-assignments/{meetingAssignmentId}`

**Auth:** `meeting:book`

Removes a room assignment from an assignment (releases the booked room).

**Path Params**
| Param | Type | Description |
|-------|------|-------------|
| `assignmentId` | integer | ID of the parent assignment |
| `meetingAssignmentId` | integer | ID of the room assignment to remove |

**Response `200 OK`** (empty body)

---

## Video Reservations

> Video reservations link a meeting assignment to a video conferencing resource.

### `GET /api/assignments/{assignmentId}/video-reservations`

**Auth:** `meeting:view`

Retrieves all video reservations for a specific assignment.

**Path Params**
| Param | Type | Description |
|-------|------|-------------|
| `assignmentId` | integer | ID of the assignment |

**Response `200 OK`** — Array of video reservation objects.

---

### `POST /api/assignments/{assignmentId}/video-reservations`

**Auth:** `meeting:book`

Adds a video reservation to an assignment.

**Path Params**
| Param | Type | Description |
|-------|------|-------------|
| `assignmentId` | integer | ID of the assignment |

**Request Body** (`Map<String, Object>`) — video resource details (codec, bridge, etc.)

**Response `200 OK`** (empty body)

---

### `GET /api/video-reservations/{videoReservationId}`

**Auth:** `meeting:view`

Retrieves a single video reservation by ID.

**Path Params**
| Param | Type | Description |
|-------|------|-------------|
| `videoReservationId` | integer | ID of the video reservation |

**Response `200 OK`** — Video reservation detail object.

---

### `PUT /api/video-reservations/{videoReservationId}`

**Auth:** `meeting:book`

Updates a video reservation.

**Path Params**
| Param | Type | Description |
|-------|------|-------------|
| `videoReservationId` | integer | ID of the video reservation |

**Request Body** (`Map<String, Object>`) — fields to update.

**Response `200 OK`** (empty body)

---

### `DELETE /api/video-reservations/{videoReservationId}`

**Auth:** `meeting:book`

Deletes a video reservation (releases the video resource).

**Path Params**
| Param | Type | Description |
|-------|------|-------------|
| `videoReservationId` | integer | ID of the video reservation |

**Response `200 OK`** (empty body)

---

## Participants

### `GET /api/assignments/{assignmentId}/participants`

**Auth:** `meeting:view`

Lists all participants of an assignment.

**Path Params**
| Param | Type | Description |
|-------|------|-------------|
| `assignmentId` | integer | ID of the assignment |

**Response `200 OK`** — Array of participant objects.

---

### `POST /api/assignments/{assignmentId}/participants`

**Auth:** `meeting:participants:update`

Adds a participant to an assignment.

**Path Params**
| Param | Type | Description |
|-------|------|-------------|
| `assignmentId` | integer | ID of the assignment |

**Request Body** (`Map<String, Object>`) — e.g. `{ "employeeId": 15 }`

**Response `200 OK`** (empty body)

---

### `DELETE /api/assignments/{assignmentId}/participants/{participantId}`

**Auth:** `meeting:participants:update`

Removes a participant from an assignment.

**Path Params**
| Param | Type | Description |
|-------|------|-------------|
| `assignmentId` | integer | ID of the assignment |
| `participantId` | integer | ID of the participant record to remove |

**Response `200 OK`** (empty body)

---

## Rooms

### `GET /api/rooms`

**Auth:** `meeting:view`

Retrieves a list of rooms with optional filters.

**Query Params**
| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `locationId` | integer | No | Filter by location |
| `buildingId` | integer | No | Filter by building |
| `roomTypeId` | integer | No | Filter by room type |
| `capacity` | integer | No | Minimum capacity |
| `status` | string | No | Room status (e.g. `ACTIVE`) |
| `isVideoRoom` | boolean | No | If `true`, returns only video-enabled rooms |

**Response `200 OK`** — Array of room objects.

---

### `GET /api/rooms/{roomId}`

**Auth:** `meeting:view`

Retrieves a single room's full details.

**Path Params**
| Param | Type | Description |
|-------|------|-------------|
| `roomId` | integer | ID of the room |

**Response `200 OK`** — Room detail object.

---

### `POST /api/rooms`

**Auth:** `room:manage`

Creates a new room.

**Request Body** (`Map<String, Object>`) — room details (name, buildingId, capacity, roomTypeId, isVideoRoom, etc.)

**Response `200 OK`** (empty body)

---

### `PUT /api/rooms/{roomId}`

**Auth:** `room:manage`

Updates an existing room.

**Path Params**
| Param | Type | Description |
|-------|------|-------------|
| `roomId` | integer | ID of the room |

**Request Body** (`Map<String, Object>`) — fields to update.

**Response `200 OK`** (empty body)

---

### `DELETE /api/rooms/{roomId}`

**Auth:** `room:manage`

Deletes a room.

**Path Params**
| Param | Type | Description |
|-------|------|-------------|
| `roomId` | integer | ID of the room |

**Response `200 OK`** (empty body)

---

### `GET /api/rooms/{roomId}/availability`

**Auth:** `meeting:view`

Returns the availability schedule for a room within a given time window.

**Path Params**
| Param | Type | Description |
|-------|------|-------------|
| `roomId` | integer | ID of the room |

**Query Params**
| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `startUtc` | string | No | Start of window (ISO-8601 UTC) |
| `endUtc` | string | No | End of window (ISO-8601 UTC) |

**Response `200 OK`** — Array of availability slot objects.

---

## Lookup / Reference Data

> All lookup endpoints require `meeting:view` authority.

### `GET /api/locations`

Returns all office locations.

**Response `200 OK`** — Array of `{ locationId, name, ... }`.

---

### `GET /api/buildings`

Returns buildings, optionally filtered by location.

**Query Params**
| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `locationId` | integer | No | Filter buildings by location |

**Response `200 OK`** — Array of `{ buildingId, name, locationId, ... }`.

---

### `GET /api/room-types`

Returns all room types (e.g. Conference, Huddle, Training).

**Response `200 OK`** — Array of `{ roomTypeId, name }`.

---

### `GET /api/time-zones`

Returns all available time zones.

**Response `200 OK`** — Array of `{ tzId, tzName, utcOffset, ... }`.

---

### `GET /api/employees`

Returns an employee directory, optionally filtered.

**Query Params**
| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `locationId` | integer | No | Filter by location |
| `title` | string | No | Filter by job title |

**Response `200 OK`** — Array of `{ employeeId, firstName, lastName, email, title, locationId }`.

---

## Notifications

### `GET /api/notifications`

**Auth:** `meeting:view`

Retrieves notifications, optionally filtered by employee.

**Query Params**
| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `employeeId` | integer | No | Filter notifications for a specific employee |

**Response `200 OK`** — Array of notification objects.

---

## Audit Logs

### `GET /api/audit-logs`

**Auth:** Roles `ADMIN`, `MANAGER`, or `APPROVER`

Retrieves the audit log, optionally filtered by entity type and/or ID.

**Query Params**
| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `entityType` | string | No | Entity type to filter (e.g. `ASSIGNMENT`, `ROOM`) |
| `entityId` | integer | No | ID of the specific entity to filter on |

**Response `200 OK`** — Array of audit log entries.

---

## Authority / Role Reference

| Authority | Granted to | Description |
|-----------|-----------|-------------|
| `meeting:view` | All authenticated users | Read access to assignments, rooms, lookups, notifications |
| `meeting:create` | EMPLOYEE and above | Create, update, delete, cancel assignments |
| `meeting:book` | EMPLOYEE and above | Add/update/delete room and video reservations |
| `meeting:participants:update` | EMPLOYEE and above | Add or remove participants from an assignment |
| `meeting:override` | APPROVER, ADMIN | Override assignment constraints/conflicts |
| `user:manage` | ADMIN | Full CRUD on user accounts and roles |
| `room:manage` | ADMIN, MANAGER | Create, update, delete rooms |
| `equipment:manage` | ADMIN, MANAGER | Full CRUD on equipment and room assignments |
