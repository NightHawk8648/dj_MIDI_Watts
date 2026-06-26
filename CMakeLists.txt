FROM node:20-bullseye-slim

# Create app directory
WORKDIR /app

# Copy the entire project
COPY . .

# Install dependencies for desktop hub
WORKDIR /app/desktop
RUN npm install

# Expose the port (Cloud Run supplies the PORT environment variable)
ENV PORT=8080
EXPOSE 8080

# Run the backend
CMD ["npm", "start"]
