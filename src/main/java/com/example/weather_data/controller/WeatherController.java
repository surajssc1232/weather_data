package com.example.weather_data.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.weather_data.service.WeatherService;

@Controller
public class WeatherController {

    private final WeatherService weatherService;
    private final List<String> indianMetros = Arrays.asList("Delhi", "Mumbai", "Chennai", "Bengaluru", "Kolkata", "Hyderabad");

    @Autowired
    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping({"/", "/home"})
    public String home(Model model) {
        model.addAttribute("lastUpdate", LocalDateTime.now());
        return "home";
    }

    @GetMapping("/weather/current")
    public String getCurrentWeatherRedirect() {
        return "redirect:/weather/Delhi"; // Default to Delhi, or you can show a city selection page
    }

    @GetMapping("/weather/{city}")
    public String getCurrentWeather(@PathVariable String city, Model model) {
        if (!weatherService.isValidCity(city)) {
            return "redirect:/home";
        }

        Map<String, Object> weatherData = weatherService.getCurrentWeatherData(city);
        
        model.addAttribute("city", city);
        model.addAttribute("cities", indianMetros);
        model.addAttribute("temperature", weatherData.get("temperature"));
        model.addAttribute("feelsLike", weatherData.get("feelsLike"));
        model.addAttribute("weatherCondition", weatherData.get("weatherCondition"));
        model.addAttribute("weatherIcon", weatherData.get("weatherIcon"));
        model.addAttribute("windSpeed", weatherData.get("windSpeed"));
        model.addAttribute("windDirection", weatherData.get("windDirection"));
        model.addAttribute("windGusts", weatherData.get("windGusts"));
        model.addAttribute("aqi", weatherData.get("aqi"));
        model.addAttribute("aqiCategory", weatherData.get("aqiCategory"));
        model.addAttribute("currentTime", weatherData.get("currentTime"));
        model.addAttribute("lastUpdated", weatherData.get("lastUpdated"));

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
