package com.buzzmeet.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "User_Credentials")
@Getter
@Setter
@NoArgsConstructor
public class UserCredential {

	@Id
	@Column(name = "CredentialId")
	private Integer credentialId;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "EmployeeId", nullable = false)
	private Employee employee;

	@Column(name = "PasswordHash", nullable = false, length = 255)
	private String passwordHash;

	@Column(name = "LastLogin")
	private LocalDateTime lastLogin;

	@Column(name = "IsActive", length = 1)
	private String isActive;

	@Column(name = "FailedLoginAttempts")
	private Integer failedLoginAttempts;

	@Column(name = "LockedUntil")
	private LocalDateTime lockedUntil;

	@Column(name = "PasswordResetToken", length = 255)
	private String passwordResetToken;

	@Column(name = "PasswordResetExpiry")
	private LocalDateTime passwordResetExpiry;

	@Column(name = "CreatedAt")
	private LocalDateTime createdAt;

	@Column(name = "UpdatedAt")
	private LocalDateTime updatedAt;
}