package com.example.weather_data.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WeatherResponse {
    private Main main;
    private java.util.List<WeatherData> weather;
    private String name;
    private long dt;
    private Wind wind;
    private Coord coord;

    // Getters and setters
    public Main getMain() { return main; }
    public void setMain(Main main) { this.main = main; }
    
    public java.util.List<WeatherData> getWeather() { return weather; }
    public void setWeather(java.util.List<WeatherData> weather) { this.weather = weather; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public long getDt() { return dt; }
    public void setDt(long dt) { this.dt = dt; }
    
    public Wind getWind() { return wind; }
    public void setWind(Wind wind) { this.wind = wind; }
    
    public Coord getCoord() { return coord; }
    public void setCoord(Coord coord) { this.coord = coord; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Main {
        private double temp;
        private double feels_like;
        private int humidity;

        public double getTemp() { return temp; }
        public void setTemp(double temp) { this.temp = temp; }
        
        public double getFeels_like() { return feels_like; }
        public void setFeels_like(double feels_like) { this.feels_like = feels_like; }
        
        public int getHumidity() { return humidity; }
        public void setHumidity(int humidity) { this.humidity = humidity; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WeatherData {
        private String main;
        private String description;
        private String icon;

        public String getMain() { return main; }
        public void setMain(String main) { this.main = main; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Wind {
        private double speed;
        private double deg;
        private Double gust;

        public double getSpeed() { return speed; }
        public void setSpeed(double speed) { this.speed = speed; }
        
        public double getDeg() { return deg; }
        public void setDeg(double deg) { this.deg = deg; }
        
        public Double getGust() { return gust; }
        public void setGust(Double gust) { this.gust = gust; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Coord {
        private double lat;
        private double lon;

        public double getLat() { return lat; }
        public void setLat(double lat) { this.lat = lat; }
        
        public double getLon() { return lon; }
        public void setLon(double lon) { this.lon = lon; }
    }
}
