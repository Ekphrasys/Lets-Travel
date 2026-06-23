FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline -Dmaven.test.skip=true || true
COPY src ./src
RUN set -e; \
    for attempt in 1 2 3; do \
      mvn -q -B package -Dmaven.test.skip=true && exit 0; \
      echo "Maven build attempt ${attempt} failed, retrying..."; \
      sleep 5; \
    done; \
    exit 1

FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache wget
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --retries=10 --start-period=60s \
  CMD wget -qO- --no-check-certificate https://localhost:8080/actuator/health | grep -q UP || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
