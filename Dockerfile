FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /src
COPY pom.xml .
COPY src ./src
RUN apt-get update && apt-get install -y maven && \
    mvn -B package -DskipTests && \
    rm -rf /root/.m2

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /src/target/sovereign-brain-1.0.0-SOVEREIGN.jar /app/sovereign-brain.jar
ENTRYPOINT ["java", "-jar", "/app/sovereign-brain.jar"]
