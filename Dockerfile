# ---- build stage ----
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy the pom.xml and dependencies to the container
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

# Copy the source code to the container and build the application
COPY src/ src/
RUN mvn -q -DskipTests package

# ---- runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app

USER root
RUN apt-get update && apt-get install -y --no-install-recommends curl \
  && rm -rf /var/lib/apt/lists/*
RUN useradd -r -u 1001 appuser
USER appuser

COPY --from=build /app/target/todo-service.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=20s --retries=3 \
  CMD curl -fsS http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java","-jar","/app/app.jar"]
