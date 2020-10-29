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
import java.net.Socket;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.Thread.sleep;

public class FileClient {

	private final Logger LOGGER;

	// Program Arguments
	protected String hostname;
	protected int portNum;
	protected String requestedFilename;
	protected String outputFilename;

	// Request Commands
	private final String GET_COMMAND = "GET";
	private final String LIST_COMMAND = "LIST";
	private final String[] validCommandList = {GET_COMMAND, LIST_COMMAND};

	// Response Values
	private final String SUCCESS_RESPONSE = "SUCCESS";
	private final String FAILED_RESPONSE = "FAILED";
	private final String DENIED_RESPONSE = "DENIED";
	private final String[] validResponseList = {SUCCESS_RESPONSE, FAILED_RESPONSE, DENIED_RESPONSE};

	// TODO Temporary
	protected boolean getRequest = false;


	private SSLSocket clientSocket;
	private BufferedReader serverConnectionInput;
	private OutputStream connectionOutputStream;
	private PrintWriter printWriter;

	//Main Method
	public FileClient(int port, String hostname) {
		this.hostname = hostname;
		this.portNum = port;
		// Configure logger
		LOGGER = Logger.getLogger(FileClient.class.getName());
		LOGGER.setLevel(Level.FINEST);
	}

	protected void configureClient(){
		// Check Args
		if(! acceptedArgs())
		{
			// TODO Fail
		}

		try {
			// Start SSL Connection
			clientSocket = this.getSSLSocket();

			try {
				clientSocket.startHandshake();
				serverConnectionInput = new BufferedReader(
						new InputStreamReader(
								clientSocket.getInputStream()));
				connectionOutputStream = clientSocket.getOutputStream();
				printWriter = new PrintWriter(connectionOutputStream);
			} catch (IOException ioe) {
				LOGGER.info("Unable to establish connection.");
				LOGGER.info(ioe.getMessage());
				ioe.printStackTrace();
				throw ioe;
			}

			//Get Certificate for the session
			LOGGER.fine("Confirming peer certificates.");
			SSLSession session = clientSocket.getSession();
			X509Certificate sessionCertificate = (X509Certificate) session.getPeerCertificates()[0];

			//Get the CommonName and compare
			if (getCommonName(sessionCertificate).equalsIgnoreCase(this.hostname))
				LOGGER.info("Verified Host to be:" + this.hostname);
			else
				LOGGER.info(String.format("Unable to Verify Host %s Status.", this.hostname));



			//Close socket
//			clientSocket.close();

		} catch(Exception e) {
			//TODO split this function into sparate functions and have better error handling
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	public boolean getFile(String requestedFilename, String outputFilename){
		// Setup Client
		this.requestedFilename = requestedFilename;
		this.outputFilename = outputFilename;
		configureClient();

		// Process a Copy / Get file Request
		processGetRequest(printWriter);
		List<String> responseLineList = collectResponse(serverConnectionInput);
		processGetResponse(responseLineList, clientSocket);
		return true;
	}
	public boolean listFiles(){
		// Setup Client
		configureClient();

		// Process a List folder Request
		processListRequest(printWriter);
		List<String> responseLineList = collectResponse(serverConnectionInput);
		processListResponse(responseLineList, serverConnectionInput);
		return true;
	}
	public static boolean acceptedArgs()
	{
		//TODO Check the args input
		return true;
	}
	protected boolean checkResponseLineIsValid(List<String> responseLine)
	{
		return true;
	}
	public List<String> collectResponse(BufferedReader serverConnectionInput){
		List<String> responseLineList = null;
		try {
			String responseLine = serverConnectionInput.readLine();
			responseLineList = Arrays.asList(responseLine.split(" "));
			LOGGER.info("Response received from Server: " + String.join(" ", responseLineList));
		} catch (IOException ioException){
			LOGGER.log(Level.SEVERE, ioException.getMessage(), ioException);
			// TODO Might re-raise
		}
		if (!checkResponseLineIsValid(responseLineList)) {
			LOGGER.log(Level.SEVERE, responseLineList.toString());
			return null;
		}
		return responseLineList;
	}
	protected void processGetRequest(PrintWriter printWriter){
		printWriter.println(GET_COMMAND + " " + this.requestedFilename);
		printWriter.flush();
	}
	public void processGetResponse(List<String> responseLineList, Socket clientSocket){
		String command = responseLineList.get(0);
		switch (command.toUpperCase()) {
			case "SUCCESS":
				processSuccessGetResponse(clientSocket);
				break;
//			case "FAILED":
//				processFailedResponse();
//				break;
//			default:
//				processInvalidRequest(requestLine, fileList, clientConnectionOutput);
		}
	}
	public void processListRequest(PrintWriter printWriter){
			printWriter.println(LIST_COMMAND);
			printWriter.flush();
	}


	public void processListResponse(List<String> responseLineList, BufferedReader serverConnectionInput){
		String command = responseLineList.get(0);
		switch (command.toUpperCase()) {
			case SUCCESS_RESPONSE:
				processSuccessListResponse(serverConnectionInput);
				break;
//			case FAILED_RESPONSE:
//				processFailedResponse();
//				break;
//			default:
//				processInvalidRequest(requestLine, fileList, clientConnectionOutput);
		}
	}

	public void processSuccessGetResponse(Socket socket){
		try {
			LOGGER.info(String.format("Created a new file: %s", this.outputFilename));
			// Create new file
			File newFile = new File(this.outputFilename);
			FileOutputStream fos = new FileOutputStream(newFile);

			// Write to file
			FileHandler.copyFile(socket.getInputStream(), fos);
//			fos.close();


		}catch(FileNotFoundException notFoundException){
			LOGGER.log(Level.SEVERE, notFoundException.getMessage(), notFoundException);
		}catch(IOException ioException){
			LOGGER.log(Level.SEVERE, ioException.getMessage(), ioException);
		}
	}
	public void processSuccessListResponse(BufferedReader serverConnectionInput){
		try {
			String readList;
			while(null != (readList = serverConnectionInput.readLine())){
				System.out.println(readList);
//				LOGGER.fine(readList);
			}
		}catch(IOException ioException){
			LOGGER.log(Level.SEVERE, ioException.getMessage(), ioException);
		}
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
