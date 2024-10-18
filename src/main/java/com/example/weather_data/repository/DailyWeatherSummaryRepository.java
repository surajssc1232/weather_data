package com.example.weather_data.repository;

import com.example.weather_data.entity.DailyWeatherSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyWeatherSummaryRepository extends JpaRepository<DailyWeatherSummary, Long> {
    List<DailyWeatherSummary> findByDate(LocalDate date);
    Optional<DailyWeatherSummary> findByCityAndDate(String city, LocalDate date);

    @Query("SELECT MAX(d.date) FROM DailyWeatherSummary d WHERE d.date <= :date")
    LocalDate findClosestDateWithData(@Param("date") LocalDate date);

    @Query("SELECT MIN(d.date) FROM DailyWeatherSummary d")
    LocalDate findEarliestDate();
}
