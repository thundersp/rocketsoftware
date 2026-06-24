USE buzzmeet;

SET NAMES utf8mb4;

DROP TABLE IF EXISTS Audit_Logs;
DROP TABLE IF EXISTS Notifications;
DROP TABLE IF EXISTS Video_Reservations;
DROP TABLE IF EXISTS Meeting_Participants;
DROP TABLE IF EXISTS Meeting_Assignments;
DROP TABLE IF EXISTS Room_Equipment;
DROP TABLE IF EXISTS Equipment;
DROP TABLE IF EXISTS Rooms;
DROP TABLE IF EXISTS Room_Types;
DROP TABLE IF EXISTS Buildings;
DROP TABLE IF EXISTS Employee_Roles;
DROP TABLE IF EXISTS User_Credentials;
DROP TABLE IF EXISTS Assignments;
DROP TABLE IF EXISTS Roles;
DROP TABLE IF EXISTS Time_Zones;

CREATE TABLE Time_Zones (
    TimeZoneId INT PRIMARY KEY,
    ZoneName VARCHAR(100) NOT NULL UNIQUE,
    GMTOffsetMinutes INT,
    IsDSTSupported CHAR(1) DEFAULT 'N',
    IsActive CHAR(1) DEFAULT 'Y'
);

CREATE TABLE Roles (
    RoleId INT PRIMARY KEY,
    RoleName VARCHAR(50) NOT NULL UNIQUE,
    Description VARCHAR(255)
);

CREATE TABLE User_Credentials (
    CredentialId INT PRIMARY KEY,
    EmployeeId INT NOT NULL UNIQUE,
    PasswordHash VARCHAR(255) NOT NULL,
    LastLogin DATETIME NULL,
    IsActive CHAR(1) DEFAULT 'Y',
    FailedLoginAttempts INT DEFAULT 0,
    LockedUntil DATETIME NULL,
    PasswordResetToken VARCHAR(255) NULL,
    PasswordResetExpiry DATETIME NULL,
    CreatedAt DATETIME NOT NULL,
    UpdatedAt DATETIME NOT NULL,
    CONSTRAINT fk_user_credentials_employee
        FOREIGN KEY (EmployeeId) REFERENCES Employee(id)
);

CREATE TABLE Employee_Roles (
    EmployeeId INT NOT NULL,
    RoleId INT NOT NULL,
    AssignedAt DATETIME NOT NULL,
    IsActive CHAR(1) DEFAULT 'Y',
    PRIMARY KEY (EmployeeId, RoleId),
    CONSTRAINT fk_employee_roles_employee
        FOREIGN KEY (EmployeeId) REFERENCES Employee(id),
    CONSTRAINT fk_employee_roles_role
        FOREIGN KEY (RoleId) REFERENCES Roles(RoleId)
);

CREATE TABLE Buildings (
    BuildingId INT PRIMARY KEY,
    LocationId INT NOT NULL,
    BuildingName VARCHAR(100) NOT NULL,
    AddressLine1 VARCHAR(255),
    AddressLine2 VARCHAR(255),
    Status VARCHAR(20) NOT NULL,
    CONSTRAINT fk_buildings_location
        FOREIGN KEY (LocationId) REFERENCES Locations(id)
);

CREATE TABLE Room_Types (
    RoomTypeId INT PRIMARY KEY,
    TypeName VARCHAR(50) NOT NULL UNIQUE,
    Description VARCHAR(255),
    IsBookable CHAR(1) DEFAULT 'Y',
    IsVideoEnabled CHAR(1) DEFAULT 'N',
    RequiresApproval CHAR(1) DEFAULT 'N'
);

CREATE TABLE Rooms (
    RoomId INT PRIMARY KEY,
    BuildingId INT NOT NULL,
    RoomTypeId INT NOT NULL,
    RoomCode VARCHAR(20) NOT NULL,
    RoomName VARCHAR(100) NOT NULL,
    Capacity INT,
    Floor INT,
    IsVideoRoom CHAR(1) DEFAULT 'N',
    DialInInfo VARCHAR(255) NULL,
    Status VARCHAR(20) NOT NULL,
    Notes VARCHAR(500),
    CONSTRAINT uq_rooms_building_code UNIQUE (BuildingId, RoomCode),
    CONSTRAINT fk_rooms_building
        FOREIGN KEY (BuildingId) REFERENCES Buildings(BuildingId),
    CONSTRAINT fk_rooms_room_type
        FOREIGN KEY (RoomTypeId) REFERENCES Room_Types(RoomTypeId)
);

