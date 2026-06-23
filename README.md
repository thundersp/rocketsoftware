# Meeting Scheduler Project

A containerized meeting scheduler application with backend, frontend, and database services.

## Architecture Overview

Everything runs in its own Docker container for isolation and scalability.

- **Backend**: Java Spring Boot application
- **Frontend**: React/Vite application
- **Database**: SQL database service

## Database Schema

### User

| Column   | Type    | Constraints      |
| -------- | ------- | ---------------- |
| userId   | INT     | PRIMARY KEY      |
| username | VARCHAR | NOT NULL, UNIQUE |
| email    | VARCHAR | NOT NULL, UNIQUE |

### Role

| Column   | Type    | Constraints      |
| -------- | ------- | ---------------- |
| roleId   | INT     | PRIMARY KEY      |
| roleName | VARCHAR | NOT NULL, UNIQUE |

### User Role (Junction Table)

| Column | Type | Constraints             |
| ------ | ---- | ----------------------- |
| userId | INT  | PRIMARY KEY (Composite) |
| roleId | INT  | PRIMARY KEY (Composite) |

### Meeting

| Column      | Type     | Constraints           |
| ----------- | -------- | --------------------- |
| meetingId   | INT      | PRIMARY KEY           |
| title       | VARCHAR  | NOT NULL              |
| description | VARCHAR  |                       |
| startTime   | DATETIME | NOT NULL              |
| endTime     | DATETIME | NOT NULL              |
| organizer   | INT      | NOT NULL, FOREIGN KEY |

### Meeting Participants

| Column        | Type    | Constraints           |
| ------------- | ------- | --------------------- |
| participantId | INT     | PRIMARY KEY           |
| meetingId     | INT     | NOT NULL, FOREIGN KEY |
| userId        | INT     | NOT NULL, FOREIGN KEY |
| status        | VARCHAR | NOT NULL              |

### Assignment

| Column       | Type     | Constraints           |
| ------------ | -------- | --------------------- |
| assignmentId | INT      | PRIMARY KEY           |
| organizer    | INT      | NOT NULL, FOREIGN KEY |
| description  | VARCHAR  |                       |
| dueDate      | DATETIME |                       |

## Getting Started

### Prerequisites

- Docker & Docker Compose
- Node.js 16+ (for local frontend development)
- Java 11+ (for local backend development)

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
