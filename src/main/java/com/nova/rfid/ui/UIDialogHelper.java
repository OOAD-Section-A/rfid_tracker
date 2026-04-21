package com.nova.rfid.ui;

import com.nova.rfid.exception.RFIDException;

import javax.swing.*;
import java.awt.*;

import static com.nova.rfid.ui.ScanDashboardPanel.*;

/**
 * UI LAYER — Dialog Helper
 * SUBSYSTEM 11: BARCODE READER & RFID TRACKER — Team NOVA
 *
 * Centralises all popup dialogs for consistent UI presentation.
 * SEPARATE FILE — UI Subsystem team (Sub. 16) can reskin dialogs here.
 *
 * GRASP - Pure Fabrication: Not a domain concept; created purely to
 *   reduce duplication across UI panels.
 * GRASP - Low Coupling: All panels use this helper; no JOptionPane duplication.
 * SOLID - SRP: Solely responsible for showing dialogs.
 */
public class UIDialogHelper {

    private UIDialogHelper() {} // Utility class

    public static void showWarning(Component parent, RFIDException e) {
        showDialog(parent, "Warning — " + e.getErrorCode(),
                   e.getMessage(), ACCENT_ORG, "⚠");
    }

    public static void showError(Component parent, RFIDException e) {
        showDialog(parent, "Error — " + e.getErrorCode(),
                   e.getMessage(), ACCENT_RED, "✗");
    }

    public static void showFatalError(Component parent, RFIDException e) {
        showDialog(parent, "CRITICAL — " + e.getErrorCode(),
                   e.getMessage() + "\n\nPlease contact the system administrator.",
                   ACCENT_RED, "⛔");
    }

    public static void showInfo(Component parent, String message) {
        showDialog(parent, "Information", message, ACCENT_BLUE, "ℹ");
    }

    private static void showDialog(Component parent, String title,
                                    String message, Color accent, String icon) {
        JPanel panel = new JPanel(new BorderLayout(12, 0));
        panel.setBackground(BG_CARD);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));
        iconLabel.setForeground(accent);

        JTextArea text = new JTextArea(message);
        text.setFont(FONT_SMALL);
        text.setForeground(TEXT_PRI);
        text.setBackground(BG_CARD);
        text.setEditable(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setColumns(35);

        panel.add(iconLabel, BorderLayout.WEST);
        panel.add(text,      BorderLayout.CENTER);

        UIManager.put("OptionPane.background",   BG_CARD);
        UIManager.put("Panel.background",        BG_CARD);
        UIManager.put("OptionPane.messageForeground", TEXT_PRI);

        JOptionPane.showMessageDialog(parent, panel, title,
                JOptionPane.PLAIN_MESSAGE);
    }
}
