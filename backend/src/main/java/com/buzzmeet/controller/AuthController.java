package com.buzzmeet.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.buzzmeet.dto.request.AuthLoginRequest;
import com.buzzmeet.dto.response.AuthTokenResponse;
import com.buzzmeet.dto.response.CurrentUserResponse;
import com.buzzmeet.security.ApplicationUser;
import com.buzzmeet.service.AuthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/login")
	public ResponseEntity<AuthTokenResponse> login(@Valid @RequestBody AuthLoginRequest request) {
		return ResponseEntity.ok(authService.login(request));
	}

	@GetMapping("/me")
	public ResponseEntity<CurrentUserResponse> currentUser(@AuthenticationPrincipal ApplicationUser user) {
		return ResponseEntity.ok(authService.currentUser(user));
	}
}