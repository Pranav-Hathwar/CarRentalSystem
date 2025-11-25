# Use a lightweight Java image
FROM openjdk:17-jdk-slim

# Set working directory
WORKDIR /app

# Copy all project files
COPY . .

# Create bin directory for compilation
RUN mkdir -p bin

# Compile the application
RUN javac -d bin -sourcepath src src/com/carrental/ui/WebServerLauncher.java

# Expose the port the app runs on
EXPOSE 8080

# Command to run the application
CMD ["java", "-cp", "bin", "com.carrental.ui.WebServerLauncher"]
