# syntax=docker/dockerfile:1

# --- build: plain mvn (mvnw is checked out with CRLF on Windows) ---
FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
COPY src src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -DskipTests package \
 && cp "$(ls target/*.jar | grep -v -- '-plain.jar' | head -n 1)" /src/app.jar

# --- runtime ---
FROM eclipse-temurin:21-jre-alpine
# POI autoSizeColumn and PDFBox need fontconfig + a system font on Alpine.
RUN apk add --no-cache fontconfig ttf-dejavu \
 && addgroup -S app && adduser -S app -G app \
 && mkdir -p /app/var/audit-spool && chown -R app:app /app
WORKDIR /app
COPY --from=build /src/app.jar app.jar
USER app

EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=90s --retries=5 \
    CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"UP"' || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
