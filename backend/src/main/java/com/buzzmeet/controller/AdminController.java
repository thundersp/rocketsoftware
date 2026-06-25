package com.buzzmeet.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.buzzmeet.service.AdminService;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PreAuthorize("hasAuthority('user:manage')")
    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getUsers(
            @RequestParam(required = false) Boolean activeOnly,
            @RequestParam(required = false) String roleName) {
        return ResponseEntity.ok(adminService.getUsers(activeOnly, roleName));
    }

    @PreAuthorize("hasAuthority('user:manage')")
    @PostMapping("/users")
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody Map<String, Object> request) {
        Integer employeeId = adminService.createUser(request);
        return ResponseEntity.ok(Map.of("employeeId", employeeId));
    }

    @PreAuthorize("hasAuthority('user:manage')")
    @PutMapping("/users/{employeeId}")
    public ResponseEntity<Void> updateUser(@PathVariable Integer employeeId,
            @RequestBody Map<String, Object> request) {
        adminService.updateUser(employeeId, request);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAuthority('user:manage')")
    @PostMapping("/users/{employeeId}/deactivate")
    public ResponseEntity<Void> deactivateUser(@PathVariable Integer employeeId) {
        adminService.deactivateUser(employeeId);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAuthority('user:manage')")
    @PostMapping("/users/{employeeId}/roles")
    public ResponseEntity<Void> assignRole(@PathVariable Integer employeeId,
            @RequestBody Map<String, Object> request) {
        adminService.assignRole(employeeId, request);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAuthority('equipment:manage')")
    @GetMapping("/equipment")
    public ResponseEntity<List<Map<String, Object>>> getEquipment(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(adminService.getEquipment(status));
    }

    @PreAuthorize("hasAuthority('equipment:manage')")
    @PostMapping("/equipment")
    public ResponseEntity<Map<String, Object>> createEquipment(@RequestBody Map<String, Object> request) {
        Integer equipmentId = adminService.createEquipment(request);
        return ResponseEntity.ok(Map.of("equipmentId", equipmentId));
    }

    @PreAuthorize("hasAuthority('equipment:manage')")
    @PutMapping("/equipment/{equipmentId}")
    public ResponseEntity<Void> updateEquipment(@PathVariable Integer equipmentId,
            @RequestBody Map<String, Object> request) {
        adminService.updateEquipment(equipmentId, request);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAuthority('equipment:manage')")
    @PostMapping("/equipment/{equipmentId}/assign-room")
    public ResponseEntity<Void> assignEquipmentToRoom(@PathVariable Integer equipmentId,
            @RequestBody Map<String, Object> request) {
        adminService.assignEquipmentToRoom(equipmentId, request);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAuthority('equipment:manage')")
    @PostMapping("/equipment/{equipmentId}/retire")
    public ResponseEntity<Void> retireEquipment(@PathVariable Integer equipmentId,
            @RequestBody(required = false) Map<String, Object> request) {
        adminService.retireEquipment(equipmentId, request != null ? request : Map.of());
        return ResponseEntity.ok().build();
    }
}
