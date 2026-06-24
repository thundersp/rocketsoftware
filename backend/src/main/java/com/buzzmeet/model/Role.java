package com.buzzmeet.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Roles")
@Getter
@Setter
@NoArgsConstructor
public class Role {

	@Id
	@Column(name = "RoleId")
	private Integer roleId;

	@Column(name = "RoleName", length = 50, nullable = false)
	private String roleName;

	@Column(name = "Description", length = 255)
	private String description;
}