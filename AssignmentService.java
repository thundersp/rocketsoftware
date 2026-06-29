package com.buzzmeet.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buzzmeet.exception.ResourceNotFoundException;
import com.buzzmeet.security.CurrentUserService;
import com.buzzmeet.security.Permissions;

@Service
public class AssignmentService {

    private static final DateTimeFormatter DB_UTC_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Set<String> ASSIGNMENT_STATUSES = Set.of("DRAFT", "SCHEDULED", "CANCELLED", "COMPLETED",
        "OVERRIDDEN");
    private static final Set<String> PRIORITY_STATUSES = Set.of("LOW", "NORMAL", "HIGH", "URGENT");
    private static final Set<String> MEETING_ASSIGNMENT_STATUSES = Set.of("RESERVED", "CANCELLED", "RELEASED");
    private static final Set<String> VIDEO_STATUSES = Set.of("CONFIRMED", "CANCELLED", "ACTIVE");
    private static final Set<String> PARTICIPANT_STATUSES = Set.of("ORGANIZER", "ATTENDEE", "APPROVER");
    private static final Set<String> PARTICIPANT_RESPONSE_STATUSES = Set.of("PENDING", "ACCEPTED", "DECLINED",
        "TENTATIVE");

    private final NamedParameterJdbcTemplate jdbc;
    private final CurrentUserService currentUserService;
    private final AuditLogWriter auditLogWriter;

    public AssignmentService(NamedParameterJdbcTemplate jdbc, CurrentUserService currentUserService,
            AuditLogWriter auditLogWriter) {
        this.jdbc = jdbc;
        this.currentUserService = currentUserService;
        this.auditLogWriter = auditLogWriter;
    }

    public void createAssignment(Map<String, Object> request) {
        Integer currentEmployeeId = currentUserService.currentEmployeeId();
        Integer organizerId = getInteger(request, "organizerId");
        if (organizerId == null) {
            organizerId = currentEmployeeId;
        }
        if (!organizerId.equals(currentEmployeeId) && !currentUserService.hasAuthority(Permissions.MEETING_OVERRIDE)) {
            throw new AuthorizationDeniedException("Only managers/admins can create meetings for another organizer");
        }

        Integer assignmentId = nextId("Assignments", "AssignmentId");
        String sql = "INSERT INTO Assignments (AssignmentId, OrganizerId, MeetingTitle, Description, StartUTC, EndUTC, SecondaryTimeZoneId, Status, Priority, CreatedAt, UpdatedAt, CancelledBy, OverriddenBy, PreviousAssignmentId, IsRecurring, RecurrencePattern) "
                + "VALUES (:assignmentId, :organizerId, :meetingTitle, :description, :startUtc, :endUtc, :secondaryTimeZoneId, :status, :priority, NOW(), NOW(), :cancelledBy, :overriddenBy, :previousAssignmentId, :isRecurring, :recurrencePattern)";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("assignmentId", assignmentId);
        params.addValue("organizerId", organizerId);
        params.addValue("meetingTitle", getString(request, "meetingTitle"));
        params.addValue("description", getString(request, "description"));
        params.addValue("startUtc", normalizeUtc(getString(request, "startUtc"), "startUtc"));
        params.addValue("endUtc", normalizeUtc(getString(request, "endUtc"), "endUtc"));
        params.addValue("secondaryTimeZoneId", getInteger(request, "secondaryTimeZoneId"));
        params.addValue("status", normalizeStatus(getString(request, "status"), "SCHEDULED", ASSIGNMENT_STATUSES,
            "status"));
        params.addValue("priority", normalizeStatus(getString(request, "priority"), "NORMAL", PRIORITY_STATUSES,
            "priority"));
        params.addValue("cancelledBy", getInteger(request, "cancelledBy"));
        params.addValue("overriddenBy", getInteger(request, "overriddenBy"));
        params.addValue("previousAssignmentId", getInteger(request, "previousAssignmentId"));
        params.addValue("isRecurring", toYesNo(request.get("isRecurring")));
        params.addValue("recurrencePattern", getString(request, "recurrencePattern"));
        jdbc.update(sql, params);
    }

