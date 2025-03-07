FROM eclipse-temurin:21-jre

WORKDIR /app
COPY ../target/thermos.jar /app/thermos.jar

CMD ["java", "-XX:MaxRAMPercentage=80", "-XX:+UseG1GC", "-jar", "thermos.jar"]
