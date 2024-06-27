# Use a base image with Java 22 and Maven
FROM maven:3.9.7-sapmachine-22

# Set the working directory
WORKDIR /app


# Set the timezone
ENV TZ=Europe/Kiev

# Copy the Maven project descriptor and source files
COPY pom.xml .
COPY src ./src

# Copy application properties
COPY src/main/resources/application.properties /app/application.properties

# Package the application
RUN mvn clean package -DskipTests

# Expose the application port
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "target/open-day-bot-telegram-0.1.jar"]