FROM maven:3-amazoncorretto-19 as builder
COPY ./ ./

RUN mvn clean install -DskipTests

FROM amazoncorretto:19-alpine
WORKDIR /usr/src/app

COPY --from=builder app/target/*.jar springboot-cloudwatch-1.0.jar

EXPOSE 9958
EXPOSE 8929
EXPOSE 8080

ENTRYPOINT java -jar "/usr/src/app/springboot-cloudwatch-1.0.jar"