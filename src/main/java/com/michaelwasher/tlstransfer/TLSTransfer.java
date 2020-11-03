package com.michaelwasher.tlstransfer;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;

import java.util.concurrent.Callable;

@Command(name = "tlstransfer", version = "tlstransfer-v0.1",
        subcommands = { CommandLine.HelpCommand.class, CommandlineClient.class },
        mixinStandardHelpOptions = true)
public class TLSTransfer implements Runnable {

    @Spec CommandSpec spec;
    @Command(name = "server", description = "Use the TLS Transfer Server to host files.")
    void TLSTransferServer(
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
        int exitCode = new CommandLine(new TLSTransfer()).execute(args);
        System.exit(exitCode);
    }
}

@Command(name = "client", description = "Use the TLSTransfer Client to connect to a TLSTransfer Server.")
class CommandlineClient implements Callable<Integer> {
    @Spec CommandSpec spec;

    // Setting Parameters
    @Command(name = "list", description = "Use to list all files accessible from the connected server.")
    void listCommand(
        @Parameters(arity = "1", paramLabel = "<hostname>",
                description = "") String hostname,
                @Parameters(arity = "1", paramLabel = "<port>",
        description = "TCP port number to connect to") int port){
        FileClient fileClient = new FileClient(port, hostname);
        fileClient.listFiles();
        return;
    }

    @Command(name = "copy", description = "Copy file(s) from the connected server.")
    void copy(
            @Parameters(arity = "1", paramLabel = "<hostname>",
            description = "") String hostname,
            @Parameters(arity = "1", paramLabel = "<port>",
                  description = "TCP port number to connect to") int port,
            @Parameters(arity = "1", paramLabel = "<filename>",
                description = "") String requestedFilename,
            @Option(names = { "-o", "--output-file" }, paramLabel = "<output-file>",
                description = "The output file name.") String outputFilename){
        if(outputFilename == null){
            outputFilename = "_" + requestedFilename;
        }
        FileClient fileClient = new FileClient(port, hostname);
        fileClient.getFile(requestedFilename.trim(), outputFilename.trim());
    }


    @Override public Integer call() {
        throw new ParameterException(spec.commandLine(), "Specify a subcommand");
    }
}