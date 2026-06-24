package com.buzzmeet.dto.response;

public record TimeZoneResponse(
	Integer timeZoneId,
	String zoneName,
	Integer gmtOffsetMinutes,
	String isDstSupported,
	String isActive
) {
}