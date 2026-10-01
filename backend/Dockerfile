# Backend GeoLogix — build multi-etapa (Java 21, sin necesidad de Maven local).
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw mvnw.cmd pom.xml ./
RUN chmod +x mvnw && ./mvnw -q dependency:go-offline
COPY src/ src/
RUN ./mvnw -q package -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/target/backend-*.jar app.jar
EXPOSE 8080
# Render free = 512MB RAM: heap recortado, respeta PORT y JAVA_OPTS.
ENV JAVA_OPTS="-Xms128m -Xmx300m -XX:+UseSerialGC"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
