package com.example.weather_data;

import com.example.weather_data.entity.DailyWeatherSummary;
import com.example.weather_data.repository.DailyWeatherSummaryRepository;
import com.example.weather_data.service.WeatherResponse;
import com.example.weather_data.service.WeatherService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@TestPropertySource(properties = {
    "OPENWEATHERMAP_API_KEY=test-api-key"
})
public class WeatherServiceTest {

    @Autowired
    private WeatherService weatherService;

    @MockBean
    private RestTemplate restTemplate;

    @MockBean
    private DailyWeatherSummaryRepository repository;

    @BeforeAll
    public static void setUp() {
        System.setProperty("OPENWEATHERMAP_API_KEY", "test-api-key");
    }

    @Test
    public void testGetWeather() {
        WeatherResponse mockResponse = new WeatherResponse();
        mockResponse.setName("TestCity");
        WeatherResponse.Main main = new WeatherResponse.Main();
        main.setTemp(300.15); // 27°C
        mockResponse.setMain(main);

        when(restTemplate.getForObject(anyString(), eq(WeatherResponse.class), anyMap()))
                .thenReturn(mockResponse);

        WeatherResponse response = weatherService.getWeather("TestCity");
        assertNotNull(response);
        assertEquals("TestCity", response.getName());
        assertEquals(300.15, response.getMain().getTemp(), 0.01);
    }

    @Test
    public void testConvertKelvinToCelsius() {
        double kelvin = 300.15;
        double celsius = weatherService.convertKelvinToCelsius(kelvin);
        assertEquals(27.0, celsius, 0.01);
    }

    @Test
    public void testGetWeatherStats() {
        LocalDate testDate = LocalDate.now();
        DailyWeatherSummary summary1 = new DailyWeatherSummary();
        summary1.setAverageTemp(25.0);
        summary1.setMaxTemp(28.0);
        summary1.setMinTemp(22.0);
        summary1.setDominantCondition("Sunny");

        DailyWeatherSummary summary2 = new DailyWeatherSummary();
        summary2.setAverageTemp(22.0);
        summary2.setMaxTemp(25.0);
        summary2.setMinTemp(19.0);
        summary2.setDominantCondition("Cloudy");

        when(repository.findByDate(testDate)).thenReturn(Arrays.asList(summary1, summary2));

        Map<String, Object> stats = weatherService.getWeatherStats(testDate);

        assertEquals(testDate, stats.get("date"));
        assertEquals("23.50", stats.get("averageTemp"));
        assertEquals("28.00", stats.get("maxTemp"));
        assertEquals("19.00", stats.get("minTemp"));
        assertEquals("Sunny", stats.get("dominantCondition"));
    }

    @Test
    public void testConsecutiveTemperatureAlerts() {
        // Set the thresholds for testing
        ReflectionTestUtils.setField(weatherService, "tempThreshold", 35.0);
        ReflectionTestUtils.setField(weatherService, "consecutiveThreshold", 2);

        WeatherResponse mockResponse = new WeatherResponse();
        mockResponse.setName("TestCity");
        WeatherResponse.Main main = new WeatherResponse.Main();
        mockResponse.setMain(main);

        // First update: above threshold
        main.setTemp(310.15); // 37°C
        boolean firstAlert = weatherService.checkAlertThresholds(mockResponse);
        assertFalse(firstAlert);

        // Second update: above threshold (should trigger alert)
        main.setTemp(311.15); // 38°C
        boolean secondAlert = weatherService.checkAlertThresholds(mockResponse);
        assertTrue(secondAlert);

        // Third update: below threshold (should reset counter)
        main.setTemp(300.15); // 27°C
        boolean thirdAlert = weatherService.checkAlertThresholds(mockResponse);
        assertFalse(thirdAlert);

        // Fourth update: above threshold (should not trigger alert yet)
        main.setTemp(310.15); // 37°C
        boolean fourthAlert = weatherService.checkAlertThresholds(mockResponse);
        assertFalse(fourthAlert);
    }

    @Test
    public void testActiveAlerts() {
        // Set the thresholds for testing
        ReflectionTestUtils.setField(weatherService, "tempThreshold", 35.0);
        ReflectionTestUtils.setField(weatherService, "humidityThreshold", 80);
        ReflectionTestUtils.setField(weatherService, "consecutiveThreshold", 2);

        WeatherResponse mockResponse = new WeatherResponse();
        mockResponse.setName("TestCity");
        WeatherResponse.Main main = new WeatherResponse.Main();
        mockResponse.setMain(main);

        // Trigger temperature alert
        main.setTemp(310.15); // 37°C
        weatherService.checkAlertThresholds(mockResponse);
        main.setTemp(311.15); // 38°C
        weatherService.checkAlertThresholds(mockResponse);

        // Trigger humidity alert
        main.setHumidity(85);
        weatherService.checkAlertThresholds(mockResponse);

        List<String> activeAlerts = weatherService.getActiveAlerts();
        assertEquals(2, activeAlerts.size());
        assertTrue(activeAlerts.get(0).contains("Temperature in TestCity has exceeded"));
        assertTrue(activeAlerts.get(1).contains("Humidity in TestCity has exceeded"));

        weatherService.clearAlerts();
        activeAlerts = weatherService.getActiveAlerts();
        assertTrue(activeAlerts.isEmpty());
    }
}
