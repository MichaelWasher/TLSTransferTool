package com.michaelwasher.tftp;// -------- Basic com.michaelwasher.tftp.TFTP File Server ----------
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

    public FileServer(int port, String hostedFolder, String keystorePath, String keystorePassword) {
        // Set logger configuration
        LOGGER = Logger.getLogger(FileClient.class.getName());
        LOGGER.setLevel(Level.FINEST);

        // TODO allow folder to be shared
        // TODO allow folder to be listed
        //
        this.keystorePassword = keystorePassword;
        this.keystorePath = keystorePath;
        this.hostedFolder = hostedFolder;

        // Check arguments
        if (!acceptedArgs(port, hostedFolder, keystorePath, keystorePassword))
            return;

        // Check if file is present
        if (!FileHandler.directoryExist(hostedFolder)) {
            LOGGER.fine("No file exists with that name.");
            LOGGER.fine("Please input a valid file name and try again.");
            return;
        }

        try {
            LOGGER.fine("Waiting on Clients");
            // TODO Add Set normal mode without SSL available
            // TODO
            // Get ServerSocket
            SSLServerSocket serverSocket = getServerSocket(port, keystorePath, keystorePassword);

            // Create SSL Socket & Client communication
            SSLSocket socket = (SSLSocket) serverSocket.accept(); //Connection Created
            BufferedReader clientConnectionInput = new BufferedReader(
                    new InputStreamReader(
                            socket.getInputStream()));
            BufferedOutputStream clientConnectionOutput = new BufferedOutputStream(socket.getOutputStream());

            // Wait on Hello Request
            LOGGER.info("Connected to Client");
            boolean connectionActive = true;

            // Get Access to the Folder
            File directoryPath = new File(hostedFolder);
            List<String> fileList = Arrays.asList(directoryPath.list());
            LOGGER.fine("List of files and directories in the specified directory: " + String.join(" ", fileList));

            // TODO Loop for receiving commands
            while(connectionActive) {

                // Get command request from client
                String requestLine = clientConnectionInput.readLine();
                List<String> requestLineList = Arrays.asList(requestLine.split(" "));
                LOGGER.info("Request received from Client: " + requestLine);

                if (!checkRequestLineIsValid()) {
                    if (requestLineList.size() != 2) {
                        // TODO ERROR malformed command
                    }
                }

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
            LOGGER.fine("Closing socket connections.");
//            clientConnectionInput.close();
//            socket.close();

        } catch (Exception e) {
            LOGGER.info("Opps. Something went wrong.");
            e.printStackTrace();
        }

    }
    public void start(){
        LOGGER.setLevel(Level.FINEST);
        LOGGER.info("Started");
    }

    private void sendFile(String requestedFilename, BufferedOutputStream clientConnectionOutput) {
        try {
            LOGGER.fine("Opening File Connections.");
            // Open connections
            FileInputStream fis = new FileInputStream(requestedFilename);
            BufferedOutputStream bos = new BufferedOutputStream(clientConnectionOutput);

            // Send the file
            FileHandler.copyFile(fis, bos);
            LOGGER.fine("Closing socket connections.");
            // Close connections
//            fis.close();
//            bos.close();
        }catch (FileNotFoundException nfe){
            nfe.printStackTrace();
            // TODO Dump exception
        }catch (IOException ioe){
            // TODO Dump exception
            ioe.printStackTrace();
        }
    }
    private void processGetRequest(List<String> requestLine, List<String> fileList, BufferedOutputStream clientConnectionOutput){
        // TODO IF file is present then pass to Client
        // IF not present then throw message to client

        // Get File Name
        String requestedFilename = requestLine.get(1);

        // Check request from Socket against the list of files
        PrintWriter clientConnectionTextOutput = new PrintWriter(clientConnectionOutput);
        if (!fileList.contains(requestedFilename)) {
            clientConnectionTextOutput.print(FAILED_RESPONSE + ": File Not Found");
        } else{
            // Send file
            // TODO perform OS filename join correctly
            clientConnectionTextOutput.println(SUCCESS_RESPONSE + " " + requestedFilename);
            clientConnectionTextOutput.flush();
            sendFile( hostedFolder + "/" + requestedFilename, clientConnectionOutput);
        }
//        clientConnectionTextOutput.close();
    }
    private void processListRequest(List<String> requestLine, List<String> fileList, BufferedOutputStream clientConnectionOutput){
        PrintWriter clientConnectionTextOutput = new PrintWriter(clientConnectionOutput);
        clientConnectionTextOutput.println("SUCCESS LIST");
        LOGGER.info("Response: SUCCESS LIST");
        LOGGER.fine(String.join(" ", fileList));
        clientConnectionTextOutput.println(String.join(" ", fileList));
        clientConnectionTextOutput.println(String.join(" ", fileList));
        clientConnectionTextOutput.flush();
//        clientConnectionTextOutput.close();
    }
    private void processInvalidRequest(List<String> requestLine, List<String> fileList, BufferedOutputStream clientConnectionOutput){
    }

    //Test input is as expected
    public static boolean acceptedArgs(int port, String hostedFolder, String keystorePath, String keystorePassword) {
        //TODO perform actual logging checks
        if (port > PORT_MAX || port < PORT_MIN) {
            LOGGER.info(String.format("Please input a port number between %d and %d", PORT_MIN, PORT_MAX));
            return false;
        }
        return true;
    }

    public static SSLServerSocket getServerSocket(int portNum, String keystorePath,  String password) {
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
    protected boolean checkRequestLineIsValid(){
        // TODO Check request is valid based on a list of accepted values
        return true;
    }

}
