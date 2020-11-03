// -------- Basic TLSTransfer File Server ----------
//Name: Michael Washer
//
// ------------------------------------------

/*
 * This application is a simple file-transfer client that uses TLS to encrypt
 * the files before they are sent / decrypts them when they are received.
 */

package com.michaelwasher.tlstransfer;

import com.sun.org.apache.bcel.internal.classfile.Unknown;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.net.ssl.*;
import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
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

    // Sockets to Server
    private SSLSocket clientSocket;
    private BufferedReader serverConnectionInput;
    private OutputStream connectionOutputStream;
    private PrintWriter printWriter;

    // -------------------- Public Methods --------------------------
    public FileClient(int port, String hostname) {
        this.hostname = hostname;
        this.portNum = port;
        // Configure logger
        LOGGER = Logger.getLogger(FileClient.class.getName());
    }

    public boolean getFile(String requestedFilename, String outputFilename) {
        // Setup Client
        this.requestedFilename = requestedFilename;
        this.outputFilename = outputFilename;
        if (!configureClient()) {
//            return false;
        }

        // Process a Copy / Get file Request
        processGetRequest(printWriter);
        List<String> responseLineList = collectResponse(serverConnectionInput);
        if (responseLineList == null) {
            return false;
        }

        processGetResponse(responseLineList, clientSocket);
        return true;
    }

    public boolean listFiles() {
        // Setup Client
        if (!configureClient()) {
//            return false;
        }

        // Process a List folder Request
        processListRequest(printWriter);
        List<String> responseLineList = collectResponse(serverConnectionInput);
        processListResponse(responseLineList, serverConnectionInput);
        return true;
    }

    //// ----------------------------- Process Requests ------------------------------

    protected void processGetRequest(PrintWriter printWriter) {
        printWriter.println(GET_COMMAND + " " + this.requestedFilename);
        printWriter.flush();
    }

    protected void processListRequest(PrintWriter printWriter) {
        printWriter.println(LIST_COMMAND);
        printWriter.flush();
    }

    //// ----------------------------- Process Get Responses ------------------------------
    protected void processGetResponse(List<String> responseLineList, Socket clientSocket) {
        String command = responseLineList.get(0);
        switch (command.toUpperCase()) {
            case "SUCCESS":
                processSuccessGetResponse(clientSocket);
                break;
            case "FAILED":
                processFailedGetResponse(serverConnectionInput);
                break;
            default:
                processInvalidGetResponse(serverConnectionInput);
        }
    }

    protected void processInvalidGetResponse(BufferedReader serverConnectionInput) {
        LOGGER.severe("The Request/Response combo was an invalid.");
        LOGGER.severe(readUntilEmpty(serverConnectionInput));
        System.out.println("The Request/Response combo was an invalid. Please try again later.");
    }

    protected void processFailedGetResponse(BufferedReader serverConnectionInput) {
        // TODO Setup Consistent Logging for process*Respose Methods
        LOGGER.severe("Get Request Failed");
        LOGGER.severe(readUntilEmpty(serverConnectionInput));
        System.out.println("Failed to get file.");
        return;
    }

    protected void processSuccessGetResponse(Socket socket) {
        try {
            LOGGER.info(String.format("Created a new file: %s", this.outputFilename));
            // Create new file
            File newFile = new File(this.outputFilename);
            FileOutputStream fos = new FileOutputStream(newFile);

            // Write to file
            FileHandler.copyFile(socket.getInputStream(), fos);

        } catch (FileNotFoundException notFoundException) {
            LOGGER.log(Level.SEVERE, notFoundException.getMessage(), notFoundException);
        } catch (IOException ioException) {
            LOGGER.log(Level.SEVERE, ioException.getMessage(), ioException);
        }
    }

    //// ----------------------------- Process List Responses ------------------------------
    protected void processListResponse(List<String> responseLineList, BufferedReader serverConnectionInput) {
        String command = responseLineList.get(0);
        switch (command.toUpperCase()) {
            case SUCCESS_RESPONSE:
                processSuccessListResponse(serverConnectionInput);
                break;
            case FAILED_RESPONSE:
                processFailedListResponse(serverConnectionInput);
                break;
            default:
                processInvalidListResponse(serverConnectionInput);
        }
    }

    protected void processInvalidListResponse(BufferedReader serverConnectionInput) {
        LOGGER.severe("List response is invalid.");
        LOGGER.severe(readUntilEmpty(serverConnectionInput));
        System.out.println("Unable to list the folder requested. Please try again with a different server.");
    }

    protected void processFailedListResponse(BufferedReader serverConnectionInput) {
        LOGGER.severe("Failed to list the folder.");
        LOGGER.severe(readUntilEmpty(serverConnectionInput));
        System.out.println("Unable to list the folder requested. Please try again with a different server.");
    }

    protected void processSuccessListResponse(BufferedReader serverConnectionInput) {
        String inputString = readUntilEmpty(serverConnectionInput);
        LOGGER.info("Listed folder successfully");
        LOGGER.info(inputString);
        System.out.println(inputString);
    }

    /// ------------------- Utilities ---------------------

    protected String getCommonName(X509Certificate cert) {
        String commonName = "";
        try {
            String name = cert.getSubjectX500Principal().getName();
            LdapName ln = new LdapName(name);

            for (Rdn rdn : ln.getRdns()) {
                if ("CN".equalsIgnoreCase(rdn.getType()))
                    commonName = rdn.getValue().toString();
            }
        } catch (InvalidNameException ine) {
            LOGGER.severe("Unable to get the common name of the connected server.");
            LOGGER.log(Level.SEVERE, ine.getMessage(), ine);
        }
        return commonName;
    }

    protected SSLSocket getSSLSocket() {
        SSLSocket socket = null;
        try {
            //Get Socket Connection
            SSLSocketFactory sslFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            socket = (SSLSocket) sslFactory.createSocket(this.hostname, this.portNum);

            //Set Enabled Protocols such as the server
            String[] enabledProtocols = {"TLSv1.2", "TLSv1.1"};
            socket.setEnabledProtocols(enabledProtocols);

            // Setup SSL Parameters
            SSLParameters params = new SSLParameters();
            params.setEndpointIdentificationAlgorithm("HTTPS");
            socket.setSSLParameters(params);

            LOGGER.info("SSL Socket Configured");
        } catch (UnknownHostException unknownHostException) {
            LOGGER.severe("Unable to create and configure an SSL Socket.");
            LOGGER.log(Level.SEVERE, unknownHostException.getMessage(), unknownHostException);
            System.err.println("Unable to connect to server. Server is unknown host.");
        } catch (IOException ioe) {
            LOGGER.severe("Unable to create and configure an SSL Socket.");
            LOGGER.log(Level.SEVERE, ioe.getMessage(), ioe);
        }
        return socket;
    }

    protected String readUntilEmpty(BufferedReader serverConnectionInput) {
        StringBuilder totalInputString = new StringBuilder();
        try {
            String inputString;
            while (null != (inputString = serverConnectionInput.readLine())) {
                totalInputString.append(inputString);
            }
        } catch (IOException ioException) {
            LOGGER.severe("There has been an issue whilst reading from the Socket.");
            LOGGER.log(Level.SEVERE, ioException.getMessage(), ioException);
        }

        LOGGER.fine("Data read from socket: " + totalInputString.toString());
        return totalInputString.toString();
    }

    protected static boolean acceptedArgs() {
        //TODO Check the args input
        return true;
    }

    protected boolean checkResponseLineIsValid(List<String> responseLine) {
        //TODO Check Response Line
        return true;
    }

    protected List<String> collectResponse(BufferedReader serverConnectionInput) {
        List<String> responseLineList = null;
        try {
            String responseLine = serverConnectionInput.readLine();
            responseLineList = Arrays.asList(responseLine.split(" "));
            LOGGER.info("Response received from Server: " + String.join(" ", responseLineList));
        } catch (IOException ioException) {
            LOGGER.severe("There has been an issue whilst reading the response from the Socket.");
            LOGGER.log(Level.SEVERE, ioException.getMessage(), ioException);
            return null;
        }
        if (!checkResponseLineIsValid(responseLineList)) {
            LOGGER.log(Level.SEVERE, responseLineList.toString());
            return null;
        }
        return responseLineList;
    }

    protected boolean setupSockets() {
        try {
            clientSocket = this.getSSLSocket();

            // Start SSL Connection
            clientSocket.startHandshake();
            serverConnectionInput = new BufferedReader(
                    new InputStreamReader(
                            clientSocket.getInputStream()));
            connectionOutputStream = clientSocket.getOutputStream();
            printWriter = new PrintWriter(connectionOutputStream);
        } catch (IOException ioe) {
            LOGGER.severe("Unable to establish connection.");
            LOGGER.info(ioe.getMessage());
            ioe.printStackTrace();
            return false;
        }
        return true;
    }

    protected X509Certificate getSessionCertificate() {
        X509Certificate sessionCertificate = null;

        try {
            LOGGER.fine("Confirming peer certificates.");
            SSLSession session = clientSocket.getSession();
            sessionCertificate = (X509Certificate) session.getPeerCertificates()[0];
        } catch (SSLPeerUnverifiedException unverifiedException) {
            LOGGER.severe("The SSLPeer is unverified. This connection should not be trusted.");
            LOGGER.severe(unverifiedException.getMessage());
            LOGGER.log(Level.SEVERE, unverifiedException.getMessage(), unverifiedException);
        }
        return sessionCertificate;
    }

    protected boolean checkCertificateValidation(X509Certificate sessionCertificate) {
        String commonName = getCommonName(sessionCertificate);
        if (commonName != null && commonName.equalsIgnoreCase(this.hostname)) {
            LOGGER.info("Verified Host to be:" + this.hostname);
        } else {
            LOGGER.info(String.format("Unable to Verify Host %s Status.", this.hostname));
            return false;
        }
        return true;
    }

    protected boolean configureClient() {
        // Check Args
        if (!acceptedArgs() || setupSockets()) {
            return false;
        }

        //Get Certificate for the session
        X509Certificate sessionCertificate = getSessionCertificate();
        if (sessionCertificate == null || checkCertificateValidation(sessionCertificate) == false) {
            return false;
        }
        return true;
    }
}
