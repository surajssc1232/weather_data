#!/bin/bash
set -e

# Install SDKMAN
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"

# Install Java and Maven
sdk install java 17.0.9-tem
sdk install maven

# Set JAVA_HOME (SDKMAN will handle this)
echo "Using Java version:"
java -version

echo "Using Maven version:"
mvn -version

# Build the project
mvn clean package -DskipTests 