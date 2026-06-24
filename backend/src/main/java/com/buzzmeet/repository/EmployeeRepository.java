package com.buzzmeet.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.buzzmeet.model.Employee;

public interface EmployeeRepository extends JpaRepository<Employee, Integer> {

	@EntityGraph(attributePaths = { "location", "employeeRoles", "employeeRoles.role" })
	Optional<Employee> findByEmailIgnoreCase(String email);

	@EntityGraph(attributePaths = { "location" })
	List<Employee> findByLocationIdOrderByFirstNameAscLastNameAsc(Integer locationId);

	@EntityGraph(attributePaths = { "location" })
	List<Employee> findByTitleIgnoreCaseOrderByFirstNameAscLastNameAsc(String title);

	@EntityGraph(attributePaths = { "location" })
	List<Employee> findByLocationIdAndTitleIgnoreCaseOrderByFirstNameAscLastNameAsc(Integer locationId, String title);

	@EntityGraph(attributePaths = { "location" })
	List<Employee> findAllByOrderByFirstNameAscLastNameAsc();
}