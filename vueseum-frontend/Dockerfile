# Build stage
FROM node:20-slim AS builder
WORKDIR /app

# Copy package files first for caching
COPY vueseum-frontend/package*.json ./
RUN npm ci

# Copy frontend source code
COPY vueseum-frontend/. .

# Debug: Ensure all necessary files exist
RUN ls -la /app/

# Debug: Ensure svelte.config.js is present
RUN test -f /app/svelte.config.js && echo "Config exists" || echo "Missing svelte.config.js"

# Build application
RUN VITE_DEBUG=true npm run build

# Production stage
FROM node:20-slim
WORKDIR /app

# Create a non-root user
RUN useradd -r -u 1001 -g root vueseum

# Copy built application
COPY --from=builder /app/build ./build
COPY --from=builder /app/package*.json ./

# Install only production dependencies
RUN npm ci --production

# Set ownership to non-root user
RUN chown -R 1001:1001 /app
USER 1001

# Expose ports
EXPOSE 80 443

# Start the application
CMD ["node", "build"]
