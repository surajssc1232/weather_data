#!/bin/bash
set -e

# Update package list and install dependencies
sudo apt-get update
sudo apt-get install -y openjdk-17-jdk maven

# Set JAVA_HOME
export JAVA_HOME="/usr/lib/jvm/java-17-openjdk-amd64"

# Build the project
mvn clean package -DskipTests 