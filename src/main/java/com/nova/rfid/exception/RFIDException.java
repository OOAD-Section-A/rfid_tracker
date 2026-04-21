package com.nova.rfid.exception;

/**
 * ═══════════════════════════════════════════════════════════════════
 * EXCEPTION CLASSES — SUBSYSTEM 11: BARCODE READER & RFID TRACKER
 * Team NOVA
 *
 * ALIGNED WITH SHARED EXCEPTION LIST (Exception-List-Final.xlsx)
 * RFID/Barcode specific exceptions use IDs 307, 408–413.
 * Cross-cutting DB exceptions use IDs 51–53, 301, 303.
 *
 * SOLID - SRP: Each exception class has one reason to exist.
 * SOLID - OCP: New exception types can be added without changing existing code.
 * GRASP - Protected Variations: Shields rest of system from error-type changes.
 *
 * Separate file so the EXCEPTION HANDLING SUBSYSTEM TEAM (Subsystem 17)
 * can modify, extend, or replace these without touching service or UI code.
 * ═══════════════════════════════════════════════════════════════════
 */
public class RFIDException extends Exception {

    public enum Category { MAJOR, MINOR, WARNING }

    private final Category category;
    private final String   errorCode;
    private final int      exceptionListId; // ID from shared Exception-List-Final.xlsx

    public RFIDException(int exceptionListId, String errorCode,
                         Category category, String message) {
        super(message);
        this.exceptionListId = exceptionListId;
        this.errorCode       = errorCode;
        this.category        = category;
    }

    public RFIDException(int exceptionListId, String errorCode,
                         Category category, String message, Throwable cause) {
        super(message, cause);
        this.exceptionListId = exceptionListId;
        this.errorCode       = errorCode;
        this.category        = category;
    }

    public Category getCategory()       { return category; }
    public String   getErrorCode()      { return errorCode; }
    public int      getExceptionListId(){ return exceptionListId; }

    @Override
    public String toString() {
        return "[" + category + "][ID=" + exceptionListId + "] " + errorCode + ": " + getMessage();
    }

