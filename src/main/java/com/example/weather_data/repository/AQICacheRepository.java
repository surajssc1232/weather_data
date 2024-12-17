package com.example.weather_data.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.weather_data.entity.AQICache;

@Repository
public interface AQICacheRepository extends JpaRepository<AQICache, Long> {
    Optional<AQICache> findFirstByCityOrderByLastUpdatedDesc(String city);
} 