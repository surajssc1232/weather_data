package com.example.weather_data.controller;

import com.example.weather_data.entity.DailyWeatherSummary;
import com.example.weather_data.service.WeatherResponse;
import com.example.weather_data.service.WeatherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class WeatherController {

    private final WeatherService weatherService;

    @Autowired
    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping("/weather/{city}")
    public String getCurrentWeather(@PathVariable String city, Model model) {
        WeatherResponse response = weatherService.getWeather(city);
        weatherService.processWeatherData(response);

        Map<String, Object> weatherData = new HashMap<>();
        weatherData.put("city", response.getName());
        weatherData.put("main", response.getWeather().get(0).getMain());
        weatherData.put("temp", weatherService.convertKelvinToCelsius(response.getMain().getTemp()));
        weatherData.put("feels_like", weatherService.convertKelvinToCelsius(response.getMain().getFeels_like()));
        weatherData.put("timestamp", Instant.ofEpochSecond(response.getDt()).atZone(ZoneId.systemDefault()).toLocalDateTime());
        
        if (response.getWind() != null) {
            weatherData.put("windSpeed", Math.round(response.getWind().getSpeed() * 3.6)); // Convert m/s to km/h
            weatherData.put("windDirection", getWindDirection(response.getWind().getDeg()));
            weatherData.put("windGust", response.getWind().getGust() != null ? 
                Math.round(response.getWind().getGust() * 3.6) : null); // Convert m/s to km/h if gust exists
        }
        
        if (response.getCoord() != null) {
            double lat = response.getCoord().getLat();
            double lon = response.getCoord().getLon();
            int aqi = weatherService.getAQI(lat, lon);
            if (aqi != -1) {
                int standardAqi = weatherService.convertToStandardAQI(aqi);
                weatherData.put("aqi", standardAqi);
                weatherData.put("aqiCategory", weatherService.getAQICategory(standardAqi));
            }
        }

        model.addAttribute("weatherData", weatherData);
        return "current-weather";
    }

    private String getWindDirection(double degrees) {
        String[] directions = {"N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", 
                              "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"};
        int index = (int) Math.round(((degrees % 360) / 22.5)) % 16;
        return directions[index];
    }

    @GetMapping("/weather/stats")
    public String getWeatherStats(Model model) {
        Map<String, Map<String, Object>> allStats = weatherService.getAllWeatherStats();
        model.addAttribute("allStats", allStats);
        return "weather-stats";
    }

    @GetMapping("/weather/summary")
    public String getWeatherSummary(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, Model model) {
        date = (date != null) ? date : LocalDate.now();
        List<DailyWeatherSummary> summaries = weatherService.getDailySummaries(date);
        model.addAttribute("date", date);
        model.addAttribute("summaries", summaries);
        return "weather-summary";
    }

    @GetMapping("/weather/trends")
    public String getWeatherTrends(@RequestParam(required = false, defaultValue = "temperature") String dataType, Model model) {
        LocalDate today = LocalDate.now();
        Map<String, Map<String, Double>> weatherData = weatherService.getWeatherDataForToday();
        model.addAttribute("date", today);
        model.addAttribute("weatherData", weatherData);
        model.addAttribute("dataType", dataType);
        return "weather-trends";
    }

    @GetMapping({"/weather/alerts", "/alerts"})
    public String showAlerts(Model model) {
        List<String> activeAlerts = weatherService.getActiveAlerts();
        model.addAttribute("alerts", activeAlerts);
        return "weather-alerts";
    }

    @GetMapping("/weather/clear-alerts")
    @ResponseBody
    public ResponseEntity<String> clearAlerts() {
        weatherService.clearAlerts();
        return ResponseEntity.ok("All alerts have been cleared.");
    }

    @GetMapping({"/trends/{city}", "/weather/trends/{city}"})
    public String getCityTrends(@PathVariable String city, Model model) {
        if (!weatherService.isValidCity(city)) {
            model.addAttribute("error", "Invalid city name: " + city);
            return "error";
        }
        Map<String, Double> cityData = weatherService.getCityWeatherData(city);
        if (cityData.isEmpty()) {
            model.addAttribute("error", "No data available for: " + city);
            return "error";
        }
        model.addAttribute("city", city);
        model.addAttribute("cityData", cityData);
        return "city-trends";
    }
}
