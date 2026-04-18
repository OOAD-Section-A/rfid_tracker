package com.nova.rfid.pattern.structural;

import com.nova.rfid.db.DatabaseManager;
import com.nova.rfid.exception.RFIDException;
import com.nova.rfid.model.Product;
import com.nova.rfid.model.ScanRecord;
import com.nova.rfid.pattern.behavioral.*;
import com.scm.subsystems.BarcodeRFIDSubsystem;
import inventory_subsystem.AddStockStrategy;
import inventory_subsystem.InventoryExceptionSource;
import inventory_subsystem.InventoryRepository;
import inventory_subsystem.IssuingPolicy;

import java.time.LocalDateTime;
import java.util.*;

/**
 * ═══════════════════════════════════════════════════════════════════
 * STRUCTURAL PATTERN: FAÇADE — RFIDSystemFacade
 *
 * Provides a SINGLE, SIMPLIFIED interface to the entire RFID subsystem:
 *   • Tag validation (Chain of Responsibility)
 *   • Product lookup (DatabaseManager)
 *   • Transaction logging (DatabaseManager write)
 *   • Dashboard summary (DatabaseManager read)
 *
 * OTHER SUBSYSTEMS interact ONLY with this facade, never with internals.
 *   Sub. 2 (WMS)     → calls submitScan() when goods arrive
 *   Sub. 4 (Orders)  → calls submitScan() on dispatch verification
 *   Sub. 5 (Reports) → calls getScanLogs() / getTodaySummary()
 *
 * ── INTEGRATION CHANGE ────────────────────────────────────────────
 * • insertScanRecord() now writes to barcode_rfid_events (shared OOAD).
 *   Returns a String eventId instead of int transactionId.
 * • All caught RFIDExceptions are forwarded to
 *   DatabaseManager.logExceptionToSharedTable() so the DB team's
 *   subsystem_exceptions table stays populated.
 * • isDatabaseConnected() now also reports which DB is active.
 * • Inventory subsystem wired: each successful scan calls addStock()
 *   so stock levels are updated in real time.
 * • SCM Exception Handler wired: all RFID exceptions are reported
 *   to the central exception handler via BarcodeRFIDSubsystem.
 * ─────────────────────────────────────────────────────────────────
 *
 * GRASP - Controller: Central coordinator for all RFID use cases.
 * GRASP - Indirection: Shields callers from subsystem complexity.
 * GRASP - Low Coupling: Sub. 2/4/5 are coupled only to this facade.
 * GRASP - High Cohesion: Facade only coordinates RFID-related concerns.
 * SOLID - SRP: Facade coordinates; it delegates, not implements.
 * SOLID - DIP: Depends on DatabaseManager abstraction, not raw JDBC.
 * ═══════════════════════════════════════════════════════════════════
 */
public class RFIDSystemFacade {

    // ── Singleton Facade instance ─────────────────────────────────────────
    private static volatile RFIDSystemFacade instance;

    // ── Dependencies ──────────────────────────────────────────────────────
    private final DatabaseManager    dbManager;
    private final AbstractTagHandler validationChain;
    private final Set<String>        sessionScannedTags;
    private final List<ScanRecord>   sessionLog;

    // ── Inventory integration ─────────────────────────────────────────────
    private final AddStockStrategy         addStockStrategy;
    private final InventoryRepository      inventoryRepository;
    private final InventoryExceptionSource invExSource;
    private static final String INVENTORY_LOCATION = "RFID-INBOUND";
    private static final String INVENTORY_SUPPLIER = "RFID-SCAN";

    // ── SCM Exception Handler integration ────────────────────────────────
    private final BarcodeRFIDSubsystem exceptions = BarcodeRFIDSubsystem.INSTANCE;

    // ── Private constructor: assembles Chain of Responsibility ────────────
    private RFIDSystemFacade() {
        this.dbManager          = DatabaseManager.getInstance();
        this.sessionScannedTags = new HashSet<>();
        this.sessionLog         = new ArrayList<>();

        // Build validation chain:
        // EmptyTagHandler → FormatValidationHandler → SessionDuplicateHandler
        AbstractTagHandler emptyHandler  = new TagValidationHandlers.EmptyTagHandler();
        AbstractTagHandler formatHandler = new TagValidationHandlers.FormatValidationHandler();
        AbstractTagHandler dupHandler    = new TagValidationHandlers.SessionDuplicateHandler(sessionScannedTags);

        emptyHandler.setNext(formatHandler).setNext(dupHandler);
        this.validationChain = emptyHandler;

        // Inventory subsystem setup — use AddStockStrategy directly to avoid
        // InventoryService which pulls in TransferStockStrategy (has a bug on
        // their end). We only need addStock, so this is sufficient.
        this.invExSource         = new InventoryExceptionSource();
        this.inventoryRepository = new InventoryRepository(invExSource);
        this.addStockStrategy    = new AddStockStrategy();
    }

