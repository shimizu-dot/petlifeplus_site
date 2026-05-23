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

COPY --from=build /app/target/*.jar /app/app.jar

EXPOSE 10000
ENTRYPOINT ["java", "-Dserver.port=10000", "-jar", "/app/app.jar"]
