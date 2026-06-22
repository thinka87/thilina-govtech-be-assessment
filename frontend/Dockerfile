# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM node:20-alpine AS build
WORKDIR /app

# Accept the API base URL as a build-time argument.
# Default matches docker-compose: browser → localhost:8080 (backend exposed port).
ARG  VITE_API_BASE_URL=http://localhost:8080/api/v1
ENV  VITE_API_BASE_URL=$VITE_API_BASE_URL

# Install dependencies first (separate layer for better cache reuse).
COPY package.json package-lock.json ./
RUN npm ci --prefer-offline

# Copy source and build the Vite app.
COPY . .
RUN npm run build

# ── Stage 2: Serve ────────────────────────────────────────────────────────────
FROM nginx:1.27-alpine AS runtime

# Remove the default Nginx welcome page.
RUN rm -rf /usr/share/nginx/html/*

# Copy the compiled assets from the build stage.
COPY --from=build /app/dist /usr/share/nginx/html

# Use our SPA-aware Nginx config (replaces the default server block).
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Port 80 — matches docker-compose "3000:80" mapping.
EXPOSE 80

# Lightweight health check (wget ships with alpine Nginx image).
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget -qO- http://localhost/ > /dev/null || exit 1

CMD ["nginx", "-g", "daemon off;"]
