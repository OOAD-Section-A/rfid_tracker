package com.nova.rfid.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ScanRecord — status constants, getDisplayId, and timestamp formatting.
 */
class ScanRecordTest {

    @Test
    void statusConstants_haveExpectedValues() {
        assertEquals("OK",        ScanRecord.STATUS_OK);
        assertEquals("Unknown",   ScanRecord.STATUS_UNKNOWN);
        assertEquals("Duplicate", ScanRecord.STATUS_DUPLICATE);
        assertEquals("Failed",    ScanRecord.STATUS_FAILED);
    }

    @Test
    void sourceConstants_haveExpectedValues() {
        assertEquals("RFID",    ScanRecord.SOURCE_RFID);
        assertEquals("Barcode", ScanRecord.SOURCE_BARCODE);
        assertEquals("Manual",  ScanRecord.SOURCE_MANUAL);
    }

    @Test
    void constructorWithArgs_setsFields() {
        LocalDateTime now = LocalDateTime.now();
        ScanRecord r = new ScanRecord("TAG-1", now, ScanRecord.STATUS_OK, ScanRecord.SOURCE_RFID);

        assertEquals("TAG-1",            r.getRfidTag());
        assertEquals(now,                r.getTimestamp());
        assertEquals(ScanRecord.STATUS_OK, r.getStatus());
        assertEquals(ScanRecord.SOURCE_RFID, r.getSource());
    }

    @Test
    void getDisplayId_prefersEventId() {
        ScanRecord r = new ScanRecord();
        r.setTransactionId(5);
        r.setEventId("EVT-UUID-001");

        assertEquals("EVT-UUID-001", r.getDisplayId());
    }

    @Test
    void getDisplayId_fallsBackToLegacyId() {
        ScanRecord r = new ScanRecord();
        r.setTransactionId(7);
        // eventId not set

        assertEquals("LEGACY-7", r.getDisplayId());
    }

    @Test
    void getDisplayId_returnsDashWhenNeitherSet() {
        ScanRecord r = new ScanRecord();
        // neither transactionId nor eventId set

        assertEquals("—", r.getDisplayId());
    }

    @Test
    void getFormattedTimestamp_returnsFormattedString() {
        LocalDateTime dt = LocalDateTime.of(2025, 6, 15, 9, 5, 3);
        ScanRecord r = new ScanRecord();
        r.setTimestamp(dt);

        assertEquals("15-06-2025 09:05:03", r.getFormattedTimestamp());
    }

    @Test
    void getFormattedTimestamp_returnsDashWhenNull() {
        ScanRecord r = new ScanRecord();
        assertEquals("—", r.getFormattedTimestamp());
    }

    @Test
    void setters_roundTrip() {
        ScanRecord r = new ScanRecord();
        r.setRfidTag("TAG-99");
        r.setStatus(ScanRecord.STATUS_FAILED);
        r.setSource(ScanRecord.SOURCE_BARCODE);
        r.setProductName("Test Product");
        r.setProductId("P-007");

        assertEquals("TAG-99",                r.getRfidTag());
        assertEquals(ScanRecord.STATUS_FAILED, r.getStatus());
        assertEquals(ScanRecord.SOURCE_BARCODE, r.getSource());
        assertEquals("Test Product",           r.getProductName());
        assertEquals("P-007",                  r.getProductId());
    }
}
