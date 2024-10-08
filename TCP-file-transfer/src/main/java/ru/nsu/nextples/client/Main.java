package ru.nsu.nextples.client;

import org.apache.commons.cli.*;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Main {
    public static void main(String[] args) {
        Options options = new Options();
        Option helpOption = new Option("h", "help", false, "print help");
        options.addOption(helpOption);

        Option portOption = new Option("p", "port", true, "add port to connection");
        portOption.setArgs(1);
        options.addOption(portOption);

        Option fileOption = new Option("f", "filePath", true, "add path to a file");
        portOption.setArgs(1);
        portOption.setRequired(true);
        options.addOption(fileOption);

        Option addressOption = new Option("a", "address", true, "add IP-address to connection");
        addressOption.setArgs(1);
        options.addOption(addressOption);


        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Invalid command line options/arguments were passed");
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Usage: java -jar /path/to/file.jar ", options);
            System.exit(1);
        }


        if (cmd.hasOption("p") && cmd.hasOption("f") && cmd.hasOption("a")) {
            try {
                String filePath = cmd.getOptionValue("f");
                InetAddress address = InetAddress.getByName(cmd.getOptionValue("a"));
                int port = Integer.parseInt(cmd.getOptionValue("p"));

                Client client = new Client(filePath, address, port );
                client.start();

            } catch (UnknownHostException e) {
                throw new RuntimeException(e.getMessage());
            }

        }

    }
}
