package com.buzzmeet.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.buzzmeet.model.EmployeeRole;
import com.buzzmeet.model.EmployeeRoleId;

public interface EmployeeRoleRepository extends JpaRepository<EmployeeRole, EmployeeRoleId> {

	@EntityGraph(attributePaths = { "role", "employee" })
	List<EmployeeRole> findByIdEmployeeId(Integer employeeId);
}