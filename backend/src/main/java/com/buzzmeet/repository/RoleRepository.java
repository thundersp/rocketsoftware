package com.buzzmeet.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.buzzmeet.model.Role;

public interface RoleRepository extends JpaRepository<Role, Integer> {

	Optional<Role> findByRoleNameIgnoreCase(String roleName);
}