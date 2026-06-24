package com.buzzmeet.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import com.buzzmeet.exception.ResourceNotFoundException;

@Service
public class RoomService {

    private final NamedParameterJdbcTemplate jdbc;

    public RoomService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> getRooms(Integer locationId, Integer buildingId,
            Integer roomTypeId, Integer capacity, String status, Boolean isVideoRoom) {
        String sql = "SELECT r.RoomId, r.BuildingId, r.RoomTypeId, r.RoomCode, r.RoomName, r.Capacity, r.Floor, r.IsVideoRoom, r.DialInInfo, r.Status, r.Notes, "
                + "b.BuildingName, b.LocationId, rt.TypeName AS RoomTypeName "
                + "FROM Rooms r "
                + "LEFT JOIN Buildings b ON b.BuildingId = r.BuildingId "
                + "LEFT JOIN Room_Types rt ON rt.RoomTypeId = r.RoomTypeId "
                + "WHERE 1=1";
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (locationId != null) {
            sql += " AND b.LocationId = :locationId";
            params.addValue("locationId", locationId);
        }
        if (buildingId != null) {
            sql += " AND r.BuildingId = :buildingId";
            params.addValue("buildingId", buildingId);
        }
        if (roomTypeId != null) {
            sql += " AND r.RoomTypeId = :roomTypeId";
            params.addValue("roomTypeId", roomTypeId);
        }
        if (capacity != null) {
            sql += " AND r.Capacity >= :capacity";
            params.addValue("capacity", capacity);
        }
        if (status != null) {
            sql += " AND r.Status = :status";
            params.addValue("status", status);
        }
        if (isVideoRoom != null) {
            sql += " AND r.IsVideoRoom = :isVideoRoom";
            params.addValue("isVideoRoom", isVideoRoom ? "Y" : "N");
        }
        sql += " ORDER BY r.RoomCode";

        return jdbc.queryForList(sql, params);
    }

    public Map<String, Object> getRoom(Integer roomId) {
        MapSqlParameterSource params = new MapSqlParameterSource("roomId", roomId);
        String sql = "SELECT r.RoomId, r.BuildingId, r.RoomTypeId, r.RoomCode, r.RoomName, r.Capacity, r.Floor, r.IsVideoRoom, r.DialInInfo, r.Status, r.Notes, "
                + "b.BuildingName, b.LocationId, rt.TypeName AS RoomTypeName "
                + "FROM Rooms r "
                + "LEFT JOIN Buildings b ON b.BuildingId = r.BuildingId "
                + "LEFT JOIN Room_Types rt ON rt.RoomTypeId = r.RoomTypeId "
                + "WHERE r.RoomId = :roomId";
        List<Map<String, Object>> rows = jdbc.queryForList(sql, params);
        if (rows.isEmpty()) {
            throw new ResourceNotFoundException("Room not found: " + roomId);
        }
        return rows.get(0);
    }

    public void createRoom(Map<String, Object> request) {
        Integer roomId = nextId("Rooms", "RoomId");
        String sql = "INSERT INTO Rooms (RoomId, BuildingId, RoomTypeId, RoomCode, RoomName, Capacity, Floor, IsVideoRoom, DialInInfo, Status, Notes) "
                + "VALUES (:roomId, :buildingId, :roomTypeId, :roomCode, :roomName, :capacity, :floor, :isVideoRoom, :dialInInfo, :status, :notes)";
        MapSqlParameterSource params = buildRoomParameters(roomId, request);
        jdbc.update(sql, params);
    }

    public void updateRoom(Integer roomId, Map<String, Object> request) {
        Map<String, Object> existing = getRoom(roomId);
        Map<String, Object> merged = new HashMap<>(existing);
        merged.putAll(request);
        String sql = "UPDATE Rooms SET BuildingId = :buildingId, RoomTypeId = :roomTypeId, RoomCode = :roomCode, RoomName = :roomName, Capacity = :capacity, Floor = :floor, IsVideoRoom = :isVideoRoom, DialInInfo = :dialInInfo, Status = :status, Notes = :notes WHERE RoomId = :roomId";
        MapSqlParameterSource params = buildRoomParameters(roomId, merged);
        jdbc.update(sql, params);
    }

    public void deleteRoom(Integer roomId) {
        String sql = "DELETE FROM Rooms WHERE RoomId = :roomId";
        MapSqlParameterSource params = new MapSqlParameterSource("roomId", roomId);
        int updated = jdbc.update(sql, params);
        if (updated == 0) {
            throw new ResourceNotFoundException("Room not found: " + roomId);
        }
    }

    public List<Map<String, Object>> getRoomAvailability(Integer roomId, String startUtc, String endUtc) {
        String sql = "SELECT ma.MeetingAssignmentId, ma.AssignmentId, ma.StartUTC, ma.EndUTC, ma.Status FROM Meeting_Assignments ma "
                + "WHERE ma.RoomId = :roomId "
                + "AND ma.StartUTC < :endUtc AND ma.EndUTC > :startUtc";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("roomId", roomId);
        params.addValue("startUtc", startUtc);
        params.addValue("endUtc", endUtc);
        return jdbc.queryForList(sql, params);
    }

    private Integer nextId(String table, String column) {
        String sql = "SELECT COALESCE(MAX(" + column + "), 0) + 1 FROM " + table;
        return jdbc.queryForObject(sql, new MapSqlParameterSource(), Integer.class);
    }

    private MapSqlParameterSource buildRoomParameters(Integer roomId, Map<String, Object> request) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("roomId", roomId);
        params.addValue("buildingId", getInteger(request, "buildingId"));
        params.addValue("roomTypeId", getInteger(request, "roomTypeId"));
        params.addValue("roomCode", getString(request, "roomCode"));
        params.addValue("roomName", getString(request, "roomName"));
        params.addValue("capacity", getInteger(request, "capacity"));
        params.addValue("floor", getInteger(request, "floor"));
        params.addValue("isVideoRoom", toYesNo(request.get("isVideoRoom")));
        params.addValue("dialInInfo", getString(request, "dialInInfo"));
        params.addValue("status", getString(request, "status"));
        params.addValue("notes", getString(request, "notes"));
        return params;
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
        String text = String.valueOf(value).trim();
        if (text.isBlank()) {
            return null;
        }
        text = text.toUpperCase();
        return text.equals("Y") || text.equals("YES") || text.equals("TRUE") ? "Y" : "N";
    }
}
