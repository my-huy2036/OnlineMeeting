package com.onlinemeeting.ui.components;
 

import javax.swing.*;
import java.awt.*;

public class RoundedPanel extends JPanel {
    private final Color fill;
    private final int radius;

    public RoundedPanel(Color fill, int radius) {
        this.fill = fill;
        this.radius = radius;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(fill);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
        g2.dispose();
        super.paintComponent(g);
    }
}
