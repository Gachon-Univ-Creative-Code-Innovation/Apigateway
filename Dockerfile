# 1단계: 빌드
FROM gradle:8.4.0-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle bootJar -x test --no-daemon

# 2단계: 실행
FROM eclipse-temurin:17-jdk-jammy
VOLUME /tmp
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8000
ENTRYPOINT ["java", "-jar", "/app.jar"]