    public List<Map<String, Object>> getAssignments(Integer organizerId, Integer participantEmployeeId,
            String status, Integer locationId, Integer roomId, String fromUtc, String toUtc,
            String priority) {
        String sql = "SELECT a.* FROM Assignments a";
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (participantEmployeeId != null || locationId != null || roomId != null) {
            sql += " LEFT JOIN Meeting_Assignments ma ON ma.AssignmentId = a.AssignmentId";
        }
        sql += " WHERE 1=1";
        if (organizerId != null) {
            sql += " AND a.OrganizerId = :organizerId";
            params.addValue("organizerId", organizerId);
        }
        if (status != null) {
            sql += " AND a.Status = :status";
            params.addValue("status", status);
        }
        if (priority != null) {
            sql += " AND a.Priority = :priority";
            params.addValue("priority", priority);
        }
        if (roomId != null) {
            sql += " AND ma.RoomId = :roomId";
            params.addValue("roomId", roomId);
        }
        if (locationId != null) {
            sql += " AND ma.RoomId IN (SELECT RoomId FROM Rooms WHERE BuildingId IN (SELECT BuildingId FROM Buildings WHERE LocationId = :locationId))";
            params.addValue("locationId", locationId);
        }
        if (fromUtc != null) {
            sql += " AND a.StartUTC >= :fromUtc";
            params.addValue("fromUtc", fromUtc);
        }
        if (toUtc != null) {
            sql += " AND a.EndUTC <= :toUtc";
            params.addValue("toUtc", toUtc);
        }
        sql += " GROUP BY a.AssignmentId ORDER BY a.StartUTC";
        return jdbc.queryForList(sql, params);
    }

    public Map<String, Object> getAssignment(Integer assignmentId) {
        String sql = "SELECT a.* FROM Assignments a WHERE a.AssignmentId = :assignmentId";
        MapSqlParameterSource params = new MapSqlParameterSource("assignmentId", assignmentId);
        List<Map<String, Object>> rows = jdbc.queryForList(sql, params);
        if (rows.isEmpty()) {
            throw new ResourceNotFoundException("Assignment not found: " + assignmentId);
        }
        return rows.get(0);
    }

    public void updateAssignment(Integer assignmentId, Map<String, Object> request) {
        assertCanEditAssignment(assignmentId);
        Map<String, Object> existing = getAssignment(assignmentId);
        Map<String, Object> merged = new HashMap<>(existing);
        merged.putAll(request);
        String sql = "UPDATE Assignments SET OrganizerId = :organizerId, MeetingTitle = :meetingTitle, Description = :description, StartUTC = :startUtc, EndUTC = :endUtc, SecondaryTimeZoneId = :secondaryTimeZoneId, Status = :status, Priority = :priority, UpdatedAt = NOW(), CancelledBy = :cancelledBy, OverriddenBy = :overriddenBy, PreviousAssignmentId = :previousAssignmentId, IsRecurring = :isRecurring, RecurrencePattern = :recurrencePattern WHERE AssignmentId = :assignmentId";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("assignmentId", assignmentId);
        params.addValue("organizerId", getIntegerAny(merged, "organizerId", "OrganizerId"));
        params.addValue("meetingTitle", getStringAny(merged, "meetingTitle", "MeetingTitle"));
        params.addValue("description", getStringAny(merged, "description", "Description"));
        params.addValue("startUtc", normalizeUtc(getStringAny(merged, "startUtc", "StartUTC"), "startUtc"));
        params.addValue("endUtc", normalizeUtc(getStringAny(merged, "endUtc", "EndUTC"), "endUtc"));
        params.addValue("secondaryTimeZoneId", getIntegerAny(merged, "secondaryTimeZoneId", "SecondaryTimeZoneId"));
        params.addValue("status", normalizeStatus(getStringAny(merged, "status", "Status"), "SCHEDULED",
            ASSIGNMENT_STATUSES, "status"));
        params.addValue("priority", normalizeStatus(getStringAny(merged, "priority", "Priority"), "NORMAL",
            PRIORITY_STATUSES, "priority"));
        params.addValue("cancelledBy", getIntegerAny(merged, "cancelledBy", "CancelledBy"));
        params.addValue("overriddenBy", getIntegerAny(merged, "overriddenBy", "OverriddenBy"));
        params.addValue("previousAssignmentId", getIntegerAny(merged, "previousAssignmentId", "PreviousAssignmentId"));
        params.addValue("isRecurring", toYesNo(merged.get("isRecurring")));
        params.addValue("recurrencePattern", getStringAny(merged, "recurrencePattern", "RecurrencePattern"));
        jdbc.update(sql, params);
    }

