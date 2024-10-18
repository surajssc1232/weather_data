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

The application provides the following REST