    // ─────────────────────────────────────────────────────────────
    //  #411 — FILE_NOT_FOUND — MAJOR  (Barcode / RFID)
    //  Barcode/RFID data file could not be located.
    //  Handle: Log missing file path; alert data team; halt dependent scan.
    // ─────────────────────────────────────────────────────────────
    public static class FileNotFoundException extends RFIDException {
        public FileNotFoundException(String path) {
            super(411, "FILE_NOT_FOUND", Category.MAJOR,
                  "Barcode/RFID data file could not be located at: " + path
                  + ". Log missing file path; halt dependent scan operation.");
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  #412 — FILE_FORMAT_INVALID — MAJOR  (Barcode / RFID)
    //  Barcode/RFID data file is in an unrecognised or corrupt format.
    //  Handle: Reject file; log format details; alert data team.
    // ─────────────────────────────────────────────────────────────
    public static class FileFormatInvalidException extends RFIDException {
        public FileFormatInvalidException(String filename) {
            super(412, "FILE_FORMAT_INVALID", Category.MAJOR,
                  "Barcode/RFID data file '" + filename
                  + "' is in an unrecognised or corrupt format. Only .csv files are accepted.");
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  No dedicated list ID for EMPTY_FILE — mapped to WARNING
    //  (closest: general WARNING pattern for missing data)
    //  Handle: Warn user, exit gracefully, log event.
    // ─────────────────────────────────────────────────────────────
    public static class EmptyFileException extends RFIDException {
        public EmptyFileException() {
            super(0, "EMPTY_FILE", Category.WARNING,
                  "The CSV file contains no records to process.");
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  #410 — MISSING_RFID_FIELD — MAJOR  (Barcode / RFID)
    //  RFID tag is missing a required data field.
    //  Handle: Reject scan; alert warehouse staff to re-tag item.
    // ─────────────────────────────────────────────────────────────
    public static class MissingRfidFieldException extends RFIDException {
        public MissingRfidFieldException(int rowNumber) {
            super(410, "MISSING_RFID_FIELD", Category.MAJOR,
                  "Row " + rowNumber + " is missing the RFID tag value. "
                  + "Reject scan; alert warehouse staff to re-tag item.");
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  #408 — UNKNOWN_RFID_TAG — WARNING  (Barcode / RFID)
    //  Scanned RFID tag is not registered in the system.
    //  Handle: Log tag ID; quarantine item; alert warehouse staff to investigate.
    // ─────────────────────────────────────────────────────────────
    public static class UnknownRfidTagException extends RFIDException {
        private final String rfidTag;
        public UnknownRfidTagException(String tag) {
            super(408, "UNKNOWN_RFID_TAG", Category.WARNING,
                  "Scanned RFID tag '" + tag + "' is not registered in the system. "
                  + "Item quarantined; warehouse staff alerted.");
            this.rfidTag = tag;
        }
        public String getRfidTag() { return rfidTag; }
    }

    // ─────────────────────────────────────────────────────────────
    //  #409 — DUPLICATE_RFID_SCAN — WARNING  (Barcode / RFID)
    //  Same RFID tag scanned more than once at the same checkpoint.
    //  Handle: Log duplicate; discard second scan; alert staff if recurring.
    // ─────────────────────────────────────────────────────────────
    public static class DuplicateRfidScanException extends RFIDException {
        private final String rfidTag;
        public DuplicateRfidScanException(String tag) {
            super(409, "DUPLICATE_RFID_SCAN", Category.WARNING,
                  "RFID tag '" + tag + "' has already been scanned at this checkpoint. "
                  + "Duplicate discarded; staff alerted.");
            this.rfidTag = tag;
        }
        public String getRfidTag() { return rfidTag; }
    }

    // ─────────────────────────────────────────────────────────────
    //  #307 — BARCODE_DUPLICATE — MINOR  (Database Design / Barcode-RFID)
    //  Duplicate barcode detected in the system records.
    //  Handle: Reject scan; alert warehouse staff to resolve.
    // ─────────────────────────────────────────────────────────────
    public static class BarcodeDuplicateException extends RFIDException {
        public BarcodeDuplicateException(String barcode) {
            super(307, "BARCODE_DUPLICATE", Category.MINOR,
                  "Duplicate barcode '" + barcode + "' detected in system records. "
                  + "Rejected; warehouse staff alerted to resolve.");
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  #51 — DB_CONNECTION_FAILED — MAJOR  (Database Design)
    //  Cannot establish connection to the database.
    //  Handle: Retry with exponential backoff; alert ops; surface degraded UI.
    // ─────────────────────────────────────────────────────────────
    public static class DatabaseConnectionFailedException extends RFIDException {
        public DatabaseConnectionFailedException(String detail) {
            super(51, "DB_CONNECTION_FAILED", Category.MAJOR,
                  "Cannot establish connection to the database. " + detail
                  + " Retry with exponential backoff; alert ops team.");
        }
        public DatabaseConnectionFailedException(String detail, Throwable cause) {
            super(51, "DB_CONNECTION_FAILED", Category.MAJOR,
                  "Cannot establish connection to the database. " + detail, cause);
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  #413 — PRODUCT_LOOKUP_FAILED — MAJOR  (Barcode / RFID)
    //  Scanned barcode/RFID could not be matched to a product record.
    //  Handle: Log tag/barcode; alert warehouse staff; quarantine item.
    // ─────────────────────────────────────────────────────────────
    public static class ProductLookupFailedException extends RFIDException {
        public ProductLookupFailedException(String tag, Throwable cause) {
            super(413, "PRODUCT_LOOKUP_FAILED", Category.MAJOR,
                  "Scanned barcode/RFID '" + tag
                  + "' could not be matched to a product record. "
                  + "Tag logged; warehouse staff alerted; item quarantined.", cause);
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  No dedicated list ID for INVALID_TAG_FORMAT.
    //  Mapped to #410 category (MISSING_RFID_FIELD) but kept separate
    //  for UX validation — inline warning, not a scan failure.
    //  Handle: Show inline validation message, do not submit.
    // ─────────────────────────────────────────────────────────────
    public static class InvalidTagFormatException extends RFIDException {
        public InvalidTagFormatException(String tag) {
            super(410, "INVALID_TAG_FORMAT", Category.MINOR,
                  "Tag value '" + tag + "' is invalid. "
                  + "Please enter a non-empty alphanumeric tag (3–50 chars).");
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  No dedicated list ID — EXPORT_FAILED mapped to MINOR.
    //  Handle: Show error dialog, allow retry.
    // ─────────────────────────────────────────────────────────────
    public static class ExportFailedException extends RFIDException {
        public ExportFailedException(String format, Throwable cause) {
            super(0, "EXPORT_FAILED", Category.MINOR,
                  "Export to " + format + " format failed.", cause);
        }
    }
}
