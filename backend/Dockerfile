FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S desk && adduser -S desk -G desk
COPY desk-app/target/service-desk-1.0.0.jar /app/app.jar
USER desk
ENV BIND_ADDRESS=0.0.0.0 PORT=8080
EXPOSE 8080
ENTRYPOINT ["java","-XX:MaxRAMPercentage=70.0","-jar","/app/app.jar"]
