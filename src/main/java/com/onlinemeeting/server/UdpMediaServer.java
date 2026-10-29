
package com.onlinemeeting.server;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UdpMediaServer implements Runnable {
    private static final int MAX_PACKET_SIZE = 65507;
    private static final Map<SocketAddress, ClientInfo> CLIENTS = new ConcurrentHashMap<>();
    private final int port;

    public UdpMediaServer(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        try (DatagramSocket server = new DatagramSocket(port)) {
            System.out.println("UDP Media Server listening on port " + port);
            byte[] buffer = new byte[MAX_PACKET_SIZE];

            while (!server.isClosed()) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                server.receive(packet);

                byte[] data = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());

                try {
                    if (data.length < 10 || data[0] != 'O' || data[1] != 'M' || data[2] != 'V' || data[3] != '1') continue;

                    int codeLength = data[4] & 0xff;
                    if (codeLength < 6 || codeLength > 30 || data.length <= 9 + codeLength) continue;

                    String meetingCode = new String(data, 5, codeLength, java.nio.charset.StandardCharsets.US_ASCII);
                    int userId = java.nio.ByteBuffer.wrap(data, 5 + codeLength, 4).getInt();
                    if (userId <= 0) continue;

                    SocketAddress sender = packet.getSocketAddress();
                    CLIENTS.put(sender, new ClientInfo(meetingCode, userId, System.currentTimeMillis()));

                    for (Map.Entry<SocketAddress, ClientInfo> entry : CLIENTS.entrySet()) {
                        ClientInfo target = entry.getValue();
                        if (entry.getKey().equals(sender) || !target.meetingCode.equals(meetingCode)) continue;
                        if (System.currentTimeMillis() - target.lastSeen > 30000) {
                            CLIENTS.remove(entry.getKey(), target);
                            continue;
                        }

                        byte[] outgoing = data.clone();
                        DatagramPacket forwarded = new DatagramPacket(outgoing, outgoing.length, (InetSocketAddress) entry.getKey());
                        server.send(forwarded);
                    }
                } catch (RuntimeException e) {
                    System.err.println("Invalid media packet: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("UDP media server error: " + e.getMessage());
        }
    }

    private static final class ClientInfo {
        private final String meetingCode;
        private final int userId;
        private final long lastSeen;

        private ClientInfo(String meetingCode, int userId, long lastSeen) {
            this.meetingCode = meetingCode;
            this.userId = userId;
            this.lastSeen = lastSeen;
        }
    }
}
