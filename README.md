# BuzzMeet - Meeting Scheduler System

A containerized meeting scheduler application built around BuzzwordSolutions' employee directory. Enables employees to schedule and manage meetings, reserve rooms, conduct video conferences, and track all meeting-related activities with complete audit trails.

## Architecture Overview

Everything runs in its own Docker container for isolation and scalability.

- **Backend**: Java Spring Boot application
- **Frontend**: React/Vite application
- **Database**: SQL database service

## Database Schema

### Core Employee Management

#### Locations

Represents physical office locations. There are 5 offices globally.

| Column  | Type         | Constraints |
| ------- | ------------ | ----------- |
| id      | INT          | PRIMARY KEY |
| phone   | VARCHAR(50)  |             |
| street  | VARCHAR(250) |             |
| country | VARCHAR(50)  |             |
| city    | VARCHAR(50)  |             |

**Seeded offices:**

| ID | City           | Country       |
| -- | -------------- | ------------- |
| 1  | Tokyo          | Japan         |
| 2  | São Paulo      | Brazil        |
| 3  | Dallas         | United States |
| 4  | Johannesburg   | South Africa  |
| 5  | Berlin         | Germany       |

#### Employee

Represents an employee at BuzzwordSolutions. Each employee is assigned to one office location and serves as the primary user entity for the meeting scheduler.

| Column     | Type        | Constraints              |
| ---------- | ----------- | ------------------------ |
| id         | INT         | PRIMARY KEY              |
| first_name | VARCHAR(50) |                          |
| last_name  | VARCHAR(50) |                          |
| title      | VARCHAR(11) |                          |
| email      | VARCHAR(50) |                          |
| country    | VARCHAR(50) |                          |
| city       | VARCHAR(50) |                          |
| location   | INT         | FOREIGN KEY → Locations(id) |

**Employee titles:** `Developer`, `Sales Agent`, `Manager`

**Total employees:** 72 across all offices.

---

### Meeting Scheduler Tables

#### Roles

Defines user roles for access control within the meeting scheduler.

| Column       | Type        | Constraints           |
| ------------ | ----------- | --------------------- |
| RoleId       | INT         | PRIMARY KEY           |
| RoleName     | VARCHAR(UQ) | NOT NULL, UNIQUE      |
| Description  | VARCHAR     |                       |

#### Assignments (Meeting Master)

Core table for scheduling meetings/assignments organized by meeting organizers.

| Column               | Type        | Constraints                              |
| -------------------- | ----------- | ---------------------------------------- |
| AssignmentId         | INT         | PRIMARY KEY                              |
| OrganizerId          | INT         | NOT NULL, FOREIGN KEY → Employee(id)     |
| MeetingTitle         | VARCHAR     | NOT NULL                                 |
| Description          | VARCHAR     |                                          |
| StartUTC             | DATETIME    | NOT NULL                                 |
| EndUTC               | DATETIME    | NOT NULL                                 |
| SecondaryTimeZoneId  | INT         | FOREIGN KEY → TIME_ZONES(TimeZoneId)     |
| Status               | VARCHAR     | DRAFT/SCHEDULED/CANCELLED/COMPLETED      |
| Priority             | VARCHAR     | LOW/NORMAL/HIGH/URGENT                   |
| CreatedAt            | DATETIME    |                                          |
| UpdatedAt            | DATETIME    |                                          |
| CancelledBy          | INT         | FOREIGN KEY → Employee(id), NULL         |
| OverriddenBy         | INT         | FOREIGN KEY → Employee(id), NULL         |
| PreviousAssignmentId | INT         | FOREIGN KEY → Assignments(id), NULL      |

#### Meeting_Participants

Junction table linking employees to meetings/assignments.

| Column                 | Type     | Constraints                          |
| ---------------------- | -------- | ------------------------------------ |
| ParticipantId          | INT      | PRIMARY KEY                          |
| AssignmentId           | INT      | NOT NULL, FOREIGN KEY → Assignments  |
| EmployeeId             | INT      | NOT NULL, FOREIGN KEY → Employee     |
| Status                 | VARCHAR  | ORGANIZER/ATTENDEE/APPROVER          |
| ResponseStatus         | VARCHAR  | PENDING/ACCEPTED/DECLINED            |
| Responsibility         | VARCHAR  |                                      |
| InviteSentAt           | DATETIME |                                      |
| ResponseAt             | DATETIME |                                      |

#### Meeting_Assignments

Links specific meeting rooms to assignments (one assignment can use multiple rooms).

| Column             | Type     | Constraints                          |
| ------------------ | -------- | ------------------------------------ |
| MeetingAssignmentId | INT      | PRIMARY KEY                          |
| AssignmentId       | INT      | NOT NULL, FOREIGN KEY → Assignments  |
| RoomId             | INT      | NOT NULL, FOREIGN KEY → Rooms        |
| IsPrimaryRoom      | Y/N      |                                      |
| StartUTC           | DATETIME |                                      |
| EndUTC             | DATETIME |                                      |
| Status             | VARCHAR  | RESERVED/CANCELLED/RELEASED          |

#### Notifications

Tracks all notifications sent to employees regarding meetings and assignments.

