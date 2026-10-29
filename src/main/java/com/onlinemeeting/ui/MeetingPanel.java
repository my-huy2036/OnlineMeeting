
package com.onlinemeeting.ui;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinemeeting.protocol.Request;
import com.onlinemeeting.service.ClientService;
import javax.swing.*;
import java.awt.*;
import java.util.concurrent.ExecutionException;

public class MeetingPanel extends JFrame {
    private final ClientService clientService;
    private final int userId;
    private final String meetingCode;
    private final String meetingTitle;
    private final JTextArea participantsArea = new JTextArea();
    private final JLabel statusLabel = new JLabel("Dang ket noi...");
    private boolean leaving;
    private final VideoPanel videoPanel;

    public MeetingPanel(JsonNode meeting, int userId) {
        this.userId = userId;
        this.meetingCode = meeting.path("meetingCode").asText();
        this.meetingTitle = meeting.path("title").asText("Cuoc hop");
        String host = System.getenv().getOrDefault("OM_SERVER_HOST", "172.20.10.5");
        int port = Integer.parseInt(System.getenv().getOrDefault("OM_SERVER_PORT", "5000"));
        int udpPort = Integer.parseInt(System.getenv().getOrDefault("OM_UDP_PORT", "5001"));
        this.clientService = new ClientService(host, port);
        try {
            this.videoPanel = new VideoPanel(host, udpPort, meetingCode, userId);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Khong khoi tao duoc video", e);
        }
        

        setTitle("OnlineMeeting - " + meetingTitle);
        setSize(650, 500);
        setMinimumSize(new Dimension(500, 400));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setLayout(new BorderLayout(12, 12));

        JPanel header = new JPanel(new BorderLayout(8, 8));
        header.setBorder(BorderFactory.createEmptyBorder(16, 16, 4, 16));
        JLabel titleLabel = new JLabel(meetingTitle);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        JLabel codeLabel = new JLabel("Ma phong: " + meetingCode);
        header.add(titleLabel, BorderLayout.NORTH);
        header.add(codeLabel, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        participantsArea.setEditable(false);
        participantsArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
        participantsArea.setText("Dang tai danh sach thanh vien...");
        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
        center.add(new JLabel("Thanh vien trong phong"), BorderLayout.NORTH);
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, videoPanel, new JScrollPane(participantsArea));
        splitPane.setResizeWeight(0.7);
        center.add(splitPane, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        JButton refreshButton = new JButton("Lam moi thanh vien");
        JButton leaveButton = new JButton("Roi cuoc hop");
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.setBorder(BorderFactory.createEmptyBorder(4, 16, 12, 16));
        actions.add(refreshButton);
        actions.add(leaveButton);
        JPanel footer = new JPanel(new BorderLayout());
        footer.add(statusLabel, BorderLayout.NORTH);
        footer.add(actions, BorderLayout.SOUTH);
        add(footer, BorderLayout.SOUTH);

        refreshButton.addActionListener(e -> refreshParticipants());
        leaveButton.addActionListener(e -> leaveMeeting());
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                leaveMeeting();
            }
        });

        refreshParticipants();
    }

    private void refreshParticipants() {
        statusLabel.setText("Dang cap nhat danh sach...");
        new SwingWorker<JsonNode, Void>() {
            @Override
            protected JsonNode doInBackground() throws Exception {
                Request request = new Request();
                request.type = "LIST_PARTICIPANTS";
                request.meetingCode = meetingCode;
                return clientService.send(request);
            }

            @Override
            protected void done() {
                try {
                    JsonNode response = get();
                    if (!response.path("success").asBoolean()) {
                        statusLabel.setText(response.path("message").asText("Khong lay duoc danh sach"));
                        return;
                    }
                    JsonNode data = response.path("data");
                    StringBuilder text = new StringBuilder();
                    if (data.isArray()) {
                        for (JsonNode person : data) {
                            text.append(person.path("displayName").asText(person.path("username").asText("User")))
                                .append(" (").append(person.path("role").asText("participant")).append(")")
                                .append(" | Mic: ").append(person.path("micEnabled").asBoolean() ? "Bat" : "Tat")
                                .append(" | Camera: ").append(person.path("cameraEnabled").asBoolean() ? "Bat" : "Tat")
                                .append("\n");
                        }
                    }
                    participantsArea.setText(text.isEmpty() ? "Chua co thanh vien." : text.toString());
                    statusLabel.setText("Ma phong: " + meetingCode);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    statusLabel.setText("Tac vu bi gian doan");
                } catch (ExecutionException e) {
                    statusLabel.setText("Khong ket noi duoc server");
                }
            }
        }.execute();
    }

    private void leaveMeeting() {
        if (leaving) return;
        int choice = JOptionPane.showConfirmDialog(this, "Ban co chac muon roi cuoc hop?", "Xac nhan", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) return;
        leaving = true;
        statusLabel.setText("Dang roi cuoc hop...");
        new SwingWorker<JsonNode, Void>() {
            @Override
            protected JsonNode doInBackground() throws Exception {
                Request request = new Request();
                request.type = "LEAVE_MEETING";
                request.userId = userId;
                request.meetingCode = meetingCode;
                return clientService.send(request);
            }

            @Override
            protected void done() {
                try {
                    JsonNode response = get();
                    if (!response.path("success").asBoolean()) {
                        JOptionPane.showMessageDialog(MeetingPanel.this, response.path("message").asText("Khong roi duoc cuoc hop"));
                        leaving = false;
                        statusLabel.setText("Ma phong: " + meetingCode);
                        return;
                    }
                    videoPanel.closeMedia();
                    dispose();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    leaving = false;
                    statusLabel.setText("Tac vu bi gian doan");
                } catch (ExecutionException e) {
                    leaving = false;
                    JOptionPane.showMessageDialog(MeetingPanel.this, "Khong ket noi duoc server");
                }
            }
        }.execute();
    }
}
