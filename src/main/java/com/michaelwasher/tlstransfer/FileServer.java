package com.michaelwasher.tlstransfer;
// -------- Basic TLSTransfer File Server ----------
//Name: Michael Washer
//
// ------------------------------------------

/*
 * This application is a simple file-transfer client that uses TLS to encrypt
 * the files before they are sent / decrypts them when they are received.
 */

import javax.net.ssl.*;
import java.io.*;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public class FileServer {

    private static Logger LOGGER;

    private String keystorePassword;
    private String keystorePath;
    private String hostedFolder;
    private int portNum;

    // Argument Limits
    private static int PORT_MAX = 55555;
    private static int PORT_MIN = 33333;

    // Request Commands
    private final String GET_COMMAND = "GET";
    private final String LIST_COMMAND = "LIST";
    private String[] validCommandList = {GET_COMMAND, LIST_COMMAND};

    // Response Values
    private final String SUCCESS_RESPONSE = "SUCCESS";
    private final String FAILED_RESPONSE = "FAILED";
    private final String DENIED_RESPONSE = "DENIED";
    private String[] validResponseList = {SUCCESS_RESPONSE, FAILED_RESPONSE, DENIED_RESPONSE};

    // TODO allow folder to be shared
    // TODO allow folder to be listed
    // TODO Add Set normal mode without SSL available
    public FileServer(int port, String hostedFolder, String keystorePath, String keystorePassword) {
        // Set logger configuration
        LOGGER = Logger.getLogger(FileClient.class.getName());

        // Configure Server
        this.keystorePassword = keystorePassword;
        this.keystorePath = keystorePath;
        this.hostedFolder = hostedFolder;
        this.portNum = port;

        // Check arguments
        if (!acceptedArgs(port, hostedFolder, keystorePath, keystorePassword))
            return;

        // Check if file is present
        if (!FileHandler.directoryExist(hostedFolder)) {
            LOGGER.fine("No file exists with that name.");
            LOGGER.fine("Please input a valid file name and try again.");
            System.err.println("No files exist at the path provided. Please retry with another hosted folder.");
            return;
        }

        try {
            LOGGER.fine("Waiting on Clients");

            // Get ServerSocket
            SSLServerSocket serverSocket = getServerSocket(port, keystorePath, keystorePassword);

            // Create SSL Socket & Client communication
            SSLSocket socket = (SSLSocket) serverSocket.accept(); //Connection Created
            processSingleClient(socket);
        } catch (IOException ioe) {
            LOGGER.severe("Failed to accept connection.");
            LOGGER.log(Level.SEVERE, ioe.getMessage(), ioe);
            System.err.println("Failed to accept connection.");

        }

    }

    protected boolean processSingleClient(SSLSocket socket){
        LOGGER.info("Connected to Client");

        // Setup Client Connection
        BufferedReader clientConnectionInput = null;
        BufferedOutputStream clientConnectionOutput = null;
        try {
            clientConnectionInput = new BufferedReader(
                    new InputStreamReader(
                            socket.getInputStream()));
            clientConnectionOutput = new BufferedOutputStream(socket.getOutputStream());
        }catch(IOException ioe){
            LOGGER.severe("Failed to create Input Stream.");
            LOGGER.log(Level.SEVERE, ioe.getMessage(), ioe);
            System.err.println("Failed to create Input Stream for client.");
            return false;
        }

        // Get Access to the Folder
        File directoryPath = new File(hostedFolder);
        List<String> fileList = Arrays.asList(directoryPath.list());
        LOGGER.fine("List of files and directories in the specified directory: " + String.join(" ", fileList));

        // Process Commands from Client
        List<String> requestLineList = collectRequest(clientConnectionInput);
        processRequest(requestLineList, fileList, clientConnectionOutput);

        LOGGER.fine("Closing socket connections.");
        try{
            clientConnectionInput.close();
            socket.close();
        }catch(IOException ioe){
            LOGGER.severe("Failed to close client sockets.");
            LOGGER.log(Level.SEVERE, ioe.getMessage(), ioe);
            System.err.println("Failed to close client sockets.");
            return false;
        }
        LOGGER.info("Connection Closed");
        return true;
    }

    public void start(){
        LOGGER.setLevel(Level.FINEST);
        LOGGER.info("Started");
    }
    public List<String> collectRequest(BufferedReader clientConnectionInput){
        List<String> requestLineList = null;
        try {
            String requestLine = clientConnectionInput.readLine();
            LOGGER.info("Request received from Client: " + requestLine);
            requestLineList = Arrays.asList(requestLine.split(" "));
            if (checkRequestLineIsValid(requestLine)) {
            }
        }catch(IOException ioe){
            LOGGER.severe("Failed to read request from client. The request may be invalid.");
            LOGGER.log(Level.SEVERE, ioe.getMessage(), ioe);
            System.err.println("Failed to read request from client. The request may be invalid.");
        }
        return requestLineList;
    }
    protected void processRequest(List<String> requestLineList, List<String> fileList, BufferedOutputStream clientConnectionOutput){

        // Command Switch
        String command = requestLineList.get(0);
        switch(command.toUpperCase()){
            case GET_COMMAND:
                processGetRequest(requestLineList, fileList, clientConnectionOutput);
                break;
            case LIST_COMMAND:
                processListRequest(requestLineList, fileList, clientConnectionOutput);
                break;
            default:
                processInvalidRequest(requestLineList, fileList, clientConnectionOutput);
        }
    }

    protected void sendFile(String requestedFilename, BufferedOutputStream clientConnectionOutput) {
        try {
            LOGGER.fine("Opening File Connections.");
            // Open connections
            FileInputStream fis = new FileInputStream(requestedFilename);
            BufferedOutputStream bos = new BufferedOutputStream(clientConnectionOutput);

            // Send the file
            FileHandler.copyFile(fis, bos);
            LOGGER.fine("Closing socket connections.");

            // Close connections
            fis.close();
            bos.close();

        }catch (FileNotFoundException nfe){
            LOGGER.severe("Failed to find file. There has been an internal error looking for the requested file.");
            LOGGER.log(Level.SEVERE, nfe.getMessage(), nfe);
            System.err.println("Failed to find file. There has been an internal error looking for the requested file.");
        }catch (IOException ioe){
            // TODO Dump exception
            LOGGER.severe("Failed to close the input / output sockets to client.");
            LOGGER.log(Level.SEVERE, ioe.getMessage(), ioe);
            System.err.println("Failed to close the input / output sockets to client.");
        }
    }

    //// ----------------------------- Process Requests ------------------------------
    protected void processGetRequest(List<String> requestLine, List<String> fileList, BufferedOutputStream clientConnectionOutput){
        // Get File Name
        String requestedFilename = requestLine.get(1);

        // Check request from Socket against the list of files
        PrintWriter clientConnectionTextOutput = new PrintWriter(clientConnectionOutput);
        if (!fileList.contains(requestedFilename)) {
            clientConnectionTextOutput.print(FAILED_RESPONSE + ": File Not Found");
            clientConnectionTextOutput.flush();
        } else{
            // Send file
            // TODO perform OS filename join correctly
            clientConnectionTextOutput.println(SUCCESS_RESPONSE + " " + requestedFilename);
            clientConnectionTextOutput.flush();
            sendFile( hostedFolder + "/" + requestedFilename, clientConnectionOutput);
        }
    }

    protected void processListRequest(List<String> requestLine, List<String> fileList, BufferedOutputStream clientConnectionOutput){
        PrintWriter clientConnectionTextOutput = new PrintWriter(clientConnectionOutput);
        clientConnectionTextOutput.println("SUCCESS LIST");
        LOGGER.info("Response: SUCCESS LIST");
        LOGGER.info(String.join(" ", fileList));
        clientConnectionTextOutput.println(String.join(" ", fileList));
        clientConnectionTextOutput.flush();
    }

    protected void processInvalidRequest(List<String> requestLine, List<String> fileList, BufferedOutputStream clientConnectionOutput){
    }

    //// ----------------------------- Utilities ------------------------------
    protected static boolean acceptedArgs(int port, String hostedFolder, String keystorePath, String keystorePassword) {
        //TODO perform actual logging checks
        // Check keystore, password and hostedFolder are all present
//        if (port > PORT_MAX || port < PORT_MIN) {
//            LOGGER.info(String.format("Please input a port number between %d and %d", PORT_MIN, PORT_MAX));
//            return false;
//        }
        return true;
    }

    protected SSLServerSocket getServerSocket(int portNum, String keystorePath,  String password) {
        try {
            char[] passphrase = password.toCharArray();

            //Configure keystore
            SSLContext sslContext = SSLContext.getInstance("TLS");
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");

            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(new FileInputStream(keystorePath), passphrase);
            keyManagerFactory.init(keyStore, passphrase);
            sslContext.init(keyManagerFactory.getKeyManagers(), null, null);

            //Configure ServerSocket
            SSLServerSocketFactory serverSocketFactory = sslContext.getServerSocketFactory();
            SSLServerSocket serverSocket =
                    (SSLServerSocket) serverSocketFactory.createServerSocket(portNum);
            String[] enabledProtocols = {"TLSv1.2", "TLSv1.1"};
            serverSocket.setEnabledProtocols(enabledProtocols);

            return serverSocket;
        } catch (Exception e) {
            LOGGER.severe("Unable to create ServerSocket.");
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            return null;
        }
    }
    protected boolean checkRequestLineIsValid(String requestLine){
        // TODO Check request is valid based on a list of accepted values
        return true;
    }

}
