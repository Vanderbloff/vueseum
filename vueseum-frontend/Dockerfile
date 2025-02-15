# Build stage
FROM node:20-slim AS builder
WORKDIR /app

# Copy package files first
COPY package*.json ./
RUN npm ci --verbose

# Copy the entire project
COPY . .

# Ensure the src directory structure is correct
RUN echo "Verifying source structure:" && \
    ls -la src && \
    ls -la src/routes

# Build with specific environment setup
ENV NODE_ENV="production"
ENV VITE_BUILD_DEBUG="true"
RUN npm run build --verbose

# Production stage
FROM node:20-slim
WORKDIR /app

# Copy the built application
COPY --from=builder /app/build ./build
COPY --from=builder /app/package.json ./package.json

# Install production dependencies
RUN npm ci --production --verbose

EXPOSE 3000

# Start the application
CMD ["node", "build"]