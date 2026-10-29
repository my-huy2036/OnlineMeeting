
package com.onlinemeeting.ui;

import com.fasterxml.jackson.databind.JsonNode;
import com.onlinemeeting.protocol.Request;
import com.onlinemeeting.service.ClientService;
import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutionException;
public class HomePanel extends JPanel {
    private final JsonNode user;
    private final Runnable onLogout;
    private final ClientService clientService;
    private final int userId;
    private final JLabel statusLabel = new JLabel("San sang");

    public HomePanel(JsonNode user, Runnable onLogout) {
        this.user = user;
        this.onLogout = onLogout;
        this.userId = user.path("userId").asInt(user.path("UserId").asInt());
        String host = System.getenv().getOrDefault("OM_SERVER_HOST", "172.20.10.5");
        int port = Integer.parseInt(System.getenv().getOrDefault("OM_SERVER_PORT", "5000"));
        this.clientService = new ClientService(host, port);

        setLayout(new BorderLayout(16, 16));
        setBackground(new Color(247, 251, 248));
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        String displayName = user.path("displayName").asText(user.path("DisplayName").asText(user.path("username").asText("Nguoi dung")));
        JLabel heading = new JLabel("Xin chao, " + displayName);
        heading.setFont(new Font("SansSerif", Font.BOLD, 25));
        JLabel subtitle = new JLabel("Tao cuoc hop moi hoac tham gia bang ma phong.");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));

        JPanel top = new JPanel(new GridLayout(2, 1, 4, 4));
        top.setOpaque(false);
        top.add(heading);
        top.add(subtitle);
        add(top, BorderLayout.NORTH);

        JButton createButton = createButton("Tao cuoc hop");
        JButton joinButton = createButton("Tham gia cuoc hop");
        JButton logoutButton = new JButton("Dang xuat");

        JPanel cards = new JPanel(new GridLayout(1, 2, 16, 16));
        cards.setOpaque(false);
        cards.add(createCard("Tao cuoc hop", "Tao phong moi va chia se ma phong.", createButton));
        cards.add(createCard("Tham gia cuoc hop", "Nhap ma phong do nguoi khac cung cap.", joinButton));
        add(cards, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(statusLabel, BorderLayout.CENTER);
        bottom.add(logoutButton, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        createButton.addActionListener(e -> showCreateDialog());
        joinButton.addActionListener(e -> showJoinDialog());
        logoutButton.addActionListener(e -> {
            if (onLogout != null) onLogout.run();
        });
    }

    private JPanel createCard(String title, String description, JButton action) {
        JPanel card = new JPanel(new BorderLayout(10, 14));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 232, 224)),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        JLabel descriptionLabel = new JLabel("<html>" + description + "</html>");
        descriptionLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        card.add(titleLabel, BorderLayout.NORTH);
        card.add(descriptionLabel, BorderLayout.CENTER);
        card.add(action, BorderLayout.SOUTH);
        return card;
    }

    private JButton createButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        button.setBackground(new Color(35, 130, 82));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        return button;
    }


