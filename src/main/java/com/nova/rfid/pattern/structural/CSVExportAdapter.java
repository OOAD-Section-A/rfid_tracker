package com.nova.rfid.pattern.structural;

import com.nova.rfid.exception.RFIDException;
import com.nova.rfid.model.ScanRecord;

import java.io.*;
import java.util.List;

/**
 * STRUCTURAL PATTERN: ADAPTER — CSVExportAdapter
 *
 * Adapts our internal List<ScanRecord> to a file-export interface,
 * hiding the file I/O details from the Facade and UI.
 *
 * If Reporting Subsystem (Sub. 5) provides their own export library,
 * we swap only this adapter, leaving everything else untouched.
 *
 * GRASP - Low Coupling: Facade only calls exportTo(); doesn't know file I/O.
 * SOLID - OCP: New export formats (Excel, JSON) = new Adapter class.
 * SOLID - SRP: Only handles serialising ScanRecord list to CSV.
 */
public class CSVExportAdapter {

    private final List<ScanRecord> records;

    public CSVExportAdapter(List<ScanRecord> records) {
        this.records = records;
    }

    /**
     * Write records to the given file path as CSV.
     * @return absolute path of created file
     */
    public String exportTo(String filePath) throws RFIDException {
        // Default to desktop if blank
        if (filePath == null || filePath.trim().isEmpty()) {
            filePath = System.getProperty("user.home") + File.separator
                     + "rfid_export_" + System.currentTimeMillis() + ".csv";
        }

        try (PrintWriter pw = new PrintWriter(new FileWriter(filePath))) {
            // Header
            pw.println("transaction_id,rfid_tag,timestamp,status,source,product_name");
            // Rows
            for (ScanRecord r : records) {
                pw.printf("%d,%s,%s,%s,%s,%s%n",
                        r.getTransactionId(),
                        escape(r.getRfidTag()),
                        r.getFormattedTimestamp(),
                        escape(r.getStatus()),
                        escape(r.getSource()),
                        escape(r.getProductName()));
            }
            System.out.println("[CSV] Exported " + records.size()
                               + " records to: " + filePath);
            return filePath;
        } catch (IOException e) {
            throw new RFIDException.ExportFailedException("CSV", e);
        }
    }

    private String escape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
