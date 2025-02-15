# Build stage
FROM node:20-slim AS builder
WORKDIR /app

# Copy package files
COPY package*.json ./
RUN npm ci

# Copy source code
COPY . .

# Build application
RUN npm run build

# Production stage
FROM node:20-slim
WORKDIR /app

# Copy built application and package files
COPY --from=builder /app/build ./build
COPY --from=builder /app/package*.json ./

# Create SSL directory and copy certificates
RUN mkdir -p /etc/letsencrypt
COPY --from=builder /etc/letsencrypt/live/vueseum.io /etc/letsencrypt/live/vueseum.io
COPY --from=builder /etc/letsencrypt/archive/vueseum.io /etc/letsencrypt/archive/vueseum.io

# Install only production dependencies
RUN npm ci --production

# Expose ports
EXPOSE 80 443

# Start the application
CMD ["node", "build"]