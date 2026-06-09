FROM eclipse-temurin:25-jre

WORKDIR /app

RUN addgroup --system everytldr && adduser --system --ingroup everytldr everytldr

COPY build/libs/*.jar app.jar

USER everytldr

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
