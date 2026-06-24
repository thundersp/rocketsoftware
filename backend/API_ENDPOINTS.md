# BuzzMeet Backend API Guide

## Overview

This document lists the backend API endpoints for the BuzzMeet meeting scheduler system.

It is split into two sections:

- Implemented now: endpoints already scaffolded in the Spring Boot backend
- Planned next: endpoints that are part of the agreed API contract and should be implemented in the next slices

Base URL during local development:

```text
http://localhost:8080
```

All protected endpoints require a JWT bearer token in the `Authorization` header:

```http
Authorization: Bearer <access-token>
```

Default seeded password for the sample users added in the database seed:

```text
Password123!
```

## Authentication Flow

1. Call `POST /api/auth/login` with employee email and password.
2. Copy the returned JWT access token.
3. Send the token in the `Authorization` header for all protected endpoints.
4. Call `GET /api/auth/me` to verify the logged-in user and roles.

Example login flow:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "Timothee.Greswell@BuzzwordSolutions.com",
    "password": "Password123!"
  }'
```

## Error Format

All API errors should use a consistent JSON structure:

```json
{
  "timestamp": "2026-06-24T18:30:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Authentication required",
  "path": "/api/auth/me",
  "details": []
}
```

---

# Implemented Now

## 1. Auth Endpoints

### POST /api/auth/login

Authenticates a user with employee email and password.

Request body:

```json
{
  "email": "Timothee.Greswell@BuzzwordSolutions.com",
  "password": "Password123!"
}
```

Success response:

```json
{
  "accessToken": "<jwt-token>",
  "tokenType": "Bearer",
  "expiresIn": 3600000,
  "employeeId": 31,
  "email": "Timothee.Greswell@BuzzwordSolutions.com",
  "roles": [
    "ROLE_ADMIN",
    "ROLE_ORGANIZER"
  ]
}
```

Usage notes:

- `email` must match `Employee.email`
- password is validated against `User_Credentials.PasswordHash`
- the token should be stored client-side and sent as a bearer token on later requests

### GET /api/auth/me

Returns the currently authenticated user profile.

Headers:

```http
Authorization: Bearer <access-token>
```

Success response:

```json
{
  "employeeId": 31,
  "email": "Timothee.Greswell@BuzzwordSolutions.com",
  "firstName": "Timothee",
  "lastName": "Greswell",
  "title": "Manager",
  "roles": [
    "ROLE_ADMIN",
    "ROLE_ORGANIZER"
  ]
}
```

Usage notes:

- use this endpoint after login to hydrate the frontend user session
- use the returned roles to conditionally show manager-only actions such as override and global cancellation

---

## 2. Lookup Endpoints

These endpoints support dropdowns and basic reference data for forms.

### GET /api/locations

Returns all office locations.

Example:

```bash
curl http://localhost:8080/api/locations \
  -H "Authorization: Bearer <access-token>"
```

Success response:

```json
[
  {
    "id": 1,
    "phone": "+81 664 654 5882",
    "street": "384-1106, Yahara, Nerima-ku",
    "country": "Japan",
    "city": "Tokyo"
  }
]
```

Use this for:

- room creation forms
- meeting location filters
- video reservation location selection

### GET /api/buildings

Returns all buildings. Can be filtered by location.

Query parameters:

- `locationId` optional

Examples:

```bash
curl "http://localhost:8080/api/buildings" \
  -H "Authorization: Bearer <access-token>"
```

```bash
curl "http://localhost:8080/api/buildings?locationId=3" \
  -H "Authorization: Bearer <access-token>"
```

Success response:

```json
[
  {
    "buildingId": 3,
    "locationId": 3,
    "locationCity": "Dallas",
    "buildingName": "Dallas Collaboration Tower",
    "addressLine1": "3607 Fawn Valley Dr",
    "addressLine2": "Suite 400",
    "status": "ACTIVE"
  }
]
```

Use this for:

- room creation
- room filtering
- showing the building context for a room

### GET /api/room-types

Returns all available room types.

Example:

```bash
curl http://localhost:8080/api/room-types \
  -H "Authorization: Bearer <access-token>"
```

Success response:

```json
[
  {
    "roomTypeId": 1,
    "typeName": "CONFERENCE",
    "description": "Standard conference room for team meetings",
    "isBookable": "Y",
    "isVideoEnabled": "Y",
    "requiresApproval": "N"
  }
]
```

Use this for:

- room creation and edit forms
- room filtering
- approval logic in meeting booking

### GET /api/time-zones

Returns all supported time zones.

Example:

```bash
curl http://localhost:8080/api/time-zones \
  -H "Authorization: Bearer <access-token>"
