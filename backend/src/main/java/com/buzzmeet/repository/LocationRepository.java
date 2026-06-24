package com.buzzmeet.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.buzzmeet.model.Location;

public interface LocationRepository extends JpaRepository<Location, Integer> {
}