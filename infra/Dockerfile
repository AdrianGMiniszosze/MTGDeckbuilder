# Runtime image only; build the JAR with the Maven wrapper before docker build.
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
COPY --chown=app:app target/mtgdeckbuilder-*.jar /app/app.jar

EXPOSE 8080

# A production container must explicitly override this for local development.
ENV SPRING_PROFILES_ACTIVE=prod
USER app
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
