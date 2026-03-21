FROM maven:3.9.9-eclipse-temurin-21
WORKDIR /app
COPY . .
RUN mvn package -DskipTests
RUN cp /app/target/analyzer.jar .
ENTRYPOINT ["./docker-entrypoint.sh"]
