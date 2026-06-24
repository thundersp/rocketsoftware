package com.buzzmeet.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import com.buzzmeet.exception.ResourceNotFoundException;

@Service
public class AssignmentService {

    private final NamedParameterJdbcTemplate jdbc;

    public AssignmentService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void createAssignment(Map<String, Object> request) {
        Integer assignmentId = nextId("Assignments", "AssignmentId");
        String sql = "INSERT INTO Assignments (AssignmentId, OrganizerId, MeetingTitle, Description, StartUTC, EndUTC, SecondaryTimeZoneId, Status, Priority, CreatedAt, UpdatedAt, CancelledBy, OverriddenBy, PreviousAssignmentId, IsRecurring, RecurrencePattern) "
                + "VALUES (:assignmentId, :organizerId, :meetingTitle, :description, :startUtc, :endUtc, :secondaryTimeZoneId, :status, :priority, NOW(), NOW(), :cancelledBy, :overriddenBy, :previousAssignmentId, :isRecurring, :recurrencePattern)";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("assignmentId", assignmentId);
        params.addValue("organizerId", getInteger(request, "organizerId"));
        params.addValue("meetingTitle", getString(request, "meetingTitle"));
        params.addValue("description", getString(request, "description"));
        params.addValue("startUtc", getString(request, "startUtc"));
        params.addValue("endUtc", getString(request, "endUtc"));
        params.addValue("secondaryTimeZoneId", getInteger(request, "secondaryTimeZoneId"));
        params.addValue("status", getString(request, "status"));
        params.addValue("priority", getString(request, "priority"));
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
        Map<String, Object> existing = getAssignment(assignmentId);
        Map<String, Object> merged = new HashMap<>(existing);
        merged.putAll(request);
        String sql = "UPDATE Assignments SET OrganizerId = :organizerId, MeetingTitle = :meetingTitle, Description = :description, StartUTC = :startUtc, EndUTC = :endUtc, SecondaryTimeZoneId = :secondaryTimeZoneId, Status = :status, Priority = :priority, UpdatedAt = NOW(), CancelledBy = :cancelledBy, OverriddenBy = :overriddenBy, PreviousAssignmentId = :previousAssignmentId, IsRecurring = :isRecurring, RecurrencePattern = :recurrencePattern WHERE AssignmentId = :assignmentId";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("assignmentId", assignmentId);
        params.addValue("organizerId", getInteger(merged, "organizerId"));
        params.addValue("meetingTitle", getString(merged, "meetingTitle"));
        params.addValue("description", getString(merged, "description"));
        params.addValue("startUtc", getString(merged, "startUtc"));
        params.addValue("endUtc", getString(merged, "endUtc"));
        params.addValue("secondaryTimeZoneId", getInteger(merged, "secondaryTimeZoneId"));
        params.addValue("status", getString(merged, "status"));
        params.addValue("priority", getString(merged, "priority"));
        params.addValue("cancelledBy", getInteger(merged, "cancelledBy"));
        params.addValue("overriddenBy", getInteger(merged, "overriddenBy"));
        params.addValue("previousAssignmentId", getInteger(merged, "previousAssignmentId"));
        params.addValue("isRecurring", toYesNo(merged.get("isRecurring")));
        params.addValue("recurrencePattern", getString(merged, "recurrencePattern"));
        jdbc.update(sql, params);
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
        Integer meetingAssignmentId = nextId("Meeting_Assignments", "MeetingAssignmentId");
        String sql = "INSERT INTO Meeting_Assignments (MeetingAssignmentId, AssignmentId, RoomId, IsPrimaryRoom, StartUTC, EndUTC, Status) "
                + "VALUES (:meetingAssignmentId, :assignmentId, :roomId, :isPrimaryRoom, :startUtc, :endUtc, :status)";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("meetingAssignmentId", meetingAssignmentId);
        params.addValue("assignmentId", assignmentId);
        params.addValue("roomId", getInteger(request, "roomId"));
        params.addValue("isPrimaryRoom", toYesNo(request.get("isPrimaryRoom")));
        params.addValue("startUtc", getString(request, "startUtc"));
        params.addValue("endUtc", getString(request, "endUtc"));
        params.addValue("status", getString(request, "status"));
        jdbc.update(sql, params);
    }

    public void updateRoomAssignment(Integer meetingAssignmentId, Map<String, Object> request) {
        Map<String, Object> existing = getMeetingAssignment(meetingAssignmentId);
        Map<String, Object> merged = new HashMap<>(existing);
        merged.putAll(request);
        String sql = "UPDATE Meeting_Assignments SET RoomId = :roomId, IsPrimaryRoom = :isPrimaryRoom, StartUTC = :startUtc, EndUTC = :endUtc, Status = :status WHERE MeetingAssignmentId = :meetingAssignmentId";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("meetingAssignmentId", meetingAssignmentId);
        params.addValue("roomId", getInteger(merged, "roomId"));
        params.addValue("isPrimaryRoom", toYesNo(merged.get("isPrimaryRoom")));
        params.addValue("startUtc", getString(merged, "startUtc"));
        params.addValue("endUtc", getString(merged, "endUtc"));
        params.addValue("status", getString(merged, "status"));
        jdbc.update(sql, params);
    }

    public void deleteRoomAssignment(Integer assignmentId, Integer meetingAssignmentId) {
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
        params.addValue("status", getString(request, "status"));
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
        params.addValue("status", getString(merged, "status"));
        jdbc.update(sql, params);
    }

    public void deleteVideoReservation(Integer videoReservationId) {
        String sql = "DELETE FROM Video_Reservations WHERE VideoReservationId = :videoReservationId";
        MapSqlParameterSource params = new MapSqlParameterSource("videoReservationId", videoReservationId);
        int updated = jdbc.update(sql, params);
        if (updated == 0) {
            throw new ResourceNotFoundException("Video reservation not found: " + videoReservationId);
        }
    }

    public void cancelAssignment(Integer assignmentId, Map<String, Object> request) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("assignmentId", assignmentId);
        params.addValue("cancelledBy", getInteger(request, "cancelledBy"));
        params.addValue("status", getString(request, "status") != null ? getString(request, "status") : "CANCELLED");
        String sql = "UPDATE Assignments SET Status = :status, CancelledBy = :cancelledBy, UpdatedAt = NOW() WHERE AssignmentId = :assignmentId";
        int updated = jdbc.update(sql, params);
        if (updated == 0) {
            throw new ResourceNotFoundException("Assignment not found: " + assignmentId);
        }
    }

    public void overrideAssignment(Integer assignmentId, Map<String, Object> request) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("assignmentId", assignmentId);
        params.addValue("overriddenBy", getInteger(request, "overriddenBy"));
        params.addValue("status", getString(request, "status") != null ? getString(request, "status") : "OVERRIDDEN");
        String sql = "UPDATE Assignments SET Status = :status, OverriddenBy = :overriddenBy, UpdatedAt = NOW() WHERE AssignmentId = :assignmentId";
        int updated = jdbc.update(sql, params);
        if (updated == 0) {
            throw new ResourceNotFoundException("Assignment not found: " + assignmentId);
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
