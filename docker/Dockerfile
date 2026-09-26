FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="data-platform-pro"

WORKDIR /app

COPY target/*.jar app.jar

ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:MaxMetaspaceSize=256m"
ENV SERVER_ADDRESS="0.0.0.0"
ENV TZ="Asia/Shanghai"

EXPOSE 8800

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Duser.timezone=Asia/Shanghai -Dserver.address=$SERVER_ADDRESS -jar /app/app.jar"]
