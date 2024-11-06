package org.nextples;

import org.nextples.proxy.ProxyServer;

public class Main {
    public static void main(String[] args) {
        int port = 1080;
        System.out.println(port);
        try(ProxyServer server = new ProxyServer(port)) {
            server.start();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}