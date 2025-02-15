FROM node:20-slim
WORKDIR /app

# Copy built application and package files
COPY build ./build
COPY package*.json ./

# Install only production dependencies
RUN npm ci --production

# Expose port
EXPOSE 3000

# Start the application
CMD ["node", "build"]