FROM maven:3.9-eclipse-temurin-25

WORKDIR /app

# Copy pom.xml and source code
COPY pom.xml .
COPY src ./src

# Expose Spring Boot port
EXPOSE 8080

# Run Spring Boot with DevTools for hot reload
CMD ["mvn", "spring-boot:run"]