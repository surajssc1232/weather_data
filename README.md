# Weather Data Application

This application provides weather data, including current conditions, forecasts, and weather alerts. It's built using Spring Boot for the backend and Thymeleaf for the frontend.

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Setup](#setup)
3. [Running the Application](#running-the-application)
4. [API Endpoints](#api-endpoints)
5. [Frontend Usage](#frontend-usage)

## Prerequisites

Before you begin, ensure you have the following installed on your system:

- Java Development Kit (JDK) 11 or later
- Maven 3.6 or later
- Git (optional, for cloning the repository)

### Installing Java

1. Visit the [AdoptOpenJDK website](https://adoptopenjdk.net/)
2. Download and install the appropriate JDK version for your operating system
3. Set the JAVA_HOME environment variable to point to your JDK installation

### Installing Maven

1. Visit the [Apache Maven website](https://maven.apache.org/download.cgi)
2. Download the binary zip archive
3. Extract it to a directory of your choice
4. Add the `bin` directory to your system's PATH

## Setup

1. Clone the repository (or download and extract the ZIP file):   ```
   git clone https://github.com/yourusername/weather-data-application.git   ```

2. Navigate to the project directory:   ```
   cd weather-data-application   ```

3. Create a `.env` file in the project root directory with the following content:   ```
   WEATHER_API_KEY=your_api_key_here   ```
   Replace `your_api_key_here` with your actual Weather API key.

4. Update the `src/main/resources/application.properties` file with your database configuration if needed.

5. Install dependencies:   ```
   mvn clean install   ```

## Running the Application

To run the application, use the following command in the project root directory:

```
mvn spring-boot:run
```

The application will start, and you can access it at `http://localhost:8080`.

## API Endpoints

The application provides the following REST API endpoints:

1. Get current weather:
   - URL: `/api/weather/current`
   - Method: GET
   - Query Parameters:
     - `city` (required): The name of the city
   - Example: `http://localhost:8080/api/weather/current?city=London`

2. Get weather forecast:
   - URL: `/api/weather/forecast`
   - Method: GET
   - Query Parameters:
     - `city` (required): The name of the city
   - Example: `http://localhost:8080/api/weather/forecast?city=London`

3. Get weather alerts:
   - URL: `/api/weather/alerts`
   - Method: GET
   - Query Parameters:
     - `city` (required): The name of the city
   - Example: `http://localhost:8080/api/weather/alerts?city=London`

4. Get daily weather summary:
   - URL: `/api/weather/daily-summary`
   - Method: GET
   - Query Parameters:
     - `city` (required): The name of the city
     - `date` (required): The date for the summary (format: YYYY-MM-DD)
   - Example: `http://localhost:8080/api/weather/daily-summary?city=London&date=2023-04-15`

## Frontend Usage

The application provides two main pages for viewing weather information:

1. Weather Alerts Page:
   - URL: `/weather-alerts`
   - This page displays current weather alerts for a specified city.
   - To use:
     1. Navigate to `http://localhost:8080/weather-alerts`
     2. Enter a city name in the input field
     3. Click the "Get Alerts" button to view the alerts for that city

2. Weather Statistics Page:
   - URL: `/weather-stats`
   - This page shows various weather statistics for a specified city.
   - To use:
     1. Navigate to `http://localhost:8080/weather-stats`
     2. Enter a city name in the input field
     3. Click the "Get Statistics" button to view the weather statistics for that city

Both pages use Thymeleaf templates to render the data received from the backend API endpoints.

## Configuration

The application uses the following configuration files:

1. `src/main/resources/application.properties`: Contains general application settings.

2. `src/main/resources/application-secret.properties`: Contains sensitive information such as API keys and database credentials. This file should not be committed to version control.

3. `.env`: Contains environment-specific variables. This file should also not be committed to version control.

### Setting up application-secret.properties

Create a file named `application-secret.properties` in the `src/main/resources/` directory with the following content:

```properties
# API Keys
weather.api.key=${WEATHER_API_KEY}

# Database Configuration
spring.datasource.username=your_database_username
spring.datasource.password=your_database_password
```

Replace `your_database_username` and `your_database_password` with your actual database credentials.

The `WEATHER_API_KEY` will be read from the `.env` file, so make sure it's set there.

## Environment Variables

Create a `.env` file in the project root directory with the following content:

```
WEATHER_API_KEY=your_actual_weather_api_key
DB_USERNAME=your_database_username
DB_PASSWORD=your_database_password
```

Replace the placeholder values with your actual OpenWeatherMap API key and database credentials.

## Important Note

Both `application-secret.properties` and `.env` files contain sensitive information and should not be committed to version control. They are included in the `.gitignore` file to prevent accidental commits.

To use these environment variables in your application, you'll need to configure your IDE or deployment environment to load the `.env` file. For local development, you can use a library like `dotenv-java` to load these variables automatically.

## Database

The application uses a database to store daily weather summaries. The `DailyWeatherSummary` entity is used to represent this data. Make sure your database is set up correctly and the connection details in `application.properties` are accurate.

## Troubleshooting

If you encounter any issues while setting up or running the application, try the following:

1. Ensure all prerequisites are installed correctly.
2. Check that the Weather API key in the `.env` file is valid.
3. Verify that the database connection details in `application.properties` are correct.
4. If you're having trouble with Maven, try running `mvn clean install -U` to force update dependencies.

## Contributing

Contributions to this project are welcome. Please fork the repository and submit a pull request with your changes.


