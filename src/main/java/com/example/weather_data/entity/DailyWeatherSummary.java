package com.example.weather_data.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "daily_weather_summaries")
public class DailyWeatherSummary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String city;
    private double averageTemp;
    private double maxTemp;
    private double minTemp;
    private String dominantCondition;
    private LocalDate date;
    private int humidity;
    private int dataPointCount; // To keep track of how many data points were used for aggregation

    // Getters and setters
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

    public double getAverageTemp() {
        return averageTemp;
    }

    public void setAverageTemp(double averageTemp) {
        this.averageTemp = averageTemp;
    }

    public double getMaxTemp() {
        return maxTemp;
    }

    public void setMaxTemp(double maxTemp) {
        this.maxTemp = maxTemp;
    }

    public double getMinTemp() {
        return minTemp;
    }

    public void setMinTemp(double minTemp) {
        this.minTemp = minTemp;
    }

    public String getDominantCondition() {
        return dominantCondition;
    }

    public void setDominantCondition(String dominantCondition) {
        this.dominantCondition = dominantCondition;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public int getHumidity() {
        return humidity;
    }

    public void setHumidity(int humidity) {
        this.humidity = humidity;
    }

    public int getDataPointCount() {
        return dataPointCount;
    }

    public void setDataPointCount(int dataPointCount) {
        this.dataPointCount = dataPointCount;
    }

    // Add a toString method for easier debugging
    @Override
    public String toString() {
        return "DailyWeatherSummary{" +
                "id=" + id +
                ", city='" + city + '\'' +
                ", date=" + date +
                ", averageTemp=" + averageTemp +
                ", maxTemp=" + maxTemp +
                ", minTemp=" + minTemp +
                ", dominantCondition='" + dominantCondition + '\'' +
                ", humidity=" + humidity +
                ", dataPointCount=" + dataPointCount +
                '}';
    }
}
