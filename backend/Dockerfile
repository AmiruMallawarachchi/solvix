FROM maven:3.9.9-eclipse-temurin-17-alpine AS builder
WORKDIR /workspace
COPY pom.xml .
RUN mvn --batch-mode --no-transfer-progress dependency:go-offline
COPY src ./src
RUN mvn --batch-mode --no-transfer-progress clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S -g 10001 solvix && adduser -S -D -H -u 10001 -G solvix solvix
COPY --from=builder --chown=solvix:solvix /workspace/target/solvix-backend-*.jar /app/app.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
