package com.buzzmeet.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.buzzmeet.model.Role;

public interface RoleRepository extends JpaRepository<Role, Integer> {
}