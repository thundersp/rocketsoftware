package com.buzzmeet.dto.response;

import java.util.Set;

public record CurrentUserResponse(
	Integer employeeId,
	String email,
	String firstName,
	String lastName,
	String title,
	Set<String> roles
) {
}