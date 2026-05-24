FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY backend/.mvn/ /app/.mvn/
COPY backend/mvnw /app/mvnw
COPY backend/pom.xml /app/pom.xml
COPY backend/src/ /app/src/
COPY frontend/public/ /app/frontend/public/

RUN chmod +x ./mvnw
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# Install pg_dump / psql for the DB backup feature.
# Uses the official PGDG apt repository to match Render's PostgreSQL 16 server version.
RUN apt-get update \
    && apt-get install -y --no-install-recommends gnupg lsb-release curl \
    && curl -fsSL https://www.postgresql.org/media/keys/ACCC4CF8.asc \
       | gpg --dearmor -o /usr/share/keyrings/postgresql.gpg \
    && echo "deb [signed-by=/usr/share/keyrings/postgresql.gpg] https://apt.postgresql.org/pub/repos/apt $(lsb_release -cs)-pgdg main" \
       > /etc/apt/sources.list.d/pgdg.list \
    && apt-get update \
    && apt-get install -y --no-install-recommends postgresql-client-17 \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/*.jar /app/app.jar
COPY --from=build /app/frontend/public/ /app/frontend/public/

EXPOSE 10000
ENTRYPOINT ["java", "-Dserver.port=10000", "-jar", "/app/app.jar"]
