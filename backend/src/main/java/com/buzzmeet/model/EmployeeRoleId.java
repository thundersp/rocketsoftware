package com.buzzmeet.model;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class EmployeeRoleId implements Serializable {

	private static final long serialVersionUID = 1L;

	@Column(name = "EmployeeId")
	private Integer employeeId;

	@Column(name = "RoleId")
	private Integer roleId;

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof EmployeeRoleId that)) {
			return false;
		}
		return Objects.equals(employeeId, that.employeeId)
			&& Objects.equals(roleId, that.roleId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(employeeId, roleId);
	}
}