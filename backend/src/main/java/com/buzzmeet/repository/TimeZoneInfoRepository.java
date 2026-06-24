package com.buzzmeet.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.buzzmeet.model.TimeZoneInfo;

public interface TimeZoneInfoRepository extends JpaRepository<TimeZoneInfo, Integer> {

	List<TimeZoneInfo> findAllByOrderByZoneNameAsc();
}