FROM eclipse-temurin:17-jdk-alpine

# glibc 호환성을 위한 gcompat 설치
RUN apk add --no-cache gcompat

# 애플리케이션 JAR 파일을 컨테이너로 복사
ARG JAR_FILE=./build/libs/*.jar
COPY ${JAR_FILE} /app.jar

# Spring 프로파일 설정
ENV SPRING_PROFILES_ACTIVE=prod

# 애플리케이션 실행
ENTRYPOINT ["java", "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}", "-jar", "/app.jar"]
