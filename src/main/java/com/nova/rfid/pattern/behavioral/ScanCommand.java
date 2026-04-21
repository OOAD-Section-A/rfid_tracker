package com.nova.rfid.pattern.behavioral;

import com.nova.rfid.exception.RFIDException;
import com.nova.rfid.model.ScanRecord;

/**
 * BEHAVIORAL PATTERN: COMMAND
 *
 * Encapsulates a scan request as an object, decoupling the sender (UI)
 * from the executor (service layer). Enables logging, queuing, and undo.
 *
 * GRASP - Controller: ScanCommand acts as the entry point for one scan action.
 * SOLID - SRP: Each ScanCommand encapsulates exactly one scan operation.
 * SOLID - OCP: New command types (e.g., BulkScanCommand) added without changes.
 */
public interface ScanCommand {
    ScanRecord execute() throws RFIDException;
}
