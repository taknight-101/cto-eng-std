# Build context MUST be the repository root (see infra/docker-compose.yml `context: ..`).
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
COPY security-platform-lib/pom.xml security-platform-lib/
COPY http-client-lib/pom.xml http-client-lib/
COPY common-lib/pom.xml common-lib/
COPY spring-api-example/pom.xml spring-api-example/
COPY downstream-api/pom.xml downstream-api/
RUN mvn -q -B dependency:go-offline || true

COPY . .
RUN mvn -q -pl downstream-api -am -DskipTests package

FROM eclipse-temurin:21-jre-jammy AS otel
WORKDIR /otel
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar /otel/opentelemetry-javaagent.jar

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends wget && rm -rf /var/lib/apt/lists/*
COPY --from=build /workspace/downstream-api/target/downstream-api.jar /app/app.jar
COPY --from=otel /otel/opentelemetry-javaagent.jar /otel/opentelemetry-javaagent.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
