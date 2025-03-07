FROM eclipse-temurin:21-jre

WORKDIR /app
COPY thermos.jar /app/thermos.jar

EXPOSE 8080

CMD ["java", "-XX:MaxRAMPercentage=80", "-XX:+UseG1GC", "-jar", "thermos.jar"]