CREATE TABLE Equipment (
    EquipmentId INT PRIMARY KEY,
    EquipmentName VARCHAR(100) NOT NULL UNIQUE,
    Category VARCHAR(100),
    Description VARCHAR(255),
    Status VARCHAR(20) NOT NULL
);

CREATE TABLE Room_Equipment (
    RoomId INT NOT NULL,
    EquipmentId INT NOT NULL,
    Quantity INT,
    IsActive CHAR(1) DEFAULT 'Y',
    Notes VARCHAR(500),
    PRIMARY KEY (RoomId, EquipmentId),
    CONSTRAINT fk_room_equipment_room
        FOREIGN KEY (RoomId) REFERENCES Rooms(RoomId),
    CONSTRAINT fk_room_equipment_equipment
        FOREIGN KEY (EquipmentId) REFERENCES Equipment(EquipmentId)
);

CREATE TABLE Assignments (
    AssignmentId INT PRIMARY KEY,
    OrganizerId INT NOT NULL,
    MeetingTitle VARCHAR(150) NOT NULL,
    Description TEXT,
    StartUTC DATETIME NOT NULL,
    EndUTC DATETIME NOT NULL,
    SecondaryTimeZoneId INT NULL,
    Status VARCHAR(20) NOT NULL,
    Priority VARCHAR(20) NOT NULL,
    CreatedAt DATETIME NOT NULL,
    UpdatedAt DATETIME NOT NULL,
    CancelledBy INT NULL,
    OverriddenBy INT NULL,
    PreviousAssignmentId INT NULL,
    IsRecurring CHAR(1) DEFAULT 'N',
    RecurrencePattern VARCHAR(20) NULL,
    CONSTRAINT fk_assignments_organizer
        FOREIGN KEY (OrganizerId) REFERENCES Employee(id),
    CONSTRAINT fk_assignments_secondary_timezone
        FOREIGN KEY (SecondaryTimeZoneId) REFERENCES Time_Zones(TimeZoneId),
    CONSTRAINT fk_assignments_cancelled_by
        FOREIGN KEY (CancelledBy) REFERENCES Employee(id),
    CONSTRAINT fk_assignments_overridden_by
        FOREIGN KEY (OverriddenBy) REFERENCES Employee(id),
    CONSTRAINT fk_assignments_previous
        FOREIGN KEY (PreviousAssignmentId) REFERENCES Assignments(AssignmentId)
);

CREATE TABLE Meeting_Assignments (
    MeetingAssignmentId INT PRIMARY KEY,
    AssignmentId INT NOT NULL,
    RoomId INT NOT NULL,
    IsPrimaryRoom CHAR(1) DEFAULT 'N',
    StartUTC DATETIME NOT NULL,
    EndUTC DATETIME NOT NULL,
    Status VARCHAR(20) NOT NULL,
    CONSTRAINT fk_meeting_assignments_assignment
        FOREIGN KEY (AssignmentId) REFERENCES Assignments(AssignmentId),
    CONSTRAINT fk_meeting_assignments_room
        FOREIGN KEY (RoomId) REFERENCES Rooms(RoomId)
);

CREATE TABLE Meeting_Participants (
    ParticipantId INT PRIMARY KEY,
    AssignmentId INT NOT NULL,
    EmployeeId INT NOT NULL,
    Status VARCHAR(20) NOT NULL,
    ResponseStatus VARCHAR(20) NOT NULL,
    Responsibility VARCHAR(255),
    InviteSentAt DATETIME NOT NULL,
    ResponseAt DATETIME NULL,
    CONSTRAINT uq_meeting_participants UNIQUE (AssignmentId, EmployeeId),
    CONSTRAINT fk_meeting_participants_assignment
        FOREIGN KEY (AssignmentId) REFERENCES Assignments(AssignmentId),
    CONSTRAINT fk_meeting_participants_employee
        FOREIGN KEY (EmployeeId) REFERENCES Employee(id)
);