| Column               | Type     | Constraints                          |
| -------------------- | -------- | ------------------------------------ |
| NotificationId       | INT      | PRIMARY KEY                          |
| AssignmentId         | INT      | FOREIGN KEY → Assignments, NULL      |
| EmployeeId           | INT      | FOREIGN KEY → Employee               |
| NotificationType     | VARCHAR  | MEETING/UPDATED/CANCELLED/REMINDER   |
| Message              | VARCHAR  |                                      |
| Status               | VARCHAR  | PENDING/SENT/FAILED                  |
| SentAt               | DATETIME |                                      |

#### Video_Reservations

Manages video conference reservations for meetings.

| Column             | Type     | Constraints                          |
| ------------------ | -------- | ------------------------------------ |
| VideoReservationId | INT      | PRIMARY KEY                          |
| MeetingAssignmentId | INT      | NOT NULL, FOREIGN KEY → Meeting_Assignments |
| AssignmentId       | INT      | FOREIGN KEY → Assignments            |
| LocationId         | INT      | FOREIGN KEY → Locations              |
| TimeZoneId         | INT      | FOREIGN KEY → TIME_ZONES             |
| IsVideoEnabled     | Y/N      |                                      |
| ConnectionLink     | VARCHAR  |                                      |
| Status             | VARCHAR  | CONFIRMED/CANCELLED                  |
| CreatedAt          | DATETIME |                                      |

#### Time_Zones

Global time zone lookup table.

| Column            | Type     | Constraints |
| ----------------- | -------- | ----------- |
| TimeZoneId        | INT      | PRIMARY KEY |
| TimeZoneName      | VARCHAR  |             |
| OffsetUTCMinutes  | INT      |             |
| ZoneNameUQ        | VARCHAR  | UNIQUE      |
| IsActive          | Y/N      |             |

#### Buildings

Office buildings within locations.

| Column           | Type     | Constraints                   |
| ---------------- | -------- | ----------------------------- |
| BuildingId       | INT      | PRIMARY KEY                   |
| LocationId       | INT      | FOREIGN KEY → Locations       |
| BuildingName     | VARCHAR  |                               |
| Address          | VARCHAR  |                               |
| Status           | VARCHAR  | ACTIVE/INACTIVE               |

#### Rooms

Meeting rooms within buildings.

| Column           | Type     | Constraints                   |
| ---------------- | -------- | ----------------------------- |
| RoomId           | INT      | PRIMARY KEY                   |
| BuildingId       | INT      | FOREIGN KEY → Buildings       |
| RoomTypeId       | INT      | FOREIGN KEY → Room_Types      |
| RoomCode         | VARCHAR  | UQ within building            |
| RoomName         | VARCHAR  |                               |
| Capacity         | INT      |                               |
| Floor            | INT      |                               |
| Status           | VARCHAR  | ACTIVE/INACTIVE/MAINTENANCE   |
| Notes            | VARCHAR  |                               |

#### Room_Types

Classification of room types (e.g., conference, boardroom, training).

| Column              | Type     | Constraints |
| ------------------- | -------- | ----------- |
| RoomTypeId          | INT      | PRIMARY KEY |
| TypeName            | VARCHAR  | UQ          |
| BookableY/N         | Y/N      |             |
| RequireApprovalY/N  | Y/N      |             |

#### Room_Equipment

Maps equipment to rooms.

| Column      | Type | Constraints                   |
| ----------- | ---- | ----------------------------- |
| RoomId      | INT  | PRIMARY KEY, FOREIGN KEY → Rooms |
| EquipmentId | INT  | PRIMARY KEY, FOREIGN KEY → Equipment |
| Quantity    | INT  |                               |
| IsActive    | Y/N  |                               |
| Notes       | VARCHAR |                            |

#### Equipment

Available equipment that can be reserved with rooms.

| Column              | Type     | Constraints |
| ------------------- | -------- | ----------- |
| EquipmentId         | INT      | PRIMARY KEY |
| EquipmentName       | VARCHAR  | UQ          |
| Category            | VARCHAR  |             |
| Description         | VARCHAR  |             |
| Status              | VARCHAR  | ACTIVE/INACTIVE |

#### Audit_Logs

Complete audit trail of all important actions.

| Column      | Type     | Constraints                   |
| ----------- | -------- | ----------------------------- |
| LogId       | INT      | PRIMARY KEY                   |
| EmployeeId  | INT      | FOREIGN KEY → Employee, NULL allowed |
| Action      | VARCHAR  |                               |
| EntityType  | VARCHAR  |                               |
| EntityId    | INT      |                               |
| OldValues   | JSON     |                               |
| NewValues   | JSON     |                               |
| IPAddress   | VARCHAR  |                               |
| CreatedAt   | DATETIME |                               |

---

## Key Features

✅ One Meeting (ASSIGNMENT) can have multiple rooms (local or remote) be reserved  
✅ One primary room is marked  
✅ Participants (employees) are invited to the meeting  
✅ Supports double booking through room reservations  
✅ Supports global meetings across time zones  
✅ Audit logs track all important actions


### Running with Docker Compose

```bash
docker-compose up
```

This will start:

- Backend on port 8080
- Frontend on port 5173
- Database on port 3306

## Project Structure

```
rocketsoftware/
├── backend/          # Java Spring Boot application
├── frontend/         # React + Vite application
├── database/         # Database initialization scripts
├── docker-compose.yml
└── README.md
```