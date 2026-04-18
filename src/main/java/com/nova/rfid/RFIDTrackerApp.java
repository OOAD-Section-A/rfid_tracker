package com.nova.rfid;

import com.nova.rfid.ui.MainAppWindow;

import javax.swing.*;

/**
 * APPLICATION ENTRY POINT
 * SUBSYSTEM 11: BARCODE READER & RFID TRACKER — Team NOVA
 *
 * Run this class to start the application.
 * Prerequisites:
 *   1. MySQL running on localhost:3306
 *   2. Run database_setup.sql to create schema and seed data
 *   3. mysql-connector-java JAR on classpath
 */
public class RFIDTrackerApp {

    public static void main(String[] args) {

        // ── INVENTORY INTEGRATION TEST (remove after verification) ────────
        System.out.println("--- Inventory Integration Test ---");
        try {
            inventory_subsystem.InventoryExceptionSource exSource =
                    new inventory_subsystem.InventoryExceptionSource();
            inventory_subsystem.InventoryRepository repo =
                    new inventory_subsystem.InventoryRepository(exSource);
            inventory_subsystem.AddStockStrategy strategy =
                    new inventory_subsystem.AddStockStrategy();

            // Simulate 3 scans of the same SKU
            strategy.execute("SKU-TEST-001", "RFID-INBOUND", "RFID-SCAN", 1,
                    repo, exSource, inventory_subsystem.IssuingPolicy.FIFO);
            strategy.execute("SKU-TEST-001", "RFID-INBOUND", "RFID-SCAN", 1,
                    repo, exSource, inventory_subsystem.IssuingPolicy.FIFO);
            strategy.execute("SKU-TEST-001", "RFID-INBOUND", "RFID-SCAN", 1,
                    repo, exSource, inventory_subsystem.IssuingPolicy.FIFO);

            // Check total stock
            inventory_subsystem.InventoryItem item =
                    repo.find("SKU-TEST-001", "RFID-INBOUND", "RFID-SCAN");
            System.out.println("Stock after 3 scans: " + item.getTotalQuantity()); // expect 3
            System.out.println("PASS: Inventory integration working.");
        } catch (Exception e) {
            System.out.println("FAIL: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("--- End Test ---");
        // ── END TEST ──────────────────────────────────────────────────────

        // Ensure Swing runs on EDT (Event Dispatch Thread)
        SwingUtilities.invokeLater(() -> {
            System.setProperty("awt.useSystemAAFontSettings", "on");
            System.setProperty("swing.aatext", "true");

            System.out.println("═══════════════════════════════════════════");
            System.out.println("  SCM SUBSYSTEM 11 — RFID TRACKER          ");
            System.out.println("  Team NOVA                                 ");
            System.out.println("═══════════════════════════════════════════");

            MainAppWindow window = new MainAppWindow();
            window.setVisible(true);
        });
    }
}