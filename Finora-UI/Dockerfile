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

# envsubst is included in nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html

# Store nginx config as a template
COPY nginx.conf /etc/nginx/templates/default.conf.template

EXPOSE 80

# nginx:alpine's entrypoint auto-processes /etc/nginx/templates/*.template using envsubst
CMD ["nginx", "-g", "daemon off;"]

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost/ || exit 1

# Start NGINX
CMD ["nginx", "-g", "daemon off;"]