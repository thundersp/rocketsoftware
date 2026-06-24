package com.buzzmeet.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Locations")
@Getter
@Setter
@NoArgsConstructor
public class Location {

	@Id
	private Integer id;

	@Column(length = 50)
	private String phone;

	@Column(length = 250)
	private String street;

	@Column(length = 50)
	private String country;

	@Column(length = 50)
	private String city;
}