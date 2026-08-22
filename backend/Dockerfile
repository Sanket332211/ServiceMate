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
# Stage 2: Lightweight Java 17 Runtime
# ------------------------------------------
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copy the built JAR from the builder stage
COPY --from=build /app/target/*.jar app.jar

# Expose default port (Render will dynamically override via PORT env variable)
EXPOSE 8085

# Run the Spring Boot application with IPv4 stack preference for cloud database connectivity
ENTRYPOINT ["java", "-Djava.net.preferIPv4Stack=true", "-Djava.net.preferIPv4Addresses=true", "-jar", "app.jar"]
