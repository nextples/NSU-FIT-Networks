package ru.nsu.nextples.server;

import java.io.*;
import java.net.Socket;

import static java.lang.Thread.sleep;
import static ru.nsu.nextples.client.Client.*;

public class ServerTask implements Runnable {
    private final Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private final SessionInfo sessionInfo;

    public ServerTask(Socket socket) {
        this.socket = socket;
        sessionInfo = new SessionInfo();
    }

    @Override
    public void run() {
        try {
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            receive();

            in.close();
            out.close();

        } catch (IOException e) {
            sessionInfo.setFinished(true);
            throw new RuntimeException(e);
        }
    }

    private void receive() throws IOException {
        Thread speedometer = null;
        try {
            System.out.println("Server ready to get file size");
            long fileSize = in.readLong();
            System.out.println("server got file size");

            sessionInfo.setFileSize(fileSize);
            sessionInfo.setFileName(in.readUTF());

            System.out.println("File to receive: " + sessionInfo.getFileName() + ", size: " + sessionInfo.getFileSize() + " bytes\n");

            out.writeInt(SUCCESS);

            sessionInfo.setStartTime(System.currentTimeMillis());
            speedometer = new Thread(new TransferSpeedometer(sessionInfo));
            speedometer.start();

            int bytesRead = 0;
            File file = createFile("./uploads/" + sessionInfo.getFileName());
            FileOutputStream fos = new FileOutputStream(file);
            byte[] buffer = new byte[BUFFER_SIZE];

            long size = sessionInfo.getFileSize();
            while (size > 0 && (bytesRead = in.read(buffer, 0, (int) Math.min(buffer.length, sessionInfo.getFileSize()))) != -1) {
                long currentBytesReceived = sessionInfo.getBytesReceived();
                sessionInfo.setBytesReceived(currentBytesReceived + bytesRead);
                fos.write(buffer, 0, bytesRead);
                size -= bytesRead;
            }

            sessionInfo.setFinished(true);

//            sleep(500);

            if (size != 0) {
                System.out.printf("File \"%s\" corrupted! Please, try again.%n", sessionInfo.getFileName());
                out.writeInt(FAILURE);
                fos.close();
                this.socket.close();
                System.exit(1);
            }
            else {
                System.out.printf("File \"%s\" received. Avg. speed:  %.3f MB/sec.%n",
                        sessionInfo.getFileName(), sessionInfo.getAvgSpeed());
            }

            out.writeInt(SUCCESS);
            speedometer.join();
            fos.close();
        }
        catch (Exception e) {
            sessionInfo.setFinished(true);
            out.writeInt(FAILURE);
            this.socket.close();
            assert speedometer != null;
            speedometer.interrupt();
            throw new RuntimeException(e);
        }

    }

    private File createFile(String name){
        int n = 0;
        File file = new File(name);
        if (file.exists()){
            boolean res = false;
            while (!res) {
                n++;
                res = file.renameTo(new File("./uploads/" + sessionInfo.getFileName() + "(" + n + ")")); //move
            }
        }
        return file;
    }
}
