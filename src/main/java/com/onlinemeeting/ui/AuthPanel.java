
package com.onlinemeeting.ui;

import com.onlinemeeting.protocol.Request;
import com.onlinemeeting.ui.components.HintPasswordField;
import com.onlinemeeting.ui.components.HintTextField;
import com.onlinemeeting.ui.components.RoundedPanel;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.function.Consumer;

public class AuthPanel extends JPanel {
    private final Consumer<Request> onLogin;
    private final Consumer<Request> onRegister;
    private final Consumer<String> onStatus;
    private final CardLayout cards = new CardLayout();
    private final JPanel forms = new JPanel(cards);
    private JLabel messageLabel;
    private JButton loginButton;
    private JButton registerButton;

    public AuthPanel(Consumer<Request> onLogin, Consumer<Request> onRegister, Consumer<String> onStatus, String host, int port) {
        this.onLogin = onLogin;
        this.onRegister = onRegister;
        this.onStatus = onStatus;
        setLayout(new GridLayout(1, 2));
        setBackground(Theme.BACKGROUND);
        add(createBrandPanel());
        add(createFormArea(host, port));
    }

    private JPanel createBrandPanel() {
        JPanel panel = Theme.verticalPanel(Theme.DARK_GREEN);
        panel.setBorder(new EmptyBorder(45, 42, 42, 30));
        panel.add(Theme.label("OM", 25, Font.BOLD, Color.WHITE));
        panel.add(Box.createVerticalStrut(48));
        panel.add(Theme.label("Không gian họp", 30, Font.BOLD, Color.WHITE));
        panel.add(Box.createVerticalStrut(4));
        panel.add(Theme.label("của riêng bạn.", 30, Font.BOLD, new Color(151, 224, 177)));
        panel.add(Box.createVerticalStrut(22));
        panel.add(Theme.label("Kết nối và cộng tác dễ dàng.", 15, Font.PLAIN, new Color(218, 235, 224)));
        panel.add(Box.createVerticalStrut(38));
        panel.add(Theme.label("✓  Tạo phòng họp nhanh chóng", 14, Font.PLAIN, Color.WHITE));
        panel.add(Box.createVerticalStrut(18));
        panel.add(Theme.label("✓  Tham gia bằng mã cuộc họp", 14, Font.PLAIN, Color.WHITE));
        panel.add(Box.createVerticalStrut(18));
        panel.add(Theme.label("✓  Trò chuyện cùng mọi người", 14, Font.PLAIN, Color.WHITE));
        return panel;
    }

    private JPanel createFormArea(String host, int port) {
        JPanel area = new JPanel(new GridBagLayout());
        area.setBackground(Theme.BACKGROUND);
        area.setBorder(new EmptyBorder(25, 28, 25, 28));

        JPanel wrapper = new JPanel(new BorderLayout(0, 15));
        wrapper.setOpaque(false);
        wrapper.setPreferredSize(new Dimension(390, 570));

        JPanel heading = Theme.verticalPanel(Theme.BACKGROUND);
        heading.add(Theme.label("Chào mừng đến với", 15, Font.PLAIN, Theme.MUTED));
        heading.add(Box.createVerticalStrut(5));
        heading.add(Theme.label("OnlineMeeting", 29, Font.BOLD, Theme.DARK_GREEN));
        heading.add(Box.createVerticalStrut(7));
        heading.add(Theme.label("Kết nối dễ dàng. Họp hiệu quả.", 14, Font.PLAIN, Theme.MUTED));

        forms.setOpaque(false);
        forms.add(inScrollPane(createLoginForm()), "login");
        forms.add(createRegisterForm(), "register");

        messageLabel = Theme.label(" ", 12, Font.PLAIN, Theme.MUTED);
        messageLabel.setPreferredSize(new Dimension(390, 28));

        JPanel footer = new JPanel();
        footer.setOpaque(false);
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.add(messageLabel);
        footer.add(Theme.label("Máy chủ: " + host + ":" + port, 12, Font.PLAIN, Theme.MUTED));

        wrapper.add(heading, BorderLayout.NORTH);
        wrapper.add(forms, BorderLayout.CENTER);
        wrapper.add(footer, BorderLayout.SOUTH);
        area.add(wrapper);
        return area;
    }

    private JPanel createLoginForm() {
        JPanel panel = new RoundedPanel(Color.WHITE, 22);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(24, 24, 24, 24));
        panel.add(Theme.label("Đăng nhập", 21, Font.BOLD, Theme.DARK_GREEN));
        panel.add(Box.createVerticalStrut(7));
        panel.add(Theme.label("Nhập thông tin để tiếp tục.", 13, Font.PLAIN, Theme.MUTED));
        panel.add(Box.createVerticalStrut(25));

