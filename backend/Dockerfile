FROM eclipse-temurin:25-jre

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends wget \
    && rm -rf /var/lib/apt/lists/*

RUN addgroup --system everytldr && adduser --system --ingroup everytldr everytldr

COPY build/libs/*.jar app.jar

USER everytldr

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
