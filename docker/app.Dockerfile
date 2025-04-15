FROM eclipse-temurin:21-jre

WORKDIR /app
RUN apt-get update

# add libc and libgomp, these are required for XGBoost
RUN apt-get install -y libc6 libgomp1

# add SCIP, this is used to solve the network problems
RUN curl -SL https://www.scipopt.org/download/release/SCIPOptSuite-9.2.1-Linux-ubuntu24.deb -o scip.deb
RUN apt-get install -y ./scip.deb && rm scip.deb

COPY thermos.jar /app/thermos.jar

EXPOSE 3000

CMD ["java", "-XX:MaxRAMPercentage=80", "-XX:+UseG1GC", "-jar", "thermos.jar"]