    @Transactional
    public void deleteAssignment(Integer assignmentId) {
        assertCanEditAssignment(assignmentId);
        // Ensure the assignment exists before cascading cleanup.
        getAssignment(assignmentId);

        String replacementSql = "SELECT COUNT(*) FROM Assignments WHERE PreviousAssignmentId = :assignmentId";
        MapSqlParameterSource checkParams = new MapSqlParameterSource("assignmentId", assignmentId);
        Integer replacementCount = jdbc.queryForObject(replacementSql, checkParams, Integer.class);
        if (replacementCount != null && replacementCount > 0) {
            throw new IllegalStateException(
                    "Assignment cannot be deleted because replacement assignments exist: " + assignmentId);
        }

        MapSqlParameterSource params = new MapSqlParameterSource("assignmentId", assignmentId);

        String deleteVideoReservations = "DELETE vr FROM Video_Reservations vr "
                + "JOIN Meeting_Assignments ma ON vr.MeetingAssignmentId = ma.MeetingAssignmentId "
                + "WHERE ma.AssignmentId = :assignmentId";
        jdbc.update(deleteVideoReservations, params);

        String deleteMeetingAssignments = "DELETE FROM Meeting_Assignments WHERE AssignmentId = :assignmentId";
        jdbc.update(deleteMeetingAssignments, params);

        String deleteParticipants = "DELETE FROM Meeting_Participants WHERE AssignmentId = :assignmentId";
        jdbc.update(deleteParticipants, params);

        String deleteNotifications = "DELETE FROM Notifications WHERE AssignmentId = :assignmentId";
        jdbc.update(deleteNotifications, params);

        String deleteAssignment = "DELETE FROM Assignments WHERE AssignmentId = :assignmentId";
        int updated = jdbc.update(deleteAssignment, params);
        if (updated == 0) {
            throw new ResourceNotFoundException("Assignment not found: " + assignmentId);
        }
    }

    public List<Map<String, Object>> getRoomAssignments(Integer roomId, Integer locationId,
            String status, String fromUtc, String toUtc) {
        String sql = "SELECT ma.* FROM Meeting_Assignments ma WHERE 1=1";
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (roomId != null) {
            sql += " AND ma.RoomId = :roomId";
            params.addValue("roomId", roomId);
        }
        if (locationId != null) {
            sql += " AND ma.RoomId IN (SELECT RoomId FROM Rooms WHERE BuildingId IN (SELECT BuildingId FROM Buildings WHERE LocationId = :locationId))";
            params.addValue("locationId", locationId);
        }
        if (status != null) {
            sql += " AND ma.Status = :status";
            params.addValue("status", status);
        }
        if (fromUtc != null) {
            sql += " AND ma.EndUTC >= :fromUtc";
            params.addValue("fromUtc", fromUtc);
        }
        if (toUtc != null) {
            sql += " AND ma.StartUTC <= :toUtc";
            params.addValue("toUtc", toUtc);
        }
        sql += " ORDER BY ma.StartUTC";
        return jdbc.queryForList(sql, params);
    }

    public List<Map<String, Object>> getAssignmentRoomAssignments(Integer assignmentId) {
        String sql = "SELECT ma.* FROM Meeting_Assignments ma WHERE ma.AssignmentId = :assignmentId ORDER BY ma.StartUTC";
        MapSqlParameterSource params = new MapSqlParameterSource("assignmentId", assignmentId);
        return jdbc.queryForList(sql, params);
    }

