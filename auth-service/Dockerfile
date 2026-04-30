# syntax=docker/dockerfile:1.7-labs
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml pom.xml
COPY config-server config-server
COPY eureka-server eureka-server
COPY api-gateway api-gateway
COPY auth-service auth-service
COPY event-service event-service
COPY registration-service registration-service
COPY notification-service notification-service
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp -pl auth-service -am clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/auth-service/target/auth-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
