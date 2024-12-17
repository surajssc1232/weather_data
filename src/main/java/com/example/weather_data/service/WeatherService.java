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

import com.example.weather_data.entity.AQICache;
import com.example.weather_data.entity.DailyWeatherSummary;
import com.example.weather_data.repository.AQICacheRepository;
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
    private final String iqAirUrl = "http://api.airvisual.com/v2/city?city={city}&state={state}&country=India&key={apiKey}";

    @Value("${IQAIR_API_KEY}")
    private String iqAirKey;

    // City state mappings for IQAir API
    private final Map<String, String> cityStateMapping = Map.of(
        "Delhi", "Delhi",
        "Mumbai", "Maharashtra",
        "Chennai", "Tamil Nadu",
        "Bengaluru", "Karnataka",
        "Kolkata", "West Bengal",
        "Hyderabad", "Telangana"
    );

    @Autowired
    private DailyWeatherSummaryRepository repository;

    @Autowired
    private AQICacheRepository aqiCacheRepository;

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
                
                // Fetch AQI data from IQAir
                int aqi = getAQIForCity(city);
                if (aqi != -1) {
                    cityData.put("aqi", (double) aqi);
                } else {
                    cityData.put("aqi", 0.0);
                }
            }
            data.put(city, cityData);
        }
        
        return data;
    }

    public int getAQI(double lat, double lon) {
        // We'll use the city name instead of lat/lon for IQAir
        // This method is kept for compatibility
        return -1;
    }

    private int getAQIForCity(String city) {
        // First check cache
        Optional<AQICache> cachedAQI = aqiCacheRepository.findFirstByCityOrderByLastUpdatedDesc(city);
        if (cachedAQI.isPresent() && cachedAQI.get().isValid()) {
            logger.info("Using cached AQI value {} for {}", cachedAQI.get().getAqiValue(), city);
            return cachedAQI.get().getAqiValue();
        }

        // If not in cache or expired, fetch from API
        int aqiValue = fetchAQIFromAPI(city);
        
        // If API call successful, cache the result
        if (aqiValue != -1) {
            AQICache aqiCache = new AQICache(city, aqiValue);
            aqiCacheRepository.save(aqiCache);
            logger.info("Cached new AQI value {} for {}", aqiValue, city);
        } else if (cachedAQI.isPresent()) {
            // If API call failed but we have cached data (even if expired), use it
            logger.info("API call failed, using expired cache value {} for {}", cachedAQI.get().getAqiValue(), city);
            return cachedAQI.get().getAqiValue();
        }
        
        return aqiValue;
    }

    private int fetchAQIFromAPI(String city) {
        // First, get the city's location details from OpenWeatherMap if it's not a mapped city
        String state = cityStateMapping.get(city);
        if (state == null) {
            WeatherResponse weatherResponse = getWeather(city);
            if (weatherResponse != null && weatherResponse.getCoord() != null) {
                // Use the coordinates to get the nearest city data from IQAir
                try {
                    String nearestCityUrl = String.format(
                        "http://api.airvisual.com/v2/nearest_city?lat=%.4f&lon=%.4f&key=%s",
                        weatherResponse.getCoord().getLat(),
                        weatherResponse.getCoord().getLon(),
                        iqAirKey
                    );
                    ResponseEntity<Map> response = restTemplate.getForEntity(nearestCityUrl, Map.class);
                    Map<String, Object> responseBody = response.getBody();
                    
                    if (responseBody != null && responseBody.get("status").equals("success")) {
                        Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                        Map<String, Object> current = (Map<String, Object>) data.get("current");
                        Map<String, Object> pollution = (Map<String, Object>) current.get("pollution");
                        
                        int aqiValue = ((Number) pollution.get("aqius")).intValue();
                        logger.info("Successfully retrieved AQI value {} for custom city {} using coordinates", aqiValue, city);
                        return aqiValue;
                    }
                } catch (Exception e) {
                    logger.error("Error fetching AQI data for custom city {} using coordinates: {}", city, e.getMessage());
                }
            }
            return -1;
        }

        // For mapped cities, use the existing logic
        Map<String, String> params = new HashMap<>();
        params.put("city", city);
        params.put("state", state);
        params.put("country", "India");
        params.put("apiKey", iqAirKey);

        try {
            logger.info("Fetching AQI data for {}, {}", params.get("city"), params.get("state"));
            String apiUrl = String.format("http://api.airvisual.com/v2/city?city=%s&state=%s&country=%s&key=%s",
                params.get("city"), params.get("state"), "India", iqAirKey);
            ResponseEntity<Map> response = restTemplate.getForEntity(apiUrl, Map.class);
            
            Map<String, Object> responseBody = response.getBody();
            logger.info("Response from IQAir API for {}: {}", city, responseBody);
            
            if (responseBody != null && responseBody.get("status").equals("success")) {
                Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                Map<String, Object> current = (Map<String, Object>) data.get("current");
                Map<String, Object> pollution = (Map<String, Object>) current.get("pollution");
                
                int aqiValue = ((Number) pollution.get("aqius")).intValue();
                logger.info("Successfully retrieved AQI value {} for {}", aqiValue, city);
                return aqiValue;
            } else {
                logger.error("Failed to get AQI data for {}. Response: {}", city, responseBody);
                // For Hyderabad, try alternative name if first attempt fails
                if (city.equals("Hyderabad")) {
                    apiUrl = String.format("http://api.airvisual.com/v2/city?city=%s&state=%s&country=%s&key=%s",
                        "Hyderabad City", "Telangana", "India", iqAirKey);
                    response = restTemplate.getForEntity(apiUrl, Map.class);
                    responseBody = response.getBody();
                    
                    if (responseBody != null && responseBody.get("status").equals("success")) {
                        Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                        Map<String, Object> current = (Map<String, Object>) data.get("current");
                        Map<String, Object> pollution = (Map<String, Object>) current.get("pollution");
                        
                        int aqiValue = ((Number) pollution.get("aqius")).intValue();
                        logger.info("Successfully retrieved AQI value {} for Hyderabad City", aqiValue);
                        return aqiValue;
                    }
                    // If both attempts fail for Hyderabad, return default value
                    return 150;
                }
            }
        } catch (Exception e) {
            logger.error("Error fetching AQI data for {}: {}", city, e.getMessage());
            if (city.equals("Hyderabad")) {
                return 150; // Default value for Hyderabad if API fails
            }
        }
        return -1;
    }

    public String getAQICategory(int aqi) {
        // Using US EPA AQI categories (which IQAir uses)
        if (aqi <= 50) return "Good";
        if (aqi <= 100) return "Moderate";
        if (aqi <= 150) return "Unhealthy for Sensitive Groups";
        if (aqi <= 200) return "Unhealthy";
        if (aqi <= 300) return "Very Unhealthy";
        return "Hazardous";
    }

    // Add the missing method for AQI conversion
    public int convertToStandardAQI(int aqi) {
        // IQAir already provides AQI in US EPA standard, so we just return it
        return aqi;
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
                    cityData.put("aqi", (double) aqi);
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

    public Map<String, Object> getCurrentWeatherData(String city) {
        Map<String, Object> weatherData = new HashMap<>();
        WeatherResponse response = getWeather(city);
        
        if (response != null && response.getMain() != null) {
            // Temperature data
            double tempCelsius = convertKelvinToCelsius(response.getMain().getTemp());
            double feelsLikeCelsius = convertKelvinToCelsius(response.getMain().getFeels_like());
            weatherData.put("temperature", String.format("%.1f", tempCelsius));
            weatherData.put("feelsLike", String.format("%.1f", feelsLikeCelsius));

            // Weather condition and icon
            if (response.getWeather() != null && !response.getWeather().isEmpty()) {
                WeatherResponse.WeatherData weatherInfo = response.getWeather().get(0);
                weatherData.put("weatherCondition", weatherInfo.getMain());
                weatherData.put("weatherIcon", weatherInfo.getIcon());
                logger.info("Weather icon code: {}", weatherInfo.getIcon());
            } else {
                weatherData.put("weatherCondition", "N/A");
                weatherData.put("weatherIcon", null);
            }

            // Wind data
            if (response.getWind() != null) {
                double windSpeedKmh = response.getWind().getSpeed() * 3.6; // Convert m/s to km/h
                weatherData.put("windSpeed", String.format("%.1f", windSpeedKmh));
                weatherData.put("windDirection", getWindDirection(response.getWind().getDeg()));
                
                if (response.getWind().getGust() != null) {
                    double windGustKmh = response.getWind().getGust() * 3.6;
                    weatherData.put("windGusts", String.format("%.1f", windGustKmh));
                } else {
                    weatherData.put("windGusts", null);
                }
            } else {
                weatherData.put("windSpeed", "N/A");
                weatherData.put("windDirection", "N/A");
                weatherData.put("windGusts", null);
            }
            
            // AQI data
            int aqi = getAQIForCity(city);
            weatherData.put("aqi", aqi);
            weatherData.put("aqiCategory", getAQICategory(aqi));
            
            // Timestamps
            weatherData.put("currentTime", LocalDateTime.now());
            weatherData.put("lastUpdated", LocalDateTime.ofInstant(
                Instant.ofEpochSecond(response.getDt()), 
                ZoneId.systemDefault()
            ));
        } else {
            // Set default values if weather data is not available
            weatherData.put("temperature", "N/A");
            weatherData.put("feelsLike", "N/A");
            weatherData.put("weatherCondition", "N/A");
            weatherData.put("weatherIcon", null);
            weatherData.put("windSpeed", "N/A");
            weatherData.put("windDirection", "N/A");
            weatherData.put("windGusts", null);
            weatherData.put("aqi", -1);
            weatherData.put("aqiCategory", "N/A");
            weatherData.put("currentTime", LocalDateTime.now());
            weatherData.put("lastUpdated", LocalDateTime.now());
        }
        
        return weatherData;
    }

    private String getWindDirection(Double degrees) {
        if (degrees == null) return "N/A";
        
        String[] directions = {"N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
                             "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"};
        int index = (int) Math.round(((degrees % 360) / 22.5));
        return directions[index % 16];
    }

}
