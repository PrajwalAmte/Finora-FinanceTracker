# Multi-stage build for React frontend
FROM node:20-alpine AS build

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm ci

# Copy source code
COPY . .

# Build with API path
ENV VITE_API_BASE_URL=/api

# Build the application
RUN npm run build

# Production stage with NGINX
FROM nginx:alpine

# Copy built assets from build stage
COPY --from=build /app/dist /usr/share/nginx/html

# Copy custom NGINX configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Expose port (Railway sets PORT env var)
EXPOSE 80

# Start NGINX - it will use PORT env var from Railway
CMD ["sh", "-c", "nginx -g 'daemon off; events { worker_connections 256; }'"]

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost/ || exit 1

# Start NGINX
CMD ["nginx", "-g", "daemon off;"]