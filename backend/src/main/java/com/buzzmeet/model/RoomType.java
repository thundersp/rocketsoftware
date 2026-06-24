package com.buzzmeet.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Room_Types")
@Getter
@Setter
@NoArgsConstructor
public class RoomType {

	@Id
	@Column(name = "RoomTypeId")
	private Integer roomTypeId;

	@Column(name = "TypeName", nullable = false, length = 50)
	private String typeName;

	@Column(name = "Description", length = 255)
	private String description;

	@Column(name = "IsBookable", length = 1)
	private String isBookable;

	@Column(name = "IsVideoEnabled", length = 1)
	private String isVideoEnabled;

	@Column(name = "RequiresApproval", length = 1)
	private String requiresApproval;
}