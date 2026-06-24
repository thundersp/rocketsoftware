package com.buzzmeet.dto.response;

public record BuildingResponse(
	Integer buildingId,
	Integer locationId,
	String locationCity,
	String buildingName,
	String addressLine1,
	String addressLine2,
	String status
) {
}