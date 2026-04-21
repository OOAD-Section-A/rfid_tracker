package com.nova.rfid.pattern.behavioral;

import com.nova.rfid.exception.RFIDException;

import java.util.Set;

/**
 * Concrete Handlers for the Chain of Responsibility.
 *
 * All in one file for convenience; each inner class is independent.
 * The chain is assembled in RFIDSystemFacade.
 */
public class TagValidationHandlers {

    // ─── Handler 1: Empty / Null check ────────────────────────────────────
    public static class EmptyTagHandler extends AbstractTagHandler {
        @Override
        public String handle(String rfidTag) throws RFIDException {
            if (rfidTag == null || rfidTag.trim().isEmpty()) {
                throw new RFIDException.InvalidTagFormatException(rfidTag == null ? "null" : "");
            }
            return passToNext(rfidTag.trim());
        }
    }

    // ─── Handler 2: Basic Format Validation ───────────────────────────────
    // Allows letters, digits, and hyphens. Min 3 chars.
    public static class FormatValidationHandler extends AbstractTagHandler {
        @Override
        public String handle(String rfidTag) throws RFIDException {
            if (!rfidTag.matches("^[A-Za-z0-9\\-]{3,50}$")) {
                throw new RFIDException.InvalidTagFormatException(rfidTag);
            }
            return passToNext(rfidTag.toUpperCase());
        }
    }

    // ─── Handler 3: Session Duplicate Check ───────────────────────────────
    public static class SessionDuplicateHandler extends AbstractTagHandler {
        private final Set<String> sessionScannedTags;

        public SessionDuplicateHandler(Set<String> sessionScannedTags) {
            this.sessionScannedTags = sessionScannedTags;
        }

        @Override
        public String handle(String rfidTag) throws RFIDException {
            if (sessionScannedTags.contains(rfidTag)) {
                throw new RFIDException.DuplicateRfidScanException(rfidTag);
            }
            return passToNext(rfidTag);
        }
    }
}
