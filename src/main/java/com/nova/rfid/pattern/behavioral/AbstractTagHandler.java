package com.nova.rfid.pattern.behavioral;

import com.nova.rfid.exception.RFIDException;

/**
 * ═══════════════════════════════════════════════════════════════════
 * BEHAVIORAL PATTERN: CHAIN OF RESPONSIBILITY
 *
 * Each handler decides whether to process or pass the RFID tag.
 * Handlers:
 *   1. EmptyTagHandler     → checks blank / null
 *   2. FormatValidator     → checks alphanumeric format
 *   3. SessionDuplicateHandler → checks in-session duplicate
 *   4. DatabaseLookupHandler  → final DB check (product exists?)
 *
 * GRASP - Low Coupling: Handlers don't know about each other's internals.
 * GRASP - Protected Variations: Adding new rules = new handler, no edits.
 * SOLID - OCP: Chain is extended by adding new AbstractTagHandler subclasses.
 * SOLID - SRP: Each handler has one validation responsibility.
 * ═══════════════════════════════════════════════════════════════════
 */
public abstract class AbstractTagHandler {

    protected AbstractTagHandler next;

    /** Chain the next handler. Returns 'next' for fluent chaining. */
    public AbstractTagHandler setNext(AbstractTagHandler next) {
        this.next = next;
        return next;
    }

    /**
     * Process this RFID tag. Subclasses validate, then call next.handle()
     * or throw an RFIDException if validation fails.
     *
     * @param rfidTag tag string from user input
     * @return rfidTag (potentially normalized) if all handlers pass
     * @throws RFIDException if any handler detects a violation
     */
    public abstract String handle(String rfidTag) throws RFIDException;

    protected String passToNext(String rfidTag) throws RFIDException {
        if (next != null) {
            return next.handle(rfidTag);
        }
        return rfidTag; // end of chain — all checks passed
    }
}
