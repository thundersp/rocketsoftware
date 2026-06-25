package com.buzzmeet.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.buzzmeet.service.RoomService;

@RestController
@RequestMapping("/api")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PreAuthorize("hasAuthority('meeting:view')")
    @GetMapping("/rooms")
    public ResponseEntity<List<Map<String, Object>>> getRooms(
            @RequestParam(required = false) Integer locationId,
            @RequestParam(required = false) Integer buildingId,
            @RequestParam(required = false) Integer roomTypeId,
            @RequestParam(required = false) Integer capacity,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean isVideoRoom) {
        return ResponseEntity.ok(roomService.getRooms(locationId, buildingId, roomTypeId, capacity, status,
                isVideoRoom));
    }

    @PreAuthorize("hasAuthority('meeting:view')")
    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<Map<String, Object>> getRoom(@PathVariable Integer roomId) {
        return ResponseEntity.ok(roomService.getRoom(roomId));
    }

    @PreAuthorize("hasAuthority('room:manage')")
    @PostMapping("/rooms")
    public ResponseEntity<Void> createRoom(@RequestBody Map<String, Object> request) {
        roomService.createRoom(request);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAuthority('room:manage')")
    @PutMapping("/rooms/{roomId}")
    public ResponseEntity<Void> updateRoom(@PathVariable Integer roomId,
            @RequestBody Map<String, Object> request) {
        roomService.updateRoom(roomId, request);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAuthority('room:manage')")
    @DeleteMapping("/rooms/{roomId}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Integer roomId) {
        roomService.deleteRoom(roomId);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAuthority('meeting:view')")
    @GetMapping("/rooms/{roomId}/availability")
    public ResponseEntity<List<Map<String, Object>>> getRoomAvailability(@PathVariable Integer roomId,
            @RequestParam(required = false) String startUtc,
            @RequestParam(required = false) String endUtc) {
        return ResponseEntity.ok(roomService.getRoomAvailability(roomId, startUtc, endUtc));
    }
}
