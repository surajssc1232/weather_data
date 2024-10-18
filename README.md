# Weather Data Application

This is a Spring Boot application that provides weather data through a RESTful API.

## Prerequisites

Before you begin, ensure you have met the following requirements:

* Java Development Kit (JDK) 11 or later
* Maven 3.6 or later
* Git
* An IDE of your choice (e.g., IntelliJ IDEA, Eclipse, VS Code)
* A free API key from OpenWeatherMap

## Setting Up the Project

1. Clone the repository:
   ```
   git clone https://github.com/yourusername/weather-data-application.git
   cd weather-data-application
   ```

2. Set up your API key:
   - Create a `.env` file in the root directory of the project
   - Add your OpenWeatherMap API key to the `.env` file:
     ```
     OPENWEATHERMAP_API_KEY=your_api_key_here
     ```

3. Build the project:
   ```
   mvn clean install
   ```

4. Run the application:
   ```
   mvn spring-boot:run
   ```

The application should now be running on `http://localhost:8080`.

## API Endpoints

- GET `/api/weather?city={cityName}` - Get current weather data for a specific city

## Running Tests

To run the automated tests for this system, use the following command:

```
mvn test
```

## Configuration

The application uses the following properties files:

- `src/main/resources/application.properties`: Contains general application configuration
- `src/main/resources/application-secret.properties`: Contains sensitive information (not tracked by Git)

Make sure to properly configure these files before running the application.

## Project Structure

- `src/main/java/com/example/weather_data/WeatherDataApplication.java`: Main application class
- `src/main/java/com/example/weather_data/controller/WeatherController.java`: REST controller for weather endpoints
- `src/main/java/com/example/weather_data/service/WeatherService.java`: Service layer for weather data retrieval
- `src/main/java/com/example/weather_data/service/WeatherResponse.java`: POJO for weather response data
- `src/test/java/com/example/weather_data/WeatherServiceTest.java`: Unit tests for WeatherService
- `src/test/java/com/example/weather_data/WeatherDataApplicationTests.java`: Integration tests for the application

## Environment Variables

The application uses the following environment variable:

- `OPENWEATHERMAP_API_KEY`: Your OpenWeatherMap API key (stored in `.env` file)

## Contributing

1. Fork the project
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a pull request

## Contact

Suraj Singh - surajssc1232@gmail.com

Project Link: [https://github.com/yourusername/weather-data-application](https://github.com/yourusername/weather-data-application)
