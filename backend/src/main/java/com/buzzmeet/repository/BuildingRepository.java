package com.buzzmeet.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.buzzmeet.model.Building;

public interface BuildingRepository extends JpaRepository<Building, Integer> {

	@EntityGraph(attributePaths = { "location" })
	List<Building> findAllByOrderByBuildingNameAsc();

	@EntityGraph(attributePaths = { "location" })
	List<Building> findByLocationIdOrderByBuildingNameAsc(Integer locationId);
}