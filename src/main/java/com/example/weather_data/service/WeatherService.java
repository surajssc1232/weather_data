package com.example.weather_data.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.example.weather_data.entity.DailyWeatherSummary;
import com.example.weather_data.repository.DailyWeatherSummaryRepository;

@Service
public class WeatherService {
    private static final Logger logger = LoggerFactory.getLogger(WeatherService.class);

    private final RestTemplate restTemplate;
    private final String apiKey;

    @Autowired
    public WeatherService(RestTemplate restTemplate, Environment env) {
        this.restTemplate = restTemplate;
        String tempApiKey = env.getProperty("OPENWEATHERMAP_API_KEY");
        if (tempApiKey == null || tempApiKey.isEmpty()) {
            logger.error("OPENWEATHERMAP_API_KEY is not set in the environment");
            tempApiKey = "API_KEY_NOT_SET";
        } else {
            logger.info("OPENWEATHERMAP_API_KEY successfully loaded");
        }
        this.apiKey = tempApiKey;
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

    private final String historicalUrl = "https://api.openweathermap.org/data/2.5/onecall/timemachine?lat={lat}&lon={lon}&dt={timestamp}&appid={apiKey}";

    private final String aqiUrl = "http://api.openweathermap.org/data/2.5/air_pollution?lat={lat}&lon={lon}&appid={apiKey}";

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

    public Map<LocalDate, Map<String, Object>> getWeatherTrendsWithHistorical(LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, Map<String, Object>> trends = new LinkedHashMap<>();
        
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            Map<String, Object> dailyData = new HashMap<>();
            for (String city : indianMetros) {
                WeatherResponse currentWeather = getWeather(city);
                if (currentWeather != null && currentWeather.getMain() != null) {
                    double temp = convertKelvinToCelsius(currentWeather.getMain().getTemp());
                    dailyData.put(city, temp);
                }
            }
            trends.put(currentDate, dailyData);
            currentDate = currentDate.plusDays(1);
        }
        
        return trends;
    }

    public Map<String, Double> getWeatherTrendsForToday() {
        Map<String, Double> trends = new HashMap<>();
        LocalDate today = LocalDate.now();
        
        for (String city : indianMetros) {
            Optional<DailyWeatherSummary> summary = repository.findByCityAndDate(city, today);
            if (summary.isPresent()) {
                trends.put(city, summary.get().getAverageTemp());
            } else {
                WeatherResponse currentWeather = getWeather(city);
                if (currentWeather != null && currentWeather.getMain() != null) {
                    double temp = convertKelvinToCelsius(currentWeather.getMain().getTemp());
                    trends.put(city, temp);
                }
            }
        }
        
        return trends;
    }

    public Map<String, Map<String, Double>> getWeatherDataForToday() {
        Map<String, Map<String, Double>> data = new HashMap<>();
        LocalDate today = LocalDate.now();
        
        for (String city : indianMetros) {
            Map<String, Double> cityData = new HashMap<>();
            WeatherResponse weatherResponse = getWeather(city);
            if (weatherResponse != null && weatherResponse.getMain() != null) {
                cityData.put("temperature", convertKelvinToCelsius(weatherResponse.getMain().getTemp()));
                cityData.put("humidity", (double) weatherResponse.getMain().getHumidity());
                
                // Fetch AQI data with new ranges
                if (weatherResponse.getCoord() != null) {
                    double lat = weatherResponse.getCoord().getLat();
                    double lon = weatherResponse.getCoord().getLon();
                    int aqi = getAQI(lat, lon);
                    cityData.put("aqi", (double) convertToStandardAQI(aqi));
                    cityData.put("aqiCategory", (double) getAQICategoryValue(aqi));
                }
            }
            data.put(city, cityData);
        }
        
        return data;
    }

    public int convertToStandardAQI(int openWeatherMapAQI) {
        // OpenWeatherMap AQI values:
        // 1 = Good (0-50)
        // 2 = Fair (51-100)
        // 3 = Moderate (101-150)
        // 4 = Poor (151-200)
        // 5 = Very Poor (201-300)
        // >5 = Hazardous (>300)
        Map<Integer, int[]> aqiRanges = new HashMap<>() {{
            put(1, new int[]{0, 50});      // Good
            put(2, new int[]{51, 100});    // Fair
            put(3, new int[]{101, 150});   // Moderate
            put(4, new int[]{151, 200});   // Poor
            put(5, new int[]{201, 300});   // Very Poor
        }};

        if (openWeatherMapAQI < 1 || openWeatherMapAQI > 5) {
            return 301; // Hazardous
        }

        int[] range = aqiRanges.get(openWeatherMapAQI);
        // Return the middle value of the range
        return (range[0] + range[1]) / 2;
    }

