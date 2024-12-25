#!/bin/bash
set -e

# Create directories in home
mkdir -p "$HOME/java"
mkdir -p "$HOME/maven"

# Download and install Java
curl -L "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.9%2B9/OpenJDK17U-jdk_x64_linux_hotspot_17.0.9_9.tar.gz" -o java.tar.gz
tar -xzf java.tar.gz -C "$HOME/java" --strip-components=1
export JAVA_HOME="$HOME/java"
export PATH="$JAVA_HOME/bin:$PATH"

# Download and install Maven
curl -L "https://dlcdn.apache.org/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz" -o maven.tar.gz
tar -xzf maven.tar.gz -C "$HOME/maven" --strip-components=1
export PATH="$HOME/maven/bin:$PATH"

# Clean up downloaded archives
rm java.tar.gz maven.tar.gz

# Verify installations
echo "Java version:"
java -version
echo "Maven version:"
mvn -version

# Build the project
mvn clean package -DskipTests 