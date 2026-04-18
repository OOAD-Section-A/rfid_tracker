package com.nova.rfid.ui;

import com.nova.rfid.exception.RFIDException;
import com.nova.rfid.model.ScanRecord;
import com.nova.rfid.pattern.structural.RFIDSystemFacade;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * ═══════════════════════════════════════════════════════════════════
 * UI LAYER — SCAN DASHBOARD
 * SUBSYSTEM 11: BARCODE READER & RFID TRACKER — Team NOVA
 *
 * SEPARATE FILE — can be updated by UI SUBSYSTEM TEAM (Sub. 16)
 *   without touching service, db, or exception layers.
 *
 * Matches wireframe: left sidebar navigation, stat cards, recent scans
 * table, and manual entry panel on the right.
 *
 * GRASP - Controller: Handles user events, delegates to Facade.
 * GRASP - Low Coupling: Only communicates through RFIDSystemFacade.
 * SOLID - SRP: Only responsible for rendering the Scan Dashboard view.
 * ═══════════════════════════════════════════════════════════════════
 */
public class ScanDashboardPanel extends JPanel {

    // ── Colour palette ────────────────────────────────────────────────────
    static final Color BG_DARK     = new Color(18,  20,  28);
    static final Color BG_SURFACE  = new Color(26,  30,  42);
    static final Color BG_CARD     = new Color(34,  38,  55);
    static final Color ACCENT_BLUE = new Color(56, 132, 255);
    static final Color ACCENT_GRN  = new Color(34, 197, 94);
    static final Color ACCENT_RED  = new Color(239, 68, 68);
    static final Color ACCENT_ORG  = new Color(251,146, 60);
    static final Color TEXT_PRI    = new Color(240,242,255);
    static final Color TEXT_SEC    = new Color(148,156,180);
    static final Color BORDER_COL  = new Color(48,  54,  74);

    // ── Fonts ─────────────────────────────────────────────────────────────
    static final Font  FONT_TITLE  = new Font("JetBrains Mono", Font.BOLD,  15);
    static final Font  FONT_BODY   = new Font("JetBrains Mono", Font.PLAIN, 12);
    static final Font  FONT_SMALL  = new Font("JetBrains Mono", Font.PLAIN, 11);
    static final Font  FONT_STAT   = new Font("JetBrains Mono", Font.BOLD,  32);
    static final Font  FONT_LABEL  = new Font("JetBrains Mono", Font.BOLD,  11);

    // ── State ─────────────────────────────────────────────────────────────
    private final RFIDSystemFacade facade = RFIDSystemFacade.getInstance();

    // ── Stat labels (updated on each scan) ───────────────────────────────
    private JLabel lblTotal, lblSuccessful, lblFailed;

    // ── Recent scans table ────────────────────────────────────────────────
    private DefaultTableModel tableModel;

    // ── Manual entry ──────────────────────────────────────────────────────
    private JTextField  txtTagInput;
    private JToggleButton btnSourceRFID, btnSourceBarcode;
    private JLabel      lblStatus;

    // ── Source indicator badges ───────────────────────────────────────────
    private JLabel lblBarcodeOnline, lblRfidOnline;

    public ScanDashboardPanel() {
        setLayout(new BorderLayout());
        setBackground(BG_DARK);
        buildUI();
        refreshDashboard();
        startAutoRefresh();
    }

    // ══════════════════════════════════════════════════════════════════════
    // BUILD UI
    // ══════════════════════════════════════════════════════════════════════
    private void buildUI() {

        // ── Top Bar ───────────────────────────────────────────────────────
        JPanel topBar = buildTopBar();
        add(topBar, BorderLayout.NORTH);

        // ── Content area ──────────────────────────────────────────────────
        JPanel content = new JPanel(new BorderLayout(12, 12));
        content.setBackground(BG_DARK);
        content.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // Stat cards row
        content.add(buildStatCardsPanel(), BorderLayout.NORTH);

        // Centre: recent scans table + manual entry
        JPanel centre = new JPanel(new BorderLayout(12, 0));
        centre.setOpaque(false);
        centre.add(buildRecentScansPanel(), BorderLayout.CENTER);
        centre.add(buildManualEntryPanel(), BorderLayout.EAST);
        content.add(centre, BorderLayout.CENTER);

        add(content, BorderLayout.CENTER);
    }

