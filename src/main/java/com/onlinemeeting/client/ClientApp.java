
package com.onlinemeeting.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.onlinemeeting.protocol.Request;
import com.onlinemeeting.service.ClientService;
import com.onlinemeeting.ui.AuthPanel;
import com.onlinemeeting.ui.HomePanel;
import javax.swing.*;
import java.awt.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientApp extends JFrame {
    private final String host = System.getenv().getOrDefault("OM_SERVER_HOST", "172.20.10.5");
    private final int port = Integer.parseInt(System.getenv().getOrDefault("OM_SERVER_PORT", "5000"));
    private final ClientService clientService = new ClientService(host, port);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private AuthPanel authPanel;

    public ClientApp() {
        setTitle("OnlineMeeting");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 720);
        setMinimumSize(new Dimension(850, 620));
        setLocationRelativeTo(null);
        showAuthScreen();
    }

    private void showAuthScreen() {
        authPanel = new AuthPanel(this::login, this::register, this::setValidationStatus, host, port);
        setContentPane(authPanel);
        revalidate();
        repaint();
    }

    private void login(Request request) {
        submit(request, true);
    }

    private void register(Request request) {
        submit(request, false);
    }

    private void submit(Request request, boolean isLogin) {
        AuthPanel currentPanel = authPanel;
        currentPanel.setBusy(true);
        currentPanel.setStatus("Đang kết nối đến máy chủ...", true);

        executor.submit(() -> {
            JsonNode response;
            try {
                response = clientService.send(request);
            } catch (Exception e) {
                String error = e.getMessage() == null ? "Không thể kết nối máy chủ." : e.getMessage();
                SwingUtilities.invokeLater(() -> {
                    if (authPanel != currentPanel) return;
                    currentPanel.setBusy(false);
                    currentPanel.setStatus("Lỗi kết nối: " + error, false);
                });
                return;
            }

            SwingUtilities.invokeLater(() -> {
                if (authPanel != currentPanel) return;
                boolean success = response.path("success").asBoolean(false);
                String message = response.path("message").asText("Không có phản hồi từ máy chủ.");
                currentPanel.setBusy(false);
                currentPanel.setStatus(message, success);
                if (success && isLogin) showHome(response.path("data"));
            });
        });
    }

    private void setValidationStatus(String message) {
        if (authPanel != null) authPanel.setStatus(message, false);
    }

    private void showHome(JsonNode user) {
        HomePanel homePanel = new HomePanel(user, () -> {
            showAuthScreen();
            setSize(1000, 720);
            setLocationRelativeTo(null);
        });
        setContentPane(homePanel);
        setSize(1000, 720);
        setLocationRelativeTo(null);
        revalidate();
        repaint();
    }

    @Override
    public void dispose() {
        executor.shutdownNow();
        super.dispose();
    }

    public static void main(String[] args) {
        // The native macOS Aqua LAF may paint custom-border text fields invisibly.
        // Metal gives our custom UI consistent controls on macOS and Windows.
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            UIManager.put("TextField.background", Color.WHITE);
            UIManager.put("TextField.foreground", new Color(45, 65, 52));
            UIManager.put("PasswordField.background", Color.WHITE);
            UIManager.put("PasswordField.foreground", new Color(45, 65, 52));
        } catch (Exception e) {
            System.err.println("Không thể thiết lập giao diện đa nền tảng: " + e.getMessage());
        }
        SwingUtilities.invokeLater(() -> new ClientApp().setVisible(true));
    }
}
