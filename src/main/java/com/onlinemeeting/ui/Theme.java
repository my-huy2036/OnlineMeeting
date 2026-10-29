package com.onlinemeeting.ui;

 

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public final class Theme {
    public static final Color GREEN = new Color(35, 130, 82);
    public static final Color DARK_GREEN = new Color(18, 61, 47);
    public static final Color BACKGROUND = new Color(247, 250, 248);
    public static final Color MUTED = new Color(112, 130, 119);
    public static final Color BORDER = new Color(222, 232, 225);
    public static final Color ERROR = new Color(190, 45, 45);

    private Theme() {}

    public static Font font(int size, int style) {
        return new Font(Font.SANS_SERIF, style, size);
    }

    public static JLabel label(String text, int size, int style, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font(size, style));
        label.setForeground(color);
        return label;
    }

    public static JPanel verticalPanel(Color color) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(color);
        return panel;
    }

    public static JButton button(String text) {
        JButton button = new JButton(text);
        button.setFont(font(14, Font.BOLD));
        button.setForeground(Color.WHITE);
        button.setBackground(GREEN);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(300, 44));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (button.isEnabled()) button.setBackground(new Color(27, 108, 67));
            }

            public void mouseExited(java.awt.event.MouseEvent e) {
                button.setBackground(GREEN);
            }
        });
        return button;
    }

    public static JButton outlineButton(String text) {
        JButton button = new JButton(text);
        button.setFont(font(13, Font.BOLD));
        button.setForeground(DARK_GREEN);
        button.setBackground(Color.WHITE);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER), new EmptyBorder(9, 14, 9, 14)));
        return button;
    }


    /** Make text inputs predictable on macOS Aqua / dark appearance and Windows. */
    public static void styleTextField(JTextField field) {
        field.setOpaque(true);
        field.setBackground(Color.WHITE);
        field.setForeground(new Color(45, 65, 52));
        field.setCaretColor(DARK_GREEN);
        field.setSelectedTextColor(Color.WHITE);
        field.setSelectionColor(GREEN);
        field.setFont(font(14, Font.PLAIN));
        field.setPreferredSize(new Dimension(300, 44));
        field.setMinimumSize(new Dimension(100, 40));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(214, 227, 217)),
                new EmptyBorder(9, 12, 9, 12)));
    }

    public static void addField(JPanel panel, String title, JComponent field) {
        JLabel caption = label(title, 13, Font.BOLD, new Color(55, 75, 63));
        caption.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (field instanceof JTextField input) styleTextField(input);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        panel.add(caption);
        panel.add(Box.createVerticalStrut(7));
        panel.add(field);
        panel.add(Box.createVerticalStrut(17));
    }
}
