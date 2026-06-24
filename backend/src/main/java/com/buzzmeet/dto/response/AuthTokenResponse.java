package com.buzzmeet.dto.response;

import java.util.Set;

public record AuthTokenResponse(
	String accessToken,
	String tokenType,
	long expiresIn,
	Integer employeeId,
	String email,
	Set<String> roles
) {
}