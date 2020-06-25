FROM openjdk:8-alpine
COPY target/uberjar/fuelsurcharges.jar /fuelsurcharges/app.jar
EXPOSE 3000
CMD ["java", "-jar", "/fuelsurcharges/app.jar"]
