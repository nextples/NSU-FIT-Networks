package org.nextples;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MulticastApp {
    private final MulticastSocket socket;
    private final InetAddress group;
    private final int port;
    private final String localId;
    private final Map<String, Long> activeCopies;

    public MulticastApp(String multicastAddress, int port) throws IOException {
        this.port = port;
        group = InetAddress.getByName(multicastAddress);
        socket = new MulticastSocket(port);
        socket.joinGroup(group);

        localId = generateNewID();
        activeCopies = new ConcurrentHashMap<>();
    }

    public String generateNewID() {
        try {
            return InetAddress.getLocalHost().getHostAddress() + "-" + System.currentTimeMillis();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public void sendData(String message) throws IOException {
        byte[] buffer = message.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, port);
        socket.send(packet);
    }

    public void receiveMessages() throws IOException {
        byte[] buffer = new byte[256];

        while (true) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            String received = new String(packet.getData(), 0, packet.getLength());

            if (!received.startsWith(localId)) {
                String[] parts = received.split(";");
                String senderId = parts[0]; // Идентификатор отправителя
                long timestamp = System.currentTimeMillis();
                activeCopies.put(senderId, timestamp);
                System.out.println("Обнаружена копия: " + senderId + " (всего активных: " + activeCopies.size() + ")");
            }
        }
    }

    public void startPeriodicBroadcast() {
        new Thread(() -> {
            try {
                while (true) {
                    sendData(localId + "; Hello World!");
                    long currentTime = System.currentTimeMillis();
                    
                    List<String> elementsToRemove = new ArrayList<>();
                    for (Map.Entry<String, Long> entry : activeCopies.entrySet()) {
                        if (currentTime - entry.getValue() > 5000) {
                            elementsToRemove.add(entry.getKey());
                        }
                    }

                    for (String key : elementsToRemove) {
                        activeCopies.remove(key);
                    }

                    Thread.sleep(2000);
                }
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e.getMessage());
            }
        }).start();
    }

    public void close() throws IOException {
        socket.leaveGroup(group);
        socket.close();
    }

    public static void main(String[] args) throws IOException {
        String multicastAddress = /*args[0]*/ "239.255.255.250" /*"ff02::1"*/;
        int port = 12345;
        MulticastApp app = new MulticastApp(multicastAddress, port);

        new Thread(() -> {
            try {
                app.receiveMessages();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        app.startPeriodicBroadcast();
    }
}
