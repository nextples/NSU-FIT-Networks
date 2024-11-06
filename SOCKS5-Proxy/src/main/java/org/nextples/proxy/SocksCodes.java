package org.nextples.proxy;

public class SocksCodes {
    static final byte SOCKS_VERSION = 0x05;
    static final byte NO_ACCEPT_METHOD = (byte) 0xFF;

    static final byte SUCCESS = 0x00;
    static final byte ERROR = 0x01;
    static final byte FORBIDDEN = 0x02;
    static final byte HOST_UNREACHABLE = 0x04;
    static final byte UNSUPPORTED_CMD = 0x07;
    static final byte UNSUPPORTED_ADDRESS = 0x08;

    static final byte NO_AUTH_REQUIRED = 0x00;
    static final byte IPV4 = 0x01;
    static final byte DOMAIN_NAME = 0x03;
    static final byte IPV6 = 0x04;

    static final byte CONNECT = 0x01;
    static final byte RESERVED = 0x00;
}
