package com.buzzmeet.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AuthSignupRequest(
        @NotBlank @Size(max = 50) String firstName,
        @NotBlank @Size(max = 50) String lastName,
        @Email @NotBlank @Size(max = 50) String email,
        @NotBlank @Size(min = 8, max = 128) String password,
        @NotNull Integer locationId,
        @NotBlank String role,
        @Size(max = 11) String title,
        @Size(max = 50) String country,
        @Size(max = 50) String city) {
}