    public void addRoomAssignment(Integer assignmentId, Map<String, Object> request) {
        assertCanEditAssignment(assignmentId);
        Integer meetingAssignmentId = nextId("Meeting_Assignments", "MeetingAssignmentId");
        Integer roomId = getInteger(request, "roomId");
        String startUtc = normalizeUtc(getString(request, "startUtc"), "startUtc");
        String endUtc = normalizeUtc(getString(request, "endUtc"), "endUtc");
        List<Map<String, Object>> conflicts = findRoomConflicts(roomId, startUtc, endUtc, null);
        if (!conflicts.isEmpty()) {
            if (!currentUserService.hasAuthority(Permissions.MEETING_OVERRIDE)) {
                throw new AuthorizationDeniedException("Booking conflict exists. Only managers/admins can override.");
            }
            Map<String, Object> details = new HashMap<>();
            details.put("roomId", roomId);
            details.put("startUtc", startUtc);
            details.put("endUtc", endUtc);
            details.put("reason", getString(request, "overrideReason"));
            auditLogWriter.log("MEETING_OVERRIDE", "ASSIGNMENT", assignmentId,
                    Map.of("conflicts", conflicts), details);
        }
        String sql = "INSERT INTO Meeting_Assignments (MeetingAssignmentId, AssignmentId, RoomId, IsPrimaryRoom, StartUTC, EndUTC, Status) "
                + "VALUES (:meetingAssignmentId, :assignmentId, :roomId, :isPrimaryRoom, :startUtc, :endUtc, :status)";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("meetingAssignmentId", meetingAssignmentId);
        params.addValue("assignmentId", assignmentId);
        params.addValue("roomId", roomId);
        params.addValue("isPrimaryRoom", toYesNo(request.get("isPrimaryRoom")));
        params.addValue("startUtc", startUtc);
        params.addValue("endUtc", endUtc);
        params.addValue("status", normalizeStatus(getString(request, "status"), "RESERVED",
            MEETING_ASSIGNMENT_STATUSES, "room assignment status"));
        jdbc.update(sql, params);
    }

    public void updateRoomAssignment(Integer meetingAssignmentId, Map<String, Object> request) {
        Map<String, Object> existing = getMeetingAssignment(meetingAssignmentId);
        Integer assignmentId = getInteger(existing, "AssignmentId");
        assertCanEditAssignment(assignmentId);
        Map<String, Object> merged = new HashMap<>(existing);
        merged.putAll(request);

        Integer roomId = getIntegerAny(merged, "roomId", "RoomId");
        String startUtc = normalizeUtc(getStringAny(merged, "startUtc", "StartUTC"), "startUtc");
        String endUtc = normalizeUtc(getStringAny(merged, "endUtc", "EndUTC"), "endUtc");
        List<Map<String, Object>> conflicts = findRoomConflicts(roomId, startUtc, endUtc, meetingAssignmentId);
        if (!conflicts.isEmpty()) {
            if (!currentUserService.hasAuthority(Permissions.MEETING_OVERRIDE)) {
                throw new AuthorizationDeniedException("Booking conflict exists. Only managers/admins can override.");
            }
            Map<String, Object> details = new HashMap<>();
            details.put("meetingAssignmentId", meetingAssignmentId);
            details.put("roomId", roomId);
            details.put("startUtc", startUtc);
            details.put("endUtc", endUtc);
            details.put("reason", getString(request, "overrideReason"));
            auditLogWriter.log("MEETING_OVERRIDE", "ASSIGNMENT", assignmentId,
                    Map.of("conflicts", conflicts), details);
        }

        String sql = "UPDATE Meeting_Assignments SET RoomId = :roomId, IsPrimaryRoom = :isPrimaryRoom, StartUTC = :startUtc, EndUTC = :endUtc, Status = :status WHERE MeetingAssignmentId = :meetingAssignmentId";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("meetingAssignmentId", meetingAssignmentId);
        params.addValue("roomId", roomId);
        params.addValue("isPrimaryRoom", toYesNo(merged.get("isPrimaryRoom")));
        params.addValue("startUtc", startUtc);
        params.addValue("endUtc", endUtc);
        params.addValue("status", normalizeStatus(getStringAny(merged, "status", "Status"), "RESERVED",
            MEETING_ASSIGNMENT_STATUSES, "room assignment status"));
        jdbc.update(sql, params);
    }

    public void deleteRoomAssignment(Integer assignmentId, Integer meetingAssignmentId) {
        assertCanEditAssignment(assignmentId);
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("assignmentId", assignmentId);
        params.addValue("meetingAssignmentId", meetingAssignmentId);
        String sql = "DELETE FROM Meeting_Assignments WHERE MeetingAssignmentId = :meetingAssignmentId AND AssignmentId = :assignmentId";
        int updated = jdbc.update(sql, params);
        if (updated == 0) {
            throw new ResourceNotFoundException("Meeting assignment not found: " + meetingAssignmentId);
        }
    }

