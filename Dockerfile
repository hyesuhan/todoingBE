## docker file
FROM eclipse-temurin:21-jre-jammy
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar
WORKDIR /app
RUN mkdir -p logs && chmod 777 logs
# shell 형식이어야 JAVA_OPTS(예: -Xmx256m)가 실제로 적용됨 — exec 형식(["java","-jar",...])은
# 환경변수 치환을 안 해서 무시됨
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app.jar"]