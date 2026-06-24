package com.buzzmeet.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.buzzmeet.model.UserCredential;

public interface UserCredentialRepository extends JpaRepository<UserCredential, Integer> {

	@EntityGraph(attributePaths = { "employee", "employee.location", "employee.employeeRoles", "employee.employeeRoles.role" })
	Optional<UserCredential> findByEmployeeEmailIgnoreCase(String email);
}