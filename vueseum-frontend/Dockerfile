# Build stage
FROM node:20-slim AS builder
WORKDIR /app

# Debug: Show build context
RUN pwd
RUN ls -la /app

# Copy package files
COPY package*.json ./
RUN ls -la /app  # Show what got copied

# Copy source code
COPY . .
RUN ls -la /app  # Show everything that got copied

# Build application
RUN npm run build --verbose

# Final stage with Nginx
FROM nginx:stable-alpine
WORKDIR /app

# Install Node.js in final stage (needed for adapter-node)
RUN apk add --update nodejs npm

# Copy built application
COPY --from=builder /app/build /app
COPY --from=builder /app/package*.json /app/

# Install production dependencies
RUN npm ci --production

# Copy Nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Create directory for SSL certificates
RUN mkdir -p /etc/letsencrypt/live/vueseum.io \
    && mkdir -p /etc/letsencrypt/archive/vueseum.io

# Copy entrypoint script
COPY docker-entrypoint.sh /usr/local/bin/
RUN chmod +x /usr/local/bin/docker-entrypoint.sh

# Expose ports
EXPOSE 80 443

ENTRYPOINT ["docker-entrypoint.sh"]

# Start both Nginx and Node.js application
CMD ["sh", "-c", "nginx && node build"]
# CMD ["sh", "-c", "nginx & exec node build"]