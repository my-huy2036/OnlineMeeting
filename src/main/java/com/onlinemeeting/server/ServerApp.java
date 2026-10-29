
package com.onlinemeeting.server;

import com.onlinemeeting.database.DatabaseConnection;

public class ServerApp {

    public static void main(String[] args) {
        int port = 5000;
        int udpPort = Integer.parseInt(System.getenv().getOrDefault("OM_UDP_PORT", "5001"));
        Thread mediaThread = new Thread(new UdpMediaServer(udpPort), "udp-media-server");
        mediaThread.setDaemon(false);
        mediaThread.start();

        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }

        if (DatabaseConnection.testConnection()) {
            System.out.println("SQL Server connected successfully.");
        } else {
            System.err.println(
                    "WARNING: SQL Server connection failed. "
                    + "PING still works, but LOGIN will fail.");
        }

        try {
            new TcpServer(port).start();
        } catch (Exception e) {
            System.err.println("Cannot start TCP Server: "
                    + e.getMessage());
        }
    }
}
