
package com.onlinemeeting.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TcpServer {

    private final int port;

    public TcpServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        ExecutorService pool = Executors.newFixedThreadPool(100);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("TCP Server listening on port " + port);

            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                pool.execute(new ClientHandler(socket));
            }
        } finally {
            pool.shutdown();
        }
    }
}
