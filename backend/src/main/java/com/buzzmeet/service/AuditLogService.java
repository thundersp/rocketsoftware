package com.buzzmeet.service;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {

    private final NamedParameterJdbcTemplate jdbc;

    public AuditLogService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> getAuditLogs(String entityType, Integer entityId) {
        String sql = "SELECT a.* FROM Audit_Logs a WHERE 1=1";
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (entityType != null) {
            sql += " AND a.EntityType = :entityType";
            params.addValue("entityType", entityType);
        }
        if (entityId != null) {
            sql += " AND a.EntityId = :entityId";
            params.addValue("entityId", entityId);
        }
        sql += " ORDER BY a.CreatedAt DESC";
        return jdbc.queryForList(sql, params);
    }
}
