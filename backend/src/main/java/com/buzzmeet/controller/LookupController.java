package com.buzzmeet.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.buzzmeet.dto.response.BuildingResponse;
import com.buzzmeet.dto.response.EmployeeLookupResponse;
import com.buzzmeet.dto.response.LocationResponse;
import com.buzzmeet.dto.response.RoomTypeResponse;
import com.buzzmeet.dto.response.TimeZoneResponse;
import com.buzzmeet.service.LookupService;

@RestController
public class LookupController {

	private final LookupService lookupService;

	public LookupController(LookupService lookupService) {
		this.lookupService = lookupService;
	}

	@GetMapping("/api/locations")
	public ResponseEntity<List<LocationResponse>> getLocations() {
		return ResponseEntity.ok(lookupService.getLocations());
	}

	@GetMapping("/api/buildings")
	public ResponseEntity<List<BuildingResponse>> getBuildings(@RequestParam(required = false) Integer locationId) {
		return ResponseEntity.ok(lookupService.getBuildings(locationId));
	}

	@GetMapping("/api/room-types")
	public ResponseEntity<List<RoomTypeResponse>> getRoomTypes() {
		return ResponseEntity.ok(lookupService.getRoomTypes());
	}

	@GetMapping("/api/time-zones")
	public ResponseEntity<List<TimeZoneResponse>> getTimeZones() {
		return ResponseEntity.ok(lookupService.getTimeZones());
	}

	@GetMapping("/api/employees")
	public ResponseEntity<List<EmployeeLookupResponse>> getEmployees(
			@RequestParam(required = false) Integer locationId,
			@RequestParam(required = false) String title) {
		return ResponseEntity.ok(lookupService.getEmployees(locationId, title));
	}
}