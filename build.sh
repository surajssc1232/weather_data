#!/bin/bash
set -e

# Install dependencies using apt-get without sudo
apt-get update
apt-get install -y openjdk-17-jdk maven

# Set JAVA_HOME
export JAVA_HOME="/usr/lib/jvm/java-17-openjdk-amd64"

# Build the project
mvn clean package -DskipTests 