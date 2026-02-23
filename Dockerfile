FROM eclipse-temurin:25-jdk-alpine

# 앱이 올라갈 작업 디렉토리
WORKDIR /app

# CI에서 미리 ./gradlew clean build 로 jar를 만들어 둔다고 가정
# build/libs 안에 생성된 jar 파일을 app.jar 로 복사
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

# 컨테이너에서 열어줄 포트 (Spring Boot 기본 8080)
EXPOSE 8080

# 스프링 부트 앱 실행
ENTRYPOINT ["java", "-jar", "app.jar"]