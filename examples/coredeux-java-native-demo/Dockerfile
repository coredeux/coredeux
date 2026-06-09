FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY . .

RUN mvn -f pom.xml -DskipTests install
RUN mvn -f examples/coredeux-java-native-demo/pom.xml -DskipTests package

FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY --from=build /workspace/examples/coredeux-java-native-demo/target/coredeux-java-native-demo-0.1.0-SNAPSHOT.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
