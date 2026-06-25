package com.buzzmeet.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buzzmeet.exception.ResourceNotFoundException;

@Service
public class AdminService {

    private final NamedParameterJdbcTemplate jdbc;
    private final AuditLogWriter auditLogWriter;

    public AdminService(NamedParameterJdbcTemplate jdbc, AuditLogWriter auditLogWriter) {
        this.jdbc = jdbc;
        this.auditLogWriter = auditLogWriter;
    }

    public List<Map<String, Object>> getUsers(Boolean activeOnly, String roleName) {
        String sql = "SELECT e.id AS EmployeeId, e.first_name AS FirstName, e.last_name AS LastName, e.email AS Email, "
                + "e.title AS Title, e.country AS Country, e.city AS City, e.location AS LocationId, "
                + "uc.CredentialId, uc.IsActive AS CredentialActive, r.RoleName "
                + "FROM Employee e "
                + "LEFT JOIN User_Credentials uc ON uc.EmployeeId = e.id "
                + "LEFT JOIN Employee_Roles er ON er.EmployeeId = e.id AND er.IsActive = 'Y' "
                + "LEFT JOIN Roles r ON r.RoleId = er.RoleId "
                + "WHERE 1=1";
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (activeOnly != null) {
            sql += " AND uc.IsActive = :active";
            params.addValue("active", activeOnly ? "Y" : "N");
        }
        if (roleName != null && !roleName.isBlank()) {
            sql += " AND UPPER(r.RoleName) = :roleName";
            params.addValue("roleName", roleName.trim().toUpperCase());
        }
        sql += " ORDER BY e.first_name, e.last_name";
        return jdbc.queryForList(sql, params);
    }

    @Transactional
    public Integer createUser(Map<String, Object> request) {
        Integer employeeId = nextId("Employee", "id");
        Integer credentialId = nextId("User_Credentials", "CredentialId");

        String insertEmployee = "INSERT INTO Employee (id, first_name, last_name, title, email, country, city, location) "
                + "VALUES (:id, :firstName, :lastName, :title, :email, :country, :city, :locationId)";
        MapSqlParameterSource employeeParams = new MapSqlParameterSource();
        employeeParams.addValue("id", employeeId);
        employeeParams.addValue("firstName", getString(request, "firstName"));
        employeeParams.addValue("lastName", getString(request, "lastName"));
        employeeParams.addValue("title", getString(request, "title"));
        employeeParams.addValue("email", getString(request, "email"));
        employeeParams.addValue("country", getString(request, "country"));
        employeeParams.addValue("city", getString(request, "city"));
        employeeParams.addValue("locationId", getInteger(request, "locationId"));
        jdbc.update(insertEmployee, employeeParams);

        String insertCredential = "INSERT INTO User_Credentials (CredentialId, EmployeeId, PasswordHash, LastLogin, IsActive, FailedLoginAttempts, LockedUntil, PasswordResetToken, PasswordResetExpiry, CreatedAt, UpdatedAt) "
                + "VALUES (:credentialId, :employeeId, :passwordHash, NULL, :isActive, 0, NULL, NULL, NULL, NOW(), NOW())";
        MapSqlParameterSource credentialParams = new MapSqlParameterSource();
        credentialParams.addValue("credentialId", credentialId);
        credentialParams.addValue("employeeId", employeeId);
        credentialParams.addValue("passwordHash", defaultIfBlank(getString(request, "passwordHash"), "{noop}Password123!"));
        credentialParams.addValue("isActive", defaultIfBlank(getString(request, "isActive"), "Y"));
        jdbc.update(insertCredential, credentialParams);

        Integer roleId = resolveRoleId(request);
        if (roleId != null) {
            assignRole(employeeId, roleId);
        }

        auditLogWriter.log("USER_CREATE", "EMPLOYEE", employeeId, null, getUserById(employeeId));
        return employeeId;
    }

