FROM maven:3-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml .
COPY src ./src

RUN mvn -B -DskipTests package \
    && cp target/backend-*.jar /app.jar

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app.jar ./app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]