```

Success response:

```json
[
  {
    "timeZoneId": 3,
    "zoneName": "America/Chicago",
    "gmtOffsetMinutes": -360,
    "isDstSupported": "Y",
    "isActive": "Y"
  }
]
```

Use this for:

- assignment creation
- video reservation setup
- local time rendering support

### GET /api/employees

Returns employees for participant pickers and organizer selection.

Query parameters:

- `locationId` optional
- `title` optional

Examples:

```bash
curl "http://localhost:8080/api/employees" \
  -H "Authorization: Bearer <access-token>"
```

```bash
curl "http://localhost:8080/api/employees?locationId=3&title=Manager" \
  -H "Authorization: Bearer <access-token>"
```

Success response:

```json
[
  {
    "id": 31,
    "firstName": "Timothee",
    "lastName": "Greswell",
    "title": "Manager",
    "email": "Timothee.Greswell@BuzzwordSolutions.com",
    "country": "Japan",
    "city": "Tokyo",
    "locationId": 1
  }
]
```

Use this for:

- organizer selection
- participant selection
- manager lookup for approvals and overrides

---

# Planned Next

The following endpoints are part of the target API design for the full system.

## 3. Room Management Endpoints

### GET /api/rooms

Purpose:

- list meeting rooms
- support room search and filtering

Query parameters:

- `locationId` optional
- `buildingId` optional
- `roomTypeId` optional
- `capacity` optional
- `status` optional
- `isVideoRoom` optional

Example:

```bash
curl "http://localhost:8080/api/rooms?locationId=3&isVideoRoom=true" \
  -H "Authorization: Bearer <access-token>"
