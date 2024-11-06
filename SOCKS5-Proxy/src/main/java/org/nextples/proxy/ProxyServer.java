package org.nextples.proxy;

import org.nextples.service.CacheEntry;
import org.nextples.dns.DnsResolver;
import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Arrays;
import java.util.List;

public class ProxyServer implements AutoCloseable{

    static final int IPV4_LENGTH = 4;
    static final int IPV6_LENGTH = 16;

    private final Selector selector;
    private final DnsResolver resolver;
    private final ServerSocketChannel serverChannel;
    private final int port;

    public ProxyServer(int port) throws IOException{
        this.port = port;

        this.selector = Selector.open();
        this.serverChannel = ServerSocketChannel.open();
        this.serverChannel.socket().bind(new InetSocketAddress(InetAddress.getByName("localhost"), port));
        this.serverChannel.configureBlocking(false);                           // Устанавливаем неблокирующий режим
        this.serverChannel.register(this.selector, SelectionKey.OP_ACCEPT);

        DatagramChannel dnsChannel = DatagramChannel.open();
        dnsChannel.configureBlocking(false);
        dnsChannel.register(this.selector, SelectionKey.OP_READ);
        SocketAddress dnsServerAddress = new InetSocketAddress(ResolverConfig.getCurrentConfig().servers()
                .getFirst().getAddress(), DnsResolver.DOMAIN_PORT);
        dnsChannel.connect(dnsServerAddress);

        this.resolver = new DnsResolver(dnsChannel);
    }

    public void start() {
        try {
            while (!Thread.currentThread().isInterrupted()) {

                int readyChannels = selector.select();

                if (readyChannels >= 0) {
                    for (SelectionKey key : selector.selectedKeys()) {
                        keyWork(key);
                    }
                    selector.selectedKeys().clear();
                }
            }
        } catch (IllegalArgumentException | IOException ioExc) {
            System.out.println(ioExc.getMessage());
            ioExc.printStackTrace(System.err);
        }
    }

    private void keyWork(SelectionKey key) throws IOException {
        try {
            if (!key.isValid()) return;

            if (key.isAcceptable()) {
                acceptClient();
            }

            else if (key.isConnectable()) {
                connect(key);
            }
            else if (key.isReadable()) {
                if (key.channel() instanceof DatagramChannel) {
                    readDnsAnswer(key);
                }
                else {
                    readData(key);
                }
            }
            else if (key.isWritable()) {
                writeData(key);
            }

        }
        catch (IOException ioExc) {
            closeConnection(key);
        }
        catch (IllegalArgumentException iaExc) {
            System.out.println(iaExc.getMessage());
            closeConnection(key);
        }
        catch (RuntimeException re){
            System.out.println(re.getMessage());
        }
    }

