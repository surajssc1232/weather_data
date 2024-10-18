package com.example.weather_data.service;

import com.example.weather_data.entity.DailyWeatherSummary;
import com.example.weather_data.repository.DailyWeatherSummaryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WeatherService {
    @Value("${OPENWEATHERMAP_API_KEY:default-key}")
    private String apiKey;
    
    private final String url = "https://api.openweathermap.org/data/2.5/weather?q={city}&appid={apiKey}";
    private final List<String> indianMetros = Arrays.asList("Delhi", "Mumbai", "Chennai", "Bengaluru", "Kolkata", "Hyderabad");
    private final Map<String, String> cityMapping = Map.of("Bangalore", "Bengaluru");

    @Autowired
    private DailyWeatherSummaryRepository repository;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${weather.alert.temp.threshold}")
    private double tempThreshold;

    @Value("${weather.alert.humidity.threshold}")
    private int humidityThreshold;

    @Value("${weather.alert.consecutive.threshold}")
    private int consecutiveThreshold;

    private Map<String, Integer> consecutiveBreaches = new HashMap<>();

    private List<String> activeAlerts = new ArrayList<>();

    @Scheduled(fixedRateString = "${weather.fetch.interval}")
    public void fetchWeatherDataForIndianMetros() {
        System.out.println("Fetching weather data at: " + LocalDateTime.now());
        for (String city : indianMetros) {
            WeatherResponse response = getWeather(city);
            if (response != null) {
                processWeatherData(response);
                checkAlertThresholds(response);
            } else {
                System.out.println("Failed to fetch weather data for " + city);
            }
        }
    }

    public WeatherResponse getWeather(String city) {
        Map<String, String> params = new HashMap<>();
        params.put("city", cityMapping.getOrDefault(city, city));
        params.put("apiKey", apiKey);

        try {
            return restTemplate.getForObject(url, WeatherResponse.class, params);
        } catch (Exception e) {
            System.out.println("Error fetching weather data for " + city + ": " + e.getMessage());
            return null;
        }
    }

    public double convertKelvinToCelsius(double kelvin) {
        return kelvin - 273.15;
    }

    public void processWeatherData(WeatherResponse response) {
        if (response != null && response.getMain() != null && !response.getWeather().isEmpty()) {
            LocalDate today = LocalDate.now();
            String city = response.getName();
            double temp = convertKelvinToCelsius(response.getMain().getTemp());
            String condition = response.getWeather().get(0).getMain();

            DailyWeatherSummary summary = repository.findByCityAndDate(city, today)
                    .orElse(new DailyWeatherSummary());

            if (summary.getId() == null) {
                summary.setCity(city);
                summary.setDate(today);
                summary.setMaxTemp(temp);
                summary.setMinTemp(temp);
                summary.setAverageTemp(temp);
                summary.setDominantCondition(condition);
                summary.setDataPointCount(1);
            } else {
                int newCount = summary.getDataPointCount() + 1;
                double newAvg = (summary.getAverageTemp() * summary.getDataPointCount() + temp) / newCount;
                summary.setAverageTemp(newAvg);
                summary.setMaxTemp(Math.max(summary.getMaxTemp(), temp));
                summary.setMinTemp(Math.min(summary.getMinTemp(), temp));
                summary.setDataPointCount(newCount);

                // Update dominant condition
                Map<String, Integer> conditionCounts = new HashMap<>();
                conditionCounts.put(summary.getDominantCondition(), summary.getDataPointCount() - 1);
                conditionCounts.put(condition, conditionCounts.getOrDefault(condition, 0) + 1);
                summary.setDominantCondition(Collections.max(conditionCounts.entrySet(), Map.Entry.comparingByValue()).getKey());
            }

            summary.setHumidity(response.getMain().getHumidity());
            repository.save(summary);
        }
    }

    public boolean checkAlertThresholds(WeatherResponse response) {
        double currentTemp = convertKelvinToCelsius(response.getMain().getTemp());
        int currentHumidity = response.getMain().getHumidity();
        String city = response.getName();
        
        boolean alertTriggered = false;
        
        if (currentTemp > tempThreshold) {
            int breaches = consecutiveBreaches.getOrDefault(city, 0) + 1;
            consecutiveBreaches.put(city, breaches);
            
            if (breaches >= consecutiveThreshold) {
                String alert = "ALERT: Temperature in " + city + " has exceeded " + tempThreshold + 
                               "°C for " + breaches + " consecutive updates. Current temperature: " + 
                               String.format("%.2f", currentTemp) + "°C";
                System.out.println(alert);
                activeAlerts.add(alert);
                alertTriggered = true;
            }
        } else {
            consecutiveBreaches.put(city, 0);
        }
        
        if (currentHumidity > humidityThreshold) {
            String alert = "ALERT: Humidity in " + city + " has exceeded " + humidityThreshold + 
                           "%. Current humidity: " + currentHumidity + "%";
            System.out.println(alert);
            activeAlerts.add(alert);
            alertTriggered = true;
        }
        
        return alertTriggered;
    }

    public List<String> getActiveAlerts() {
        return new ArrayList<>(activeAlerts);
    }

    public void clearAlerts() {
        activeAlerts.clear();
    }

    public Map<String, Object> getWeatherStats(LocalDate date) {
        List<DailyWeatherSummary> summaries = repository.findByDate(date);
        
        DoubleSummaryStatistics tempStats = summaries.stream()
                .mapToDouble(DailyWeatherSummary::getAverageTemp)
                .summaryStatistics();

        Map<String, Long> conditionCounts = summaries.stream()
                .collect(Collectors.groupingBy(DailyWeatherSummary::getDominantCondition, Collectors.counting()));

        String dominantCondition = conditionCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Unknown");

        Map<String, Object> stats = new HashMap<>();
        stats.put("date", date);
        stats.put("averageTemp", String.format("%.2f", tempStats.getAverage()));
        stats.put("maxTemp", String.format("%.2f", tempStats.getMax()));
        stats.put("minTemp", String.format("%.2f", tempStats.getMin()));
        stats.put("dominantCondition", dominantCondition);

        return stats;
    }

    public void printDailyWeatherSummary(LocalDate date) {
        Map<String, Object> stats = getWeatherStats(date);
        System.out.println("Weather Summary for " + date);
        System.out.println("Average Temperature: " + stats.get("averageTemp") + "°C");
        System.out.println("Maximum Temperature: " + stats.get("maxTemp") + "°C");
        System.out.println("Minimum Temperature: " + stats.get("minTemp") + "°C");
        System.out.println("Dominant Weather Condition: " + stats.get("dominantCondition"));
    }

    public List<DailyWeatherSummary> getDailySummaries(LocalDate date) {
        return repository.findByDate(date);
    }

    public Map<String, Map<String, Object>> getAllWeatherStats(LocalDate date, List<String> cities) {
        return cities.stream()
            .collect(Collectors.toMap(
                city -> city,
                city -> {
                    Optional<DailyWeatherSummary> summaryOpt = repository.findByCityAndDate(cityMapping.getOrDefault(city, city), date);
                    if (summaryOpt.isEmpty()) {
                        return Map.of("hasError", true, "errorMessage", "No data available");
                    }
                    DailyWeatherSummary summary = summaryOpt.get();
                    return new HashMap<String, Object>() {{
                        put("hasError", false);
                        put("averageTemp", String.format("%.2f", summary.getAverageTemp()));
                        put("maxTemp", String.format("%.2f", summary.getMaxTemp()));
                        put("minTemp", String.format("%.2f", summary.getMinTemp()));
                        put("dominantCondition", summary.getDominantCondition());
                        put("humidity", summary.getHumidity());
                    }};
                }
            ));
    }

    public Map<String, Map<String, Object>> getAllWeatherStats() {
        return indianMetros.stream()
            .collect(Collectors.toMap(
                city -> city,
                city -> {
                    WeatherResponse response = getWeather(city);
                    if (response == null || response.getMain() == null || response.getWeather().isEmpty()) {
                        return Map.of("hasError", true, "errorMessage", "No data available");
                    }
                    return Map.of(
                        "hasError", false,
                        "main", response.getWeather().get(0).getMain(),
                        "temp", String.format("%.2f", convertKelvinToCelsius(response.getMain().getTemp())),
                        "feels_like", String.format("%.2f", convertKelvinToCelsius(response.getMain().getFeels_like())),
                        "dt", LocalDateTime.ofInstant(Instant.ofEpochSecond(response.getDt()), ZoneId.systemDefault()).toString()
                    );
                }
            ));
    }
}
