FROM maven:3.6-alpine

COPY . /tmp/src
USER root

RUN echo "---> Building application from source..."
RUN cd /tmp/src && mvn clean package -DskipTests=true -Dmaven.skip.tests=true

ENV JAVA_OPTS="-Djava.util.logging.ConsoleHandler.level=FINEST  -Djavax.net.ssl.trustStore=certificates/localhost/KeyStore.jks -Djavax.net.ssl.trustStorePassword=tester1234"
WORKDIR /tmp/src/
ENTRYPOINT java $JAVA_OPTS -jar target/tlstransfer.jar  $0 $@
