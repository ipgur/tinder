FROM adoptopenjdk/openjdk12-openj9

# change this if your port changes too or if protocol becomes https
HEALTHCHECK --interval=1m --timeout=10s \
  CMD curl -f http://localhost:8080/healthcheck || exit 1

RUN mkdir /app
ADD target/lib/ /app/lib
ADD target/original-app.jar /app/app.jar
WORKDIR /app

CMD java -jar app.jar
