package com.buzzmeet.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Employee_Roles")
@Getter
@Setter
@NoArgsConstructor
public class EmployeeRole {

	@EmbeddedId
	private EmployeeRoleId id;

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("employeeId")
	@JoinColumn(name = "EmployeeId")
	private Employee employee;

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("roleId")
	@JoinColumn(name = "RoleId")
	private Role role;

	@Column(name = "AssignedAt")
	private LocalDateTime assignedAt;

	@Column(name = "IsActive", length = 1)
	private String isActive;
}