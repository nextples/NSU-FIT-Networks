package org.nextples.places;

import org.nextples.places.server.MyHttpServer;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        try {
            MyHttpServer server = new MyHttpServer();
            server.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
