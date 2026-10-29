
package com.onlinemeeting.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinemeeting.protocol.Request;
import com.onlinemeeting.protocol.Response;
import com.onlinemeeting.service.AuthService;
import com.onlinemeeting.service.MeetingService;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientHandler implements Runnable {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final AuthService AUTH_SERVICE = new AuthService();
    private static final MeetingService MEETING_SERVICE = new MeetingService();
    private final Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (Socket client = socket;
             BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Response response;
                Request request;
                try {
                    request = MAPPER.readValue(line, Request.class);
                } catch (IOException e) {
                    System.err.println("JSON parse error: " + e.getMessage());
                    System.err.println("Received data: " + line);
                    response = Response.error("Du lieu JSON gui len khong hop le");
                    writer.write(MAPPER.writeValueAsString(response));
                    writer.newLine();
                    writer.flush();
                    continue;
                }

                try {
                    response = handleRequest(request);
                } catch (RuntimeException e) {
                    System.err.println("Request processing error: " + e.getMessage());
                    e.printStackTrace();
                    response = Response.error("Loi xu ly yeu cau: " + e.getMessage());
                }

                writer.write(MAPPER.writeValueAsString(response));
                writer.newLine();
                writer.flush();
            }
        } catch (IOException e) {
            System.err.println("Client connection error: " + e.getMessage());
        }
    }

    private Response handleRequest(Request request) {
        if (request == null || request.type == null || request.type.isBlank()) {
            return Response.error("Thieu loai yeu cau");
        }

        return switch (request.type.trim().toUpperCase()) {
            case "PING" -> Response.ok("PONG", null);
            case "LOGIN" -> AUTH_SERVICE.login(request.username, request.password, socket.getInetAddress());
            case "REGISTER" -> AUTH_SERVICE.register(request.username, request.password, request.displayName, request.email);
            case "CREATE_MEETING" -> MEETING_SERVICE.createMeeting(request.userId, request.title, request.description, request.scheduledStartAt);
            case "JOIN_MEETING" -> MEETING_SERVICE.joinMeetingByCode(request.userId, request.meetingCode);
            case "LEAVE_MEETING" -> MEETING_SERVICE.leaveMeeting(request.userId, request.meetingCode);
            case "LIST_PARTICIPANTS" -> MEETING_SERVICE.listParticipants(request.meetingCode);
            default -> Response.error("Lenh khong duoc ho tro: " + request.type);
        };
    }
}
