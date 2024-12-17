# Use an official Maven image with OpenJDK
FROM maven:3.8-openjdk-11

# Set the working directory
WORKDIR /app

# Copy the project files
COPY pom.xml .
COPY src ./src

# Build the application
RUN mvn clean install

# Copy the JAR file from the target directory
COPY target//weather_data-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
# Make port 8080 available

# Run the JAR file
ENTRYPOINT ["java", "-jar", "app.jar"]