    public static RFIDSystemFacade getInstance() {
        if (instance == null) {
            synchronized (RFIDSystemFacade.class) {
                if (instance == null) instance = new RFIDSystemFacade();
            }
        }
        return instance;
    }

    // ══════════════════════════════════════════════════════════════════════
    // PRIMARY API — called by UI and other subsystems
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Submit an RFID / barcode scan.
     *
     * Flow:
     *   1. Validate tag through Chain of Responsibility
     *   2. Lookup product in DB (shared products table or legacy)
     *   3. Create ScanRecord (status = OK | Unknown | Failed)
     *   4. Persist to barcode_rfid_events (shared) or scan_transactions (legacy)
     *   5. Log any exception to subsystem_exceptions (shared)
     *   6. Add to in-memory session tracking
     *   7. Notify inventory subsystem (addStock) for successful scans
     *
     * @param rfidTag  tag entered by operator / scanned by device
     * @param source   "RFID" | "Barcode" | "Manual"
     * @return ScanResult wrapping the persisted record and resolved product
     * @throws RFIDException propagated to UI layer for user feedback
     */
    public ScanResult submitScan(String rfidTag, String source) throws RFIDException {
        // Step 1: Chain of Responsibility validation
        // Catches duplicate and format errors before any DB work
        String normalizedTag;
        try {
            normalizedTag = validationChain.handle(rfidTag);
        } catch (RFIDException.DuplicateRfidScanException e) {
            exceptions.onDuplicateRfidScan(rfidTag, "RFID-INBOUND");
            throw e;
        } catch (RFIDException.MissingRfidFieldException e) {
            exceptions.onMissingRfidField(rfidTag, "tag");
            throw e;
        }

        // Step 2: Product lookup
        Product product;
        String  status;
        try {
            product = dbManager.findProductByRfidTag(normalizedTag);
            status  = (product != null) ? ScanRecord.STATUS_OK : ScanRecord.STATUS_UNKNOWN;
        } catch (RFIDException e) {
            status  = ScanRecord.STATUS_FAILED;
            product = null;
            // Report to SCM exception handler
            exceptions.onProductLookupFailed(normalizedTag);
            // Persist the failed record THEN log exception to shared table
            ScanRecord failedRecord = buildRecord(normalizedTag, source, status, null);
            persistRecord(failedRecord);
            reportExceptionToSharedDb(e, normalizedTag);
            throw e;
        }

        // Step 3 & 4: Build and persist
        ScanRecord record = buildRecord(normalizedTag, source, status, product);
        persistRecord(record);

        // Step 5: Track in session (after successful write)
        sessionScannedTags.add(normalizedTag);
        sessionLog.add(record);

        // Step 6: Unknown tag → report to SCM handler, log warning, throw
        if (product == null) {
            exceptions.onUnknownRfidTag(normalizedTag, "RFID-INBOUND");
            RFIDException.UnknownRfidTagException unknownEx =
                    new RFIDException.UnknownRfidTagException(normalizedTag);
            reportExceptionToSharedDb(unknownEx, normalizedTag);
            throw unknownEx;
        }

        // Step 7: Notify inventory — each scan = one stock transaction
        if (product.getSku() != null && !product.getSku().isBlank()) {
            addStockStrategy.execute(
                    product.getSku(), INVENTORY_LOCATION, INVENTORY_SUPPLIER, 1,
                    inventoryRepository, invExSource, IssuingPolicy.FIFO
            );
        }

        return new ScanResult(record, product);
    }

    /**
     * Get today's scan summary: [totalScans, successful, failed].
     * Used by ScanDashboard.
     */
    public int[] getTodaySummary() throws RFIDException {
        return dbManager.getTodaySummary();
    }

