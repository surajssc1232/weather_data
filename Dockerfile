# Build stage
FROM maven:3.9.6-eclipse-temurin-17-focal AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/weather_data-0.0.1-SNAPSHOT.jar app.jar

# Add Spring Boot startup optimization flags
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75 -XX:InitialRAMPercentage=50 -Dspring.main.lazy-initialization=true"

EXPOSE 8080
CMD java $JAVA_OPTS -jar app.jar