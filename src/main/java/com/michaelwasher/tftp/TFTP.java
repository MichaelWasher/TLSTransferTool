package com.michaelwasher.tftp;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;

@Command(name = "TFTP", version = "TFTP-v0.1",
        subcommands = { CommandLine.HelpCommand.class },
        mixinStandardHelpOptions = true)
public class TFTP implements Runnable {

    @Spec CommandSpec spec;
    @Command(name = "client", description = "Use the TFTP Client to connect to a TFTP Server.")
    void TFPTClient(
        @Parameters(arity = "1", paramLabel = "<hostname>",
                description = "") String hostname,
        @Parameters(arity = "1", paramLabel = "<port>",
                description = "TCP port number to connect to") int port,
        @Parameters(arity = "1", paramLabel = "<filename>",
                description = "") String requestedFilename,
        @Option(names = { "-o", "--output-file" }, paramLabel = "<output-file>",
                description = "The output file name.") String outputFilename
    ) {
        System.out.println("TFPTClient");
        if(outputFilename == null){
            outputFilename = "_" + requestedFilename;
        }
        FileClient fileClient = new FileClient(port, hostname, requestedFilename.trim(), outputFilename.trim());
        fileClient.start();
    }
    @Command(name = "server", description = "Use the TFTP Server to host files.")
    void TFTPServer(
            @Parameters(arity = "1", paramLabel = "<port>",
                    description = "TCP port number to connect to") int port,
            @Parameters(arity = "1", paramLabel = "<hostedFolder>",
                    description = "") String hostedFolder,
            @Option(names = { "-k", "--key-store" }, paramLabel = "<keystore-path>",
                    description = "The path to find the default keystore used for encryption.") String keyStore,
            @Option(names = { "-s", "--key-store-password" }, paramLabel = "<keystore-password>",
                    description = "The password to unlock the defined keystore.") String keyStorePassword ) {
        FileServer fileServer = new FileServer(port, hostedFolder, keyStore, keyStorePassword);
        fileServer.start();
    }

    @Override
    public void run() {
        throw new ParameterException(spec.commandLine(), "Specify a subcommand");
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new TFTP()).execute(args);
        System.exit(exitCode);
    }
}