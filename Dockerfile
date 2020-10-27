FROM maven:3.6-alpine

COPY . /tmp/src
USER root

RUN echo "---> Building application from source..."
RUN cd /tmp/src && mvn clean package -DskipTests=true -Dmaven.skip.tests=true

ENV JAVA_OPTS=""
ENTRYPOINT java $JAVA_OPTS -jar /tmp/src/target/tftp.jar  $0 $@
