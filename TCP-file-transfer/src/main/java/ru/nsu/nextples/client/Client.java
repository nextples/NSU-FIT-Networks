package ru.nsu.nextples.client;

import ru.nsu.nextples.exception.FileTransferException;
import ru.nsu.nextples.exception.LongFileException;
import ru.nsu.nextples.exception.LongFileNameException;
import ru.nsu.nextples.exception.MetaDataTransferException;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

import static java.lang.Thread.sleep;

public class Client {
    public static final int SUCCESS = 0;
    public static final int FAILURE = 1;
    public static final int BUFFER_SIZE = 4096;


    private final String filePath;
    private final InetAddress address;
    private final int port;

    private DataInputStream in;
    private DataOutputStream out;

    public Client(String filePath, InetAddress address, int port) {
        this.filePath = filePath;
        this.address = address;
        this.port = port;
    }

    public void start() {
        try (Socket socket = new Socket(address, port)) {
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            sendFile();

            in.close();
            out.close();

        } catch (IOException | LongFileNameException | LongFileException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private void sendFile() throws LongFileNameException, LongFileException, IOException {
        File file = new File(filePath);
        long fileSize = file.length();
        String fileName = file.getName();

        if (fileName.length() > 4096) {
            throw new LongFileNameException();
        }
        if (fileSize / 1024 / 1024 / 1024 / 1024 > 1) {
            throw new LongFileException();
        }

        out.writeLong(fileSize);
        out.writeUTF(fileName);

        int status = in.readInt();
        if (status != SUCCESS) {
            throw new MetaDataTransferException();
        }

        System.out.println("Server got file metadata. Starting sending file data...");

        FileInputStream fis = new FileInputStream(file);
        int bytesRead = 0;
        byte[] buffer = new byte[4096];
        while ((bytesRead = fis.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
            out.flush();
        }
        fis.close();

        status = in.readInt();
        if (status != SUCCESS) {
            throw new FileTransferException();
        }
        System.out.println("Server got file successfully");
    }
}
