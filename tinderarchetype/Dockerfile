FROM adoptopenjdk/openjdk11-openj9

# change this if your port changes too or if protocol becomes https
HEALTHCHECK --interval=1m --timeout=10s \
  CMD curl -f http://localhost:8080/healthcheck || exit 1

ADD target/app.jar /app.jar

CMD java -jar /app.jar
