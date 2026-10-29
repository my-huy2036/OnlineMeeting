
package com.onlinemeeting.ui;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

public final class CalendarDateTimePicker {
    private CalendarDateTimePicker() {}

    public static LocalDateTime showDialog(Component parent, LocalDateTime initial) {
        LocalDateTime start = initial == null ? LocalDateTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0) : initial;
        LocalDate[] selectedDate = {start.toLocalDate()};
        YearMonth[] displayedMonth = {YearMonth.from(start)};
        LocalDateTime[] result = {null};

        Window owner = SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, "Chọn ngày và giờ họp", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout(12, 12));

        JPanel main = new JPanel(new BorderLayout(8, 8));
        main.setBorder(BorderFactory.createEmptyBorder(16, 16, 8, 16));

        JPanel monthHeader = new JPanel(new BorderLayout(8, 8));
        JButton previousButton = new JButton("<");
        JButton nextButton = new JButton(">");
        JLabel monthLabel = new JLabel("", SwingConstants.CENTER);
        monthLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        monthHeader.add(previousButton, BorderLayout.WEST);
        monthHeader.add(monthLabel, BorderLayout.CENTER);
        monthHeader.add(nextButton, BorderLayout.EAST);

        JPanel calendar = new JPanel(new GridLayout(0, 7, 4, 4));
        JPanel timePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        timePanel.setBorder(BorderFactory.createTitledBorder("Thời gian bắt đầu"));

        JComboBox<Integer> hours = new JComboBox<>();
        JComboBox<Integer> minutes = new JComboBox<>();
        for (int i = 0; i < 24; i++) hours.addItem(i);
        for (int i = 0; i < 60; i += 5) minutes.addItem(i);
        hours.setSelectedItem(start.getHour());
        minutes.setSelectedItem((start.getMinute() / 5) * 5);
        timePanel.add(new JLabel("Giờ:"));
        timePanel.add(hours);
        timePanel.add(new JLabel("Phút:"));
        timePanel.add(minutes);

        Runnable[] renderCalendar = new Runnable[1];
        renderCalendar[0] = () -> {
            monthLabel.setText(displayedMonth[0].format(DateTimeFormatter.ofPattern("MM/yyyy")));
            calendar.removeAll();

            String[] weekdays = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};
            for (String weekday : weekdays) {
                JLabel label = new JLabel(weekday, SwingConstants.CENTER);
                label.setFont(new Font("SansSerif", Font.BOLD, 12));
                calendar.add(label);
            }

            LocalDate firstDay = displayedMonth[0].atDay(1);
            int offset = firstDay.getDayOfWeek().getValue() - 1;

            for (int i = 0; i < offset; i++) calendar.add(new JLabel(""));

            for (int day = 1; day <= displayedMonth[0].lengthOfMonth(); day++) {
                LocalDate date = displayedMonth[0].atDay(day);
                JButton dayButton = new JButton(String.valueOf(day));
                dayButton.setMargin(new Insets(5, 5, 5, 5));
                if (date.equals(selectedDate[0])) {
                    dayButton.setBackground(new Color(35, 130, 82));
                    dayButton.setForeground(Color.WHITE);
                    dayButton.setOpaque(true);
                    dayButton.setBorderPainted(false);
                }
                dayButton.addActionListener(e -> {
                    selectedDate[0] = date;
                    renderCalendar[0].run();
                });
                calendar.add(dayButton);
            }

            calendar.revalidate();
            calendar.repaint();
        };

        previousButton.addActionListener(e -> {
            displayedMonth[0] = displayedMonth[0].minusMonths(1);
            renderCalendar[0].run();
        });

        nextButton.addActionListener(e -> {
            displayedMonth[0] = displayedMonth[0].plusMonths(1);
            renderCalendar[0].run();
        });

        renderCalendar[0].run();

        JPanel calendarContainer = new JPanel(new BorderLayout(8, 8));
        calendarContainer.add(monthHeader, BorderLayout.NORTH);
        calendarContainer.add(calendar, BorderLayout.CENTER);

        main.add(calendarContainer, BorderLayout.CENTER);
        main.add(timePanel, BorderLayout.SOUTH);
        dialog.add(main, BorderLayout.CENTER);

        JButton confirmButton = new JButton("Xác nhận");
        JButton cancelButton = new JButton("Hủy");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setBorder(BorderFactory.createEmptyBorder(0, 16, 12, 16));
        buttons.add(cancelButton);
        buttons.add(confirmButton);
        dialog.add(buttons, BorderLayout.SOUTH);

        confirmButton.addActionListener(e -> {
            LocalDateTime selected = selectedDate[0].atTime((Integer) hours.getSelectedItem(), (Integer) minutes.getSelectedItem());
            if (selected.isBefore(LocalDateTime.now().withSecond(0).withNano(0))) {
                JOptionPane.showMessageDialog(dialog, "Thời gian bắt đầu không được ở trong quá khứ.");
                return;
            }
            result[0] = selected;
            dialog.dispose();
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        dialog.setSize(390, 430);
        dialog.setMinimumSize(new Dimension(360, 400));
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
        return result[0];
    }
}