    @Transactional
    public void updateUser(Integer employeeId, Map<String, Object> request) {
        Map<String, Object> oldValues = getUserById(employeeId);

        String updateEmployee = "UPDATE Employee SET first_name = COALESCE(:firstName, first_name), "
                + "last_name = COALESCE(:lastName, last_name), title = COALESCE(:title, title), "
                + "email = COALESCE(:email, email), country = COALESCE(:country, country), "
                + "city = COALESCE(:city, city), location = COALESCE(:locationId, location) "
                + "WHERE id = :employeeId";
        MapSqlParameterSource employeeParams = new MapSqlParameterSource();
        employeeParams.addValue("employeeId", employeeId);
        employeeParams.addValue("firstName", getString(request, "firstName"));
        employeeParams.addValue("lastName", getString(request, "lastName"));
        employeeParams.addValue("title", getString(request, "title"));
        employeeParams.addValue("email", getString(request, "email"));
        employeeParams.addValue("country", getString(request, "country"));
        employeeParams.addValue("city", getString(request, "city"));
        employeeParams.addValue("locationId", getInteger(request, "locationId"));
        int employeeUpdates = jdbc.update(updateEmployee, employeeParams);
        if (employeeUpdates == 0) {
            throw new ResourceNotFoundException("User not found: " + employeeId);
        }

        String isActive = getString(request, "isActive");
        if (isActive != null) {
            String updateCredential = "UPDATE User_Credentials SET IsActive = :isActive, UpdatedAt = NOW() WHERE EmployeeId = :employeeId";
            jdbc.update(updateCredential,
                    new MapSqlParameterSource().addValue("employeeId", employeeId).addValue("isActive", isActive));
        }

        String passwordHash = getString(request, "passwordHash");
        if (passwordHash != null && !passwordHash.isBlank()) {
            String updatePassword = "UPDATE User_Credentials SET PasswordHash = :passwordHash, UpdatedAt = NOW() WHERE EmployeeId = :employeeId";
            jdbc.update(updatePassword,
                    new MapSqlParameterSource().addValue("employeeId", employeeId).addValue("passwordHash", passwordHash));
        }

        auditLogWriter.log("USER_UPDATE", "EMPLOYEE", employeeId, oldValues, getUserById(employeeId));
    }

    @Transactional
    public void deactivateUser(Integer employeeId) {
        Map<String, Object> oldValues = getUserById(employeeId);
        String sql = "UPDATE User_Credentials SET IsActive = 'N', UpdatedAt = NOW() WHERE EmployeeId = :employeeId";
        int updated = jdbc.update(sql, new MapSqlParameterSource("employeeId", employeeId));
        if (updated == 0) {
            throw new ResourceNotFoundException("Credential not found for user: " + employeeId);
        }
        auditLogWriter.log("USER_DEACTIVATE", "EMPLOYEE", employeeId, oldValues, getUserById(employeeId));
    }

    @Transactional
    public void assignRole(Integer employeeId, Map<String, Object> request) {
        Integer roleId = resolveRoleId(request);
        if (roleId == null) {
            throw new IllegalArgumentException("roleId or roleName is required");
        }
        assignRole(employeeId, roleId);
    }

    @Transactional
    public List<Map<String, Object>> getEquipment(String status) {
        String sql = "SELECT e.* FROM Equipment e WHERE 1=1";
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (status != null && !status.isBlank()) {
            sql += " AND e.Status = :status";
            params.addValue("status", status);
        }
        sql += " ORDER BY e.EquipmentName";
        return jdbc.queryForList(sql, params);
    }

