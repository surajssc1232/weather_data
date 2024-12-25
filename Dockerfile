# filepath: /d:/projects/weather_data/Dockerfile
# Use Maven with Eclipse Temurin for build
FROM eclipse-temurin:17-jdk-focal

# Set the working directory
WORKDIR /app

# Copy the project files
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline

COPY src ./src
RUN ./mvnw clean package -DskipTests

# Make port 8080 available to the world outside this container
EXPOSE 8080

# Run the JAR file
CMD ["java", "-jar", "target/weather_data-0.0.1-SNAPSHOT.jar"]