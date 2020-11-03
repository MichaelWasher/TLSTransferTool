# TLS Transfer Tool
This project is a small file-server and client written in Java. I wrote this to play with TLS / SSL and Java KeyStores.
This application does not come with any guarantees. It is tested with basic SHA-checks and is not meant to be reliable.

The Server uses TLS to communicate with Client, which verifies the Servers certificates at the beginning of the sessions.

## Building
#### Build with Docker
`$ docker build -t tlstransfer .`

#### Build using Maven 
`$ mvn package`

## Using the TLS Transfer Tool
To run the TLS Transfer Tool Server outside of the Docker containers, Java must be configured to use the appropriate keystore as the server certificates are self-signed.

Note: the server keys provided in the `./certificates` folders are signed by an untrusted CA. 
These are used in the Docker builds but default Java truststore MUST include the relevant CA to successfully connect.

If you choose to use your own certificate that is signed by a publicly trusted CA then this will not be required.
~~~
JAVA_OPTS="-Djavax.net.ssl.trustStore=certificates/localhost/KeyStore.jks -Djavax.net.ssl.trustStorePassword=tester1234"
alias tlstransfer="java $JAVA_OPTS -jar target/tlstransfer.jar"
~~~
#### Run the server
`$ tlstransfer server --key-store=certificates/localhost/KeyStore.jks --key-store-password=tester1234 33333 /shared`
#### Run the client
`$ tlstransfer client list localhost 33333`
`$ tlstransfer client copy localhost 33333 <filename> -o <output-file>` 

## Run Tests
To run the tests pytest is required.
`pytest ./test/main.py`