package com.buzzmeet.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.buzzmeet.repository.UserCredentialRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

	private final UserCredentialRepository userCredentialRepository;

	public CustomUserDetailsService(UserCredentialRepository userCredentialRepository) {
		this.userCredentialRepository = userCredentialRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String username) {
		return userCredentialRepository.findByEmployeeEmailIgnoreCase(username)
			.map(ApplicationUser::new)
			.orElseThrow(() -> new UsernameNotFoundException("No user found for email: " + username));
	}
}