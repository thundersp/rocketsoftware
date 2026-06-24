package com.buzzmeet.dto.response;

public record EmployeeLookupResponse(
	Integer id,
	String firstName,
	String lastName,
	String title,
	String email,
	String country,
	String city,
	Integer locationId
) {
}