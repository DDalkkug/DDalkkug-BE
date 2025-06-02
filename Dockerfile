# Dockerfile

# jdk17 Image Start
FROM openjdk:17

ARG JAR_FILE=build/libs/backend-0.0.1-SNAPSHOT.jar
ADD ${JAR_FILE} drumtong_Backend.jar
ENTRYPOINT ["java","-jar","-Duser.timezone=Asia/Seoul","drumtong_Backend.jar"]