package org.nextples.service;

public class CacheEntry {
    private int port;
    private byte[] addr;

    public CacheEntry(int port, byte[] addr) {
        this.port = port;
        this.addr = addr;
    }

    public int getPort() {
        return port;
    }

    public byte[] getAddr() {
        return addr;
    }
}