    public List<Map<String, Object>> getAssignmentVideoReservations(Integer assignmentId) {
        String sql = "SELECT vr.* FROM Video_Reservations vr "
                + "JOIN Meeting_Assignments ma ON vr.MeetingAssignmentId = ma.MeetingAssignmentId "
                + "WHERE ma.AssignmentId = :assignmentId ORDER BY vr.CreatedAt";
        MapSqlParameterSource params = new MapSqlParameterSource("assignmentId", assignmentId);
        return jdbc.queryForList(sql, params);
    }

    public void addAssignmentVideoReservation(Integer assignmentId, Map<String, Object> request) {
        assertCanEditAssignment(assignmentId);
        Integer meetingAssignmentId = getInteger(request, "meetingAssignmentId");
        String sql = "SELECT COUNT(*) FROM Meeting_Assignments ma WHERE ma.MeetingAssignmentId = :meetingAssignmentId AND ma.AssignmentId = :assignmentId";
        MapSqlParameterSource guard = new MapSqlParameterSource();
        guard.addValue("meetingAssignmentId", meetingAssignmentId);
        guard.addValue("assignmentId", assignmentId);
        Integer count = jdbc.queryForObject(sql, guard, Integer.class);
        if (count == null || count == 0) {
            throw new ResourceNotFoundException("Meeting assignment not found for assignment: " + meetingAssignmentId);
        }
        Integer videoReservationId = nextId("Video_Reservations", "VideoReservationId");
        String insert = "INSERT INTO Video_Reservations (VideoReservationId, MeetingAssignmentId, LocationId, TimeZoneId, VideoTitle, IsPrimaryLocation, IsVideoEnabled, ConnectionLink, DialInInfo, Status, CreatedAt) "
                + "VALUES (:videoReservationId, :meetingAssignmentId, :locationId, :timeZoneId, :videoTitle, :isPrimaryLocation, :isVideoEnabled, :connectionLink, :dialInInfo, :status, NOW())";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("videoReservationId", videoReservationId);
        params.addValue("meetingAssignmentId", meetingAssignmentId);
        params.addValue("locationId", getInteger(request, "locationId"));
        params.addValue("timeZoneId", getInteger(request, "timeZoneId"));
        params.addValue("videoTitle", getString(request, "videoTitle"));
        params.addValue("isPrimaryLocation", toYesNo(request.get("isPrimaryLocation")));
        params.addValue("isVideoEnabled", toYesNo(request.get("isVideoEnabled")));
        params.addValue("connectionLink", getString(request, "connectionLink"));
        params.addValue("dialInInfo", getString(request, "dialInInfo"));
        params.addValue("status", normalizeStatus(getString(request, "status"), "CONFIRMED", VIDEO_STATUSES,
            "video reservation status"));
        jdbc.update(insert, params);
    }

    public Map<String, Object> getVideoReservation(Integer videoReservationId) {
        String sql = "SELECT vr.* FROM Video_Reservations vr WHERE vr.VideoReservationId = :videoReservationId";
        MapSqlParameterSource params = new MapSqlParameterSource("videoReservationId", videoReservationId);
        List<Map<String, Object>> rows = jdbc.queryForList(sql, params);
        if (rows.isEmpty()) {
            throw new ResourceNotFoundException("Video reservation not found: " + videoReservationId);
        }
        return rows.get(0);
    }

    public void updateVideoReservation(Integer videoReservationId, Map<String, Object> request) {
        Map<String, Object> existing = getVideoReservation(videoReservationId);
        Integer assignmentId = assignmentIdByVideoReservation(videoReservationId);
        assertCanEditAssignment(assignmentId);
        Map<String, Object> merged = new HashMap<>(existing);
        merged.putAll(request);
        String sql = "UPDATE Video_Reservations SET LocationId = :locationId, TimeZoneId = :timeZoneId, VideoTitle = :videoTitle, IsPrimaryLocation = :isPrimaryLocation, IsVideoEnabled = :isVideoEnabled, ConnectionLink = :connectionLink, DialInInfo = :dialInInfo, Status = :status WHERE VideoReservationId = :videoReservationId";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("videoReservationId", videoReservationId);
        params.addValue("locationId", getInteger(merged, "locationId"));
        params.addValue("timeZoneId", getInteger(merged, "timeZoneId"));
        params.addValue("videoTitle", getString(merged, "videoTitle"));
        params.addValue("isPrimaryLocation", toYesNo(merged.get("isPrimaryLocation")));
        params.addValue("isVideoEnabled", toYesNo(merged.get("isVideoEnabled")));
        params.addValue("connectionLink", getString(merged, "connectionLink"));
        params.addValue("dialInInfo", getString(merged, "dialInInfo"));
        params.addValue("status", normalizeStatus(getStringAny(merged, "status", "Status"), "CONFIRMED",
            VIDEO_STATUSES, "video reservation status"));
        jdbc.update(sql, params);
    }