    @Transactional
    public Integer createEquipment(Map<String, Object> request) {
        Integer equipmentId = nextId("Equipment", "EquipmentId");
        String sql = "INSERT INTO Equipment (EquipmentId, EquipmentName, Category, Description, Status) "
                + "VALUES (:equipmentId, :equipmentName, :category, :description, :status)";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("equipmentId", equipmentId);
        params.addValue("equipmentName", getString(request, "equipmentName"));
        params.addValue("category", getString(request, "category"));
        params.addValue("description", getString(request, "description"));
        params.addValue("status", defaultIfBlank(getString(request, "status"), "ACTIVE"));
        jdbc.update(sql, params);
        auditLogWriter.log("EQUIPMENT_CREATE", "EQUIPMENT", equipmentId, null, getEquipmentById(equipmentId));
        return equipmentId;
    }

    @Transactional
    public void updateEquipment(Integer equipmentId, Map<String, Object> request) {
        Map<String, Object> oldValues = getEquipmentById(equipmentId);
        String sql = "UPDATE Equipment SET EquipmentName = COALESCE(:equipmentName, EquipmentName), "
                + "Category = COALESCE(:category, Category), Description = COALESCE(:description, Description), "
                + "Status = COALESCE(:status, Status) WHERE EquipmentId = :equipmentId";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("equipmentId", equipmentId);
        params.addValue("equipmentName", getString(request, "equipmentName"));
        params.addValue("category", getString(request, "category"));
        params.addValue("description", getString(request, "description"));
        params.addValue("status", getString(request, "status"));
        int updated = jdbc.update(sql, params);
        if (updated == 0) {
            throw new ResourceNotFoundException("Equipment not found: " + equipmentId);
        }
        auditLogWriter.log("EQUIPMENT_UPDATE", "EQUIPMENT", equipmentId, oldValues, getEquipmentById(equipmentId));
    }

