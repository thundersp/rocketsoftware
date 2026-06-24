package com.buzzmeet.dto.response;

public record RoomTypeResponse(
	Integer roomTypeId,
	String typeName,
	String description,
	String isBookable,
	String isVideoEnabled,
	String requiresApproval
) {
}