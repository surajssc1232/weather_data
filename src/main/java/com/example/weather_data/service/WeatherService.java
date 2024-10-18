package com.example.weather_data.service;

import com.example.weather_data.entity.DailyWeatherSummary;
import com.example.weather_data.repository.DailyWeatherSummaryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WeatherService {
    private final RestTemplate restTemplate;
    private final String apiKey;

    @Autowired
    public WeatherService(RestTemplate restTemplate, @Value("${openweathermap.api.key}") String apiKey) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
    }

    private final String url = "https://api.openweathermap.org/data/2.5/weather?q={city}&appid={apiKey}";
    private final List<String> indianMetros = Arrays.asList("Delhi", "Mumbai", "Chennai", "Bengaluru", "Kolkata", "Hyderabad");
    private final Map<String, String> cityMapping = Map.of("Bangalore", "Bengaluru");

    @Autowired
    private DailyWeatherSummaryRepository repository;

    @Value("${weather.alert.temp.threshold:35.0}")
    private double tempThreshold;

    @Value("${weather.alert.humidity.threshold:80}")
    private int humidityThreshold;

    @Value("${weather.alert.consecutive.threshold:2}")
    private int consecutiveThreshold;

    private Map<String, Integer> consecutiveBreaches = new HashMap<>();

    private List<String> activeAlerts = new ArrayList<>();

    private Map<String, Double> lastTemperature = new HashMap<>();
    private Map<String, Integer> lastHumidity = new HashMap<>();

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

    @Transactional
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
            System.out.println("Saved daily summary: " + summary);
        }
    }

    public boolean checkAlertThresholds(WeatherResponse response) {
        double currentTemp = convertKelvinToCelsius(response.getMain().getTemp());
        int currentHumidity = response.getMain().getHumidity();
        String city = response.getName();
        
        boolean alertTriggered = false;
        
        // Check temperature change
        if (lastTemperature.containsKey(city)) {
            double tempDiff = currentTemp - lastTemperature.get(city);
            if (Math.abs(tempDiff) >= 5 || tempDiff > 1) { // Alert if temperature changes by 5°C or more, or increases by more than 1°C
                String alert = String.format("%s: Temperature has changed by %.2f°C. Current temperature: %.2f°C", 
                                             city, tempDiff, currentTemp);
                System.out.println(alert);
                activeAlerts.add(alert);
                alertTriggered = true;
            }
        }
        
        // Check humidity change
        if (lastHumidity.containsKey(city)) {
            int humidityDiff = currentHumidity - lastHumidity.get(city);
            if (Math.abs(humidityDiff) >= 20 || humidityDiff > 2) { // Alert if humidity changes by 20% or more, or increases by more than 2%
                String alert = String.format("%s: Humidity has changed by %d%%. Current humidity: %d%%", 
                                             city, humidityDiff, currentHumidity);
                System.out.println(alert);
                activeAlerts.add(alert);
                alertTriggered = true;
            }
        }
        
        // Update last known values
        lastTemperature.put(city, currentTemp);
        lastHumidity.put(city, currentHumidity);
        
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

        Map.Entry<String, Long> dominantConditionEntry = conditionCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);

        String dominantCondition = dominantConditionEntry != null ? dominantConditionEntry.getKey() : "Unknown";
        String dominantConditionReason = dominantConditionEntry != null 
            ? String.format("Occurred %d out of %d times", dominantConditionEntry.getValue(), summaries.size())
            : "No data available";

        Map<String, Object> stats = new HashMap<>();
        stats.put("date", date);
        stats.put("averageTemp", String.format("%.2f", tempStats.getAverage()));
        stats.put("maxTemp", String.format("%.2f", tempStats.getMax()));
        stats.put("minTemp", String.format("%.2f", tempStats.getMin()));
        stats.put("dominantCondition", dominantCondition);
        stats.put("dominantConditionReason", dominantConditionReason);

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

    public List<DailyWeatherSummary> getDailySummaries(LocalDate date) {
        return repository.findByDate(date);
    }

    public Map<LocalDate, Map<String, Object>> getWeatherTrends(LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, Map<String, Object>> trends = new LinkedHashMap<>();
        
        // Get all data from the earliest date in the database up to the end date
        LocalDate earliestDate = repository.findEarliestDate();
        if (earliestDate == null || earliestDate.isAfter(startDate)) {
            earliestDate = startDate;
        }
        
        LocalDate currentDate = earliestDate;
        while (!currentDate.isAfter(endDate)) {
            List<DailyWeatherSummary> summaries = getDailySummaries(currentDate);
            DoubleSummaryStatistics tempStats = summaries.stream()
                    .mapToDouble(DailyWeatherSummary::getAverageTemp)
                    .summaryStatistics();
            
            Map<String, Object> dailyStats = new HashMap<>();
            dailyStats.put("averageTemp", tempStats.getAverage());
            dailyStats.put("maxTemp", tempStats.getMax());
            dailyStats.put("minTemp", tempStats.getMin());
            
            trends.put(currentDate, dailyStats);
            currentDate = currentDate.plusDays(1);
        }
        return trends;
    }
}
