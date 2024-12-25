# filepath: /d:/projects/weather_data/Dockerfile
# Use Maven with Eclipse Temurin for build
FROM eclipse-temurin:17-jdk-focal

# Install Maven
RUN apt-get update && \
    apt-get install -y maven

# Set the working directory
WORKDIR /app

# Copy the project files
COPY pom.xml ./
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Make port 8080 available to the world outside this container
EXPOSE 8080

# Run the JAR file
CMD ["java", "-jar", "target/weather_data-0.0.1-SNAPSHOT.jar"]