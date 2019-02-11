FROM adoptopenjdk/openjdk11-openj9
ARG JAR_FILE
ADD target/$JAR_FILE /app.jar
CMD java -jar /app.jar