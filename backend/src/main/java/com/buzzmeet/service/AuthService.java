package com.buzzmeet.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buzzmeet.dto.request.AuthLoginRequest;
import com.buzzmeet.dto.response.AuthTokenResponse;
import com.buzzmeet.dto.response.CurrentUserResponse;
import com.buzzmeet.security.ApplicationUser;
import com.buzzmeet.security.JwtService;

@Service
public class AuthService {

	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;

	public AuthService(AuthenticationManager authenticationManager, JwtService jwtService) {
		this.authenticationManager = authenticationManager;
		this.jwtService = jwtService;
	}

	@Transactional(readOnly = true)
	public AuthTokenResponse login(AuthLoginRequest request) {
		Authentication authentication = authenticationManager.authenticate(
			new UsernamePasswordAuthenticationToken(request.email(), request.password()));
		ApplicationUser user = (ApplicationUser) authentication.getPrincipal();
		return new AuthTokenResponse(
			jwtService.generateToken(user),
			"Bearer",
			3600000L,
			user.getEmployeeId(),
			user.getEmail(),
			user.roleNames());
	}

	@Transactional(readOnly = true)
	public CurrentUserResponse currentUser(ApplicationUser user) {
		return new CurrentUserResponse(
			user.getEmployeeId(),
			user.getEmail(),
			user.getFirstName(),
			user.getLastName(),
			user.getTitle(),
			user.roleNames());
	}
}