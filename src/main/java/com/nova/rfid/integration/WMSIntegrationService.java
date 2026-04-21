package com.nova.rfid.integration;

import com.nova.rfid.exception.RFIDException;
import com.nova.rfid.model.Product;
import com.nova.rfid.model.ScanRecord;
import com.nova.rfid.pattern.structural.RFIDSystemFacade;
import java.util.List;

/**
 * Stable integration point for Subsystem 2 (Warehouse Management).
 *
 * NEVER change:
 * - Package, class name, or any method signature
 * - Do NOT add inventory_subsystem imports to this file
 *
 * You CAN freely change everything else in your codebase.
 */
public class WMSIntegrationService {

    private static WMSIntegrationService instance;
    private final RFIDSystemFacade facade;

    /**
     * CRITICAL: Constructor must catch ALL failures from RFIDSystemFacade.getInstance().
     * If it throws (e.g., because inventory classes are absent from WMS classpath),
     * facade must be set to null — NOT propagate the exception.
     * Each method must null-check facade before use.
     */
    private WMSIntegrationService() {
        RFIDSystemFacade temp = null;
        try {
            temp = RFIDSystemFacade.getInstance();
        } catch (Throwable t) {
            System.err.println("[WMSIntegrationService] Facade init failed: "
                    + t.getMessage());
        }
        this.facade = temp;
    }

    public static synchronized WMSIntegrationService getInstance() {
        if (instance == null) {
            instance = new WMSIntegrationService();
        }
        return instance;
    }

    /**
     * Called by WMS during inbound receiving for each scanned tag.
     * Returns null on failure — WMS handles null safely.
     * CRITICAL: Do NOT call AddStockStrategy here. WMS owns inventory updates.
     *
     * @param rfidTag Raw tag string from scanner hardware
     * @param source  "RFID" or "BARCODE"
     * @return ScanRecord persisted in your DB, or null on failure
     */
    public ScanRecord submitScan(String rfidTag, String source) {
        if (facade == null) return null;
        try {
            RFIDSystemFacade.ScanResult result = facade.submitScan(rfidTag, source);
            return result != null ? result.getRecord() : null;
        } catch (RFIDException e) {
            System.err.println("[WMSIntegrationService] submitScan failed: "
                    + e.getMessage());
            return null;
        }
    }

    /**
     * Called by WMS to identify which WMS Product a scanned tag belongs to.
     * Returns null if tag is unknown or lookup fails.
     *
     * @param rfidTag Raw tag string from scanner hardware
     * @param source  "RFID" or "BARCODE"
     * @return Your Product object, or null
     */
    public Product getProductFromScan(String rfidTag, String source) {
        if (facade == null) return null;
        try {
            RFIDSystemFacade.ScanResult result = facade.submitScan(rfidTag, source);
            return result != null ? result.getProduct() : null;
        } catch (RFIDException e) {
            System.err.println("[WMSIntegrationService] getProductFromScan failed: "
                    + e.getMessage());
            return null;
        }
    }

    /**
     * Called by WMS for dock activity display and Subsystem 5 reporting.
     *
     * @param limit Maximum records to return
     * @return Recent scan records, most recent first. Empty list on failure.
     */
    public List<ScanRecord> getRecentScans(int limit) {
        if (facade == null) return List.of();
        try {
            return facade.getRecentScans(limit);
        } catch (RFIDException e) {
            return List.of();
        }
    }

    /**
     * Called by WMS for dashboard summary display.
     *
     * @return int[3] — [total scans today, successful, failed].
     *         Return new int[]{0, 0, 0} on failure, never null.
     */
    public int[] getTodaySummary() {
        if (facade == null) return new int[]{0, 0, 0};
        try {
            return facade.getTodaySummary();
        } catch (RFIDException e) {
            return new int[]{0, 0, 0};
        }
    }
}