    private void acceptClient() throws IOException{
        SocketChannel clientChannel = serverChannel.accept();
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ);
    }

    private void connect(SelectionKey key) throws IOException{
        SocketChannel channel = (SocketChannel) key.channel();
        KeyAttachment attachment = (KeyAttachment) key.attachment();
        channel.finishConnect();

        sendSuccessAnswer((SocketChannel) attachment.getDestinationKey().channel());

        attachment.getDestinationKey().interestOps(SelectionKey.OP_READ);
        key.interestOps(0);
    }

    private void sendSuccessAnswer(SocketChannel channel) throws IOException {
        ByteBuffer respBuf = ByteBuffer.allocate(Short.SIZE + 4 +
                InetAddress.getLoopbackAddress().getAddress().length);
        respBuf.put(SocksCodes.SOCKS_VERSION);
        respBuf.put(SocksCodes.SUCCESS);
        respBuf.put(SocksCodes.RESERVED);
        respBuf.put(SocksCodes.IPV4);
        respBuf.put(InetAddress.getLoopbackAddress().getAddress());
        respBuf.putShort((short)port);
        respBuf.flip();
        channel.write(respBuf);
    }

    private void sendFailureAnswer(SocketChannel channel, byte flag) throws IOException{
        channel.write(ByteBuffer.wrap(new byte[]{SocksCodes.SOCKS_VERSION, flag, SocksCodes.RESERVED}));
    }

    private void readDnsAnswer(SelectionKey key) throws IOException{
        ByteBuffer ans = ByteBuffer.allocate(KeyAttachment.BUFFER_SIZE);
        DatagramChannel channel = (DatagramChannel) key.channel();
        int bytesRead = channel.read(ans);
        ans.flip();

        if (bytesRead <= 0) return;

        Message message = new Message(ans);
        int senderId = message.getHeader().getID();
        List<Record> answerRecords = message.getSection(Section.ANSWER);
        InetAddress ipAddress = answerRecords.stream()
                .filter(it -> it instanceof ARecord)
                .limit(1)
                .map(it -> (ARecord) it)
                .findAny()
                .orElseThrow(() -> new RuntimeException("No dns resolve"))
                .getAddress();

        byte[] address = ipAddress.getAddress();
        int port1 = resolver.getClientsMatch().get(senderId).getKey();
        SelectionKey curKey = resolver.getClientsMatch().get(senderId).getValue();

        byte[] str = resolver.getTmp().get(senderId);
        if (str != null) {
            resolver.getCache().put(Arrays.hashCode(str), new CacheEntry(port1, address));
            System.out.println("new in cache: "+new String(str) + " " + ipAddress + ":" + port1);
        }

        connectToSite(address, port1, curKey);
    }

    private void connectToSite(byte[] addr, int port, SelectionKey key) throws IOException,IllegalArgumentException{
        try {

            SocketChannel siteChannel = SocketChannel.open();
            siteChannel.configureBlocking(false);
            siteChannel.connect(new InetSocketAddress(InetAddress.getByAddress(addr), port));

            SelectionKey dstKey = siteChannel.register(key.selector(), SelectionKey.OP_CONNECT);
            key.interestOps(0);

            KeyAttachment attachment = (KeyAttachment) key.attachment();
            attachment.setDestinationKey(dstKey);

            KeyAttachment dstAttachment = new KeyAttachment();
            dstAttachment.setDestinationKey(key);
            dstKey.attach(dstAttachment);

            attachment.setOutputBuffer(dstAttachment.getInputBuffer());
            dstAttachment.setOutputBuffer(attachment.getInputBuffer());

        } catch (UnknownHostException e){
            System.out.println(e.getMessage());
            sendFailureAnswer((SocketChannel) key.channel(), SocksCodes.HOST_UNREACHABLE);
            throw new IllegalArgumentException("Unknown host!");
        }
    }

    private void readData(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        KeyAttachment attachment = (KeyAttachment) key.attachment();

        if (attachment == null) {
            attachment = new KeyAttachment();
            key.attach(attachment);
        }

        int bytesRead = channel.read(attachment.getInputBuffer());

        if (bytesRead == 0 || bytesRead == -1) return;

        if (attachment.getDestinationKey() == null) { //// если нет ключа куда ищем его в хедере
            readHeader(key, bytesRead);
        } else {
            key.interestOps(key.interestOps() ^ SelectionKey.OP_READ);
            attachment.getDestinationKey().interestOps(attachment.getDestinationKey().interestOps() | SelectionKey.OP_WRITE);
            attachment.getInputBuffer().flip();
        }
    }

    private void readHeader(SelectionKey key, int len) throws IOException,IllegalArgumentException {
        SocketChannel channel = (SocketChannel) key.channel();
        KeyAttachment attachment = (KeyAttachment) key.attachment();

        byte[] header = attachment.getInputBuffer().array();

        if (header.length < 3) {
            sendFailureAnswer(channel, SocksCodes.ERROR);
            throw new IllegalArgumentException("Incorrect header");
        }
        switch (attachment.getStatus()) {
            case KeyAttachment.AUTH -> {
                if (header[0] != SocksCodes.SOCKS_VERSION) {
                    sendFailureAnswer(channel, SocksCodes.NO_ACCEPT_METHOD);
                    throw new IllegalArgumentException("Incorrect SOCKS version");
                }
                authenticationHandler(header, channel, attachment);
                attachment.getInputBuffer().flip();
                attachment.getInputBuffer().clear();
            }
            case KeyAttachment.REQUEST -> {
                requestHandler(header, key, channel, len);
                attachment.getInputBuffer().flip();
                attachment.getInputBuffer().clear();
            }
            default -> {
                sendFailureAnswer(channel, SocksCodes.ERROR);
                throw new IllegalArgumentException("Incorrect status");
            }
        }
    }

    private void authenticationHandler(byte[] header, SocketChannel channel, KeyAttachment attachment)
            throws IOException,IllegalArgumentException {
        if (checkAuthMethod(header)) {
            channel.write(ByteBuffer.wrap(new byte[]{
                    SocksCodes.SOCKS_VERSION, SocksCodes.NO_AUTH_REQUIRED}));
            attachment.setStatus(KeyAttachment.REQUEST);
        } else {
            sendFailureAnswer(channel, SocksCodes.FORBIDDEN);
            throw new IllegalArgumentException("Client has not NoAuth method");
        }
    }