    public void deleteVideoReservation(Integer videoReservationId) {
        Integer assignmentId = assignmentIdByVideoReservation(videoReservationId);
        assertCanEditAssignment(assignmentId);
        String sql = "DELETE FROM Video_Reservations WHERE VideoReservationId = :videoReservationId";
        MapSqlParameterSource params = new MapSqlParameterSource("videoReservationId", videoReservationId);
        int updated = jdbc.update(sql, params);
        if (updated == 0) {
            throw new ResourceNotFoundException("Video reservation not found: " + videoReservationId);
        }
    }

    public void cancelAssignment(Integer assignmentId, Map<String, Object> request) {
        assertCanEditAssignment(assignmentId);
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("assignmentId", assignmentId);
        params.addValue("cancelledBy", getInteger(request, "cancelledBy"));
        params.addValue("status", normalizeStatus(getString(request, "status"), "CANCELLED", ASSIGNMENT_STATUSES,
            "status"));
        String sql = "UPDATE Assignments SET Status = :status, CancelledBy = :cancelledBy, UpdatedAt = NOW() WHERE AssignmentId = :assignmentId";
        int updated = jdbc.update(sql, params);
        if (updated == 0) {
            throw new ResourceNotFoundException("Assignment not found: " + assignmentId);
        }
    }

    public void overrideAssignment(Integer assignmentId, Map<String, Object> request) {
        if (!currentUserService.hasAuthority(Permissions.MEETING_OVERRIDE)) {
            throw new AuthorizationDeniedException("Only managers/admins can override meetings");
        }
        Map<String, Object> oldValues = getAssignment(assignmentId);

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("assignmentId", assignmentId);
        params.addValue("overriddenBy", currentUserService.currentEmployeeId());
        params.addValue("status", normalizeStatus(getString(request, "status"), "OVERRIDDEN", ASSIGNMENT_STATUSES,
            "status"));
        String sql = "UPDATE Assignments SET Status = :status, OverriddenBy = :overriddenBy, UpdatedAt = NOW() WHERE AssignmentId = :assignmentId";
        int updated = jdbc.update(sql, params);
        if (updated == 0) {
            throw new ResourceNotFoundException("Assignment not found: " + assignmentId);
        }

        Map<String, Object> details = new HashMap<>();
        details.put("status", params.getValue("status"));
        details.put("overriddenBy", params.getValue("overriddenBy"));
        details.put("reason", getString(request, "reason"));
        auditLogWriter.log("MEETING_OVERRIDE", "ASSIGNMENT", assignmentId, oldValues, details);
    }

    public List<Map<String, Object>> getParticipants(Integer assignmentId) {
        String sql = "SELECT mp.* FROM Meeting_Participants mp WHERE mp.AssignmentId = :assignmentId ORDER BY mp.InviteSentAt";
        return jdbc.queryForList(sql, new MapSqlParameterSource("assignmentId", assignmentId));
    }

