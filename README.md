# Trivial File Transfer Protocol (TFTP)

## Building
### Build with Docker
docker build -t tftp .
#### For faster subsequent build times, store the maven artifacts
docker build -v -t tftp .
### Build using Maven 
mvn package

### Build using S2i
s2i build git@github.com:MichaelWasher/TrivialFTP.git appuio/s2i-maven-java tftp

## Usage 
### Run with Docker
#### Run the server
`$ docker run tftp server `
#### Run the client
`$ docker run tftp client` 


Setting up SSL in Java
-----------
~~~
openssl genrsa -out tftp-server.example.com.key 2048
openssl req -new -key tftp-server.example.com.key -out tftp-server.example.com.csr
openssl x509 -req -in tftp-server.example.com.csr -CA rootCA.crt -CAkey rootCA.key -CAcreateserial -out tftp-server.example.com.crt -days 500 -sha256
~~~
By default there are a couple of certificates provided for `localhost` and `tftp_server.example.com`.

### Debugging
To configure Debug network logs, the below values can be set for the JAVA_OPTS environment variable.
"-Djavax.net.debug=all"


