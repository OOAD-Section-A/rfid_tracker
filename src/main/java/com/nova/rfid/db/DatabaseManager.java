package com.nova.rfid.db;

import com.nova.rfid.exception.RFIDException;
import com.nova.rfid.model.Product;
import com.nova.rfid.model.ScanRecord;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import com.jackfruit.scm.database.facade.SupplyChainDatabaseFacade;
import com.jackfruit.scm.database.adapter.BarcodeTrackingAdapter;
import com.jackfruit.scm.database.model.BarcodeRfidEvent;
import com.scm.subsystems.BarcodeRFIDSubsystem;

/**
 * ═══════════════════════════════════════════════════════════════════
 * DATABASE LAYER — SUBSYSTEM 11: BARCODE READER & RFID TRACKER
 * Team NOVA
 *
 * DESIGN PATTERN — CREATIONAL: SINGLETON
 *   Ensures only ONE database connection manager exists across the
 *   entire application.
 *
 * ── CONFIGURATION ─────────────────────────────────────────────────
 *
 * Credentials are loaded from database.properties on the classpath.
 * Copy database.properties.template → database.properties and fill
 * in your local MySQL details. Never commit database.properties.
 *
 * ── INTEGRATION WITH SHARED OOAD DATABASE ─────────────────────────
 *
 * Tables owned by OTHER subsystems that we READ:
 *   products          → Inventory subsystem
 *                        columns: product_id VARCHAR(50), product_name,
 *                                 category, sku, sub_category, supplier_id,
 *                                 unit_of_measure, zone
 *
 * Tables we WRITE (owned by Subsystem 11):
 *   barcode_rfid_events
 *                        columns: event_id, product_id, rfid_tag,
 *                                 product_name, category, description,
 *                                 transaction_id, warehouse_id,
 *                                 event_timestamp, status, source
 *
 *   subsystem_exceptions (shared — we INSERT, DB team owns schema)
 *                        columns: exception_id, exception_name,
 *                                 subsystem_name, severity, timestamp_utc,
 *                                 duration_ms, exception_message, error_code,
 *                                 stack_trace, inner_exception, user_account,
 *                                 handling_plan, retry_count, status,
 *                                 resolved_at
 * ═══════════════════════════════════════════════════════════════════
 */
public class DatabaseManager {

    // ── Singleton ─────────────────────────────────────────────────────────
    private static volatile DatabaseManager instance;
    private Connection connection;

    // ── Connection config — loaded from database.properties ───────────────
    private static final String DB_DRIVER = "com.mysql.cj.jdbc.Driver";

    private String DB_URL;
    private String DB_URL_FALLBACK;
    private String DB_USER;
    private String DB_PASSWORD;

    /** Which DB we are currently connected to */
    private String activeDbUrl;

    // ── DB module JAR adapter fields ──────────────────────────────────────
    private SupplyChainDatabaseFacade scmFacade;
    private BarcodeTrackingAdapter    barcodeAdapter;

    // ── SCM Exception Handler integration ────────────────────────────────
    private final BarcodeRFIDSubsystem exceptions = BarcodeRFIDSubsystem.INSTANCE;

