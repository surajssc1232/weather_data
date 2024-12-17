# filepath: /d:/projects/weather_data/Dockerfile
# Use Eclipse Temurin for the build stage
FROM eclipse-temurin:17-jdk AS build

# Set the working directory
WORKDIR /app

# Copy the project files
COPY pom.xml .
COPY src ./src

# Build the application
RUN ./mvnw clean install -DskipTests

# Use Eclipse Temurin JRE for the runtime stage
FROM eclipse-temurin:17-jre

# Set the working directory in the container
WORKDIR /

# Copy the JAR file from the build stage
COPY --from=build /app/target/weather_data-0.0.1-SNAPSHOT.jar app.jar

# Make port 8080 available to the world outside this container
EXPOSE 8080

# Run the JAR file
ENTRYPOINT ["java", "-jar", "app.jar"]