package com.buzzmeet.service;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final NamedParameterJdbcTemplate jdbc;

    public NotificationService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> getNotifications(Integer employeeId) {
        String sql = "SELECT n.* FROM Notifications n WHERE 1=1";
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (employeeId != null) {
            sql += " AND n.EmployeeId = :employeeId";
            params.addValue("employeeId", employeeId);
        }
        sql += " ORDER BY n.SentAt DESC";
        return jdbc.queryForList(sql, params);
    }
}