    public int getAQI(double lat, double lon) {
        Map<String, Object> params = new HashMap<>();
        params.put("lat", lat);
        params.put("lon", lon);
        params.put("apiKey", apiKey);

        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(aqiUrl, Map.class, params);
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("list")) {
                List<Map<String, Object>> list = (List<Map<String, Object>>) responseBody.get("list");
                if (!list.isEmpty()) {
                    Map<String, Object> firstItem = list.get(0);
                    Map<String, Object> components = (Map<String, Object>) firstItem.get("components");
                    
                    // Get all pollutant values
                    double pm2_5 = ((Number) components.get("pm2_5")).doubleValue();
                    double pm10 = ((Number) components.get("pm10")).doubleValue();
                    double no2 = ((Number) components.get("no2")).doubleValue();
                    double o3 = ((Number) components.get("o3")).doubleValue();
                    double so2 = ((Number) components.get("so2")).doubleValue();
                    double co = ((Number) components.get("co")).doubleValue();
                    
                    // Calculate individual AQI for each pollutant
                    int pm25Aqi = calculatePM25AQI(pm2_5);
                    int pm10Aqi = calculatePM10AQI(pm10);
                    int no2Aqi = calculateNO2AQI(no2);
                    int o3Aqi = calculateO3AQI(o3);
                    int so2Aqi = calculateSO2AQI(so2);
                    int coAqi = calculateCOAQI(co);
                    
                    // Return the highest AQI value (worst air quality)
                    return Math.max(Math.max(Math.max(pm25Aqi, pm10Aqi), 
                                  Math.max(no2Aqi, o3Aqi)), 
                                  Math.max(so2Aqi, coAqi));
                }
            }
        } catch (Exception e) {
            logger.error("Error fetching AQI data: " + e.getMessage());
        }
        return -1;
    }

    private int calculatePM25AQI(double pm25) {
        // PM2.5 breakpoints as per EPA standards
        if (pm25 <= 12.0) return linearScale(pm25, 0, 12.0, 0, 50);
        if (pm25 <= 35.4) return linearScale(pm25, 12.1, 35.4, 51, 100);
        if (pm25 <= 55.4) return linearScale(pm25, 35.5, 55.4, 101, 150);
        if (pm25 <= 150.4) return linearScale(pm25, 55.5, 150.4, 151, 200);
        if (pm25 <= 250.4) return linearScale(pm25, 150.5, 250.4, 201, 300);
        if (pm25 <= 350.4) return linearScale(pm25, 250.5, 350.4, 301, 400);
        return linearScale(pm25, 350.5, 500.4, 401, 500);
    }

    private int calculatePM10AQI(double pm10) {
        // PM10 breakpoints
        if (pm10 <= 54) return linearScale(pm10, 0, 54, 0, 50);
        if (pm10 <= 154) return linearScale(pm10, 55, 154, 51, 100);
        if (pm10 <= 254) return linearScale(pm10, 155, 254, 101, 150);
        if (pm10 <= 354) return linearScale(pm10, 255, 354, 151, 200);
        if (pm10 <= 424) return linearScale(pm10, 355, 424, 201, 300);
        if (pm10 <= 504) return linearScale(pm10, 425, 504, 301, 400);
        return linearScale(pm10, 505, 604, 401, 500);
    }

    private int calculateNO2AQI(double no2) {
        // NO2 breakpoints (in ppb)
        if (no2 <= 53) return linearScale(no2, 0, 53, 0, 50);
        if (no2 <= 100) return linearScale(no2, 54, 100, 51, 100);
        if (no2 <= 360) return linearScale(no2, 101, 360, 101, 150);
        if (no2 <= 649) return linearScale(no2, 361, 649, 151, 200);
        if (no2 <= 1249) return linearScale(no2, 650, 1249, 201, 300);
        if (no2 <= 1649) return linearScale(no2, 1250, 1649, 301, 400);
        return linearScale(no2, 1650, 2049, 401, 500);
    }

    private int calculateO3AQI(double o3) {
        // O3 breakpoints (in ppb)
        if (o3 <= 54) return linearScale(o3, 0, 54, 0, 50);
        if (o3 <= 70) return linearScale(o3, 55, 70, 51, 100);
        if (o3 <= 85) return linearScale(o3, 71, 85, 101, 150);
        if (o3 <= 105) return linearScale(o3, 86, 105, 151, 200);
        if (o3 <= 200) return linearScale(o3, 106, 200, 201, 300);
        return 301; // Beyond "Hazardous" level
    }

    private int calculateSO2AQI(double so2) {
        // SO2 breakpoints (in ppb)
        if (so2 <= 35) return linearScale(so2, 0, 35, 0, 50);
        if (so2 <= 75) return linearScale(so2, 36, 75, 51, 100);
        if (so2 <= 185) return linearScale(so2, 76, 185, 101, 150);
        if (so2 <= 304) return linearScale(so2, 186, 304, 151, 200);
        return 201; // Beyond "Very Unhealthy" level
    }

    private int calculateCOAQI(double co) {
        // CO breakpoints (in ppm)
        if (co <= 4.4) return linearScale(co, 0, 4.4, 0, 50);
        if (co <= 9.4) return linearScale(co, 4.5, 9.4, 51, 100);
        if (co <= 12.4) return linearScale(co, 9.5, 12.4, 101, 150);
        if (co <= 15.4) return linearScale(co, 12.5, 15.4, 151, 200);
        return 201; // Beyond "Very Unhealthy" level
    }

    private int linearScale(double value, double bpLow, double bpHigh, int aqiLow, int aqiHigh) {
        return (int) Math.round(((aqiHigh - aqiLow) / (bpHigh - bpLow)) * (value - bpLow) + aqiLow);
    }

    // Update AQI category ranges to match actual values
    public String getAQICategory(int aqi) {
        if (aqi <= 50) return "Good";
        if (aqi <= 100) return "Moderate";
        if (aqi <= 150) return "Unhealthy for Sensitive Groups";
        if (aqi <= 200) return "Unhealthy";
        if (aqi <= 300) return "Very Unhealthy";
        return "Hazardous";
    }

    private int getAQICategoryValue(int aqi) {
            if (aqi <= 33) return 1;    // Very Good
            if (aqi <= 66) return 2;    // Good
            if (aqi <= 99) return 3;    // Fair
            if (aqi <= 149) return 4;   // Poor
            if (aqi <= 200) return 5;   // Very Poor
            return 6;                   // Hazardous
    }

    private String getAQIHealthAdvice(int aqi) {
        if (aqi <= 33) return "Enjoy activities";
        if (aqi <= 66) return "Enjoy activities";
        if (aqi <= 99) return "People unusually sensitive to air pollution: Plan strenuous outdoor activities when air quality is better";
        if (aqi <= 149) return "Sensitive groups: Cut back or reschedule strenuous outdoor activities";
        if (aqi <= 200) return "Sensitive groups: Avoid strenuous outdoor activities\nEveryone: Cut back or reschedule strenuous outdoor activities";
        return "Sensitive groups: Avoid all outdoor physical activities\nEveryone: Significantly cut back on outdoor physical activities";
    }

    public Map<String, Double> getCityWeatherData(String city) {
        Map<String, Double> cityData = new HashMap<>();
        WeatherResponse weatherResponse;

        if (indianMetros.contains(city) || cityMapping.containsKey(city)) {
            weatherResponse = getWeather(city);
        } else {
            weatherResponse = getWeatherForNonMetroCity(city);
        }

        if (weatherResponse != null && weatherResponse.getMain() != null) {
            cityData.put("temperature", convertKelvinToCelsius(weatherResponse.getMain().getTemp()));
            cityData.put("humidity", (double) weatherResponse.getMain().getHumidity());
            
            if (weatherResponse.getCoord() != null) {
                double lat = weatherResponse.getCoord().getLat();
                double lon = weatherResponse.getCoord().getLon();
                int aqi = getAQI(lat, lon);
                if (aqi != -1) {
                    cityData.put("aqi", (double) convertToStandardAQI(aqi));
                    cityData.put("aqiCategory", (double) getAQICategoryValue(aqi));
                }
            }
        }
        return cityData;
    }

    private WeatherResponse getWeatherForNonMetroCity(String city) {
        Map<String, String> params = new HashMap<>();
        params.put("city", city);
        params.put("apiKey", apiKey);

        try {
            return restTemplate.getForObject(url, WeatherResponse.class, params);
        } catch (Exception e) {
            System.out.println("Error fetching weather data for " + city + ": " + e.getMessage());
            return null;
        }
    }

    public boolean isValidCity(String city) {
        // We'll consider any non-empty string as a potentially valid city name
        return city != null && !city.trim().isEmpty();
    }

}
