FROM openjdk:17

# 애플리케이션 JAR 파일을 컨테이너로 복사
ARG JAR_FILE=./build/libs/*.jar
COPY ${JAR_FILE} /app.jar

# 환경변수를 Dockerfile에서 설정 (여기서 DB URL, 사용자명, 비밀번호 설정)
ENV DB_URL=${DB_URL}
ENV DB_USERNAME=${DB_USERNAME}
ENV DB_PASSWORD=${DB_PASSWORD}
ENV S3_ACCESS_KEY=${S3_ACCESS_KEY}
ENV S3_SECRET_KEY=${S3_SECRET_KEY}
ENV S3_BUCKET_NAME=${S3_BUCKET_NAME}
ENV S3_REGION=${S3_REGION}
ENV JWT_KEY=${JWT_KEY}

# Spring 프로파일 설정
ENV SPRING_PROFILES_ACTIVE=prod

# 애플리케이션 실행
ENTRYPOINT ["java", "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}", "-jar", "/app.jar"]
