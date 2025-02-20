# 1단계 : 빌드 환경
FROM eclipse-temurin:17-jdk-alpine AS builder
# 작업 디렉토리 설정
WORKDIR /app
# 애플리케이션 JAR 파일을 컨테이너로 복사
ARG JAR_FILE=./build/libs/*.jar
COPY ${JAR_FILE} /app.jar

# 2단계 : 실행 환경
FROM eclipse-temurin:17-jre-alpine
# glibc 호환성을 위한 gcompat 설치
RUN apk add --no-cache gcompat
# 빌드된 JAR 파일을 복사
COPY --from=builder /app.jar /app.jar
# 애플리케이션 실행
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "/app.jar"]