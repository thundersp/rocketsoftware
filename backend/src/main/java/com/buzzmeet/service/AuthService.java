package com.buzzmeet.service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buzzmeet.dto.request.AuthLoginRequest;
import com.buzzmeet.dto.request.AuthSignupRequest;
import com.buzzmeet.dto.response.AuthTokenResponse;
import com.buzzmeet.dto.response.CurrentUserResponse;
import com.buzzmeet.model.Employee;
import com.buzzmeet.model.EmployeeRole;
import com.buzzmeet.model.EmployeeRoleId;
import com.buzzmeet.model.Location;
import com.buzzmeet.model.Role;
import com.buzzmeet.model.UserCredential;
import com.buzzmeet.repository.EmployeeRepository;
import com.buzzmeet.repository.EmployeeRoleRepository;
import com.buzzmeet.repository.LocationRepository;
import com.buzzmeet.repository.RoleRepository;
import com.buzzmeet.repository.UserCredentialRepository;
import com.buzzmeet.security.ApplicationUser;
import com.buzzmeet.security.JwtService;

@Service
public class AuthService {
	private static final Set<String> SIGNUP_ROLES = Set.of("EMPLOYEE", "MANAGER", "ADMIN");

	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;
    private final EmployeeRepository employeeRepository;
    private final LocationRepository locationRepository;
    private final RoleRepository roleRepository;
    private final EmployeeRoleRepository employeeRoleRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final NamedParameterJdbcTemplate jdbc;

	public AuthService(AuthenticationManager authenticationManager, JwtService jwtService,
            EmployeeRepository employeeRepository,
            LocationRepository locationRepository,
            RoleRepository roleRepository,
            EmployeeRoleRepository employeeRoleRepository,
            UserCredentialRepository userCredentialRepository,
            PasswordEncoder passwordEncoder,
            NamedParameterJdbcTemplate jdbc) {
		this.authenticationManager = authenticationManager;
		this.jwtService = jwtService;
		this.employeeRepository = employeeRepository;
		this.locationRepository = locationRepository;
		this.roleRepository = roleRepository;
		this.employeeRoleRepository = employeeRoleRepository;
		this.userCredentialRepository = userCredentialRepository;
		this.passwordEncoder = passwordEncoder;
		this.jdbc = jdbc;
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

	@Transactional
	public AuthTokenResponse signup(AuthSignupRequest request) {
		String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
		if (employeeRepository.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
			throw new IllegalArgumentException("An account already exists for this email");
		}

		String normalizedRole = request.role().trim().toUpperCase(Locale.ROOT);
		if (!SIGNUP_ROLES.contains(normalizedRole)) {
			throw new IllegalArgumentException("Invalid role. Allowed roles: EMPLOYEE, MANAGER, ADMIN");
		}

		Location location = locationRepository.findById(request.locationId())
			.orElseThrow(() -> new IllegalArgumentException("Invalid locationId: " + request.locationId()));

		Role role = roleRepository.findByRoleNameIgnoreCase(normalizedRole)
			.orElseThrow(() -> new IllegalArgumentException("Role not configured in database: " + normalizedRole));

		Integer employeeId = nextId("Employee", "id");
		Employee employee = new Employee();
		employee.setId(employeeId);
		employee.setFirstName(request.firstName().trim());
		employee.setLastName(request.lastName().trim());
		employee.setEmail(normalizedEmail);
		employee.setTitle(defaultTitle(request.title(), normalizedRole));
		employee.setCountry(defaultFromLocation(request.country(), location.getCountry()));
		employee.setCity(defaultFromLocation(request.city(), location.getCity()));
		employee.setLocation(location);
		employeeRepository.save(employee);

		UserCredential credential = new UserCredential();
		credential.setCredentialId(nextId("User_Credentials", "CredentialId"));
		credential.setEmployee(employee);
		credential.setPasswordHash(passwordEncoder.encode(request.password()));
		credential.setIsActive("Y");
		credential.setFailedLoginAttempts(0);
		credential.setCreatedAt(LocalDateTime.now());
		credential.setUpdatedAt(LocalDateTime.now());
		userCredentialRepository.save(credential);

		EmployeeRole employeeRole = new EmployeeRole();
		EmployeeRoleId employeeRoleId = new EmployeeRoleId();
		employeeRoleId.setEmployeeId(employeeId);
		employeeRoleId.setRoleId(role.getRoleId());
		employeeRole.setId(employeeRoleId);
		employeeRole.setEmployee(employee);
		employeeRole.setRole(role);
		employeeRole.setAssignedAt(LocalDateTime.now());
		employeeRole.setIsActive("Y");
		employeeRoleRepository.save(employeeRole);

		return login(new AuthLoginRequest(normalizedEmail, request.password()));
	}

	private Integer nextId(String table, String column) {
		String sql = "SELECT COALESCE(MAX(" + column + "), 0) + 1 FROM " + table;
		return jdbc.queryForObject(sql, new MapSqlParameterSource(), Integer.class);
	}

	private String defaultTitle(String requestedTitle, String role) {
		if (requestedTitle != null && !requestedTitle.isBlank()) {
			return requestedTitle.trim();
		}
		if ("MANAGER".equals(role)) {
			return "Manager";
		}
		if ("ADMIN".equals(role)) {
			return "Admin";
		}
		return "Employee";
	}

	private String defaultFromLocation(String value, String fallback) {
		if (value != null && !value.isBlank()) {
			return value.trim();
		}
		return fallback;
	}
}