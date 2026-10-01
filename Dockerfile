# ---------- build ----------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build


COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S app && adduser -S app -G app

WORKDIR /app
COPY --from=build /build/target/*.jar app.jar

USER app

EXPOSE 8080 9091

HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 CMD wget -q --spider http://127.0.0.1:${MANAGEMENT_PORT:-9091}/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

