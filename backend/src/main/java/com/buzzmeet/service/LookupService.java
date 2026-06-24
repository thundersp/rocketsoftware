package com.buzzmeet.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buzzmeet.dto.response.BuildingResponse;
import com.buzzmeet.dto.response.EmployeeLookupResponse;
import com.buzzmeet.dto.response.LocationResponse;
import com.buzzmeet.dto.response.RoomTypeResponse;
import com.buzzmeet.dto.response.TimeZoneResponse;
import com.buzzmeet.repository.BuildingRepository;
import com.buzzmeet.repository.EmployeeRepository;
import com.buzzmeet.repository.LocationRepository;
import com.buzzmeet.repository.RoomTypeRepository;
import com.buzzmeet.repository.TimeZoneInfoRepository;

@Service
public class LookupService {

	private final LocationRepository locationRepository;
	private final BuildingRepository buildingRepository;
	private final RoomTypeRepository roomTypeRepository;
	private final TimeZoneInfoRepository timeZoneInfoRepository;
	private final EmployeeRepository employeeRepository;

	public LookupService(LocationRepository locationRepository,
			BuildingRepository buildingRepository,
			RoomTypeRepository roomTypeRepository,
			TimeZoneInfoRepository timeZoneInfoRepository,
			EmployeeRepository employeeRepository) {
		this.locationRepository = locationRepository;
		this.buildingRepository = buildingRepository;
		this.roomTypeRepository = roomTypeRepository;
		this.timeZoneInfoRepository = timeZoneInfoRepository;
		this.employeeRepository = employeeRepository;
	}

	@Transactional(readOnly = true)
	public List<LocationResponse> getLocations() {
		return locationRepository.findAll().stream()
			.map(location -> new LocationResponse(
				location.getId(),
				location.getPhone(),
				location.getStreet(),
				location.getCountry(),
				location.getCity()))
			.toList();
	}

	@Transactional(readOnly = true)
	public List<BuildingResponse> getBuildings(Integer locationId) {
		return (locationId == null ? buildingRepository.findAllByOrderByBuildingNameAsc()
			: buildingRepository.findByLocationIdOrderByBuildingNameAsc(locationId))
			.stream()
			.map(building -> new BuildingResponse(
				building.getBuildingId(),
				building.getLocation().getId(),
				building.getLocation().getCity(),
				building.getBuildingName(),
				building.getAddressLine1(),
				building.getAddressLine2(),
				building.getStatus()))
			.toList();
	}

	@Transactional(readOnly = true)
	public List<RoomTypeResponse> getRoomTypes() {
		return roomTypeRepository.findAllByOrderByTypeNameAsc().stream()
			.map(roomType -> new RoomTypeResponse(
				roomType.getRoomTypeId(),
				roomType.getTypeName(),
				roomType.getDescription(),
				roomType.getIsBookable(),
				roomType.getIsVideoEnabled(),
				roomType.getRequiresApproval()))
			.toList();
	}

	@Transactional(readOnly = true)
	public List<TimeZoneResponse> getTimeZones() {
		return timeZoneInfoRepository.findAllByOrderByZoneNameAsc().stream()
			.map(timeZone -> new TimeZoneResponse(
				timeZone.getTimeZoneId(),
				timeZone.getZoneName(),
				timeZone.getGmtOffsetMinutes(),
				timeZone.getIsDstSupported(),
				timeZone.getIsActive()))
			.toList();
	}

	@Transactional(readOnly = true)
	public List<EmployeeLookupResponse> getEmployees(Integer locationId, String title) {
		return fetchEmployees(locationId, title).stream()
			.map(employee -> new EmployeeLookupResponse(
				employee.getId(),
				employee.getFirstName(),
				employee.getLastName(),
				employee.getTitle(),
				employee.getEmail(),
				employee.getCountry(),
				employee.getCity(),
				employee.getLocation().getId()))
			.toList();
	}

	private List<com.buzzmeet.model.Employee> fetchEmployees(Integer locationId, String title) {
		if (locationId != null && title != null && !title.isBlank()) {
			return employeeRepository.findByLocationIdAndTitleIgnoreCaseOrderByFirstNameAscLastNameAsc(locationId, title);
		}
		if (locationId != null) {
			return employeeRepository.findByLocationIdOrderByFirstNameAscLastNameAsc(locationId);
		}
		if (title != null && !title.isBlank()) {
			return employeeRepository.findByTitleIgnoreCaseOrderByFirstNameAscLastNameAsc(title);
		}
		return employeeRepository.findAllByOrderByFirstNameAscLastNameAsc();
	}
}