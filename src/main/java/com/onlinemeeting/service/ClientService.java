package com.onlinemeeting.service;
 

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinemeeting.protocol.Request;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientService {
    private final ObjectMapper mapper = new ObjectMapper();
    private final String host;
    private final int port;

    public ClientService(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public JsonNode send(Request request) throws IOException {
        try (Socket socket = new Socket(host, port)) {
            socket.setSoTimeout(10000);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            writer.write(mapper.writeValueAsString(request));
            writer.newLine();
            writer.flush();
            String line = reader.readLine();
            if (line == null) throw new IOException("Server đã đóng kết nối");
            return mapper.readTree(line);
        }
    }
}
