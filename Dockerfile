FROM openjdk:17

# 애플리케이션 JAR 파일을 컨테이너로 복사
ARG JAR_FILE=./build/libs/*.jar
COPY ${JAR_FILE} /app.jar

# Spring 프로파일 설정
ENV SPRING_PROFILES_ACTIVE=prod

# 애플리케이션 실행
ENTRYPOINT ["java", "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}", "-jar", "/app.jar"]
