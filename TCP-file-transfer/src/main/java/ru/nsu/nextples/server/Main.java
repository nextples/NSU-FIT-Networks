package ru.nsu.nextples.server;

import org.apache.commons.cli.*;

public class Main {
    public static void main(String[] args) {
        Options options = new Options();
        Option helpOption = new Option("h", "help", false, "print help");
        options.addOption(helpOption);

        Option portOption = new Option("p", "port", true, "add port to connection");
        portOption.setArgs(1);
        options.addOption(portOption);

        Option threadsNumOption = new Option("t", "threads", true, "max number of threads");
        threadsNumOption.setArgs(1);
        options.addOption(threadsNumOption);

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

        if (cmd.hasOption("p") && cmd.hasOption("t")) {
            int port = Integer.parseInt(cmd.getOptionValue("p"));
            int threadsNum = Integer.parseInt(cmd.getOptionValue("t"));
            Server server = new Server(port, threadsNum);
            server.start();
        }
    }
}
