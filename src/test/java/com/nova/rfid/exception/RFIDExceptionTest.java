package com.nova.rfid.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RFIDException and its inner exception subclasses.
 * Verifies error codes, categories, IDs, and message content.
 */
class RFIDExceptionTest {

    // ── FileNotFoundException ────────────────────────────────────────────

    @Test
    void fileNotFound_hasCorrectCodeAndCategory() {
        RFIDException ex = new RFIDException.FileNotFoundException("/data/scan.csv");

        assertEquals("FILE_NOT_FOUND",                ex.getErrorCode());
        assertEquals(RFIDException.Category.MAJOR,    ex.getCategory());
        assertEquals(411,                             ex.getExceptionListId());
        assertTrue(ex.getMessage().contains("/data/scan.csv"));
    }

    // ── FileFormatInvalidException ───────────────────────────────────────

    @Test
    void fileFormatInvalid_hasCorrectCodeAndCategory() {
        RFIDException ex = new RFIDException.FileFormatInvalidException("scan.xlsx");

        assertEquals("FILE_FORMAT_INVALID",           ex.getErrorCode());
        assertEquals(RFIDException.Category.MAJOR,    ex.getCategory());
        assertEquals(412,                             ex.getExceptionListId());
        assertTrue(ex.getMessage().contains("scan.xlsx"));
    }

    // ── EmptyFileException ───────────────────────────────────────────────

    @Test
    void emptyFile_isWarning() {
        RFIDException ex = new RFIDException.EmptyFileException();

        assertEquals("EMPTY_FILE",                    ex.getErrorCode());
        assertEquals(RFIDException.Category.WARNING,  ex.getCategory());
    }

    // ── MissingRfidFieldException ────────────────────────────────────────

    @Test
    void missingRfidField_includesRowNumber() {
        RFIDException ex = new RFIDException.MissingRfidFieldException(5);

        assertEquals("MISSING_RFID_FIELD",            ex.getErrorCode());
        assertEquals(RFIDException.Category.MAJOR,    ex.getCategory());
        assertEquals(410,                             ex.getExceptionListId());
        assertTrue(ex.getMessage().contains("5"));
    }

    // ── UnknownRfidTagException ──────────────────────────────────────────

    @Test
    void unknownRfidTag_exposesTagAndIsWarning() {
        RFIDException.UnknownRfidTagException ex =
                new RFIDException.UnknownRfidTagException("TAG-XYZ");

        assertEquals("TAG-XYZ",                       ex.getRfidTag());
        assertEquals("UNKNOWN_RFID_TAG",              ex.getErrorCode());
        assertEquals(RFIDException.Category.WARNING,  ex.getCategory());
        assertEquals(408,                             ex.getExceptionListId());
        assertTrue(ex.getMessage().contains("TAG-XYZ"));
    }

    // ── DuplicateRfidScanException ───────────────────────────────────────

    @Test
    void duplicateRfidScan_exposesTagAndIsWarning() {
        RFIDException.DuplicateRfidScanException ex =
                new RFIDException.DuplicateRfidScanException("TAG-DUP");

        assertEquals("TAG-DUP",                       ex.getRfidTag());
        assertEquals("DUPLICATE_RFID_SCAN",           ex.getErrorCode());
        assertEquals(RFIDException.Category.WARNING,  ex.getCategory());
        assertEquals(409,                             ex.getExceptionListId());
    }

    // ── InvalidTagFormatException ────────────────────────────────────────

    @Test
    void invalidTagFormat_isMajorAndContainsTag() {
        RFIDException ex = new RFIDException.InvalidTagFormatException("!bad!");

        assertEquals("INVALID_TAG_FORMAT",            ex.getErrorCode());
        assertEquals(RFIDException.Category.MINOR,    ex.getCategory());
        assertTrue(ex.getMessage().contains("!bad!"));
    }

    // ── BarcodeDuplicateException ────────────────────────────────────────

    @Test
    void barcodeDuplicate_isMinor() {
        RFIDException ex = new RFIDException.BarcodeDuplicateException("BC-123");

        assertEquals("BARCODE_DUPLICATE",             ex.getErrorCode());
        assertEquals(RFIDException.Category.MINOR,    ex.getCategory());
        assertEquals(307,                             ex.getExceptionListId());
        assertTrue(ex.getMessage().contains("BC-123"));
    }

    // ── ExportFailedException ────────────────────────────────────────────

    @Test
    void exportFailed_wrapsOriginalCause() {
        Throwable cause = new RuntimeException("disk full");
        RFIDException ex = new RFIDException.ExportFailedException("CSV", cause);

        assertEquals("EXPORT_FAILED",                 ex.getErrorCode());
        assertEquals(RFIDException.Category.MINOR,    ex.getCategory());
        assertSame(cause,                             ex.getCause());
        assertTrue(ex.getMessage().contains("CSV"));
    }

    // ── toString ────────────────────────────────────────────────────────

    @Test
    void toString_includesCategoryCodeAndMessage() {
        RFIDException ex = new RFIDException.FileNotFoundException("/path");
        String s = ex.toString();

        assertTrue(s.contains("MAJOR"));
        assertTrue(s.contains("FILE_NOT_FOUND"));
    }
}
