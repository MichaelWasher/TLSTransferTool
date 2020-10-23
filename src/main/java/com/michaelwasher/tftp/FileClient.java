// -------- Basic com.michaelwasher.tftp.TFTP File Server ----------
//Name: Michael Washer
//
// ------------------------------------------

/*
 * This application is a simple file-transfer client that uses TLS to encrypt
 * the files before they are sent / decrypts them when they are received.
 */

package com.michaelwasher.tftp;

import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.security.cert.X509Certificate;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileClient {

	private Logger LOGGER;

	// Program Arguments
	protected String hostname;
	protected int portNum;
	protected String requestedFilename;
	protected String outputFilename;

	//Main Method
	public FileClient(int port, String hostname, String requestedFilename, String outputFilename) {
		this.hostname = hostname;
		this.requestedFilename = requestedFilename;
		this.outputFilename = outputFilename;
		this.portNum = port;
		// Configure logger
		LOGGER = Logger.getLogger(FileClient.class.getName());
		LOGGER.setLevel(Level.FINER);
		ConsoleHandler handler = new ConsoleHandler();
		handler.setLevel(Level.FINER);
		LOGGER.addHandler(handler);

	}

	public void start(){
		// Check Args
		if(! acceptedArgs())
		{
			// TODO Fail
		}

		try
		{
			// Start SSL Connection
			SSLSocket socket = this.getSSLSocket();
			try{
				socket.startHandshake();
			}catch(IOException ioe){
				LOGGER.info("Socket handshake failed.");
				LOGGER.info(ioe.getMessage());
				ioe.printStackTrace();
				throw ioe;
			}

			//Get Certificate for the session
			LOGGER.fine("Confirming peer certificates.");
			SSLSession session = socket.getSession();
			X509Certificate sessionCertificate = (X509Certificate)session.getPeerCertificates()[0];

			//Get the CommonName and compare
			if(getCommonName(sessionCertificate).equalsIgnoreCase(this.hostname))
				LOGGER.info("Verified Host to be:" + this.hostname);
			else
				LOGGER.info(String.format("Unable to Verify Host %s Status.", this.hostname));

			// TODO Send a request for the filename
			OutputStream os = socket.getOutputStream();
			PrintWriter printWriter = new PrintWriter(os);
			printWriter.println(this.requestedFilename);
			printWriter.flush();

			//Send create new file of _FileName
			File newFile = new File(this.outputFilename);

			System.out.println(String.format("Created a new file: %s", this.outputFilename));

			// Get In / Out Streams
			FileOutputStream fos = new FileOutputStream(newFile);
			FileHandler.copyFile(socket.getInputStream(),fos);

			//Close all sockets and things at the end.

			fos.close();
			socket.close();

		}catch(Exception e)
		{
			//TODO split this function into sparate functions and have better error handling
			System.out.println("Opps. Something went wrong.");
			e.printStackTrace();
		}
	}
	public static boolean acceptedArgs()
	{
		//TODO Check the args input
		return true;
	}



	public static String getCommonName(X509Certificate cert) throws Exception
	{
		String name = cert.getSubjectX500Principal().getName();
		LdapName ln = new LdapName(name);
		String cn = null;

		for(Rdn rdn : ln.getRdns())
		{
			if("CN".equalsIgnoreCase(rdn.getType()))
				cn = rdn.getValue().toString();
		}
		return cn;

	}
	public SSLSocket getSSLSocket() throws IOException{
		//Get Socket Connection
		SSLSocketFactory sslFactory = (SSLSocketFactory)SSLSocketFactory.getDefault();
		SSLSocket socket = (SSLSocket)sslFactory.createSocket(this.hostname, this.portNum);

		//Set Enabled Protocols such as the server
		String[] enabledProtocols = {"TLSv1.2", "TLSv1.1"};
		socket.setEnabledProtocols(enabledProtocols);

		// Setup SSL Parameters
		SSLParameters params = new SSLParameters();
		params.setEndpointIdentificationAlgorithm("HTTPS");
		socket.setSSLParameters(params);

		LOGGER.info("SSL Socket Configured");

		return socket;
	}
}
