# syntax=docker/dockerfile:1.7

FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY . .

ARG MAVEN_RETRY_OPTS="-B -ntp -Dmaven.wagon.http.retryHandler.count=5 -Dmaven.wagon.httpconnectionManager.ttlSeconds=120"

RUN --mount=type=cache,target=/root/.m2 mvn ${MAVEN_RETRY_OPTS} -f pom.xml -DskipTests install
RUN --mount=type=cache,target=/root/.m2 mvn ${MAVEN_RETRY_OPTS} -f examples/coredeux-drl-spring-boot-demo/pom.xml -DskipTests package

FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY --from=build /workspace/examples/coredeux-drl-spring-boot-demo/target/coredeux-drl-spring-boot-demo-0.1.0-SNAPSHOT.jar /app/app.jar

EXPOSE 8080 5005

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
