# Build and development stage
FROM node:20-slim AS base

WORKDIR /app

# First copy just the lock files to leverage caching
COPY package-lock.json package.json ./

# Now install dependencies
RUN npm ci

# Install TypeScript and its dependencies explicitly
RUN npm install -D typescript @tsconfig/svelte @types/node

# Set development or production setup
FROM base AS final
WORKDIR /app

# Copy source code
COPY . .

# Set default to production, can be overridden in compose
ENV NODE_ENV=production
ENV PORT=3000

# Expose ports
EXPOSE 3000
EXPOSE 24678

# Set up entrypoint
COPY docker-entrypoint.sh /usr/local/bin/
RUN chmod +x /usr/local/bin/docker-entrypoint.sh
ENTRYPOINT ["docker-entrypoint.sh"]

# Default to production start
CMD ["node", "build"]