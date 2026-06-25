package com.buzzmeet.service;

import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.buzzmeet.security.CurrentUserService;

@Service
public class AuditLogWriter {

    private final NamedParameterJdbcTemplate jdbc;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;

    public AuditLogWriter(NamedParameterJdbcTemplate jdbc, CurrentUserService currentUserService,
            ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.currentUserService = currentUserService;
        this.objectMapper = objectMapper;
    }

    public void log(String action, String entityType, Integer entityId, Map<String, Object> oldValues,
            Map<String, Object> newValues) {
        Integer logId = nextId();
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("logId", logId);
        params.addValue("employeeId", currentUserService.currentEmployeeId());
        params.addValue("action", action);
        params.addValue("entityType", entityType);
        params.addValue("entityId", entityId);
        params.addValue("oldValues", stringifyJson(oldValues));
        params.addValue("newValues", stringifyJson(newValues));
        params.addValue("ipAddress", null);

        String sql = "INSERT INTO Audit_Logs (LogId, EmployeeId, Action, EntityType, EntityId, OldValues, NewValues, IPAddress, CreatedAt) "
                + "VALUES (:logId, :employeeId, :action, :entityType, :entityId, :oldValues, :newValues, :ipAddress, NOW())";
        jdbc.update(sql, params);
    }

    private Integer nextId() {
        String sql = "SELECT COALESCE(MAX(LogId), 0) + 1 FROM Audit_Logs";
        return jdbc.queryForObject(sql, new MapSqlParameterSource(), Integer.class);
    }

    private String stringifyJson(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JacksonException e) {
            return null;
        }
    }
}
