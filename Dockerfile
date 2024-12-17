# filepath: /d:/projects/weather_data/Dockerfile
# Use an official Maven image with OpenJDK
FROM maven:3.8-openjdk-11 AS build

# Set the working directory
WORKDIR /app

# Copy the project files
COPY pom.xml .
COPY src ./src

# Build the application
RUN mvn clean install

# Use an official OpenJDK runtime as a parent image
FROM openjdk:11-jre-slim

# Set the working directory in the container
WORKDIR /

# Copy the JAR file from the build stage
COPY --from=build /app/target/weather_data-0.0.1-SNAPSHOT.jar app.jar

# Make port 8080 available to the world outside this container
EXPOSE 8080

# Run the JAR file
ENTRYPOINT ["java", "-jar", "app.jar"]