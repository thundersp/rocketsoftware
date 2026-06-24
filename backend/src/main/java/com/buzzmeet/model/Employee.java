package com.buzzmeet.model;

import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Employee")
@Getter
@Setter
@NoArgsConstructor
public class Employee {

	@Id
	private Integer id;

	@Column(name = "first_name", length = 50)
	private String firstName;

	@Column(name = "last_name", length = 50)
	private String lastName;

	@Column(length = 11)
	private String title;

	@Column(length = 50)
	private String email;

	@Column(length = 50)
	private String country;

	@Column(length = 50)
	private String city;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "location")
	private Location location;

	@OneToMany(mappedBy = "employee", fetch = FetchType.LAZY)
	private Set<EmployeeRole> employeeRoles = new LinkedHashSet<>();
}