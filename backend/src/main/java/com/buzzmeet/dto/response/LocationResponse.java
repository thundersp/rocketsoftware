package com.buzzmeet.dto.response;

public record LocationResponse(
	Integer id,
	String phone,
	String street,
	String country,
	String city
) {
}