        JTextField username = new HintTextField("Nhập tên đăng nhập");
        JPasswordField password = new HintPasswordField("Nhập mật khẩu");
        Theme.addField(panel, "Tên đăng nhập", username);
        Theme.addField(panel, "Mật khẩu", password);

        loginButton = Theme.button("Đăng nhập");
        panel.add(loginButton);
        panel.add(Box.createVerticalStrut(15));
        panel.add(linkRow("Chưa có tài khoản?", "Đăng ký ngay", () -> {
            clearStatus();
            cards.show(forms, "register");
        }));

        loginButton.addActionListener(e -> {
            String user = username.getText().trim();
            String pass = new String(password.getPassword());
            if (user.isBlank() || pass.isEmpty()) {
                onStatus.accept("Vui lòng nhập tên đăng nhập và mật khẩu.");
                return;
            }
            Request request = new Request();
            request.type = "LOGIN";
            request.username = user;
            request.password = pass;
            clearStatus();
            onLogin.accept(request);
        });

        password.addActionListener(e -> loginButton.doClick());
        return panel;
    }

    private JComponent createRegisterForm() {
        JPanel panel = new RoundedPanel(Color.WHITE, 22);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(18, 24, 18, 24));
        panel.add(Theme.label("Tạo tài khoản", 21, Font.BOLD, Theme.DARK_GREEN));
        panel.add(Box.createVerticalStrut(5));
        panel.add(Theme.label("Điền thông tin để bắt đầu.", 13, Font.PLAIN, Theme.MUTED));
        panel.add(Box.createVerticalStrut(15));

        JTextField username = new HintTextField("3–50 ký tự");
        JTextField displayName = new HintTextField("Tên hiển thị");
        JTextField email = new HintTextField("Email không bắt buộc");
        JPasswordField password = new HintPasswordField("Tối thiểu 8 ký tự");
        JPasswordField confirm = new HintPasswordField("Nhập lại mật khẩu");

        Theme.addField(panel, "Tên đăng nhập *", username);
        Theme.addField(panel, "Tên hiển thị *", displayName);
        Theme.addField(panel, "Email", email);
        Theme.addField(panel, "Mật khẩu *", password);
        Theme.addField(panel, "Xác nhận mật khẩu *", confirm);

        registerButton = Theme.button("Tạo tài khoản");
        panel.add(registerButton);
        panel.add(Box.createVerticalStrut(12));
        panel.add(linkRow("Đã có tài khoản?", "Đăng nhập", () -> {
            clearStatus();
            cards.show(forms, "login");
        }));

        registerButton.addActionListener(e -> {
            String user = username.getText().trim();
            String name = displayName.getText().trim();
            String mail = email.getText().trim();
            String pass = new String(password.getPassword());
            String confirmPass = new String(confirm.getPassword());

            if (user.isBlank() || name.isBlank() || pass.isEmpty()) {
                onStatus.accept("Vui lòng nhập đủ các thông tin bắt buộc.");
                return;
            }
            if (user.length() < 3 || user.length() > 50) {
                onStatus.accept("Tên đăng nhập phải dài từ 3 đến 50 ký tự.");
                return;
            }
            if (pass.length() < 8) {
                onStatus.accept("Mật khẩu cần có ít nhất 8 ký tự.");
                return;
            }
            if (!pass.equals(confirmPass)) {
                onStatus.accept("Mật khẩu xác nhận không khớp.");
                return;
            }

            Request request = new Request();
            request.type = "REGISTER";
            request.username = user;
            request.password = pass;
            request.displayName = name;
            request.email = mail;
            clearStatus();
            onRegister.accept(request);
        });

        return inScrollPane(panel);
    }

    private JScrollPane inScrollPane(JComponent content) {
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getViewport().setBackground(Color.WHITE);
        return scroll;
    }

    private JPanel linkRow(String text, String link, Runnable action) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        row.setOpaque(false);
        row.add(Theme.label(text, 12, Font.PLAIN, Theme.MUTED));

        JButton button = new JButton(link);
        button.setFont(Theme.font(12, Font.BOLD));
        button.setForeground(Theme.GREEN);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(e -> action.run());
        row.add(button);
        return row;
    }

    private void clearStatus() {
        setStatus(" ", true);
    }

    public void setStatus(String message, boolean success) {
        if (messageLabel == null) return;
        messageLabel.setText(message == null || message.isBlank() ? " " : message);
        messageLabel.setForeground(success ? Theme.GREEN : new Color(190, 45, 45));
    }

    public void setBusy(boolean busy) {
        if (loginButton != null) loginButton.setEnabled(!busy);
        if (registerButton != null) registerButton.setEnabled(!busy);
    }
}
