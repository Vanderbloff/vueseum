# Build stage
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /app

# Copy pom.xml first to cache dependencies
COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw .
RUN chmod +x mvnw

# Download dependencies
RUN ./mvnw dependency:go-offline

# Copy source code
COPY src ./src

# Build application
RUN ./mvnw package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Create a non-root user
RUN useradd -r -u 1001 -g root vueseum

# Copy JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Set ownership to non-root user
RUN chown 1001 app.jar

USER root
RUN mkdir -p /var/log/vueseum && \
    chown -R 1001:1001 /var/log/vueseum

# Switch to non-root user
USER 1001

# Expose port
EXPOSE 8080

# Run application
ENTRYPOINT ["java", "--enable-preview", "-jar", "app.jar"]