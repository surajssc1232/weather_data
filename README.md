# Weather Data Application

This is a Spring Boot application that provides weather data through a RESTful API. It fetches data from OpenWeatherMap and offers various features including weather alerts, daily summaries, and air quality information.

## Prerequisites

Before you begin, ensure you have met the following requirements:

* Java Development Kit (JDK) 11 or later
* Maven 3.6 or later
* Git
* An IDE of your choice (e.g., IntelliJ IDEA, Eclipse, VS Code)
* A valid API key from OpenWeatherMap

### Setting up Prerequisites

#### 1. Install Java Development Kit (JDK)

1. Visit the [Oracle JDK download page](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html) or [OpenJDK](https://adoptopenjdk.net/).
2. Download the appropriate version for your operating system.
3. Run the installer and follow the installation wizard.
4. Set the JAVA_HOME environment variable:
   - On Windows: 
     1. Right-click on 'This PC' and select 'Properties'.
     2. Click on 'Advanced system settings'.
     3. Click on 'Environment Variables'.
     4. Under System Variables, click 'New'.
     5. Set Variable name as 'JAVA_HOME' and Variable value as the path to your JDK installation (e.g., C:\Program Files\Java\jdk-11).
   - On macOS/Linux:
     1. Open your shell configuration file (e.g., ~/.bash_profile, ~/.zshrc).
     2. Add the following line: `export JAVA_HOME=/path/to/your/jdk`
5. Add Java to your PATH:
   - On Windows: Add `%JAVA_HOME%\bin` to your Path environment variable.
   - On macOS/Linux: Add `export PATH=$JAVA_HOME/bin:$PATH` to your shell configuration file.
6. Verify the installation by opening a new terminal and typing: `java -version`

#### 2. Install Maven

1. Download Maven from the [official Apache Maven site](https://maven.apache.org/download.cgi).
2. Extract the archive to a directory of your choice.
3. Set the M2_HOME environment variable:
   - On Windows: Follow the same steps as for JAVA_HOME, but use 'M2_HOME' as the variable name and the path to your Maven installation as the value.
   - On macOS/Linux: Add `export M2_HOME=/path/to/maven` to your shell configuration file.
4. Add Maven to your PATH:
   - On Windows: Add `%M2_HOME%\bin` to your Path environment variable.
   - On macOS/Linux: Add `export PATH=$M2_HOME/bin:$PATH` to your shell configuration file.
5. Verify the installation by opening a new terminal and typing: `mvn -version`

#### 3. Install Git

1. Download Git from the [official Git website](https://git-scm.com/downloads).
2. Run the installer and follow the installation wizard.
3. Verify the installation by opening a new terminal and typing: `git --version`

#### 4. Configure Git (Optional)

1. Open a terminal or command prompt.
2. Set your name: `git config --global user.name "Your Name"`
3. Set your email: `git config --global user.email "youremail@example.com"`

Now that you have set up Java, Maven, and Git, you can proceed with setting up the project.

## Setting Up the Project

1. Clone the repository:
   ```
   git clone https://github.com/yourusername/weather-data-application.git
   cd weather-data-application
   ```

2. Set up your API key:
   - Sign up for a free account at OpenWeatherMap: https://home.openweathermap.org/users/sign_up
   - After signing up, navigate to your API keys page: https://home.openweathermap.org/api_keys
   - Generate a new API key if you don't have one already
   - Create a `.env` file in the root directory of the project:
   - FOR UNIX BASED OS : ```touch .env ```
   - Open the `.env` file with your preferred text editor
   - Add your OpenWeatherMap API key to the `.env` file:
     ```
     OPENWEATHERMAP_API_KEY=your_api_key_here
     ```
   - Save and close the `.env` file
   - Ensure that `.env` is added to your `.gitignore` file to keep your API key secure
   - Verify that the application can read the API key by running:
     ```
     grep OPENWEATHERMAP_API_KEY .env
     ```
   Note: Never commit your API key to version control. Keep it secret and secure.

3. Build the project:
   ```
   mvn clean install
   ```

4. Run the application:
   ```
   mvn spring-boot:run
   ```

The application should now be running on `http://localhost:8080`.

## Features

- Current weather data for Indian metro cities
- Weather alerts for temperature and humidity thresholds
- Daily weather summaries
- Air Quality Index (AQI) data
- Historical weather data
- Weather trends analysis

## API Endpoints

- GET `/api/weather?city={cityName}` - Get current weather data for a specific city
- GET `/api/weather/stats?date={date}` - Get weather statistics for a specific date
- GET `/api/weather/trends?startDate={startDate}&endDate={endDate}` - Get weather trends for a date range
- GET `/api/weather/alerts` - Get active weather alerts
- GET `/api/weather/aqi` - Get Air Quality Index data for Indian metro cities
- GET `/api/weather/trends` - Get weather trends for all Indian metro cities
- GET `/api/weather/trends/{city}` - Get weather trends for a specific city


## Configuration

The application uses the following properties files:

- `src/main/resources/application.properties`: Contains general application configuration

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
