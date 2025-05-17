FROM openjdk:24-jdk-slim
WORKDIR /./
COPY target/*.jar BotC-Discord.jar
CMD ["java", "-jar", "./BotC-Discord.jar"]
