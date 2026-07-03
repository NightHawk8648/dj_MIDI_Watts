FROM php:8.2-cli

WORKDIR /app

# Copy the entire workspace
COPY . .

# Cloud Run injects the PORT environment variable. We default to 8080 if not set.
# The built-in PHP server routes all requests to desktop/router.php
CMD php -S 0.0.0.0:${PORT:-8080} -t . desktop/router.php
