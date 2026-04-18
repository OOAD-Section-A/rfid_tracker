package com.nova.rfid.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * MODEL: ScanRecord
 *
 * GRASP - Information Expert: Knows all data about one scan transaction.
 * SOLID - SRP: Only represents a scan entry; no logic.
 *
 * ── INTEGRATION CHANGE ────────────────────────────────────────────
 * The shared OOAD schema uses barcode_rfid_events with:
 *   event_id  VARCHAR(50) — replaces the old INT transaction_id
 *
 * Both fields are kept for backward compatibility:
 *   transactionId (int)  — legacy scm_rfid_db fallback
 *   eventId       (String) — shared OOAD table PK
 *
 * The facade and DB layer set whichever is applicable.
 * UI layers should prefer getEventId() for display.
 * ─────────────────────────────────────────────────────────────────
 *
 * Maps to DB table: barcode_rfid_events (shared) / scan_transactions (legacy)
 * READ by: Subsystem 5 (Reporting), Subsystem 2 (Warehouse)
 * WRITE by: This subsystem (11)
 */
public class ScanRecord {

    public static final String STATUS_OK        = "OK";
    public static final String STATUS_UNKNOWN   = "Unknown";
    public static final String STATUS_DUPLICATE = "Duplicate";
    public static final String STATUS_FAILED    = "Failed";

    public static final String SOURCE_RFID    = "RFID";
    public static final String SOURCE_BARCODE = "Barcode";
    public static final String SOURCE_MANUAL  = "Manual";

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    // ── Legacy PK (scm_rfid_db fallback) ──────────────────────────────────
    private int    transactionId;

    // ── Shared PK (OOAD barcode_rfid_events) ─────────────────────────────
    private String eventId;

    private String        rfidTag;
    private LocalDateTime timestamp;
    private String        status;
    private String        source;
    private String        productName; // resolved from join with products table

    public ScanRecord() {}

    public ScanRecord(String rfidTag, LocalDateTime timestamp,
                      String status, String source) {
        this.rfidTag   = rfidTag;
        this.timestamp = timestamp;
        this.status    = status;
        this.source    = source;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────
    public int           getTransactionId()               { return transactionId; }
    public void          setTransactionId(int id)         { this.transactionId = id; }

    /** Shared-DB event ID (UUID-based string). Use this when on OOAD DB. */
    public String        getEventId()                     { return eventId; }
    public void          setEventId(String id)            { this.eventId = id; }

    /**
     * Returns the best available ID for display:
     * eventId if set (OOAD), else "LEGACY-" + transactionId.
     */
    public String        getDisplayId() {
        if (eventId != null && !eventId.isBlank()) return eventId;
        return transactionId > 0 ? "LEGACY-" + transactionId : "—";
    }

    public String        getRfidTag()                     { return rfidTag; }
    public void          setRfidTag(String t)             { this.rfidTag = t; }
    public LocalDateTime getTimestamp()                   { return timestamp; }
    public void          setTimestamp(LocalDateTime ts)   { this.timestamp = ts; }
    public String        getStatus()                      { return status; }
    public void          setStatus(String s)              { this.status = s; }
    public String        getSource()                      { return source; }
    public void          setSource(String s)              { this.source = s; }
    public String        getProductName()                 { return productName; }
    public void          setProductName(String n)         { this.productName = n; }

    public String getFormattedTimestamp() {
        return timestamp != null ? timestamp.format(FORMATTER) : "—";
    }
}