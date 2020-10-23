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
openssl req \
       -newkey rsa:2048 -nodes -keyout tftp_server.pem \
       -out tftp_server.csr
openssl req \
       -key tftp_server.pem \
       -new -out tftp_server.csr
openssl x509 \
       -signkey domain.key \
       -in domain.csr \
       -req -days 365 -out domain.crt
openssl x509 \
       -signkey CA-key.pem \
       -in tftp_server.csr \
       -req -days 365 -out tftp_server.csr


-----------------
openssl req -new -sha256 -key .com.key -subj "/C=US/ST=CA/O=MyOrg, Inc./CN=mydomain.com" -out mydomain.com.csr


-----------
openssl genrsa -out tftp-server.example.com.key 2048
openssl req -new -key tftp-server.example.com.key -out tftp-server.example.com.csr
openssl x509 -req -in tftp-server.example.com.csr -CA rootCA.crt -CAkey rootCA.key -CAcreateserial -out tftp-server.example.com.crt -days 500 -sha256