```

### GET /api/rooms/{roomId}

Purpose:

- fetch one room with building, location, type, and equipment context

### POST /api/rooms

Purpose:

- add a single meeting room

Request body:

```json
{
  "buildingId": 3,
  "roomTypeId": 1,
  "roomCode": "DAL-410",
  "roomName": "Mockingbird Conference",
  "capacity": 12,
  "floor": 4,
  "isVideoRoom": "Y",
  "dialInInfo": "Dallas bridge ext. 9410",
  "status": "ACTIVE",
  "notes": "Reserved for customer-facing meetings"
}
```

### PUT /api/rooms/{roomId}

Purpose:

- edit room metadata and operational status

### DELETE /api/rooms/{roomId}

Purpose:

- guarded delete or soft delete a room

Rule:

- reject deletion if future active room assignments exist

### GET /api/rooms/{roomId}/availability

Purpose:

- check if a room is free for a UTC time window

Query parameters:

- `startUtc` required
- `endUtc` required

---

## 4. Assignment Endpoints

### POST /api/assignments

Purpose:

- create a meeting assignment with rooms, participants, and video reservations in one transaction

Request body:

```json
{
  "organizerId": 31,
  "meetingTitle": "Quarterly Sales Kickoff",
  "description": "Cross-region kickoff for quarterly sales targets.",
  "startUtc": "2026-07-01T13:00:00Z",
  "endUtc": "2026-07-01T14:30:00Z",
  "secondaryTimeZoneId": 5,
  "priority": "HIGH",
  "isRecurring": "N",
  "recurrencePattern": null,
  "participants": [
    {
      "employeeId": 14,
      "status": "ATTENDEE",
      "responsibility": "Pipeline update"
    },
    {
      "employeeId": 58,
      "status": "APPROVER",
      "responsibility": "Approve room use"
    }
  ],
  "roomAssignments": [
    {
      "roomId": 103,
      "isPrimaryRoom": "Y"
    }
  ],
  "videoReservations": [
    {
      "meetingAssignmentId": 2001,
      "locationId": 3,
      "timeZoneId": 3,
      "videoTitle": "Sales Kickoff Bridge",
      "isPrimaryLocation": "Y",
      "isVideoEnabled": "Y",
      "connectionLink": "https://meet.buzzmeet.example/qsk",
      "dialInInfo": "+1-214-555-0101,,991001#"
    }
  ]
}
```

Key rules:

- organizer must be allowed to organize meetings
- `startUtc` must be before `endUtc`
- no overlapping room reservations for the same room and time window
- boardrooms requiring approval should include an approver participant

### GET /api/assignments

Purpose:

- list assignments with filters

Query parameters:

- `organizerId` optional
- `participantEmployeeId` optional
- `status` optional
- `locationId` optional
- `roomId` optional
- `fromUtc` optional
- `toUtc` optional
- `priority` optional

### GET /api/assignments/{assignmentId}

Purpose:

- return full assignment detail

Expected response sections:

- assignment header
- organizer
- participants
- room assignments
- video reservations
- timezone information
- cancellation and override metadata

### PUT /api/assignments/{assignmentId}

Purpose:

- update a non-terminal assignment

Recommended scope:

- title
- description
- time window
- participants
- room links
- video reservation links

---

## 5. Room Assignment Endpoints

### GET /api/room-assignments

Purpose:

- view meeting room assignments directly

Query parameters:

- `roomId` optional
- `locationId` optional
- `status` optional
- `fromUtc` optional
- `toUtc` optional

This endpoint directly covers the requirement to view meeting room assignments.

### GET /api/assignments/{assignmentId}/room-assignments

Purpose:

- return all room reservations linked to one assignment

### POST /api/assignments/{assignmentId}/room-assignments

Purpose:

- attach another room to an existing assignment

Request body:

```json
{
  "roomId": 106,
  "isPrimaryRoom": "N"
}
```

### PUT /api/room-assignments/{meetingAssignmentId}

Purpose:

- update reservation state

Supported status values:

- `RESERVED`
- `CANCELLED`
- `RELEASED`

### DELETE /api/assignments/{assignmentId}/room-assignments/{meetingAssignmentId}

Purpose:

- remove a linked room from an assignment when allowed

---

## 6. Video Reservation Endpoints

### GET /api/assignments/{assignmentId}/video-reservations

Purpose:

- list all video reservations for one assignment

### POST /api/assignments/{assignmentId}/video-reservations

Purpose:

- link multiple video reservations to a single assignment

This endpoint directly covers the requirement to link multiple video reservations into one assignment.

Request body:

```json
{
  "meetingAssignmentId": 2001,
  "locationId": 5,
  "timeZoneId": 5,
  "videoTitle": "EMEA Sales Bridge",
  "isPrimaryLocation": "N",
  "isVideoEnabled": "Y",
  "connectionLink": "https://meet.buzzmeet.example/qsk-emea",
  "dialInInfo": "+49-30-555-0106,,991002#"
}
```

Rules:

- the `meetingAssignmentId` must belong to the target assignment
- the location and timezone must be valid
- the reservation should be returned with both UTC meeting time and local timezone context

### GET /api/video-reservations/{videoReservationId}

Purpose:

- fetch a single video reservation with room and assignment context

### PUT /api/video-reservations/{videoReservationId}

Purpose:

- update link, dial-in info, title, or status

### DELETE /api/video-reservations/{videoReservationId}

Purpose:

- remove or cancel a linked video reservation

---

## 7. Manager Action Endpoints

### POST /api/assignments/{assignmentId}/cancel

Purpose:

- cancel an assignment

Allowed for:

- organizer of the assignment
- manager role
- admin role

Request body:

```json
{
  "reason": "Organizer is unavailable"
}
```

Effects:

- assignment status becomes `CANCELLED`
- `CancelledBy` is set
- linked room assignments are released or cancelled based on rules
- audit log row is created
- notification rows are created

### POST /api/assignments/{assignmentId}/override

Purpose:

- manager-only override of an assignment

Allowed for:

- manager role
- admin role

Request body:

```json
{
  "reason": "Priority executive scheduling conflict",
  "newStartUtc": "2026-07-01T15:00:00Z",
  "newEndUtc": "2026-07-01T16:00:00Z",
  "newRoomId": 103
}
```

Recommended behavior:

- preserve the original assignment for history
- create a replacement assignment
- link the replacement using `PreviousAssignmentId`
- set `OverriddenBy` on the old record
- create audit and notification rows

---

## 8. Operational Endpoints

### GET /api/notifications

Purpose:

- return notification history for the logged-in employee or a selected employee

Query parameters:

- `employeeId` optional if admins are allowed to inspect others

### GET /api/audit-logs

Purpose:

- return change history for assignments and related records

Query parameters:

- `entityType` optional
- `entityId` optional

---

# Recommended Role Rules

Suggested backend role behavior:

- `ROLE_EMPLOYEE`
  - can view lookups
  - can view assignments they belong to
- `ROLE_ORGANIZER`
  - can create assignments
  - can manage assignments they own
- `ROLE_APPROVER`
  - can approve restricted room workflows
- `ROLE_ADMIN`
  - can manage rooms
  - can cancel or override assignments globally

---

# Suggested Frontend Usage Order

1. Login with `POST /api/auth/login`
2. Call `GET /api/auth/me`
3. Load lookup data:
   - `GET /api/locations`
   - `GET /api/buildings`
   - `GET /api/room-types`
   - `GET /api/time-zones`
   - `GET /api/employees`
4. Load rooms or assignments depending on the page
5. Create or manage assignments
6. Use manager actions only when the signed-in user has the right role

---

# Current Implementation Status

Implemented in backend now:

- `POST /api/auth/login`
- `GET /api/auth/me`
- `GET /api/locations`
- `GET /api/buildings`
- `GET /api/room-types`
- `GET /api/time-zones`
- `GET /api/employees`

Planned next implementation:

- room CRUD
- room availability
- assignment CRUD and detail views
- room assignment views and management
- video reservation views and management
- manager cancel and override flows
- notifications and audit log read APIs