CREATE TABLE Video_Reservations (
    VideoReservationId INT PRIMARY KEY,
    MeetingAssignmentId INT NOT NULL,
    LocationId INT NOT NULL,
    TimeZoneId INT NOT NULL,
    VideoTitle VARCHAR(150),
    IsPrimaryLocation CHAR(1) DEFAULT 'N',
    IsVideoEnabled CHAR(1) DEFAULT 'Y',
    ConnectionLink VARCHAR(255),
    DialInInfo VARCHAR(255),
    Status VARCHAR(20) NOT NULL,
    CreatedAt DATETIME NOT NULL,
    CONSTRAINT fk_video_reservations_meeting_assignment
        FOREIGN KEY (MeetingAssignmentId) REFERENCES Meeting_Assignments(MeetingAssignmentId),
    CONSTRAINT fk_video_reservations_location
        FOREIGN KEY (LocationId) REFERENCES Locations(id),
    CONSTRAINT fk_video_reservations_timezone
        FOREIGN KEY (TimeZoneId) REFERENCES Time_Zones(TimeZoneId)
);

CREATE TABLE Notifications (
    NotificationId INT PRIMARY KEY,
    AssignmentId INT NULL,
    EmployeeId INT NOT NULL,
    NotificationType VARCHAR(20) NOT NULL,
    Channel VARCHAR(20) NOT NULL,
    Message VARCHAR(500),
    Status VARCHAR(20) NOT NULL,
    SentAt DATETIME NOT NULL,
    CONSTRAINT fk_notifications_assignment
        FOREIGN KEY (AssignmentId) REFERENCES Assignments(AssignmentId),
    CONSTRAINT fk_notifications_employee
        FOREIGN KEY (EmployeeId) REFERENCES Employee(id)
);

CREATE TABLE Audit_Logs (
    LogId INT PRIMARY KEY,
    EmployeeId INT NULL,
    Action VARCHAR(50) NOT NULL,
    EntityType VARCHAR(50) NOT NULL,
    EntityId INT NOT NULL,
    OldValues JSON NULL,
    NewValues JSON NULL,
    IPAddress VARCHAR(50),
    CreatedAt DATETIME NOT NULL,
    CONSTRAINT fk_audit_logs_employee
        FOREIGN KEY (EmployeeId) REFERENCES Employee(id)
);

INSERT INTO Time_Zones (TimeZoneId, ZoneName, GMTOffsetMinutes, IsDSTSupported, IsActive) VALUES
    (1, 'Asia/Tokyo', 540, 'N', 'Y'),
    (2, 'America/Sao_Paulo', -180, 'Y', 'Y'),
    (3, 'America/Chicago', -360, 'Y', 'Y'),
    (4, 'Africa/Johannesburg', 120, 'N', 'Y'),
    (5, 'Europe/Berlin', 60, 'Y', 'Y');

INSERT INTO Roles (RoleId, RoleName, Description) VALUES
    (1, 'ADMIN', 'Administrative access for scheduler configuration'),
    (2, 'ORGANIZER', 'Can create and manage meetings'),
    (3, 'APPROVER', 'Approves restricted rooms and meeting exceptions'),
    (4, 'EMPLOYEE', 'Standard employee access to view and respond to meetings');

INSERT INTO User_Credentials (
    CredentialId,
    EmployeeId,
    PasswordHash,
    LastLogin,
    IsActive,
    FailedLoginAttempts,
    LockedUntil,
    PasswordResetToken,
    PasswordResetExpiry,
    CreatedAt,
    UpdatedAt
) VALUES
    (1, 1, '{noop}Password123!', '2026-06-20 08:45:00', 'Y', 0, NULL, NULL, NULL, '2026-01-10 09:00:00', '2026-06-20 08:45:00'),
    (2, 14, '{noop}Password123!', '2026-06-21 10:00:00', 'Y', 1, NULL, NULL, NULL, '2026-01-12 09:15:00', '2026-06-21 10:00:00'),
    (3, 31, '{noop}Password123!', '2026-06-23 07:30:00', 'Y', 0, NULL, NULL, NULL, '2026-01-15 08:00:00', '2026-06-23 07:30:00'),
    (4, 46, '{noop}Password123!', '2026-06-18 11:25:00', 'Y', 0, NULL, NULL, NULL, '2026-02-01 10:30:00', '2026-06-18 11:25:00'),
    (5, 58, '{noop}Password123!', '2026-06-22 16:50:00', 'Y', 0, NULL, NULL, NULL, '2026-02-10 11:00:00', '2026-06-22 16:50:00'),
    (6, 160, '{noop}Password123!', '2026-06-24 06:55:00', 'Y', 0, NULL, NULL, NULL, '2026-03-02 12:00:00', '2026-06-24 06:55:00');

