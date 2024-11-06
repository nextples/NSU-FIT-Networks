package org.nextples.proxy;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

public class KeyAttachment {
    public static final int AUTH = 1;
    public static final int REQUEST = 2;
    public static final int BUFFER_SIZE = 1450;

    private final ByteBuffer inputBuffer;// from host to proxy server
    private ByteBuffer outputBuffer; //from proxy server to host
    private SelectionKey destinationKey;
    private int status;

    public ByteBuffer getInputBuffer() {
        return inputBuffer;
    }
    public ByteBuffer getOutputBuffer() {
        return outputBuffer;
    }
    public void setOutputBuffer(ByteBuffer outputBuffer) {
        this.outputBuffer = outputBuffer;
    }
    public SelectionKey getDestinationKey() {
        return destinationKey;
    }
    public void setDestinationKey(SelectionKey destinationKey) {
        this.destinationKey = destinationKey;
    }
    public int getStatus() {
        return status;
    }
    public void setStatus(int status) {
        this.status = status;
    }


    public KeyAttachment() {
        this.inputBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        this.outputBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        this.status = AUTH;
    }
}
