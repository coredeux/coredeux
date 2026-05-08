FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace

COPY . .

RUN mvn -pl examples/coredeux-demo -am -DskipTests package

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /workspace/examples/coredeux-demo/target/coredeux-demo-0.1.0-SNAPSHOT.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