INSERT INTO Employee_Roles (EmployeeId, RoleId, AssignedAt, IsActive) VALUES
    (1, 4, '2026-01-10 09:05:00', 'Y'),
    (14, 2, '2026-01-12 09:20:00', 'Y'),
    (31, 1, '2026-01-15 08:05:00', 'Y'),
    (31, 2, '2026-01-15 08:05:00', 'Y'),
    (46, 4, '2026-02-01 10:35:00', 'Y'),
    (58, 3, '2026-02-10 11:05:00', 'Y'),
    (160, 1, '2026-03-02 12:05:00', 'Y'),
    (160, 3, '2026-03-02 12:05:00', 'Y');

INSERT INTO Buildings (BuildingId, LocationId, BuildingName, AddressLine1, AddressLine2, Status) VALUES
    (1, 1, 'Tokyo Innovation Center', '384-1106 Yahara', 'Nerima-ku', 'ACTIVE'),
    (2, 2, 'Sao Paulo Hub', 'Rua Atalaia 752', 'Pinheiros', 'ACTIVE'),
    (3, 3, 'Dallas Collaboration Tower', '3607 Fawn Valley Dr', 'Suite 400', 'ACTIVE'),
    (4, 4, 'Johannesburg Campus', '14 Jan Frederik Avenue', 'Block B', 'ACTIVE'),
    (5, 5, 'Berlin Exchange House', '63 Romerweg', 'Floor 6', 'ACTIVE');

INSERT INTO Room_Types (RoomTypeId, TypeName, Description, IsBookable, IsVideoEnabled, RequiresApproval) VALUES
    (1, 'CONFERENCE', 'Standard conference room for team meetings', 'Y', 'Y', 'N'),
    (2, 'BOARDROOM', 'Executive room for strategic discussions', 'Y', 'Y', 'Y'),
    (3, 'TRAINING', 'Training space for workshops and enablement', 'Y', 'N', 'N'),
    (4, 'FOCUS', 'Small room for one-on-one and interview sessions', 'Y', 'N', 'N');

INSERT INTO Rooms (
    RoomId,
    BuildingId,
    RoomTypeId,
    RoomCode,
    RoomName,
    Capacity,
    Floor,
    IsVideoRoom,
    DialInInfo,
    Status,
    Notes
) VALUES
    (101, 1, 1, 'TKY-101', 'Fuji Conference', 12, 10, 'Y', 'Tokyo bridge ext. 9101', 'ACTIVE', 'Primary collaboration room for APAC meetings'),
    (102, 2, 3, 'SAO-201', 'Ibirapuera Training', 20, 2, 'N', NULL, 'ACTIVE', 'Used for onboarding and larger workshops'),
    (103, 3, 2, 'DAL-301', 'Trinity Boardroom', 14, 3, 'Y', 'Dallas bridge ext. 9301', 'ACTIVE', 'Approval required for executive meetings'),
    (104, 3, 4, 'DAL-115', 'Lone Star Focus', 4, 1, 'N', NULL, 'ACTIVE', 'Quiet room for interview loops'),
    (105, 4, 1, 'JHB-401', 'Table Mountain', 10, 4, 'Y', 'Johannesburg bridge ext. 9401', 'ACTIVE', 'Video-enabled team room'),
    (106, 5, 1, 'BER-501', 'Brandenburg', 16, 5, 'Y', 'Berlin bridge ext. 9501', 'ACTIVE', 'Used for cross-region town halls');

INSERT INTO Equipment (EquipmentId, EquipmentName, Category, Description, Status) VALUES
    (1, 'Poly Studio Bar', 'Video', 'All-in-one video conferencing bar', 'ACTIVE'),
    (2, 'Ceiling Microphone Array', 'Audio', 'Beamforming microphone array', 'ACTIVE'),
    (3, '4K Display', 'Display', 'Large-format meeting room monitor', 'ACTIVE'),
    (4, 'Wireless Presentation Hub', 'Presentation', 'Cable-free presentation device', 'ACTIVE'),
    (5, 'Portable Whiteboard', 'Collaboration', 'Mobile whiteboard for workshops', 'ACTIVE');