private void showCreateDialog() {
    JTextField titleField = new JTextField("Cuoc hop moi", 22);
    JTextField descriptionField = new JTextField(22);
    Theme.styleTextField(titleField);
    Theme.styleTextField(descriptionField);
    JCheckBox scheduleCheckBox = new JCheckBox("Lên lịch cuộc họp");
    JButton dateTimeButton = new JButton("Chọn ngày và giờ");
    JLabel dateTimeLabel = new JLabel("Bắt đầu ngay khi tạo");
    LocalDateTime[] scheduledTime = {null};

    dateTimeButton.setEnabled(false);

    scheduleCheckBox.addActionListener(e -> {
        dateTimeButton.setEnabled(scheduleCheckBox.isSelected());
        if (!scheduleCheckBox.isSelected()) {
            scheduledTime[0] = null;
            dateTimeLabel.setText("Bắt đầu ngay khi tạo");
        }
    });

    dateTimeButton.addActionListener(e -> {
        LocalDateTime selected = CalendarDateTimePicker.showDialog(this, scheduledTime[0]);
        if (selected != null) {
            scheduledTime[0] = selected;
            dateTimeLabel.setText(selected.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        }
    });

    JPanel form = new JPanel(new GridLayout(0, 1, 6, 6));
    form.add(new JLabel("Tên cuộc họp"));
    form.add(titleField);
    form.add(new JLabel("Mô tả (không bắt buộc)"));
    form.add(descriptionField);
    form.add(scheduleCheckBox);
    form.add(dateTimeButton);
    form.add(dateTimeLabel);

    int result = JOptionPane.showConfirmDialog(this, form, "Tạo cuộc họp", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
    if (result != JOptionPane.OK_OPTION) return;

    String title = titleField.getText().trim();
    String description = descriptionField.getText().trim();

    if (title.isEmpty()) {
        JOptionPane.showMessageDialog(this, "Vui lòng nhập tên cuộc họp.");
        return;
    }

    if (scheduleCheckBox.isSelected() && scheduledTime[0] == null) {
        JOptionPane.showMessageDialog(this, "Vui lòng chọn ngày và giờ bắt đầu.");
        return;
    }

    String scheduled = scheduledTime[0] == null ? null : scheduledTime[0].format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    sendMeetingRequest("CREATE_MEETING", null, title, description, scheduled, true);
}


    private void showJoinDialog() {
        JTextField codeField = new JTextField(22);
        Theme.styleTextField(codeField);
        JPanel form = new JPanel(new BorderLayout(0, 10));
        form.add(new JLabel("Nhập mã cuộc họp:"), BorderLayout.NORTH);
        form.add(codeField, BorderLayout.CENTER);
        int result = JOptionPane.showConfirmDialog(this, form, "Tham gia cuộc họp",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION || codeField.getText().isBlank()) return;
        sendMeetingRequest("JOIN_MEETING", codeField.getText().trim(), null, null, null, false);
    }

    private void sendMeetingRequest(String type, String code, String title, String description, String scheduled, boolean created) {
        if (userId <= 0) {
            JOptionPane.showMessageDialog(this, "Khong doc duoc UserId tu thong tin dang nhap. Hay kiem tra du lieu User tra ve tu server.");
            return;
        }

        statusLabel.setText("Dang gui yeu cau...");
        setButtonsEnabled(false);

        new SwingWorker<com.fasterxml.jackson.databind.JsonNode, Void>() {
            @Override
            protected com.fasterxml.jackson.databind.JsonNode doInBackground() throws Exception {
                Request request = new Request();
                request.type = type;
                request.userId = userId;
                request.meetingCode = code;
                request.title = title;
                request.description = description;
                request.scheduledStartAt = scheduled;
                return clientService.send(request);
            }

            @Override
            protected void done() {
                setButtonsEnabled(true);
                try {
                    com.fasterxml.jackson.databind.JsonNode response = get();
                    statusLabel.setText(response.path("message").asText("Da xu ly"));
                    if (!response.path("success").asBoolean()) {
                        JOptionPane.showMessageDialog(HomePanel.this, response.path("message").asText("Yeu cau that bai"));
                        return;
                    }
                    com.fasterxml.jackson.databind.JsonNode data = response.path("data");
                    if (created) {
                        JOptionPane.showMessageDialog(HomePanel.this, "Tao cuoc hop thanh cong!\nMa phong: " + data.path("meetingCode").asText());
                    }
                    MeetingPanel meetingPanel = new MeetingPanel(data, userId);
                    meetingPanel.setVisible(true);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    statusLabel.setText("Tac vu bi gian doan");
                    JOptionPane.showMessageDialog(HomePanel.this, "Tac vu bi gian doan");
                } catch (ExecutionException e) {
                    statusLabel.setText("Loi ket noi server");
                    JOptionPane.showMessageDialog(HomePanel.this, "Khong ket noi duoc server: " + e.getCause().getMessage());
                } finally {
                    setButtonsEnabled(true);
                }
            }
        }.execute();
    }

    private void setButtonsEnabled(boolean enabled) {
        for (Component component : getComponents()) {
            if (component instanceof JPanel panel) {
                for (Component child : panel.getComponents()) {
                    if (child instanceof JPanel card) {
                        for (Component item : card.getComponents()) {
                            if (item instanceof JButton button) button.setEnabled(enabled);
                        }
                    }
                }
            }
        }
    }
}
