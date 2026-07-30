FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /workspace

COPY pom.xml .
COPY src src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN mkdir -p /app/data/documents \
    && chown -R 10001:10001 /app

COPY --from=builder --chown=10001:10001 \
    /workspace/target/legal-case-management-*.jar \
    /app/application.jar

ENV LEGAL_DOCUMENT_STORAGE_ROOT=/app/data/documents

EXPOSE 8080

USER 10001:10001

ENTRYPOINT ["java", "-jar", "/app/application.jar"]