INSERT INTO Room_Equipment (RoomId, EquipmentId, Quantity, IsActive, Notes) VALUES
    (101, 1, 1, 'Y', 'Mounted under the center display'),
    (101, 3, 2, 'Y', 'Dual-screen setup'),
    (103, 2, 1, 'Y', 'Optimized for boardroom acoustics'),
    (103, 4, 1, 'Y', 'Supports guest screen sharing'),
    (105, 1, 1, 'Y', 'Installed during Q1 refresh'),
    (106, 5, 2, 'Y', 'Stored in adjacent cabinet');

INSERT INTO Assignments (
    AssignmentId,
    OrganizerId,
    MeetingTitle,
    Description,
    StartUTC,
    EndUTC,
    SecondaryTimeZoneId,
    Status,
    Priority,
    CreatedAt,
    UpdatedAt,
    CancelledBy,
    OverriddenBy,
    PreviousAssignmentId,
    IsRecurring,
    RecurrencePattern
) VALUES
    (1001, 31, 'Quarterly Sales Kickoff', 'Cross-region kickoff for quarterly sales targets and launch plans.', '2026-07-01 13:00:00', '2026-07-01 14:30:00', 5, 'SCHEDULED', 'HIGH', '2026-06-18 09:00:00', '2026-06-18 09:30:00', NULL, NULL, NULL, 'N', NULL),
    (1002, 14, 'Customer Demo Review', 'Review of customer demo flow before the enterprise prospect meeting.', '2026-07-02 15:00:00', '2026-07-02 16:00:00', 2, 'SCHEDULED', 'NORMAL', '2026-06-19 11:00:00', '2026-06-19 11:20:00', NULL, NULL, NULL, 'N', NULL),
    (1003, 160, 'Global Engineering Sync', 'Monthly architecture and delivery checkpoint across all regions.', '2026-07-03 08:00:00', '2026-07-03 09:00:00', 1, 'SCHEDULED', 'URGENT', '2026-06-20 08:00:00', '2026-06-21 10:10:00', NULL, NULL, NULL, 'Y', 'MONTHLY'),
    (1004, 160, 'Global Engineering Sync - Follow-up', 'Follow-up session for actions that need deeper review.', '2026-07-10 08:00:00', '2026-07-10 08:45:00', 1, 'DRAFT', 'NORMAL', '2026-06-22 14:00:00', '2026-06-22 14:00:00', NULL, NULL, 1003, 'Y', 'WEEKLY');

INSERT INTO Meeting_Assignments (MeetingAssignmentId, AssignmentId, RoomId, IsPrimaryRoom, StartUTC, EndUTC, Status) VALUES
    (2001, 1001, 103, 'Y', '2026-07-01 13:00:00', '2026-07-01 14:30:00', 'RESERVED'),
    (2002, 1001, 106, 'N', '2026-07-01 13:00:00', '2026-07-01 14:30:00', 'RESERVED'),
    (2003, 1002, 104, 'Y', '2026-07-02 15:00:00', '2026-07-02 16:00:00', 'RESERVED'),
    (2004, 1003, 101, 'Y', '2026-07-03 08:00:00', '2026-07-03 09:00:00', 'RESERVED'),
    (2005, 1003, 105, 'N', '2026-07-03 08:00:00', '2026-07-03 09:00:00', 'RESERVED');

INSERT INTO Meeting_Participants (
    ParticipantId,
    AssignmentId,
    EmployeeId,
    Status,
    ResponseStatus,
    Responsibility,
    InviteSentAt,
    ResponseAt
) VALUES
    (3001, 1001, 31, 'ORGANIZER', 'ACCEPTED', 'Drive kickoff agenda and action review', '2026-06-18 09:35:00', '2026-06-18 09:35:00'),
    (3002, 1001, 14, 'ATTENDEE', 'ACCEPTED', 'Present enterprise pipeline update', '2026-06-18 09:36:00', '2026-06-18 11:00:00'),
    (3003, 1001, 46, 'ATTENDEE', 'TENTATIVE', 'Cover EMEA customer expansion opportunities', '2026-06-18 09:36:00', '2026-06-19 08:15:00'),
    (3004, 1001, 58, 'APPROVER', 'ACCEPTED', 'Approve boardroom reservation', '2026-06-18 09:36:00', '2026-06-18 10:10:00'),
    (3005, 1002, 14, 'ORGANIZER', 'ACCEPTED', 'Run demo dry-run and feedback review', '2026-06-19 11:25:00', '2026-06-19 11:25:00'),
    (3006, 1002, 1, 'ATTENDEE', 'ACCEPTED', 'Represent LATAM sales requirements', '2026-06-19 11:25:00', '2026-06-20 08:50:00'),
    (3007, 1003, 160, 'ORGANIZER', 'ACCEPTED', 'Lead engineering checkpoint', '2026-06-21 10:15:00', '2026-06-21 10:15:00'),
    (3008, 1003, 31, 'ATTENDEE', 'ACCEPTED', 'Share platform support updates', '2026-06-21 10:15:00', '2026-06-21 12:00:00'),
    (3009, 1003, 58, 'ATTENDEE', 'PENDING', 'Review change control items', '2026-06-21 10:15:00', NULL),
    (3010, 1004, 160, 'ORGANIZER', 'ACCEPTED', 'Track follow-up actions', '2026-06-22 14:05:00', '2026-06-22 14:05:00');

