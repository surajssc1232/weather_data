package com.example.weather_data;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableScheduling
public class WeatherDataApplication {

	public static void main(String[] args) {
		Dotenv dotenv = Dotenv.load();
		String apiKey = System.getenv("OPENWEATHERMAP_API_KEY");
		if (apiKey == null || apiKey.isEmpty()) {
			apiKey = dotenv.get("OPENWEATHERMAP_API_KEY");
			if (apiKey != null && !apiKey.isEmpty()) {
				System.setProperty("OPENWEATHERMAP_API_KEY", apiKey);
			}
		}
		SpringApplication.run(WeatherDataApplication.class, args);
	}

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}
}
