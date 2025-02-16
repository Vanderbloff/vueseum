FROM node:20-slim

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy built application and package files
COPY build ./build
COPY package*.json ./

# Install only production dependencies
RUN npm ci --production

# Expose port
EXPOSE 3000

# Start the application (modified to be more explicit)
CMD ["node", "build/index.js"]
