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

# Build the executable Spring Boot JAR (skip tests during container build)
RUN mvn clean package -DskipTests

# ------------------------------------------
# TEMPORARY DIAGNOSTIC: Test DNS resolution & TCP port 24035 connectivity to Aiven MySQL from Render build container
# ------------------------------------------
RUN echo "=== [DIAGNOSTIC] Checking DNS & TCP connectivity to Aiven MySQL ===" && \
    (getent ahosts mysql-1326c09a-servicemate.f.aivencloud.com || echo "Host resolution fallback") && \
    (curl -v --connect-timeout 8 telnet://mysql-1326c09a-servicemate.f.aivencloud.com:24035 2>&1 || true) && \
    echo "=== [DIAGNOSTIC] Network check completed ==="

# ------------------------------------------
# Stage 2: Lightweight Java 17 Runtime
# ------------------------------------------
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copy the built JAR from the builder stage
COPY --from=build /app/target/*.jar app.jar

# Expose default port (Render will dynamically override via PORT env variable)
EXPOSE 8085

# Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
