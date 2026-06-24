package com.buzzmeet.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Buildings")
@Getter
@Setter
@NoArgsConstructor
public class Building {

	@Id
	@Column(name = "BuildingId")
	private Integer buildingId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "LocationId", nullable = false)
	private Location location;

	@Column(name = "BuildingName", length = 100)
	private String buildingName;

	@Column(name = "AddressLine1", length = 255)
	private String addressLine1;

	@Column(name = "AddressLine2", length = 255)
	private String addressLine2;

	@Column(name = "Status", length = 20)
	private String status;
}