# Build stage
FROM node:20-slim AS builder
WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies with verbose logging
RUN npm ci --verbose

# Copy source code
COPY . .

# Add verbose logging for build process
ENV NODE_OPTIONS="--trace-warnings --trace-deprecation"
ENV NPM_CONFIG_LOGLEVEL="verbose"
ENV VITE_LOG_LEVEL="debug"

# Add specific build configurations
ENV NODE_ENV="production"
ENV VITE_BUILD_DEBUG="true"

# Run build with detailed logging
RUN set -x && npm run build --verbose

# Production stage
FROM node:20-slim
WORKDIR /app

# Copy built application and package files
COPY --from=builder /app/build ./build
COPY --from=builder /app/package*.json ./

# Install only production dependencies with logging
RUN npm ci --production --verbose

# Expose port
EXPOSE 3000

# Start with trace logging enabled
ENV NODE_OPTIONS="--trace-warnings"
CMD ["node", "build"]