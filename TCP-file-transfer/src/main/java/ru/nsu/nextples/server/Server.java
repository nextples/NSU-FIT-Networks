package ru.nsu.nextples.server;

import java.io.File;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private final int port;
    private final int threadsNum;


    public Server(int port, int threadsNum) {
        this.port = port;
        this.threadsNum = threadsNum;

        File outputDir = new File("uploads");

        if (!outputDir.exists()) {
            if (outputDir.mkdirs()) {
                System.out.println("Create directory '" + outputDir.getAbsolutePath() + "'");
            }
        }
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)){

            System.out.println("Server started!\nAvailable threads: " + threadsNum);

            ExecutorService clientThreadPool = Executors.newFixedThreadPool(threadsNum);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New connection: " + socket.getInetAddress() + ":" + socket.getPort());
                clientThreadPool.execute(new ServerTask(socket));
            }

        } catch (RuntimeException e){
            System.out.println("Connection reset!");
        } catch (Exception e ){
            System.out.println(e.getMessage());
            System.exit(1);
        }
    }
}
