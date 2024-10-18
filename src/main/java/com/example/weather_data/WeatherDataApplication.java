package com.example.weather_data;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WeatherDataApplication {

	public static void main(String[] args) {
		SpringApplication.run(WeatherDataApplication.class, args);
	}
}
