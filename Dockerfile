# ---------- build ----------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# baixa as dependencias numa camada propria, para o cache so invalidar
# quando o pom mudar
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------- runtime ----------
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S app && adduser -S app -G app

WORKDIR /app
COPY --from=build /build/target/*.jar app.jar

USER app

# 8080 = API publica | 9091 = actuator, apenas para a rede interna
EXPOSE 8080 9091

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
