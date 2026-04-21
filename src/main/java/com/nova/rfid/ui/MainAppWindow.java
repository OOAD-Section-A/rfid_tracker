package com.nova.rfid.ui;

import com.nova.rfid.pattern.structural.RFIDSystemFacade;

import javax.swing.*;
import java.awt.*;

import static com.nova.rfid.ui.ScanDashboardPanel.*;

/**
 * UI LAYER — MAIN APPLICATION WINDOW
 * SUBSYSTEM 11: BARCODE READER & RFID TRACKER — Team NOVA
 *
 * Renders the full application shell:
 *   Left sidebar → navigation (Scan | Scan Log)
 *   Right area   → swappable content panels
 *
 * GRASP - Controller: Entry-point for all navigation events.
 * SOLID - SRP: Only responsible for window/layout shell.
 * SEPARATE FILE — UI Subsystem team may add new nav items here.
 *
 * ── INTEGRATION CHANGE ────────────────────────────────────────────
 * DB status badge now calls facade.getDatabaseLabel() to display
 * whether we are connected to the shared OOAD database or the local
 * scm_rfid_db fallback. No other logic changes.
 * ─────────────────────────────────────────────────────────────────
 */
public class MainAppWindow extends JFrame {

    private final CardLayout     cardLayout  = new CardLayout();
    private final JPanel         contentArea = new JPanel(cardLayout);
    private       ScanDashboardPanel dashPanel;
    private       ScanLogPanel       logPanel;

    private JButton activeSideBtn;

    public MainAppWindow() {
        setTitle("SCM System — Barcode & RFID Tracker  |  Team NOVA");
        setSize(1100, 680);
        setMinimumSize(new Dimension(900, 560));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        applyLookAndFeel();
        buildWindow();
    }

    private void buildWindow() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(BG_DARK);

        // ── Sidebar ───────────────────────────────────────────────────────
        JPanel sidebar = buildSidebar();
        add(sidebar, BorderLayout.WEST);

        // ── Content panels ────────────────────────────────────────────────
        dashPanel = new ScanDashboardPanel();
        logPanel  = new ScanLogPanel();

        contentArea.setBackground(BG_DARK);
        contentArea.add(dashPanel, "SCAN");
        contentArea.add(logPanel,  "LOG");

        add(contentArea, BorderLayout.CENTER);
    }

    // ── Sidebar ───────────────────────────────────────────────────────────
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(BG_SURFACE);
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER_COL));
        sidebar.setPreferredSize(new Dimension(180, 0));

        // Brand label
        JPanel brand = new JPanel(new BorderLayout());
        brand.setBackground(BG_SURFACE);
        brand.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COL),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));

        JLabel brandLine1 = new JLabel("SCM System");
        brandLine1.setFont(new Font("JetBrains Mono", Font.BOLD, 13));
        brandLine1.setForeground(TEXT_PRI);
        JLabel brandLine2 = new JLabel("Barcode & RFID");
        brandLine2.setFont(FONT_SMALL);
        brandLine2.setForeground(TEXT_SEC);

        JPanel brandText = new JPanel();
        brandText.setLayout(new BoxLayout(brandText, BoxLayout.Y_AXIS));
        brandText.setOpaque(false);
        brandText.add(brandLine1);
        brandText.add(brandLine2);
        brand.add(brandText);
        sidebar.add(brand);

        // Nav items
        sidebar.add(Box.createVerticalStrut(8));

        JButton btnScan = buildNavButton("■  Scan",     "SCAN");
        JButton btnLog  = buildNavButton("□  Scan Log", "LOG");

        sidebar.add(btnScan);
        sidebar.add(btnLog);

        // Set initial active
        setActiveNavButton(btnScan);
        btnScan.addActionListener(e -> {
            cardLayout.show(contentArea, "SCAN");
            setActiveNavButton(btnScan);
            dashPanel.refreshDashboard();
        });
        btnLog.addActionListener(e -> {
            cardLayout.show(contentArea, "LOG");
            setActiveNavButton(btnLog);
            logPanel.loadAllLogs();
        });

        sidebar.add(Box.createVerticalGlue());

        // ── DB status badge ───────────────────────────────────────────────
        // Uses facade.getDatabaseLabel() to show OOAD (shared) vs local DB.
        JPanel dbStatus = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        dbStatus.setBackground(BG_SURFACE);
        dbStatus.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COL),
                BorderFactory.createEmptyBorder(10, 8, 10, 8)));

        RFIDSystemFacade facade = RFIDSystemFacade.getInstance();
        boolean connected = facade.isDatabaseConnected();
        String  dbLabel   = "● " + facade.getDatabaseLabel();

        JLabel dbLbl = new JLabel(dbLabel);
        dbLbl.setFont(FONT_SMALL);
        dbLbl.setForeground(connected ? ACCENT_GRN : ACCENT_RED);
        dbStatus.add(dbLbl);
        sidebar.add(dbStatus);

        return sidebar;
    }

    private JButton buildNavButton(String text, String card) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BODY);
        btn.setForeground(TEXT_SEC);
        btn.setBackground(BG_SURFACE);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void setActiveNavButton(JButton btn) {
        if (activeSideBtn != null) {
            activeSideBtn.setBackground(BG_SURFACE);
            activeSideBtn.setForeground(TEXT_SEC);
        }
        btn.setBackground(ACCENT_BLUE.darker().darker());
        btn.setForeground(TEXT_PRI);
        activeSideBtn = btn;
    }

    // ── L&F ───────────────────────────────────────────────────────────────
    private static void applyLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            UIManager.put("ScrollBarUI", "javax.swing.plaf.basic.BasicScrollBarUI");
            UIManager.put("ScrollBar.background",      BG_SURFACE);
            UIManager.put("ScrollBar.thumb",           BORDER_COL);
            UIManager.put("ScrollBar.thumbDarkShadow", BORDER_COL);
        } catch (Exception ignored) {}
    }

    // ── Entry point ───────────────────────────────────────────────────────
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            System.setProperty("awt.useSystemAAFontSettings", "on");
            System.setProperty("swing.aatext", "true");
            new MainAppWindow().setVisible(true);
        });
    }
}