    /**
     * Get recent scans for the live feed table.
     */
    public List<ScanRecord> getRecentScans(int limit) throws RFIDException {
        return dbManager.getRecentScans(limit);
    }

    /**
     * Get all scan records (for Scan Log page).
     */
    public List<ScanRecord> getAllScanLogs() throws RFIDException {
        return dbManager.getAllScanRecords();
    }

    /**
     * Search scan logs by RFID tag fragment.
     */
    public List<ScanRecord> searchLogs(String query) throws RFIDException {
        return dbManager.searchScanRecords(query);
    }

    /**
     * Export current session log using the Adapter pattern.
     */
    public String exportSessionToCSV(String filePath) throws RFIDException {
        try {
            CSVExportAdapter adapter = new CSVExportAdapter(sessionLog);
            return adapter.exportTo(filePath);
        } catch (RFIDException.FileNotFoundException e) {
            exceptions.onFileNotFound(filePath);
            throw e;
        } catch (RFIDException.FileFormatInvalidException e) {
            exceptions.onFileFormatInvalid(filePath, "CSV");
            throw e;
        }
    }

    /**
     * Export all DB scan events to CSV.
     */
    public String exportAllLogsToCSV(String filePath) throws RFIDException {
        try {
            List<ScanRecord> all = dbManager.getAllScanRecords();
            CSVExportAdapter adapter = new CSVExportAdapter(all);
            return adapter.exportTo(filePath);
        } catch (RFIDException.FileNotFoundException e) {
            exceptions.onFileNotFound(filePath);
            throw e;
        } catch (RFIDException.FileFormatInvalidException e) {
            exceptions.onFileFormatInvalid(filePath, "CSV");
            throw e;
        }
    }

    /**
     * Reset session (clears duplicate-scan memory and session log).
     */
    public void resetSession() {
        sessionScannedTags.clear();
        sessionLog.clear();
        System.out.println("[Facade] Session reset.");
    }

    /**
     * Returns true if DB connection is live.
     * The status label in MainAppWindow calls this.
     */
    public boolean isDatabaseConnected() {
        return dbManager.isConnected();
    }

    /**
     * Returns a human-readable connection label for the UI status bar.
     * e.g. "OOAD (shared)" or "scm_rfid_db (local)" or "Offline"
     */
    public String getDatabaseLabel() {
        if (!dbManager.isConnected()) return "Offline";
        String url = dbManager.getActiveDbUrl();
        if (url.contains("OOAD"))       return "OOAD (shared)";
        if (url.contains("scm_rfid"))   return "scm_rfid_db (local)";
        return "Connected";
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private ScanRecord buildRecord(String tag, String source,
                                   String status, Product product) {
        ScanRecord record = new ScanRecord(tag, LocalDateTime.now(), status, source);
        record.setProductName(product != null ? product.getProductName() : "—");
        return record;
    }

    /**
     * Calls DatabaseManager.insertScanRecord() and sets the returned eventId
     * on the record. Swallows persistence failures gracefully (logs to stderr)
     * so a DB write error never crashes the scan flow.
     */
    private void persistRecord(ScanRecord record) {
        try {
            String eventId = dbManager.insertScanRecord(record);
            record.setEventId(eventId);
            sessionLog.add(record);
        } catch (RFIDException e) {
            System.err.println("[Facade] Warning: scan record persistence failed: " + e.getMessage());
            reportExceptionToSharedDb(e, record.getRfidTag());
        }
    }

    /**
     * Sends exception details to the shared subsystem_exceptions table.
     * Never throws — exception logging must not interrupt the main flow.
     *
     * Only MAJOR and MINOR category exceptions are written; WARNING
     * category (e.g. duplicate, unknown tag) are still logged but at
     * lower severity so the DB team can filter appropriately.
     */
    private void reportExceptionToSharedDb(RFIDException ex, String referenceId) {
        try {
            dbManager.logExceptionToSharedTable(ex, referenceId);
        } catch (Exception ignored) {
            // logExceptionToSharedTable already swallows internally,
            // but guard here too for absolute safety.
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Inner Result DTO
    // ══════════════════════════════════════════════════════════════════════
    public static class ScanResult {
        private final ScanRecord record;
        private final Product    product;

        public ScanResult(ScanRecord record, Product product) {
            this.record  = record;
            this.product = product;
        }

        public ScanRecord getRecord()  { return record; }
        public Product    getProduct() { return product; }
    }
}