    public void addParticipant(Integer assignmentId, Map<String, Object> request) {
        assertCanUpdateParticipants(assignmentId);

        Integer employeeId = getInteger(request, "employeeId");

        // Fetch the parent assignment times to check for scheduling conflicts
        Map<String, Object> assignment = getAssignment(assignmentId);
        String startUtc = getString(assignment, "StartUTC");
        String endUtc = getString(assignment, "EndUTC");
        if (startUtc == null) { startUtc = getString(assignment, "startUtc"); }
        if (endUtc == null) { endUtc = getString(assignment, "endUtc"); }

        if (employeeId != null && startUtc != null && endUtc != null) {
            Map<String, Object> conflict = findParticipantConflict(employeeId, startUtc, endUtc, assignmentId);
            if (conflict != null) {
                String conflictTitle = conflict.get("MeetingTitle") != null
                        ? conflict.get("MeetingTitle").toString()
                        : conflict.getOrDefault("meetingTitle", "another meeting").toString();
                throw new IllegalArgumentException(
                        "Employee is already scheduled in \"" + conflictTitle + "\" during this time");
            }
        }

        Integer participantId = nextId("Meeting_Participants", "ParticipantId");

        String sql = "INSERT INTO Meeting_Participants (ParticipantId, AssignmentId, EmployeeId, Status, ResponseStatus, Responsibility, InviteSentAt, ResponseAt) "
                + "VALUES (:participantId, :assignmentId, :employeeId, :status, :responseStatus, :responsibility, NOW(), :responseAt)";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("participantId", participantId);
        params.addValue("assignmentId", assignmentId);
        params.addValue("employeeId", employeeId);
        params.addValue("status", normalizeStatus(getString(request, "status"), "ATTENDEE", PARTICIPANT_STATUSES,
            "participant status"));
        params.addValue("responseStatus", normalizeStatus(getString(request, "responseStatus"), "PENDING",
            PARTICIPANT_RESPONSE_STATUSES, "participant response status"));
        params.addValue("responsibility", getString(request, "responsibility"));
        params.addValue("responseAt", normalizeUtc(getString(request, "responseAt"), "responseAt"));
        jdbc.update(sql, params);
    }

    public void removeParticipant(Integer assignmentId, Integer participantId) {
        assertCanUpdateParticipants(assignmentId);
        String sql = "DELETE FROM Meeting_Participants WHERE AssignmentId = :assignmentId AND ParticipantId = :participantId";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("assignmentId", assignmentId);
        params.addValue("participantId", participantId);
        int updated = jdbc.update(sql, params);
        if (updated == 0) {
            throw new ResourceNotFoundException("Participant not found: " + participantId);
        }
    }

    private Integer nextId(String table, String column) {
        String sql = "SELECT COALESCE(MAX(" + column + "), 0) + 1 FROM " + table;
        return jdbc.queryForObject(sql, new MapSqlParameterSource(), Integer.class);
    }

    private Map<String, Object> getMeetingAssignment(Integer meetingAssignmentId) {
        String sql = "SELECT ma.* FROM Meeting_Assignments ma WHERE ma.MeetingAssignmentId = :meetingAssignmentId";
        MapSqlParameterSource params = new MapSqlParameterSource("meetingAssignmentId", meetingAssignmentId);
        List<Map<String, Object>> rows = jdbc.queryForList(sql, params);
        if (rows.isEmpty()) {
            throw new ResourceNotFoundException("Meeting assignment not found: " + meetingAssignmentId);
        }
        return rows.get(0);
    }

    private void assertCanEditAssignment(Integer assignmentId) {
        if (currentUserService.hasAuthority(Permissions.MEETING_OVERRIDE)) {
            return;
        }
        Map<String, Object> assignment = getAssignment(assignmentId);
        Integer organizerId = getInteger(assignment, "OrganizerId");
        if (!currentUserService.currentEmployeeId().equals(organizerId)) {
            throw new AuthorizationDeniedException("Only the meeting organizer can modify this assignment");
        }
    }

    private void assertCanUpdateParticipants(Integer assignmentId) {
        assertCanEditAssignment(assignmentId);
    }

    private Map<String, Object> findParticipantConflict(Integer employeeId, String startUtc, String endUtc,
            Integer excludeAssignmentId) {
        String sql = "SELECT DISTINCT a.AssignmentId, a.MeetingTitle "
                + "FROM Assignments a "
                + "LEFT JOIN Meeting_Participants mp ON mp.AssignmentId = a.AssignmentId AND mp.EmployeeId = :employeeId "
                + "WHERE (a.OrganizerId = :employeeId OR mp.EmployeeId IS NOT NULL) "
                + "AND a.Status = 'SCHEDULED' "
                + "AND a.StartUTC < :endUtc AND a.EndUTC > :startUtc "
                + "AND a.AssignmentId != :excludeId "
                + "LIMIT 1";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("employeeId", employeeId);
        params.addValue("startUtc", startUtc);
        params.addValue("endUtc", endUtc);
        params.addValue("excludeId", excludeAssignmentId);
        List<Map<String, Object>> result = jdbc.queryForList(sql, params);
        return result.isEmpty() ? null : result.get(0);
    }

