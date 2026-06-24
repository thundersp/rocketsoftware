package com.buzzmeet.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Time_Zones")
@Getter
@Setter
@NoArgsConstructor
public class TimeZoneInfo {

	@Id
	@Column(name = "TimeZoneId")
	private Integer timeZoneId;

	@Column(name = "ZoneName", nullable = false, length = 100)
	private String zoneName;

	@Column(name = "GMTOffsetMinutes")
	private Integer gmtOffsetMinutes;

	@Column(name = "IsDSTSupported", length = 1)
	private String isDstSupported;

	@Column(name = "IsActive", length = 1)
	private String isActive;
}