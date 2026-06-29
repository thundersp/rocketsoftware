package com.buzzmeet.controller;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.buzzmeet.service.AvailabilityService;

@RestController
@RequestMapping("/api")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    /**
     * GET /api/employees/{employeeId}/availability?startUtc=&endUtc=
     * Returns all meetings for the given employee that overlap the requested time range.
     */
    @PreAuthorize("hasAuthority('meeting:view')")
    @GetMapping("/employees/{employeeId}/availability")
    public ResponseEntity<List<Map<String, Object>>> getEmployeeAvailability(
            @PathVariable Integer employeeId,
            @RequestParam String startUtc,
            @RequestParam String endUtc) {
        return ResponseEntity.ok(availabilityService.getEmployeeAvailability(employeeId, startUtc, endUtc));
    }

    /**
     * GET /api/employees/availability-status?employeeIds=1,2,3&atUtc=2026-06-30T10:00:00Z
     * Returns current availability status (AVAILABLE / IN_A_MEETING) for each requested employee.
     * atUtc defaults to now if omitted.
     */
    @PreAuthorize("hasAuthority('meeting:view')")
    @GetMapping("/employees/availability-status")
    public ResponseEntity<List<Map<String, Object>>> getEmployeesAvailabilityStatus(
            @RequestParam String employeeIds,
            @RequestParam(required = false) String atUtc) {
        List<Integer> ids = Arrays.stream(employeeIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .collect(Collectors.toList());
        String time = (atUtc != null && !atUtc.isBlank()) ? atUtc : Instant.now().toString();
        return ResponseEntity.ok(availabilityService.getEmployeesAvailabilityStatus(ids, time));
    }
}
