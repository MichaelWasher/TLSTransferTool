FROM maven:3.6-alpine

COPY . /tmp/src
USER root

RUN echo "---> Building application from source..."
RUN cd /tmp/src && mvn clean package -DskipTests=true -Dmaven.skip.tests=true

ENTRYPOINT ["java", "-Djavax.net.debug=all", "-Djavax.net.ssl.trustStore=/tmp/src/certificates/KeyStore.jks", \
                   "-Djavax.net.ssl.trustStorePassword=tester1234", "-jar", "/tmp/src/target/tftp.jar"]
CMD ["java", "-Djavax.net.debug=all", "-Djavax.net.ssl.trustStore=/tmp/src/certificates/KeyStore.jks", \
        "-Djavax.net.ssl.trustStorePassword=tester1234", "-jar", "/tmp/src/target/tftp.jar"]