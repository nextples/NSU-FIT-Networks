package org.nextples.dns;

import org.nextples.service.CacheEntry;
import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

public class DnsResolver {
    public static final int DOMAIN_PORT = 53;

    private final DatagramChannel channel;
    private int senderID;
    private final HashMap<Integer, Map.Entry<Integer, SelectionKey>> clientsMatch = new HashMap<>();
    private final HashMap<Integer, CacheEntry> cache;
    private final HashMap<Integer, byte[]> tmp;

    public DnsResolver(DatagramChannel channel) {
        this.channel = channel;
        senderID = 0;
        cache = new HashMap<>();
        tmp = new HashMap<>();
    }

    public void resolve(byte[] address, int port, SelectionKey key){
        try {
            Record record = Record.newRecord(
                    Name.fromString(new String(address, StandardCharsets.UTF_8)+'.'),
                    Type.A, DClass.IN
            );
            Message msg = new Message();
            msg.addRecord(record, Section.QUESTION);

            Header header = msg.getHeader();
            header.setID(senderID); //данное поле используется как уникальный идентификатор транзакции. Указывает на то, что пакет принадлежит одной и той же сессии “запросов-ответов” и занимает 16 бит.
            header.setFlag(Flags.AD); //The AD (authentic data) bit indicates in a response that the data included has been verified by the server providing it.
            header.setFlag(Flags.RD); //Если флаг устанавливается в запросе — это значит, что клиент просит сервер не сообщать ему промежуточных ответов, а вернуть только IP-адрес.

            clientsMatch.put(senderID, new AbstractMap.SimpleEntry<>(port, key));
            tmp.put(senderID, address);
            senderID++;

            channel.write(ByteBuffer.wrap(msg.toWire()));

        } catch (TextParseException exc) {
            throw new IllegalArgumentException("resolve: ", exc);
        } catch (IOException exc) {
            throw new IllegalArgumentException("channel write: ", exc);

        }
    }

    public HashMap<Integer, Map.Entry<Integer, SelectionKey>> getClientsMatch() {
        return clientsMatch;
    }

    public HashMap<Integer, CacheEntry> getCache() {
        return cache;
    }

    public HashMap<Integer, byte[]> getTmp() {
        return tmp;
    }
}
