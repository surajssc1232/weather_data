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
        weatherService.processWeatherData(response); // Process and save the data

        Map<String, Object> weatherData = new HashMap<>();
        weatherData.put("city", response.getName());
        weatherData.put("main", response.getWeather().get(0).getMain());
        weatherData.put("temp", weatherService.convertKelvinToCelsius(response.getMain().getTemp()));
        weatherData.put("feels_like", weatherService.convertKelvinToCelsius(response.getMain().getFeels_like()));
        weatherData.put("timestamp", Instant.ofEpochSecond(response.getDt()).atZone(ZoneId.systemDefault()).toLocalDateTime());

        model.addAttribute("weatherData", weatherData);
        return "current-weather";
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
    public String getWeatherTrends(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                   Model model) {
        startDate = (startDate != null) ? startDate : LocalDate.now().minusDays(7);
        endDate = (endDate != null) ? endDate : LocalDate.now();
        Map<LocalDate, Map<String, Object>> trends = weatherService.getWeatherTrends(startDate, endDate);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("trends", trends);
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
}
