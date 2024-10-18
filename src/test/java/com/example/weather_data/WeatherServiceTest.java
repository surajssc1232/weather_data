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
        WeatherResponse.MainData main = new WeatherResponse.MainData();
        main.setTemp(300.15); // 27°C
        mockResponse.setMain(main);

        when(restTemplate.getForObject(anyString(), eq(WeatherResponse.class), anyMap()))
                .thenReturn(mockResponse);

        WeatherResponse response = weatherService.getWeather("TestCity");
        assertNotNull(response);
        assertEquals("TestCity", response.getName());
        assertEquals(300.15, response.getMain().getTemp(), 0.01);
    }

    // ... other test methods remain unchanged ...

    @Test
    public void testActiveAlerts() {
        // Set the thresholds for testing
        ReflectionTestUtils.setField(weatherService, "tempThreshold", 35.0);
        ReflectionTestUtils.setField(weatherService, "humidityThreshold", 80);
        ReflectionTestUtils.setField(weatherService, "consecutiveThreshold", 2);

        WeatherResponse mockResponse = new WeatherResponse();
        mockResponse.setName("TestCity");
        WeatherResponse.MainData main = new WeatherResponse.MainData();
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

    @Test
    void testCheckAlertThresholds() {
        // Create a mock WeatherResponse
        WeatherResponse mockResponse = new WeatherResponse();
        
        // Create and set up the MainData object
        WeatherResponse.MainData main = new WeatherResponse.MainData();
        main.setTemp(308.15); // 35°C in Kelvin
        main.setHumidity(85);
        
        // Set the MainData object to the mockResponse
        mockResponse.setMain(main);

        // Set other necessary fields of mockResponse
        mockResponse.setName("TestCity");

        // Call the method under test
        boolean result = weatherService.checkAlertThresholds(mockResponse);

        // Assert the result
        assertTrue(result, "Alert threshold should be triggered");

        // You may want to add more assertions here based on the expected behavior
        // For example, checking if the correct alerts were added to the activeAlerts list
    }
}

