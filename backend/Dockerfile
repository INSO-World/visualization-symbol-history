FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY . .
RUN ./mvnw install -f analyzer/pom.xml -DskipTests
EXPOSE 8080
ENTRYPOINT ./mvnw spring-boot:run -f backend/pom.xml -DskipTests

#FROM eclipse-temurin:21-jdk AS build
#WORKDIR /app
#COPY . .
#RUN ./mvnw clean package
#
#FROM eclipse-temurin:21-jre AS run
#WORKDIR /app
#COPY --from=build /app/target/*.jar backend.jar
#EXPOSE 8080
#ENTRYPOINT java -jar backend.jar
