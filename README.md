# Employee Availability & Status Feature

## What This Feature Does

This feature allows users to:

1. **See if an employee is currently in a meeting** — just like Outlook shows "In a Meeting" or "Available" next to a contact.
2. **View a visual timeline of any employee's calendar** — select multiple employees and see their busy/free slots side by side (Outlook Scheduling Assistant style).
3. **Prevent scheduling conflicts** — when adding a participant to a meeting, the system checks if they are already in another meeting at that time and blocks the addition.

---

## Files Changed

### Backend — New Files

#### 1. `AvailabilityService.java`
**Location:** `backend/src/main/java/com/buzzmeet/service/AvailabilityService.java`

This service handles all availability-related database queries using JDBC.

**Methods:**

```
getEmployeeAvailability(employeeId, startUtc, endUtc)
```
- Queries the database for all SCHEDULED or DRAFT meetings that the employee is part of (either as the meeting organizer or as a participant) within the given time window.
- Returns a list of meetings with title, start time, end time, status, and whether the employee is the organizer.

```
getEmployeesAvailabilityStatus(employeeIds, atUtc)
```
- Takes a list of employee IDs and a specific point in time.
- For each employee, returns either `AVAILABLE` or `IN_A_MEETING`.
- If the employee is in a meeting, also returns the meeting title.
- Used to populate the live status badges on the Employees page.

```
findConflict(employeeId, startUtc, endUtc, excludeAssignmentId)
```
- Checks if the employee has any SCHEDULED meeting that overlaps with a given time range.
- The `excludeAssignmentId` skips the current meeting being scheduled (so we don't conflict with ourselves).
- Returns the conflicting meeting if found, or `null` if the employee is free.

**How "busy" is determined:**
An employee is considered busy if they appear as either:
- The `OrganizerId` in the `Assignments` table, OR
- An entry in the `Meeting_Participants` table
...for any meeting with `Status = 'SCHEDULED'` whose `StartUTC < queryTime < EndUTC`.

---

#### 2. `AvailabilityController.java`
**Location:** `backend/src/main/java/com/buzzmeet/controller/AvailabilityController.java`

Exposes two REST API endpoints. Both require the `meeting:view` permission (any logged-in user).

**Endpoint 1 — Get one employee's busy slots:**
```
GET /api/employees/{employeeId}/availability?startUtc=...&endUtc=...
```
- Example: `/api/employees/42/availability?startUtc=2026-06-30T09:00:00Z&endUtc=2026-06-30T18:00:00Z`
- Returns a list of meetings happening for that employee in the time window.
- Used by the Scheduling Assistant page and the AssignmentForm availability check.

**Endpoint 2 — Get current status for multiple employees:**
```
GET /api/employees/availability-status?employeeIds=1,2,3&atUtc=...
```
- Example: `/api/employees/availability-status?employeeIds=10,25,42&atUtc=2026-06-30T14:00:00Z`
- `atUtc` is optional — defaults to the current server time if not provided.
- Returns one row per employee: `{ employeeId, firstName, lastName, availabilityStatus, currentMeetingTitle }`.
- Used by the Employees page to show the live status column.

---

### Backend — Modified Files

#### 3. `AssignmentService.java`
**Location:** `backend/src/main/java/com/buzzmeet/service/AssignmentService.java`

**What was changed:** The `addParticipant()` method now checks for scheduling conflicts before adding someone to a meeting.

**Before:** Any employee could be added to any meeting regardless of their existing schedule.

**After — Step by step logic:**
1. Fetch the start and end time of the meeting being edited.
2. Run a conflict query: does this employee already have a SCHEDULED meeting that overlaps these times (other than this very meeting)?
3. If YES → throw an error: `"Employee is already scheduled in "Meeting Name" during this time"` — the API returns a 400 error.
4. If NO → proceed with adding the participant as normal.

**New private helper added:**
```
findParticipantConflict(employeeId, startUtc, endUtc, excludeAssignmentId)
```
Runs a SQL query joining `Assignments` and `Meeting_Participants` to find overlapping meetings for the employee.

---



## Summary of How Everything Connects

```
User opens Employees page
  └─> frontend calls GET /api/employees/availability-status?employeeIds=...
        └─> AvailabilityController → AvailabilityService.getEmployeesAvailabilityStatus()
              └─> SQL: checks Meeting_Participants + Assignments for each employee
                    └─> returns AVAILABLE or IN_A_MEETING per employee
                          └─> Employees.jsx shows green/blue dot badge

User opens Availability page (/employees/availability)
  └─> user picks employees and a time range
        └─> frontend calls GET /api/employees/{id}/availability?startUtc=&endUtc=
              └─> AvailabilityController → AvailabilityService.getEmployeeAvailability()
                    └─> SQL: finds overlapping SCHEDULED meetings
                          └─> EmployeeAvailability.jsx renders colored time slots

User creates a meeting (AssignmentForm) and adds a participant
  └─> frontend calls GET /api/employees/{id}/availability for selected time range
        └─> if meetings overlap → shows ⛔ warning, blocks form submit
              └─> if user bypasses frontend and calls API directly:
                    AssignmentService.addParticipant()
                      └─> calls findParticipantConflict()
                            └─> if conflict found → throws IllegalArgumentException (400 error)
```

---

## Key Rules

- An employee is **busy** if they are the **organizer** OR a **participant** in any `SCHEDULED` meeting that overlaps the query time.
- `CANCELLED`, `COMPLETED`, `OVERRIDDEN`, and `DRAFT` meetings do **not** block availability (only `SCHEDULED` blocks adding participants; `DRAFT` is shown on the calendar but does not block).
- The conflict check happens on **both** frontend (UX warning) and backend (hard enforcement).