    @Transactional
    public void assignEquipmentToRoom(Integer equipmentId, Map<String, Object> request) {
        getEquipmentById(equipmentId);
        Integer roomId = getInteger(request, "roomId");
        Integer quantity = getInteger(request, "quantity");

        String existsSql = "SELECT COUNT(*) FROM Room_Equipment WHERE RoomId = :roomId AND EquipmentId = :equipmentId";
        MapSqlParameterSource checkParams = new MapSqlParameterSource();
        checkParams.addValue("roomId", roomId);
        checkParams.addValue("equipmentId", equipmentId);
        Integer count = jdbc.queryForObject(existsSql, checkParams, Integer.class);

        if (count != null && count > 0) {
            String updateSql = "UPDATE Room_Equipment SET Quantity = :quantity, IsActive = 'Y', Notes = :notes "
                    + "WHERE RoomId = :roomId AND EquipmentId = :equipmentId";
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("roomId", roomId);
            params.addValue("equipmentId", equipmentId);
            params.addValue("quantity", quantity);
            params.addValue("notes", getString(request, "notes"));
            jdbc.update(updateSql, params);
        } else {
            String insertSql = "INSERT INTO Room_Equipment (RoomId, EquipmentId, Quantity, IsActive, Notes) "
                    + "VALUES (:roomId, :equipmentId, :quantity, 'Y', :notes)";
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("roomId", roomId);
            params.addValue("equipmentId", equipmentId);
            params.addValue("quantity", quantity);
            params.addValue("notes", getString(request, "notes"));
            jdbc.update(insertSql, params);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("roomId", roomId);
        payload.put("quantity", quantity);
        payload.put("notes", getString(request, "notes"));
        auditLogWriter.log("EQUIPMENT_ASSIGN", "EQUIPMENT", equipmentId, null, payload);
    }

    @Transactional
    public void retireEquipment(Integer equipmentId, Map<String, Object> request) {
        Map<String, Object> oldValues = getEquipmentById(equipmentId);
        String sql = "UPDATE Equipment SET Status = :status WHERE EquipmentId = :equipmentId";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("equipmentId", equipmentId);
        params.addValue("status", defaultIfBlank(getString(request, "status"), "RETIRED"));
        int updated = jdbc.update(sql, params);
        if (updated == 0) {
            throw new ResourceNotFoundException("Equipment not found: " + equipmentId);
        }

        String disableRoomMappings = "UPDATE Room_Equipment SET IsActive = 'N' WHERE EquipmentId = :equipmentId";
        jdbc.update(disableRoomMappings, new MapSqlParameterSource("equipmentId", equipmentId));
        auditLogWriter.log("EQUIPMENT_RETIRE", "EQUIPMENT", equipmentId, oldValues, getEquipmentById(equipmentId));
    }

    private void assignRole(Integer employeeId, Integer roleId) {
        Map<String, Object> oldValues = getUserById(employeeId);

        String existsSql = "SELECT COUNT(*) FROM Employee_Roles WHERE EmployeeId = :employeeId AND RoleId = :roleId";
        MapSqlParameterSource existsParams = new MapSqlParameterSource();
        existsParams.addValue("employeeId", employeeId);
        existsParams.addValue("roleId", roleId);
        Integer exists = jdbc.queryForObject(existsSql, existsParams, Integer.class);

        if (exists != null && exists > 0) {
            String sql = "UPDATE Employee_Roles SET IsActive = 'Y', AssignedAt = NOW() WHERE EmployeeId = :employeeId AND RoleId = :roleId";
            jdbc.update(sql, existsParams);
        } else {
            String sql = "INSERT INTO Employee_Roles (EmployeeId, RoleId, AssignedAt, IsActive) "
                    + "VALUES (:employeeId, :roleId, NOW(), 'Y')";
            jdbc.update(sql, existsParams);
        }

        Map<String, Object> details = new HashMap<>();
        details.put("employeeId", employeeId);
        details.put("roleId", roleId);
        auditLogWriter.log("ROLE_ASSIGNMENT_UPDATE", "EMPLOYEE", employeeId, oldValues, details);
    }

    private Integer resolveRoleId(Map<String, Object> request) {
        Integer roleId = getInteger(request, "roleId");
        if (roleId != null) {
            return roleId;
        }
        String roleName = getString(request, "roleName");
        if (roleName == null || roleName.isBlank()) {
            return null;
        }

        String sql = "SELECT RoleId FROM Roles WHERE UPPER(RoleName) = :roleName";
        return jdbc.query(sql, new MapSqlParameterSource("roleName", roleName.toUpperCase()),
                rs -> rs.next() ? rs.getInt("RoleId") : null);
    }

    private Map<String, Object> getUserById(Integer employeeId) {
        String sql = "SELECT e.id AS EmployeeId, e.first_name AS FirstName, e.last_name AS LastName, e.email AS Email, "
                + "e.title AS Title, e.country AS Country, e.city AS City, e.location AS LocationId, "
                + "uc.CredentialId, uc.IsActive AS CredentialActive "
                + "FROM Employee e LEFT JOIN User_Credentials uc ON uc.EmployeeId = e.id "
                + "WHERE e.id = :employeeId";
        List<Map<String, Object>> rows = jdbc.queryForList(sql, new MapSqlParameterSource("employeeId", employeeId));
        if (rows.isEmpty()) {
            throw new ResourceNotFoundException("User not found: " + employeeId);
        }
        return rows.get(0);
    }

    private Map<String, Object> getEquipmentById(Integer equipmentId) {
        String sql = "SELECT e.* FROM Equipment e WHERE e.EquipmentId = :equipmentId";
        List<Map<String, Object>> rows = jdbc.queryForList(sql, new MapSqlParameterSource("equipmentId", equipmentId));
        if (rows.isEmpty()) {
            throw new ResourceNotFoundException("Equipment not found: " + equipmentId);
        }
        return rows.get(0);
    }

    private Integer nextId(String table, String column) {
        String sql = "SELECT COALESCE(MAX(" + column + "), 0) + 1 FROM " + table;
        return jdbc.queryForObject(sql, new MapSqlParameterSource(), Integer.class);
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

    private String defaultIfBlank(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }
}
