package com.example.weather_data.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "aqi_cache")
public class AQICache {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private Integer aqiValue;

    @Column(nullable = false)
    private LocalDateTime lastUpdated;

    // Default 30-minute cache validity
    @Column(nullable = false)
    private LocalDateTime validUntil;

    public AQICache() {
    }

    public AQICache(String city, Integer aqiValue) {
        this.city = city;
        this.aqiValue = aqiValue;
        this.lastUpdated = LocalDateTime.now();
        this.validUntil = this.lastUpdated.plusMinutes(30);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Integer getAqiValue() {
        return aqiValue;
    }

    public void setAqiValue(Integer aqiValue) {
        this.aqiValue = aqiValue;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public LocalDateTime getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(LocalDateTime validUntil) {
        this.validUntil = validUntil;
    }

    public boolean isValid() {
        return LocalDateTime.now().isBefore(validUntil);
    }
} 