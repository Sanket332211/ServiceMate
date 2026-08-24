# ==========================================
# ServiceMate Backend - Multi-Stage Dockerfile
# Optimized for Java 17 / Spring Boot 3 on Render
# ==========================================

# ------------------------------------------
# Stage 1: Build the application with Maven & JDK 17
# ------------------------------------------
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom.xml and download dependencies for caching layer
COPY pom.xml .
COPY src ./src

# Build the executable Spring Boot JAR and compile standalone diagnostic class
RUN mvn clean package -DskipTests && \
    javac -d /app/diagnostic-classes src/main/java/com/example/carservice/diagnostic/RuntimeNetworkDiagnostic.java

# ------------------------------------------
# Stage 2: Lightweight Java 17 Runtime
# ------------------------------------------
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Install lightweight diagnostic tools (netcat, curl, openssl, dnsutils) in the runtime container
RUN apt-get update -qq && \
    apt-get install -y -qq --no-install-recommends netcat-openbsd curl openssl dnsutils > /dev/null 2>&1 && \
    rm -rf /var/lib/apt/lists/*

# Copy the built JAR and diagnostic classes from the builder stage
COPY --from=build /app/target/*.jar app.jar
COPY --from=build /app/diagnostic-classes /app/diagnostic-classes
COPY docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod +x /app/docker-entrypoint.sh

# Expose default port (Render will dynamically override via PORT env variable)
EXPOSE 8085

# Execute the runtime diagnostic script before launching Spring Boot
ENTRYPOINT ["/bin/sh", "/app/docker-entrypoint.sh"]
