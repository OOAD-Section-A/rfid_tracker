package com.nova.rfid.ui;

import com.nova.rfid.exception.RFIDException;
import com.nova.rfid.model.ScanRecord;
import com.nova.rfid.pattern.structural.RFIDSystemFacade;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.List;

import static com.nova.rfid.ui.ScanDashboardPanel.*;

/**
 * UI LAYER — SCAN LOG PAGE
 * SUBSYSTEM 11: BARCODE READER & RFID TRACKER — Team NOVA
 *
 * Matches wireframe page 2: searchable scan log with pagination
 * and CSV export button.
 *
 * GRASP - Controller: Handles search and export events.
 * SOLID - SRP: Only responsible for displaying scan log history.
 * SEPARATE FILE — safe for UI Subsystem team to modify independently.
 */
public class ScanLogPanel extends JPanel {

    private final RFIDSystemFacade  facade = RFIDSystemFacade.getInstance();
    private       DefaultTableModel tableModel;
    private       JTextField        txtSearch;
    private       JLabel            lblPageInfo;
    private       List<ScanRecord>  allRecords;

    private static final int PAGE_SIZE = 10;
    private int currentPage = 0;

    public ScanLogPanel() {
        setLayout(new BorderLayout());
        setBackground(BG_DARK);
        buildUI();
        loadAllLogs();
    }

    private void buildUI() {
        // ── Top bar ───────────────────────────────────────────────────────
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(BG_SURFACE);
        topBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COL),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)));

        JLabel title = new JLabel("Scan Log");
        title.setFont(FONT_TITLE);
        title.setForeground(TEXT_PRI);
        topBar.add(title, BorderLayout.WEST);
        add(topBar, BorderLayout.NORTH);

        // ── Content ───────────────────────────────────────────────────────
        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setBackground(BG_DARK);
        content.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // ── Search row ────────────────────────────────────────────────────
        JPanel searchRow = new JPanel(new BorderLayout(8, 0));
        searchRow.setOpaque(false);

        txtSearch = new JTextField();
        txtSearch.setFont(FONT_BODY);
        txtSearch.setBackground(BG_CARD);
        txtSearch.setForeground(TEXT_PRI);
        txtSearch.setCaretColor(TEXT_PRI);
        txtSearch.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COL, 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        txtSearch.putClientProperty("JTextField.placeholderText", "Search by Item ID...");

        JButton btnExport = buildButton("Export CSV", ACCENT_BLUE);
        btnExport.setPreferredSize(new Dimension(120, 36));

        searchRow.add(txtSearch,  BorderLayout.CENTER);
        searchRow.add(btnExport,  BorderLayout.EAST);

        txtSearch.addActionListener(e -> onSearch());
        btnExport.addActionListener(e -> onExportCSV());

        // ── Table ─────────────────────────────────────────────────────────
        String[] cols = {"TIMESTAMP", "ITEM ID", "SOURCE", "PRODUCT NAME", "STATUS"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(tableModel);
        styleTable(table);

        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(BG_CARD);
        scroll.setBackground(BG_CARD);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER_COL, 1));

        // ── Pagination row ────────────────────────────────────────────────
        JPanel pageRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        pageRow.setOpaque(false);

        JButton btnPrev = buildButton("‹", BG_CARD);
        lblPageInfo     = new JLabel("1");
        JButton btnNext = buildButton("›", BG_CARD);

        lblPageInfo.setForeground(TEXT_PRI);
        lblPageInfo.setFont(FONT_SMALL);

        btnPrev.addActionListener(e -> { if (currentPage > 0) { currentPage--; renderPage(); } });
        btnNext.addActionListener(e -> {
            if (allRecords != null && (currentPage + 1) * PAGE_SIZE < allRecords.size()) {
                currentPage++;
                renderPage();
            }
        });

        pageRow.add(btnPrev);
        pageRow.add(lblPageInfo);
        pageRow.add(btnNext);

        // Assemble content
        content.add(searchRow, BorderLayout.NORTH);
        content.add(scroll,    BorderLayout.CENTER);
        content.add(pageRow,   BorderLayout.SOUTH);

        add(content, BorderLayout.CENTER);
    }

    // ── Load all records ──────────────────────────────────────────────────
    public void loadAllLogs() {
        try {
            allRecords  = facade.getAllScanLogs();
            currentPage = 0;
            renderPage();
        } catch (RFIDException e) {
            UIDialogHelper.showError(this, e);
        }
    }

    // ── Search ────────────────────────────────────────────────────────────
    private void onSearch() {
        String query = txtSearch.getText().trim();
        try {
            allRecords  = query.isEmpty()
                         ? facade.getAllScanLogs()
                         : facade.searchLogs(query);
            currentPage = 0;
            renderPage();
        } catch (RFIDException e) {
            UIDialogHelper.showError(this, e);
        }
    }

    // ── Export ────────────────────────────────────────────────────────────
    private void onExportCSV() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("rfid_scan_log.csv"));
        int res = chooser.showSaveDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            String path = chooser.getSelectedFile().getAbsolutePath();
            try {
                String saved = facade.exportAllLogsToCSV(path);
                UIDialogHelper.showInfo(this, "Exported to:\n" + saved);
            } catch (RFIDException e) {
                UIDialogHelper.showError(this, e);
            }
        }
    }

    // ── Render current page ───────────────────────────────────────────────
    private void renderPage() {
        tableModel.setRowCount(0);
        if (allRecords == null) return;

        int from  = currentPage * PAGE_SIZE;
        int to    = Math.min(from + PAGE_SIZE, allRecords.size());
        int total = (int) Math.ceil((double) allRecords.size() / PAGE_SIZE);

        for (int i = from; i < to; i++) {
            ScanRecord r = allRecords.get(i);
            tableModel.addRow(new Object[]{
                    r.getFormattedTimestamp(),
                    r.getRfidTag(),
                    r.getSource(),
                    r.getProductName(),
                    r.getStatus()
            });
        }
        lblPageInfo.setText((currentPage + 1) + " / " + Math.max(total, 1));
    }

    // ── Styling ───────────────────────────────────────────────────────────
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
        table.getColumnModel().getColumn(4)
             .setCellRenderer(new StatusCellRenderer());
    }

    private JButton buildButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_SMALL);
        btn.setBackground(bg);
        btn.setForeground(TEXT_PRI);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ── Status renderer ───────────────────────────────────────────────────
    private static class StatusCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int col) {
            super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, col);
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
