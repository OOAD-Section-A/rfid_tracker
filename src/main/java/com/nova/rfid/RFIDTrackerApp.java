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
 *   2. database.properties configured (see src/main/resources/database.properties)
 *   3. Run database_setup.sql to create schema and seed data
 *   4. All JARs installed in local Maven repo (see pom.xml install commands)
 */
public class RFIDTrackerApp {

    public static void main(String[] args) {
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