    // ── Top bar with title and status badges ──────────────────────────────
    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(BG_SURFACE);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COL),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)));

        JLabel title = new JLabel("Scan");
        title.setFont(FONT_TITLE);
        title.setForeground(TEXT_PRI);
        bar.add(title, BorderLayout.WEST);

        JPanel badges = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        badges.setOpaque(false);

        lblBarcodeOnline = buildBadge("● Barcode Reader: ONLINE", ACCENT_GRN);
        lblRfidOnline    = buildBadge("● RFID Reader: ONLINE",    ACCENT_GRN);
        badges.add(lblBarcodeOnline);
        badges.add(lblRfidOnline);
        bar.add(badges, BorderLayout.EAST);

        return bar;
    }

    private JLabel buildBadge(String text, Color col) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_SMALL);
        l.setForeground(col);
        l.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(col.darker(), 1, true),
                BorderFactory.createEmptyBorder(3, 8, 3, 8)));
        return l;
    }

    // ── 3-up stat cards ───────────────────────────────────────────────────
    private JPanel buildStatCardsPanel() {
        JPanel row = new JPanel(new GridLayout(1, 3, 12, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));

        lblTotal      = new JLabel("000", SwingConstants.CENTER);
        lblSuccessful = new JLabel("000", SwingConstants.CENTER);
        lblFailed     = new JLabel("000", SwingConstants.CENTER);

        row.add(buildStatCard("TOTAL SCANS TODAY",   lblTotal,      ACCENT_BLUE));
        row.add(buildStatCard("SUCCESSFUL",           lblSuccessful, ACCENT_GRN));
        row.add(buildStatCard("FAILED / UNKNOWN",     lblFailed,     ACCENT_RED));
        return row;
    }

    private JPanel buildStatCard(String subtitle, JLabel numLabel, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COL, 1),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)));

        numLabel.setFont(FONT_STAT);
        numLabel.setForeground(accent);

        JLabel sub = new JLabel(subtitle, SwingConstants.CENTER);
        sub.setFont(FONT_LABEL);
        sub.setForeground(TEXT_SEC);

        card.add(numLabel, BorderLayout.CENTER);
        card.add(sub,      BorderLayout.SOUTH);
        return card;
    }

    // ── Recent scans table ────────────────────────────────────────────────
    private JPanel buildRecentScansPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(BG_CARD);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COL, 1),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));

        // Header row
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Recent Scans");
        title.setFont(FONT_LABEL);
        title.setForeground(TEXT_PRI);
        JLabel autoLabel = new JLabel("auto-updates on scan");
        autoLabel.setFont(FONT_SMALL);
        autoLabel.setForeground(TEXT_SEC);
        header.add(title,     BorderLayout.WEST);
        header.add(autoLabel, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        // Table
        String[] cols = {"TIME", "ITEM ID", "SOURCE", "PRODUCT", "STATUS"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(tableModel);
        styleTable(table);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBackground(BG_CARD);
        scroll.getViewport().setBackground(BG_CARD);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    // ── Manual entry panel (right side) ───────────────────────────────────
    private JPanel buildManualEntryPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_CARD);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COL, 1),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)));
        panel.setPreferredSize(new Dimension(240, 0));

        JLabel title = new JLabel("Manual Entry");
        title.setFont(FONT_LABEL);
        title.setForeground(TEXT_PRI);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Source toggle buttons (Barcode | RFID)
        JPanel srcToggle = new JPanel(new GridLayout(1, 2, 4, 0));
        srcToggle.setOpaque(false);
        srcToggle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        srcToggle.setAlignmentX(Component.LEFT_ALIGNMENT);

        btnSourceBarcode = new JToggleButton("Barcode");
        btnSourceRFID    = new JToggleButton("RFID");
        styleToggleButton(btnSourceBarcode);
        styleToggleButton(btnSourceRFID);
        btnSourceRFID.setSelected(true);
        btnSourceRFID.setBackground(ACCENT_BLUE);
        btnSourceRFID.setForeground(Color.WHITE);

        ButtonGroup srcGroup = new ButtonGroup();
        srcGroup.add(btnSourceBarcode);
        srcGroup.add(btnSourceRFID);

        btnSourceBarcode.addActionListener(e -> {
            btnSourceBarcode.setBackground(ACCENT_BLUE);
            btnSourceBarcode.setForeground(Color.WHITE);
            btnSourceRFID.setBackground(BG_SURFACE);
            btnSourceRFID.setForeground(TEXT_SEC);
        });
        btnSourceRFID.addActionListener(e -> {
            btnSourceRFID.setBackground(ACCENT_BLUE);
            btnSourceRFID.setForeground(Color.WHITE);
            btnSourceBarcode.setBackground(BG_SURFACE);
            btnSourceBarcode.setForeground(TEXT_SEC);
        });

        srcToggle.add(btnSourceBarcode);
        srcToggle.add(btnSourceRFID);

        // Text input
        txtTagInput = new JTextField();
        txtTagInput.setFont(FONT_BODY);
        txtTagInput.setBackground(BG_SURFACE);
        txtTagInput.setForeground(TEXT_PRI);
        txtTagInput.setCaretColor(TEXT_PRI);
        txtTagInput.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COL, 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        txtTagInput.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        txtTagInput.setAlignmentX(Component.LEFT_ALIGNMENT);
        txtTagInput.putClientProperty("JTextField.placeholderText", "Enter barcode or tag ID...");

        // Submit button
        JButton btnSubmit = new JButton("SUBMIT");
        btnSubmit.setFont(FONT_LABEL);
        btnSubmit.setBackground(ACCENT_BLUE);
        btnSubmit.setForeground(Color.WHITE);
        btnSubmit.setBorderPainted(false);
        btnSubmit.setFocusPainted(false);
        btnSubmit.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnSubmit.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        btnSubmit.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Status label
        lblStatus = new JLabel(" ");
        lblStatus.setFont(FONT_SMALL);
        lblStatus.setForeground(TEXT_SEC);
        lblStatus.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel hint = new JLabel("or scan using connected device");
        hint.setFont(FONT_SMALL);
        hint.setForeground(TEXT_SEC);
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Assemble
        panel.add(title);
        panel.add(Box.createVerticalStrut(12));
        panel.add(srcToggle);
        panel.add(Box.createVerticalStrut(8));
        panel.add(txtTagInput);
        panel.add(Box.createVerticalStrut(8));
        panel.add(btnSubmit);
        panel.add(Box.createVerticalStrut(8));
        panel.add(lblStatus);
        panel.add(Box.createVerticalStrut(4));
        panel.add(hint);
        panel.add(Box.createVerticalGlue());

        // Enter key = submit
        txtTagInput.addActionListener(e -> onSubmitScan());
        btnSubmit.addActionListener(e -> onSubmitScan());

        return panel;
    }

    // ══════════════════════════════════════════════════════════════════════
    // EVENT HANDLERS — GRASP Controller
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Handle scan submission.
     * Delegates to Facade (Structural Pattern).
     * Chain of Responsibility fires inside Facade.
     */
    private void onSubmitScan() {
        String tag = txtTagInput.getText().trim();
        String source = btnSourceRFID.isSelected()
                      ? ScanRecord.SOURCE_RFID
                      : ScanRecord.SOURCE_BARCODE;

        try {
            RFIDSystemFacade.ScanResult result = facade.submitScan(tag, source);
            // SUCCESS
            showStatus("✓ " + result.getProduct().getProductName(), ACCENT_GRN);
            txtTagInput.setText("");
            refreshDashboard();

        } catch (RFIDException.DuplicateRfidScanException e) {
            showStatus("⚠ Duplicate: " + tag, ACCENT_ORG);
            UIDialogHelper.showWarning(this, e);

        } catch (RFIDException.UnknownRfidTagException e) {
            showStatus("⚠ Unknown tag logged", ACCENT_ORG);
            UIDialogHelper.showWarning(this, e);
            refreshDashboard();

        } catch (RFIDException.InvalidTagFormatException e) {
            showStatus("✗ " + e.getMessage(), ACCENT_RED);
            UIDialogHelper.showError(this, e);

        } catch (RFIDException.DatabaseConnectionFailedException e) {
            showStatus("✗ DB Error", ACCENT_RED);
            UIDialogHelper.showFatalError(this, e);

        } catch (RFIDException e) {
            showStatus("✗ " + e.getErrorCode(), ACCENT_RED);
            UIDialogHelper.showError(this, e);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // REFRESH / DATA LOADING
    // ══════════════════════════════════════════════════════════════════════

    public void refreshDashboard() {
        SwingUtilities.invokeLater(() -> {
            loadStatCounts();
            loadRecentScans();
        });
    }

    private void loadStatCounts() {
        try {
            int[] summary = facade.getTodaySummary();
            lblTotal.setText(String.format("%03d", summary[0]));
            lblSuccessful.setText(String.format("%03d", summary[1]));
            lblFailed.setText(String.format("%03d", summary[2]));
        } catch (RFIDException e) {
            lblTotal.setText("ERR");
        }
    }

    private void loadRecentScans() {
        try {
            List<ScanRecord> recents = facade.getRecentScans(8);
            tableModel.setRowCount(0);
            for (ScanRecord r : recents) {
                tableModel.addRow(new Object[]{
                        r.getTimestamp() != null
                            ? r.getTimestamp().toLocalTime().toString().substring(0, 8)
                            : "—",
                        r.getRfidTag(),
                        r.getSource(),
                        r.getProductName(),
                        r.getStatus()
                });
            }
        } catch (RFIDException e) {
            System.err.println("[UI] Failed to load recent scans: " + e.getMessage());
        }
    }

    // ── Auto-refresh timer ────────────────────────────────────────────────
    private void startAutoRefresh() {
        Timer timer = new Timer(5000, e -> refreshDashboard());
        timer.setRepeats(true);
        timer.start();
    }

    // ══════════════════════════════════════════════════════════════════════
    // STYLE HELPERS
    // ══════════════════════════════════════════════════════════════════════
    private void styleTable(JTable table) {
        table.setBackground(BG_CARD);
        table.setForeground(TEXT_PRI);
        table.setFont(FONT_SMALL);
        table.setGridColor(BORDER_COL);
        table.setRowHeight(28);
        table.setShowVerticalLines(false);
        table.setSelectionBackground(ACCENT_BLUE.darker());
        table.setSelectionForeground(Color.WHITE);
        table.getTableHeader().setBackground(BG_SURFACE);
        table.getTableHeader().setForeground(TEXT_SEC);
        table.getTableHeader().setFont(FONT_LABEL);
        table.getTableHeader().setBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COL));

        // STATUS column renderer (coloured text)
        table.getColumnModel().getColumn(4)
             .setCellRenderer(new StatusCellRenderer());
    }

    private void styleToggleButton(JToggleButton btn) {
        btn.setFont(FONT_SMALL);
        btn.setBackground(BG_SURFACE);
        btn.setForeground(TEXT_SEC);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void showStatus(String msg, Color colour) {
        lblStatus.setText(msg);
        lblStatus.setForeground(colour);
    }

    // ── Status column renderer ────────────────────────────────────────────
    private static class StatusCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
            String status = value == null ? "" : value.toString();
            setForeground(switch (status) {
                case "OK"        -> ACCENT_GRN;
                case "Unknown"   -> ACCENT_ORG;
                case "Duplicate" -> ACCENT_BLUE;
                default          -> ACCENT_RED;
            });
            setBackground(isSelected ? ACCENT_BLUE.darker() : BG_CARD);
            setFont(FONT_SMALL);
            return this;
        }
    }
}
