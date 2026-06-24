package com.buzzmeet.security;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.buzzmeet.model.Employee;
import com.buzzmeet.model.UserCredential;

public class ApplicationUser implements UserDetails {

	private final Integer employeeId;
	private final String email;
	private final String password;
	private final String firstName;
	private final String lastName;
	private final String title;
	private final boolean enabled;
	private final Set<GrantedAuthority> authorities;

	public ApplicationUser(UserCredential credential) {
		Employee employee = credential.getEmployee();
		this.employeeId = employee.getId();
		this.email = employee.getEmail();
		this.password = credential.getPasswordHash();
		this.firstName = employee.getFirstName();
		this.lastName = employee.getLastName();
		this.title = employee.getTitle();
		this.enabled = "Y".equalsIgnoreCase(credential.getIsActive());
		this.authorities = employee.getEmployeeRoles().stream()
			.filter(role -> "Y".equalsIgnoreCase(role.getIsActive()))
			.map(role -> role.getRole().getRoleName())
			.map(String::toUpperCase)
			.map(name -> new SimpleGrantedAuthority("ROLE_" + name))
			.collect(Collectors.toUnmodifiableSet());
	}

	public Integer getEmployeeId() {
		return employeeId;
	}

	public String getEmail() {
		return email;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public String getTitle() {
		return title;
	}

	public Set<String> roleNames() {
		return authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toUnmodifiableSet());
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public String getUsername() {
		return email;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}
}