    private DatabaseManager() {
        // Load credentials from database.properties — never hardcode these
        Properties props = new Properties();
        try (InputStream in = DatabaseManager.class
                .getClassLoader().getResourceAsStream("database.properties")) {
            if (in == null) {
                System.err.println("[DB] database.properties not found on classpath.");
                System.err.println("[DB] Copy database.properties.template → database.properties and fill in your credentials.");
                return;
            }
            props.load(in);
        } catch (IOException e) {
            System.err.println("[DB] Failed to load database.properties: " + e.getMessage());
            return;
        }

        DB_URL          = props.getProperty("db.url");
        DB_URL_FALLBACK = props.getProperty("db.url.fallback");
        DB_USER         = props.getProperty("db.user");
        DB_PASSWORD     = props.getProperty("db.password");
        activeDbUrl     = DB_URL;

        try {
            Class.forName(DB_DRIVER);
            connect();
        } catch (ClassNotFoundException e) {
            System.err.println("[DB] MySQL JDBC driver not found: " + e.getMessage());
        } catch (RFIDException e) {
            System.err.println("[DB] Initial connection failed: " + e.getMessage());
        }
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            synchronized (DatabaseManager.class) {
                if (instance == null) instance = new DatabaseManager();
            }
        }
        return instance;
    }

    // ── Connect — tries primary DB first, falls back to scm_rfid_db ───────
    private void connect() throws RFIDException {
        try {
            if (connection == null || connection.isClosed()) {
                try {
                    connection  = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                    activeDbUrl = DB_URL;
                    System.out.println("[DB] Connected to shared OOAD database.");

                    // Initialize the DB module JAR adapter
                    try {
                        scmFacade      = new SupplyChainDatabaseFacade();
                        barcodeAdapter = new BarcodeTrackingAdapter(scmFacade);
                        System.out.println("[DB] DB module adapter ready.");
                    } catch (Exception adapterEx) {
                        System.err.println("[DB] DB module adapter failed to init: " + adapterEx.getMessage());
                        scmFacade      = null;
                        barcodeAdapter = null;
                    }

                } catch (SQLException primaryFail) {
                    System.err.println("[DB] Primary DB unavailable, trying fallback: "
                                       + primaryFail.getMessage());
                    connection  = DriverManager.getConnection(DB_URL_FALLBACK, DB_USER, DB_PASSWORD);
                    activeDbUrl = DB_URL_FALLBACK;
                    System.out.println("[DB] Connected to fallback scm_rfid_db database.");
                }
            }
        } catch (SQLException e) {
            throw new RFIDException.DatabaseConnectionFailedException(e.getMessage(), e);
        }
    }

    public boolean isConnected() {
        try { return connection != null && !connection.isClosed(); }
        catch (SQLException e) { return false; }
    }

    public String getActiveDbUrl() { return activeDbUrl; }

    // ══════════════════════════════════════════════════════════════════════
    // PRODUCT OPERATIONS
    // READ from shared `products` table (Inventory subsystem owner)
    // Shared schema: product_id VARCHAR(50), product_name, category, sku
    // Legacy schema: product_id INT, rfid_tag, product_name, category, description
    // ══════════════════════════════════════════════════════════════════════

    public Product findProductByRfidTag(String rfidTag) throws RFIDException {
        ensureConnected();
        if (activeDbUrl != null && activeDbUrl.equals(DB_URL)) {
            return findProductInSharedDb(rfidTag);
        } else {
            return findProductInLegacyDb(rfidTag);
        }
    }

    private Product findProductInSharedDb(String rfidTag) throws RFIDException {
        String sql = "SELECT product_id, product_name, category, sku " +
                     "FROM products WHERE product_id = ? OR sku = ? LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, rfidTag);
            ps.setString(2, rfidTag);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Product.Builder()
                        .productId(rs.getString("product_id"))
                        .rfidTag(rfidTag)
                        .productName(rs.getString("product_name"))
                        .category(rs.getString("category"))
                        .sku(rs.getString("sku"))
                        .description("")
                        .build();
            }
            return null;
        } catch (SQLException e) {
            exceptions.onProductLookupFailed(rfidTag);
            throw new RFIDException.ProductLookupFailedException(rfidTag, e);
        }
    }

    private Product findProductInLegacyDb(String rfidTag) throws RFIDException {
        String sql = "SELECT product_id, rfid_tag, product_name, category, description " +
                     "FROM products WHERE rfid_tag = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, rfidTag);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Product.Builder()
                        .productId(String.valueOf(rs.getInt("product_id")))
                        .rfidTag(rs.getString("rfid_tag"))
                        .productName(rs.getString("product_name"))
                        .category(rs.getString("category"))
                        .sku("")
                        .description(rs.getString("description"))
                        .build();
            }
            return null;
        } catch (SQLException e) {
            exceptions.onProductLookupFailed(rfidTag);
            throw new RFIDException.ProductLookupFailedException(rfidTag, e);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // SCAN EVENT OPERATIONS
    // ══════════════════════════════════════════════════════════════════════

    public String insertScanRecord(ScanRecord record) throws RFIDException {
        ensureConnected();
        if (activeDbUrl != null && activeDbUrl.equals(DB_URL)) {
            return insertToSharedEventsTable(record);
        } else {
            int legacyId = insertToLegacyScanTransactions(record);
            return "LEGACY-" + legacyId;
        }
    }

    private String insertToSharedEventsTable(ScanRecord record) throws RFIDException {
        String eventId = "EVT-" + UUID.randomUUID().toString().replace("-", "")
                                       .substring(0, 12).toUpperCase();

        String sql = "INSERT INTO barcode_rfid_events " +
                     "(event_id, product_id, rfid_tag, product_name, category, " +
                     " description, transaction_id, warehouse_id, event_timestamp, " +
                     " status, source) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, NULL, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, eventId);
            ps.setString(2, record.getRfidTag());
            ps.setString(3, record.getRfidTag());
            ps.setString(4, record.getProductName() != null ? record.getProductName() : "");
            ps.setString(5, "");
            ps.setString(6, "");
            ps.setString(7, record.getDisplayId());
            ps.setTimestamp(8, Timestamp.valueOf(record.getTimestamp()));
            ps.setString(9,  record.getStatus());
            ps.setString(10, record.getSource());
            ps.executeUpdate();
            System.out.println("[DB] Scan event inserted: " + eventId);

            // Notify the DB module JAR so its event system fires
            if (barcodeAdapter != null) {
                try {
                    BarcodeRfidEvent evt = new BarcodeRfidEvent(
                        eventId,
                        record.getRfidTag(),
                        record.getRfidTag(),
                        record.getProductName() != null ? record.getProductName() : "",
                        "",
                        "",
                        record.getDisplayId(),
                        record.getTimestamp(),
                        record.getStatus(),
                        record.getSource()
                    );
                    barcodeAdapter.recordBarcodeEvent(evt);
                } catch (Exception notifyEx) {
                    System.err.println("[DB] Adapter notify failed (insert still succeeded): "
                                       + notifyEx.getMessage());
                }
            }

            return eventId;
        } catch (SQLException e) {
            throw new RFIDException.DatabaseConnectionFailedException(
                    "Failed to insert barcode_rfid_events row: " + e.getMessage(), e);
        }
    }

    private int insertToLegacyScanTransactions(ScanRecord record) throws RFIDException {
        String sql = "INSERT INTO scan_transactions (rfid_tag, timestamp, status, source) " +
                     "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, record.getRfidTag());
            ps.setTimestamp(2, Timestamp.valueOf(record.getTimestamp()));
            ps.setString(3, record.getStatus());
            ps.setString(4, record.getSource());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            return keys.next() ? keys.getInt(1) : -1;
        } catch (SQLException e) {
            throw new RFIDException.DatabaseConnectionFailedException(
                    "Failed to insert scan_transactions row (legacy): " + e.getMessage(), e);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // EXCEPTION LOGGING
    // ══════════════════════════════════════════════════════════════════════

    public void logExceptionToSharedTable(RFIDException ex, String referenceId) {
        if (!isConnected() || activeDbUrl == null || !activeDbUrl.equals(DB_URL)) return;

        String exceptionId = "EX-RFID-" + UUID.randomUUID().toString()
                              .replace("-", "").substring(0, 10).toUpperCase();

        String sql = "INSERT INTO subsystem_exceptions " +
                     "(exception_id, exception_name, subsystem_name, severity, " +
                     " timestamp_utc, duration_ms, exception_message, error_code, " +
                     " stack_trace, inner_exception, user_account, handling_plan, " +
                     " retry_count, status, resolved_at) " +
                     "VALUES (?, ?, ?, ?, NOW(), NULL, ?, ?, ?, ?, ?, ?, 0, 'OPEN', NULL)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, exceptionId);
            ps.setString(2, ex.getErrorCode());
            ps.setString(3, "SUBSYSTEM_11_RFID_BARCODE");
            ps.setString(4, ex.getCategory().name());
            String msg = ex.getErrorCode() + ": " + ex.getMessage();
            ps.setString(5, msg.length() > 490 ? msg.substring(0, 490) : msg);
            ps.setLong(6, ex.getExceptionListId());
            ps.setString(7, getStackTraceString(ex));
            ps.setString(8, ex.getCause() != null ? ex.getCause().getMessage() : null);
            ps.setString(9,  referenceId);
            ps.setString(10, "See RFID subsystem handling plan for error code "
                    + ex.getExceptionListId());
            ps.executeUpdate();
            System.out.println("[DB] Exception logged to subsystem_exceptions: " + exceptionId);
        } catch (SQLException ignored) {
            System.err.println("[DB] Could not write to subsystem_exceptions: "
                               + ignored.getMessage());
        }
    }

    private String getStackTraceString(Exception ex) {
        if (ex == null) return null;
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement el : ex.getStackTrace()) {
            sb.append(el.toString()).append("\n");
            if (sb.length() > 2000) { sb.append("...truncated"); break; }
        }
        return sb.toString();
    }

    // ══════════════════════════════════════════════════════════════════════
    // READ OPERATIONS — Dashboard & Log
    // ══════════════════════════════════════════════════════════════════════

    public List<ScanRecord> getAllScanRecords() throws RFIDException {
        ensureConnected();
        if (activeDbUrl != null && activeDbUrl.equals(DB_URL)) {
            return fetchFromSharedEvents(null, 0);
        } else {
            String sql = "SELECT t.transaction_id, t.rfid_tag, t.timestamp, t.status, t.source, " +
                         "COALESCE(p.product_name, '—') AS product_name " +
                         "FROM scan_transactions t " +
                         "LEFT JOIN products p ON t.rfid_tag = p.rfid_tag " +
                         "ORDER BY t.timestamp DESC";
            return executeRecordQuery(sql);
        }
    }

    public List<ScanRecord> searchScanRecords(String tagFilter) throws RFIDException {
        ensureConnected();
        if (activeDbUrl != null && activeDbUrl.equals(DB_URL)) {
            return fetchFromSharedEvents(tagFilter, 0);
        } else {
            String sql = "SELECT t.transaction_id, t.rfid_tag, t.timestamp, t.status, t.source, " +
                         "COALESCE(p.product_name, '—') AS product_name " +
                         "FROM scan_transactions t " +
                         "LEFT JOIN products p ON t.rfid_tag = p.rfid_tag " +
                         "WHERE t.rfid_tag LIKE ? ORDER BY t.timestamp DESC";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, "%" + tagFilter + "%");
                return mapResultSetLegacy(ps.executeQuery());
            } catch (SQLException e) {
                throw new RFIDException.DatabaseConnectionFailedException(
                        "Search failed: " + e.getMessage(), e);
            }
        }
    }

    public int[] getTodaySummary() throws RFIDException {
        ensureConnected();
        String sql;
        if (activeDbUrl != null && activeDbUrl.equals(DB_URL)) {
            sql = "SELECT COUNT(*) AS total, " +
                  "SUM(CASE WHEN status='OK' THEN 1 ELSE 0 END) AS successful, " +
                  "SUM(CASE WHEN status IN ('Unknown','Failed','Duplicate') THEN 1 ELSE 0 END) AS failed " +
                  "FROM barcode_rfid_events WHERE DATE(event_timestamp) = CURDATE()";
        } else {
            sql = "SELECT COUNT(*) AS total, " +
                  "SUM(CASE WHEN status='OK' THEN 1 ELSE 0 END) AS successful, " +
                  "SUM(CASE WHEN status IN ('Unknown','Failed','Duplicate') THEN 1 ELSE 0 END) AS failed " +
                  "FROM scan_transactions WHERE DATE(timestamp) = CURDATE()";
        }
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                return new int[]{ rs.getInt("total"),
                                  rs.getInt("successful"),
                                  rs.getInt("failed") };
            }
        } catch (SQLException e) {
            throw new RFIDException.DatabaseConnectionFailedException(
                    "Summary query failed: " + e.getMessage(), e);
        }
        return new int[]{0, 0, 0};
    }

    public List<ScanRecord> getRecentScans(int limit) throws RFIDException {
        ensureConnected();
        if (activeDbUrl != null && activeDbUrl.equals(DB_URL)) {
            return fetchFromSharedEvents(null, limit);
        } else {
            String sql = "SELECT t.transaction_id, t.rfid_tag, t.timestamp, t.status, t.source, " +
                         "COALESCE(p.product_name, '—') AS product_name " +
                         "FROM scan_transactions t " +
                         "LEFT JOIN products p ON t.rfid_tag = p.rfid_tag " +
                         "ORDER BY t.timestamp DESC LIMIT " + limit;
            return executeRecordQuery(sql);
        }
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private List<ScanRecord> fetchFromSharedEvents(String tagFilter, int limit)
            throws RFIDException {
        StringBuilder sb = new StringBuilder(
            "SELECT e.event_id, e.rfid_tag, e.event_timestamp AS ts, " +
            "       e.status, e.source, " +
            "       COALESCE(e.product_name, p.product_name, '—') AS display_name " +
            "FROM barcode_rfid_events e " +
            "LEFT JOIN products p ON e.product_id = p.product_id " +
            "WHERE 1=1 ");
        if (tagFilter != null && !tagFilter.isBlank()) {
            sb.append("AND (e.rfid_tag LIKE ? OR e.product_id LIKE ?) ");
        }
        sb.append("ORDER BY e.event_timestamp DESC");
        if (limit > 0) sb.append(" LIMIT ").append(limit);

        try (PreparedStatement ps = connection.prepareStatement(sb.toString())) {
            if (tagFilter != null && !tagFilter.isBlank()) {
                ps.setString(1, "%" + tagFilter + "%");
                ps.setString(2, "%" + tagFilter + "%");
            }
            return mapResultSetShared(ps.executeQuery());
        } catch (SQLException e) {
            throw new RFIDException.DatabaseConnectionFailedException(
                    "Shared events query failed: " + e.getMessage(), e);
        }
    }

    private List<ScanRecord> mapResultSetShared(ResultSet rs) throws SQLException {
        List<ScanRecord> list = new ArrayList<>();
        while (rs.next()) {
            ScanRecord rec = new ScanRecord();
            rec.setEventId(rs.getString("event_id"));
            rec.setRfidTag(rs.getString("rfid_tag"));
            Timestamp ts = rs.getTimestamp("ts");
            rec.setTimestamp(ts != null ? ts.toLocalDateTime() : LocalDateTime.now());
            rec.setStatus(rs.getString("status"));
            rec.setSource(rs.getString("source"));
            rec.setProductName(rs.getString("display_name"));
            list.add(rec);
        }
        return list;
    }

    private List<ScanRecord> executeRecordQuery(String sql) throws RFIDException {
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return mapResultSetLegacy(rs);
        } catch (SQLException e) {
            throw new RFIDException.DatabaseConnectionFailedException(
                    "Query failed: " + e.getMessage(), e);
        }
    }

    private List<ScanRecord> mapResultSetLegacy(ResultSet rs) throws SQLException {
        List<ScanRecord> list = new ArrayList<>();
        while (rs.next()) {
            ScanRecord rec = new ScanRecord();
            rec.setEventId("LEGACY-" + rs.getInt("transaction_id"));
            rec.setRfidTag(rs.getString("rfid_tag"));
            Timestamp ts = rs.getTimestamp("timestamp");
            rec.setTimestamp(ts != null ? ts.toLocalDateTime() : LocalDateTime.now());
            rec.setStatus(rs.getString("status"));
            rec.setSource(rs.getString("source"));
            rec.setProductName(rs.getString("product_name"));
            list.add(rec);
        }
        return list;
    }

    private void ensureConnected() throws RFIDException {
        if (!isConnected()) connect();
    }

    public void close() {
        try {
            if (scmFacade != null) scmFacade.close();
        } catch (Exception ignored) {}

        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("[DB] Connection closed.");
            }
        } catch (SQLException ignored) {}
    }
}