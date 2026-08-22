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
# TEMPORARY DIAGNOSTIC: Test MySQL Protocol & TLS Handshake to Aiven MySQL (Zero Secrets)
# ------------------------------------------
RUN apt-get update -qq && apt-get install -y -qq default-mysql-client openssl > /dev/null 2>&1 && \
    echo "=== [DIAGNOSTIC 1: OpenSSL MySQL STARTTLS Handshake] ===" && \
    (echo "QUIT" | openssl s_client -starttls mysql -connect mysql-1326c09a-servicemate.f.aivencloud.com:24035 2>&1 | grep -E "SSL handshake|Protocol|Cipher|Verify return code|subject=|CONNECTED" || true) && \
    echo "=== [DIAGNOSTIC 2: MySQL Client Protocol & Auth Stage Probe] ===" && \
    (mysql -h mysql-1326c09a-servicemate.f.aivencloud.com -P 24035 -u avnadmin --ssl-mode=REQUIRED --connect-timeout=10 -e "SELECT 1;" 2>&1 || true) && \
    echo "=== [DIAGNOSTIC COMPLETED] ==="

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
