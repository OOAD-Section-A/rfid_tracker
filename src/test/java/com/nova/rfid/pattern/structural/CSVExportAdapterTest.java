package com.nova.rfid.pattern.structural;

import com.nova.rfid.exception.RFIDException;
import com.nova.rfid.model.ScanRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CSVExportAdapter.
 * Uses JUnit 5's @TempDir to write real files without cleanup boilerplate.
 */
class CSVExportAdapterTest {

    @TempDir
    Path tempDir;

    private ScanRecord makeRecord(String tag, String status, String source, String product) {
        ScanRecord r = new ScanRecord(tag, LocalDateTime.of(2025, 1, 15, 10, 30, 0), status, source);
        r.setProductName(product);
        return r;
    }

    @Test
    void exportTo_createsFileWithHeader() throws RFIDException, IOException {
        List<ScanRecord> records = Collections.singletonList(
                makeRecord("TAG-001", ScanRecord.STATUS_OK, ScanRecord.SOURCE_RFID, "Widget A")
        );
        String path = tempDir.resolve("export.csv").toString();

        new CSVExportAdapter(records).exportTo(path);

        List<String> lines = Files.readAllLines(Path.of(path));
        assertFalse(lines.isEmpty(), "File should not be empty");
        assertEquals("transaction_id,rfid_tag,timestamp,status,source,product_name", lines.get(0));
    }

    @Test
    void exportTo_writesOneDataRowPerRecord() throws RFIDException, IOException {
        List<ScanRecord> records = List.of(
                makeRecord("TAG-001", ScanRecord.STATUS_OK,      ScanRecord.SOURCE_RFID,    "Widget A"),
                makeRecord("TAG-002", ScanRecord.STATUS_UNKNOWN, ScanRecord.SOURCE_BARCODE, "Unknown")
        );
        String path = tempDir.resolve("multi.csv").toString();

        new CSVExportAdapter(records).exportTo(path);

        List<String> lines = Files.readAllLines(Path.of(path));
        // 1 header + 2 data rows
        assertEquals(3, lines.size());
        assertTrue(lines.get(1).contains("TAG-001"));
        assertTrue(lines.get(2).contains("TAG-002"));
    }

    @Test
    void exportTo_emptyList_writesOnlyHeader() throws RFIDException, IOException {
        String path = tempDir.resolve("empty.csv").toString();

        new CSVExportAdapter(Collections.emptyList()).exportTo(path);

        List<String> lines = Files.readAllLines(Path.of(path));
        assertEquals(1, lines.size(), "Only header row expected");
    }

    @Test
    void exportTo_escapesCommasInProductName() throws RFIDException, IOException {
        ScanRecord r = makeRecord("TAG-003", ScanRecord.STATUS_OK, ScanRecord.SOURCE_MANUAL,
                "Widget, Deluxe");  // comma in product name
        String path = tempDir.resolve("commas.csv").toString();

        new CSVExportAdapter(List.of(r)).exportTo(path);

        String content = Files.readString(Path.of(path));
        // The comma in the name should be quoted
        assertTrue(content.contains("\"Widget, Deluxe\""));
    }

    @Test
    void exportTo_returnsPathOfCreatedFile() throws RFIDException {
        String path = tempDir.resolve("result.csv").toString();

        String returned = new CSVExportAdapter(Collections.emptyList()).exportTo(path);

        assertEquals(path, returned);
    }

    @Test
    void exportTo_nullOrBlankPath_usesDefaultDesktopPath() throws RFIDException, IOException {
        // When path is blank, the adapter writes to user.home — just verify no exception thrown
        // and that the returned path ends with .csv
        String returned = new CSVExportAdapter(Collections.emptyList()).exportTo("");
        assertTrue(returned.endsWith(".csv"));
        // Clean up the generated file
        Files.deleteIfExists(Path.of(returned));
    }

    @Test
    void exportTo_throwsExportFailedException_onInvalidDirectory() {
        String badPath = "/nonexistent_directory/export.csv";

        assertThrows(RFIDException.ExportFailedException.class,
                () -> new CSVExportAdapter(Collections.emptyList()).exportTo(badPath));
    }
}