INSERT INTO Video_Reservations (
    VideoReservationId,
    MeetingAssignmentId,
    LocationId,
    TimeZoneId,
    VideoTitle,
    IsPrimaryLocation,
    IsVideoEnabled,
    ConnectionLink,
    DialInInfo,
    Status,
    CreatedAt
) VALUES
    (4001, 2001, 3, 3, 'Q3 Sales Kickoff Bridge', 'Y', 'Y', 'https://meet.buzzmeet.example/sales-kickoff', '+1-214-555-0101,,991001#', 'CONFIRMED', '2026-06-18 09:40:00'),
    (4002, 2002, 5, 5, 'Q3 Sales Kickoff EMEA Room', 'N', 'Y', 'https://meet.buzzmeet.example/sales-kickoff-emea', '+49-30-555-0106,,991002#', 'CONFIRMED', '2026-06-18 09:42:00'),
    (4003, 2004, 1, 1, 'Engineering Sync APAC', 'Y', 'Y', 'https://meet.buzzmeet.example/eng-sync', '+81-3-555-0101,,991003#', 'CONFIRMED', '2026-06-21 10:20:00'),
    (4004, 2005, 4, 4, 'Engineering Sync Africa Room', 'N', 'Y', 'https://meet.buzzmeet.example/eng-sync-africa', '+27-11-555-0105,,991004#', 'CONFIRMED', '2026-06-21 10:21:00');

INSERT INTO Notifications (NotificationId, AssignmentId, EmployeeId, NotificationType, Channel, Message, Status, SentAt) VALUES
    (5001, 1001, 14, 'CREATED', 'EMAIL', 'You have been invited to Quarterly Sales Kickoff.', 'SENT', '2026-06-18 09:37:00'),
    (5002, 1001, 46, 'CREATED', 'APP', 'EMEA dial-in details are ready for Quarterly Sales Kickoff.', 'SENT', '2026-06-18 09:43:00'),
    (5003, 1002, 1, 'UPDATED', 'CALENDAR', 'Customer Demo Review agenda was updated with demo timing.', 'SENT', '2026-06-20 08:55:00'),
    (5004, 1003, 58, 'REMINDER', 'EMAIL', 'Please respond to Global Engineering Sync.', 'PENDING', '2026-06-24 07:00:00'),
    (5005, 1004, 160, 'CREATED', 'APP', 'Draft follow-up assignment created for Global Engineering Sync.', 'SENT', '2026-06-22 14:06:00');

INSERT INTO Audit_Logs (LogId, EmployeeId, Action, EntityType, EntityId, OldValues, NewValues, IPAddress, CreatedAt) VALUES
    (6001, 31, 'CREATE', 'Assignment', 1001, NULL, JSON_OBJECT('Status', 'SCHEDULED', 'Priority', 'HIGH'), '10.10.3.21', '2026-06-18 09:30:00'),
    (6002, 58, 'APPROVE', 'Meeting_Assignment', 2001, JSON_OBJECT('Status', 'RESERVED'), JSON_OBJECT('Status', 'RESERVED', 'Approved', TRUE), '10.10.3.58', '2026-06-18 10:10:00'),
    (6003, 14, 'UPDATE', 'Assignment', 1002, JSON_OBJECT('Description', 'Review of customer demo flow.'), JSON_OBJECT('Description', 'Review of customer demo flow before the enterprise prospect meeting.'), '10.10.2.14', '2026-06-19 11:20:00'),
    (6004, 160, 'CREATE', 'Assignment', 1004, NULL, JSON_OBJECT('PreviousAssignmentId', 1003, 'Status', 'DRAFT'), '10.10.5.160', '2026-06-22 14:00:00');