//    1 байт 	Номер версии SOCKS (должен быть 0x05 для этой версии)
//    1 байт 	Количество поддерживаемых методов аутентификации
//    n байт 	Номера методов аутентификации, переменная длина, 1 байт для каждого поддерживаемого метода
    private boolean checkAuthMethod(byte[] header){
        int n = header[1];
        for (int i = 0; i < n; i++) {
            if (header[i + 2] == SocksCodes.NO_AUTH_REQUIRED) {
                return true;
            }
        }
        return false;
    }


    //Запрос клиента:
    //Размер 	Описание
    //1 байт 	Номер версии SOCKS (должен быть 0x05 для этой версии)
    //1 байт 	Код команды:
    //
    //    0x01 = установка TCP/IP соединения
    //    0x02 = назначение TCP/IP порта (binding)
    //    0x03 = ассоциирование UDP-порта
    //
    //1 байт 	Зарезервированный байт, должен быть 0x00
    //1 байт 	Тип адреса:
    //
    //    0x01 = адрес IPv4
    //    0x03 = имя домена
    //    0x04 = адрес IPv6
    //
    //Зависит от типа адреса 	Назначение адреса:
    //
    //    4 байта для адреса IPv4
    //    Первый байт — длина имени, затем следует имя домена без завершающего нуля на конце
    //    16 байт для адреса IPv6
    //
    //2 байта 	Номер порта, в порядке от старшего к младшему (big-endian)
    private void requestHandler(byte[] header, SelectionKey key, SocketChannel channel, int len) throws IOException{
        if (header[1] != SocksCodes.CONNECT) {
            System.out.println("Bad CMD in second header");
            sendFailureAnswer(channel, SocksCodes.UNSUPPORTED_CMD);
            throw new IllegalArgumentException("Unsupported command!");
        }

        byte[] addr = null;
        int port1 = (header[len - 2] & 0xFF) << 8 | (header[len - 1] & 0xFF);
        switch (header[3]) {
            case SocksCodes.IPV4 -> {
                addr = Arrays.copyOfRange(header, 4, 4 + IPV4_LENGTH);
                connectToSite(addr, port1, key);
            }
            case SocksCodes.DOMAIN_NAME -> {
                int domainLength = header[4] & 0xFF;
                addr = Arrays.copyOfRange(header, 5, 5 + domainLength);

                int hash = Arrays.hashCode(addr);
                if (!resolver.getCache().containsKey(hash)) {
                    System.out.println("found new addr:" + new String(addr));
                } else {
                    CacheEntry e = resolver.getCache().get(hash);
                    if (e != null){
                        byte[] add = e.getAddr();
                        int portTmp = e.getPort();
                        connectToSite(add, portTmp, key);
                        return;
                    }
                }

                resolver.resolve(addr, port1, key);
            }
            case SocksCodes.IPV6 -> {
                addr = Arrays.copyOfRange(header, 4, 4 + IPV6_LENGTH);
                connectToSite(addr, port1, key);
            }
            default -> {
                sendFailureAnswer(channel, SocksCodes.UNSUPPORTED_ADDRESS);
                throw new IllegalArgumentException("Bad Ip address type = " + header[3]);
            }
        }
    }

    private void writeData(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        KeyAttachment attachment = (KeyAttachment) key.attachment();
        if (!attachment.getOutputBuffer().hasRemaining()) {
            return;
        }

        int bytesWrite = channel.write(attachment.getOutputBuffer());

        if (bytesWrite == -1) {
            throw new IllegalArgumentException("Bytes write = -1");
        }
        attachment.getOutputBuffer().flip();
        attachment.getOutputBuffer().clear();
        attachment.getDestinationKey().interestOps(attachment.getDestinationKey().interestOps() | SelectionKey.OP_READ);
        key.interestOps(SelectionKey.OP_READ);
    }

    private void closeConnection (SelectionKey key) throws IOException{
        SelectionKey dstKey = ((KeyAttachment) key.attachment()).getDestinationKey();
        if (dstKey != null) {
            dstKey.channel().close();
            dstKey.cancel();
        }
        key.cancel();
        key.channel().close();
    }

    @Override
    public void close() throws Exception {
        selector.close();
        serverChannel.close();
    }
}
