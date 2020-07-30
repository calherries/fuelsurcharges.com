FROM openjdk:8-alpine
fharges.jar /fuelsurcharges/app.jar
EXPOSE 3000
CMD ["java", "-jar", "/fuelsurcharges/app.jar"]