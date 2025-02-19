# Build stage
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /app

# Copy Maven configuration files first (for better caching)
COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw .

# Make mvnw executable
RUN chmod +x mvnw

# Download dependencies separately
RUN ./mvnw dependency:go-offline -DskipTests

# Copy source code
COPY src ./src

# Build application
RUN ./mvnw package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Create non-root user
RUN useradd -r -u 1001 -g root vueseum

# Copy JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Set ownership to non-root user
RUN chown 1001 app.jar

# Create and set ownership of logs directory
USER root
RUN mkdir -p /var/log/vueseum && \
    chown -R 1001:1001 /var/log/vueseum && \
    chmod -R 755 /var/log/vueseum

# Switch to non-root user
USER 1001

# Expose port
EXPOSE 8080

# Run application
ENTRYPOINT ["java", "--enable-preview", "-jar", "app.jar"]