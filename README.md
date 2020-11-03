# Trivial File Transfer Protocol (TFTP)
This project is a small file-server and client written in Java.

The Server uses TLS to communicate with Client, which verifies the Servers certificates at the beginning of the sessions.

(Note: the server key has been signed by an untrusted CA which is present in `./certificates` and is added to Docker builds)

## Building
#### Build with Docker
`$ docker build -t tftp .`

#### Build using Maven 
`$ mvn package`

## Using the TFTP
To run the TFTP Server outside of the Docker containers, Java must be configured to use the appropriate keystore as the server certificates are self-signed.
If you choose to use your own certificate that is signed by a public trusted CA then this will not be required.
~~~
JAVA_OPTS="-Djavax.net.ssl.trustStore=certificates/localhost/KeyStore.jks -Djavax.net.ssl.trustStorePassword=tester1234"
alias tftp="java $JAVA_OPTS -jar target/tftp.jar"
~~~
#### Run the server
`$ tftp server --key-store=certificates/localhost/KeyStore.jks --key-store-password=tester1234 33333 /shared`
#### Run the client
`$ tftp client list localhost 33333`
`$ tftp client copy localhost 33333 <filename> -o <output-file>` 

## Run Tests
To run the tests pytest is required.

`cd test && pytest`