    private List<Map<String, Object>> findRoomConflicts(Integer roomId, String startUtc, String endUtc,
            Integer excludeMeetingAssignmentId) {
        String sql = "SELECT ma.MeetingAssignmentId, ma.AssignmentId, ma.StartUTC, ma.EndUTC, ma.Status "
                + "FROM Meeting_Assignments ma "
                + "WHERE ma.RoomId = :roomId "
                + "AND ma.StartUTC < :endUtc AND ma.EndUTC > :startUtc "
                + "AND (:excludeId IS NULL OR ma.MeetingAssignmentId <> :excludeId) "
                + "AND ma.Status <> 'CANCELLED'";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("roomId", roomId);
        params.addValue("startUtc", startUtc);
        params.addValue("endUtc", endUtc);
        params.addValue("excludeId", excludeMeetingAssignmentId);
        return jdbc.queryForList(sql, params);
    }

    private Integer assignmentIdByVideoReservation(Integer videoReservationId) {
        String sql = "SELECT ma.AssignmentId FROM Video_Reservations vr "
                + "JOIN Meeting_Assignments ma ON ma.MeetingAssignmentId = vr.MeetingAssignmentId "
                + "WHERE vr.VideoReservationId = :videoReservationId";
        Integer assignmentId = jdbc.query(sql,
                new MapSqlParameterSource("videoReservationId", videoReservationId),
                rs -> rs.next() ? rs.getInt("AssignmentId") : null);
        if (assignmentId == null) {
            throw new ResourceNotFoundException("Video reservation not found: " + videoReservationId);
        }
        return assignmentId;
    }

    private String getString(Map<String, Object> request, String key) {
        Object value = request.get(key);
        return value != null ? String.valueOf(value) : null;
    }

    private Integer getInteger(Map<String, Object> request, String key) {
        Object value = request.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Integer.valueOf(text);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private String getStringAny(Map<String, Object> request, String... keys) {
        for (String key : keys) {
            String value = getString(request, key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Integer getIntegerAny(Map<String, Object> request, String... keys) {
        for (String key : keys) {
            Integer value = getInteger(request, key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String normalizeStatus(String value, String defaultValue, Set<String> allowed, String fieldName) {
        String candidate = value == null || value.isBlank() ? defaultValue : value.trim().toUpperCase();
        if (!allowed.contains(candidate)) {
            throw new IllegalArgumentException("Invalid " + fieldName + ": " + value + ". Allowed values: " + allowed);
        }
        return candidate;
    }

    private String normalizeUtc(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String candidate = value.trim();
        try {
            Instant instant = Instant.parse(candidate);
            return DB_UTC_FORMAT.format(LocalDateTime.ofInstant(instant, ZoneOffset.UTC));
        } catch (DateTimeParseException ignored) {
        }
        try {
            OffsetDateTime offsetDateTime = OffsetDateTime.parse(candidate);
            return DB_UTC_FORMAT.format(offsetDateTime.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime());
        } catch (DateTimeParseException ignored) {
        }
        try {
            LocalDateTime localDateTime = LocalDateTime.parse(candidate, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return DB_UTC_FORMAT.format(localDateTime);
        } catch (DateTimeParseException ignored) {
        }
        try {
            LocalDateTime localDateTime = LocalDateTime.parse(candidate, DB_UTC_FORMAT);
            return DB_UTC_FORMAT.format(localDateTime);
        } catch (DateTimeParseException ignored) {
        }
        throw new IllegalArgumentException(
                "Invalid " + fieldName + " format. Use ISO UTC (e.g. 2026-07-01T13:00:00Z) or yyyy-MM-dd HH:mm:ss");
    }

    private String toYesNo(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean bool) {
            return bool ? "Y" : "N";
        }
        String text = String.valueOf(value).trim().toUpperCase();
        if (text.isBlank()) {
            return null;
        }
        return text.equals("Y") || text.equals("YES") || text.equals("TRUE") ? "Y" : "N";
    }
}
