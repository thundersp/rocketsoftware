package com.buzzmeet.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AvailabilityService {

    private static final DateTimeFormatter DB_UTC_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final NamedParameterJdbcTemplate jdbc;

    public AvailabilityService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Returns all scheduled/draft meetings for an employee that overlap the given time range.
     */
    public List<Map<String, Object>> getEmployeeAvailability(Integer employeeId, String startUtc, String endUtc) {
        String sql = "SELECT DISTINCT a.AssignmentId, a.MeetingTitle, a.StartUTC, a.EndUTC, a.Status, a.Priority, "
                + "mp.Status as ParticipantStatus, mp.ResponseStatus, "
                + "CASE WHEN a.OrganizerId = :employeeId THEN 'Y' ELSE 'N' END as IsOrganizer "
                + "FROM Assignments a "
                + "LEFT JOIN Meeting_Participants mp ON mp.AssignmentId = a.AssignmentId AND mp.EmployeeId = :employeeId "
                + "WHERE (a.OrganizerId = :employeeId OR mp.EmployeeId IS NOT NULL) "
                + "AND a.Status IN ('SCHEDULED', 'DRAFT') "
                + "AND a.StartUTC < :endUtc AND a.EndUTC > :startUtc "
                + "ORDER BY a.StartUTC";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("employeeId", employeeId);
        params.addValue("startUtc", normalizeUtc(startUtc, "startUtc"));
        params.addValue("endUtc", normalizeUtc(endUtc, "endUtc"));
        return jdbc.queryForList(sql, params);
    }

    /**
     * Returns availability status for each requested employee at a given UTC time.
     * Status is either "IN_A_MEETING" or "AVAILABLE".
     */
    public List<Map<String, Object>> getEmployeesAvailabilityStatus(List<Integer> employeeIds, String atUtc) {
        if (employeeIds == null || employeeIds.isEmpty()) {
            return List.of();
        }
        String normalizedAt = normalizeUtc(atUtc, "atUtc");

        String sql = "SELECT e.id AS EmployeeId, e.first_name AS FirstName, e.last_name AS LastName, "
                + "CASE WHEN EXISTS ( "
                + "  SELECT 1 FROM Assignments a2 "
                + "  LEFT JOIN Meeting_Participants mp2 ON mp2.AssignmentId = a2.AssignmentId AND mp2.EmployeeId = e.id "
                + "  WHERE (a2.OrganizerId = e.id OR mp2.EmployeeId IS NOT NULL) "
                + "  AND a2.Status = 'SCHEDULED' "
                + "  AND a2.StartUTC <= :atUtc AND a2.EndUTC > :atUtc "
                + ") THEN 'IN_A_MEETING' ELSE 'AVAILABLE' END AS AvailabilityStatus, "
                + "( SELECT a3.MeetingTitle FROM Assignments a3 "
                + "  LEFT JOIN Meeting_Participants mp3 ON mp3.AssignmentId = a3.AssignmentId AND mp3.EmployeeId = e.id "
                + "  WHERE (a3.OrganizerId = e.id OR mp3.EmployeeId IS NOT NULL) "
                + "  AND a3.Status = 'SCHEDULED' "
                + "  AND a3.StartUTC <= :atUtc AND a3.EndUTC > :atUtc "
                + "  LIMIT 1 ) AS CurrentMeetingTitle "
                + "FROM Employee e "
                + "WHERE e.id IN (:employeeIds)";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("employeeIds", employeeIds);
        params.addValue("atUtc", normalizedAt);
        return jdbc.queryForList(sql, params);
    }

    /**
     * Finds the first SCHEDULED meeting that conflicts with the given time range for the employee.
     * Excludes the given assignmentId (the meeting being scheduled). Returns null if no conflict.
     */
    public Map<String, Object> findConflict(Integer employeeId, String startUtc, String endUtc,
            Integer excludeAssignmentId) {
        String sql = "SELECT DISTINCT a.AssignmentId, a.MeetingTitle, a.StartUTC, a.EndUTC "
                + "FROM Assignments a "
                + "LEFT JOIN Meeting_Participants mp ON mp.AssignmentId = a.AssignmentId AND mp.EmployeeId = :employeeId "
                + "WHERE (a.OrganizerId = :employeeId OR mp.EmployeeId IS NOT NULL) "
                + "AND a.Status = 'SCHEDULED' "
                + "AND a.StartUTC < :endUtc AND a.EndUTC > :startUtc "
                + "AND (:excludeId IS NULL OR a.AssignmentId != :excludeId) "
                + "LIMIT 1";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("employeeId", employeeId);
        params.addValue("startUtc", normalizeUtc(startUtc, "startUtc"));
        params.addValue("endUtc", normalizeUtc(endUtc, "endUtc"));
        params.addValue("excludeId", excludeAssignmentId);
        List<Map<String, Object>> result = jdbc.queryForList(sql, params);
        return result.isEmpty() ? null : result.get(0);
    }

    private String normalizeUtc(String value, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            if (value.endsWith("Z") || value.contains("T")) {
                java.time.Instant instant = java.time.Instant.parse(
                        value.endsWith("Z") ? value : value + "Z");
                return LocalDateTime.ofInstant(instant, ZoneOffset.UTC).format(DB_UTC_FORMAT);
            }
            LocalDateTime.parse(value, DB_UTC_FORMAT);
            return value;
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid datetime format for " + field + ": " + value);
        }
    }
}
