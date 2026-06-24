package com.buzzmeet.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.buzzmeet.model.RoomType;

public interface RoomTypeRepository extends JpaRepository<RoomType, Integer> {

	List<RoomType> findAllByOrderByTypeNameAsc();
}