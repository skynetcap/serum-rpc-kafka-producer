#
# Build stage
#
FROM maven:3.8.6-eclipse-temurin-17 AS build

# serum-data
COPY src /home/app/src
COPY pom.xml /home/app
RUN mvn -T 1C -f /home/app/pom.xml clean package -DskipTests -Dmaven.test.skip

#
# Package stage
#
FROM openjdk:17.0.2-slim
COPY --from=build /home/app/target/serum-kafka-producer-1.0-SNAPSHOT.jar /usr/local/lib/serum-kafka-producer.jar
#ENV JAVA_TOOL_OPTIONS -agentlib:jdwp=transport=dt_socket,address=*:8000,server=y,suspend=n
EXPOSE 8080
ENTRYPOINT ["java","-jar","/usr/local/lib/serum-kafka-producer.jar"]