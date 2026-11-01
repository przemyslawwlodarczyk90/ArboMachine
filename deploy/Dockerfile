# syntax=docker/dockerfile:1
# Obraz ArboMachine (linux/amd64): frontend React wbudowany w JAR Spring Boot 4, runtime JRE 25.
# Budowany LOKALNIE (deploy/build-image.sh), nigdy na serwerze t3.small. Kontekst = korzen repozytorium:
#   docker build --platform linux/amd64 -f deploy/Dockerfile -t arbomachine:<wersja> .

# --- 1. Frontend: npm run build zapisuje pliki do src/main/resources/static ---
FROM node:22-alpine AS frontend
WORKDIR /build/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN mkdir -p ../src/main/resources/static && npm run build

# --- 2. Backend: JAR z gotowym frontendem (testy uruchamia sie przed budowaniem obrazu - deploy/README.md) ---
FROM maven:3.9-eclipse-temurin-25 AS backend
WORKDIR /build
COPY pom.xml ./
RUN mvn -q -B dependency:go-offline || true
COPY src/ src/
COPY --from=frontend /build/src/main/resources/static/ src/main/resources/static/
RUN mvn -q -B clean package -DskipTests \
    && cp "$(ls target/arbomachine-*.jar | grep -v plain | head -n 1)" /build/app.jar

# --- 3. Runtime: tylko JRE 25 + JAR ---
FROM eclipse-temurin:25-jre
ARG APP_VERSION=dev
ARG GIT_COMMIT=unknown
LABEL org.opencontainers.image.title="ArboMachine" \
      org.opencontainers.image.version="${APP_VERSION}" \
      org.opencontainers.image.revision="${GIT_COMMIT}"
# Pamiec (zmierzone 01.11.2026, AWS_RESOURCE_AUDIT.md): spoczynek ~300 MB, PDF ze zdjeciami ~650-680 MB RSS przy
# Xmx 384m. Nie zwiekszac Xmx bez pomiaru; kontener: mem_limit 768m. Serial GC = najmniejszy narzut.
ENV TZ=Europe/Warsaw \
    SPRING_PROFILES_ACTIVE=prod \
    ARBOMACHINE_PORT=8083 \
    ARBOMACHINE_TREE_PHOTOS_DIRECTORY=/app/storage/tree-photos \
    JAVA_OPTS="-Xms128m -Xmx384m -XX:MaxMetaspaceSize=192m -XX:ReservedCodeCacheSize=64m -XX:MaxDirectMemorySize=64m -Xss512k -XX:+UseSerialGC -XX:+ExitOnOutOfMemoryError"
RUN groupadd --system arbo && useradd --system --gid arbo --home-dir /app arbo \
    && mkdir -p /app/storage/tree-photos && chown -R arbo:arbo /app
WORKDIR /app
COPY --from=backend --chown=arbo:arbo /build/app.jar /app/app.jar
USER arbo
EXPOSE 8083
# Gotowosc = /actuator/health UP (po migracjach Flyway). Bez curl w obrazie - bash /dev/tcp.
HEALTHCHECK --interval=30s --timeout=5s --start-period=120s --retries=3 \
  CMD ["bash", "-c", "exec 3<>/dev/tcp/127.0.0.1/${ARBOMACHINE_PORT} && printf 'GET /actuator/health HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n' >&3 && grep -q '\"status\":\"UP\"' <&3"]
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
