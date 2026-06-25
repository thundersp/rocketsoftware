package com.buzzmeet.security;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
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
	private final Set<String> roleNames;
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
		this.roleNames = employee.getEmployeeRoles().stream()
			.filter(role -> "Y".equalsIgnoreCase(role.getIsActive()))
			.map(role -> role.getRole().getRoleName())
			.map(name -> name.toUpperCase(Locale.ROOT))
			.collect(Collectors.toUnmodifiableSet());

		Set<String> permissionNames = new LinkedHashSet<>();
		for (String roleName : roleNames) {
			permissionNames.addAll(mapRoleToPermissions(roleName));
		}

		Set<GrantedAuthority> mappedAuthorities = new LinkedHashSet<>();
		for (String roleName : roleNames) {
			mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_" + roleName));
		}
		for (String permissionName : permissionNames) {
			mappedAuthorities.add(new SimpleGrantedAuthority(permissionName));
		}
		this.authorities = Set.copyOf(mappedAuthorities);
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
		return roleNames;
	}

	private Set<String> mapRoleToPermissions(String roleName) {
		Set<String> permissions = new LinkedHashSet<>();
		if ("EMPLOYEE".equals(roleName) || "ORGANIZER".equals(roleName) || "APPROVER".equals(roleName)
				|| "MANAGER".equals(roleName) || "ADMIN".equals(roleName)) {
			permissions.add(Permissions.MEETING_CREATE);
			permissions.add(Permissions.MEETING_VIEW);
			permissions.add(Permissions.MEETING_BOOK);
			permissions.add(Permissions.MEETING_PARTICIPANTS_UPDATE);
		}
		if ("APPROVER".equals(roleName) || "MANAGER".equals(roleName) || "ADMIN".equals(roleName)) {
			permissions.add(Permissions.MEETING_OVERRIDE);
		}
		if ("ADMIN".equals(roleName)) {
			permissions.add(Permissions.USER_MANAGE);
			permissions.add(Permissions.ROOM_MANAGE);
			permissions.add(Permissions.EQUIPMENT_MANAGE);
		}
		return Set.copyOf(permissions);
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