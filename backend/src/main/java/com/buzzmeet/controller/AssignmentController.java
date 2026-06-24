package com.buzzmeet.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.buzzmeet.service.AssignmentService;

@RestController
@RequestMapping("/api")
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @PostMapping("/assignments")
    public ResponseEntity<Void> createAssignment(@RequestBody Map<String, Object> request) {
        assignmentService.createAssignment(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/assignments")
    public ResponseEntity<List<Map<String, Object>>> getAssignments(
            @RequestParam(required = false) Integer organizerId,
            @RequestParam(required = false) Integer participantEmployeeId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer locationId,
            @RequestParam(required = false) Integer roomId,
            @RequestParam(required = false) String fromUtc,
            @RequestParam(required = false) String toUtc,
            @RequestParam(required = false) String priority) {
        return ResponseEntity.ok(assignmentService.getAssignments(organizerId, participantEmployeeId, status,
                locationId, roomId, fromUtc, toUtc, priority));
    }

    @GetMapping("/assignments/{assignmentId}")
    public ResponseEntity<Map<String, Object>> getAssignment(@PathVariable Integer assignmentId) {
        return ResponseEntity.ok(assignmentService.getAssignment(assignmentId));
    }

    @PutMapping("/assignments/{assignmentId}")
    public ResponseEntity<Void> updateAssignment(@PathVariable Integer assignmentId,
            @RequestBody Map<String, Object> request) {
        assignmentService.updateAssignment(assignmentId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/room-assignments")
    public ResponseEntity<List<Map<String, Object>>> getRoomAssignments(
            @RequestParam(required = false) Integer roomId,
            @RequestParam(required = false) Integer locationId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String fromUtc,
            @RequestParam(required = false) String toUtc) {
        return ResponseEntity.ok(assignmentService.getRoomAssignments(roomId, locationId, status, fromUtc, toUtc));
    }

    @GetMapping("/assignments/{assignmentId}/room-assignments")
    public ResponseEntity<List<Map<String, Object>>> getAssignmentRoomAssignments(@PathVariable Integer assignmentId) {
        return ResponseEntity.ok(assignmentService.getAssignmentRoomAssignments(assignmentId));
    }

    @PostMapping("/assignments/{assignmentId}/room-assignments")
    public ResponseEntity<Void> addRoomAssignment(@PathVariable Integer assignmentId,
            @RequestBody Map<String, Object> request) {
        assignmentService.addRoomAssignment(assignmentId, request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/room-assignments/{meetingAssignmentId}")
    public ResponseEntity<Void> updateRoomAssignment(@PathVariable Integer meetingAssignmentId,
            @RequestBody Map<String, Object> request) {
        assignmentService.updateRoomAssignment(meetingAssignmentId, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/assignments/{assignmentId}/room-assignments/{meetingAssignmentId}")
    public ResponseEntity<Void> deleteRoomAssignment(@PathVariable Integer assignmentId,
            @PathVariable Integer meetingAssignmentId) {
        assignmentService.deleteRoomAssignment(assignmentId, meetingAssignmentId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/assignments/{assignmentId}/video-reservations")
    public ResponseEntity<List<Map<String, Object>>> getAssignmentVideoReservations(@PathVariable Integer assignmentId) {
        return ResponseEntity.ok(assignmentService.getAssignmentVideoReservations(assignmentId));
    }

    @PostMapping("/assignments/{assignmentId}/video-reservations")
    public ResponseEntity<Void> addAssignmentVideoReservation(@PathVariable Integer assignmentId,
            @RequestBody Map<String, Object> request) {
        assignmentService.addAssignmentVideoReservation(assignmentId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/video-reservations/{videoReservationId}")
    public ResponseEntity<Map<String, Object>> getVideoReservation(@PathVariable Integer videoReservationId) {
        return ResponseEntity.ok(assignmentService.getVideoReservation(videoReservationId));
    }

    @PutMapping("/video-reservations/{videoReservationId}")
    public ResponseEntity<Void> updateVideoReservation(@PathVariable Integer videoReservationId,
            @RequestBody Map<String, Object> request) {
        assignmentService.updateVideoReservation(videoReservationId, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/video-reservations/{videoReservationId}")
    public ResponseEntity<Void> deleteVideoReservation(@PathVariable Integer videoReservationId) {
        assignmentService.deleteVideoReservation(videoReservationId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/assignments/{assignmentId}/cancel")
    public ResponseEntity<Void> cancelAssignment(@PathVariable Integer assignmentId,
            @RequestBody Map<String, Object> request) {
        assignmentService.cancelAssignment(assignmentId, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/assignments/{assignmentId}/override")
    public ResponseEntity<Void> overrideAssignment(@PathVariable Integer assignmentId,
            @RequestBody Map<String, Object> request) {
        assignmentService.overrideAssignment(assignmentId, request);
        return ResponseEntity.ok().build();
    }
}
