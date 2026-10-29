package com.onlinemeeting.ui.components;

 

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class HintTextField extends JTextField {
    private final String hint;

    public HintTextField(String hint) {
        this.hint = hint;
        setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        setOpaque(true);
        setBackground(Color.WHITE);
        setCaretColor(new Color(18, 61, 47));
        setSelectionColor(new Color(35, 130, 82));
        setSelectedTextColor(Color.WHITE);
        setForeground(new Color(45, 65, 52));
        setPreferredSize(new Dimension(300, 44));
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(214, 227, 217)), new EmptyBorder(9, 12, 9, 12)));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (getText().isEmpty() && !isFocusOwner()) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(new Color(155, 170, 159));
            g2.setFont(getFont());
            Insets insets = getInsets();
            g2.drawString(hint, insets.left, getHeight() / 2 + g2.getFontMetrics().getAscent() / 2 - 2);
            g2.dispose();
        }
    }
}
