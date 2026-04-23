package com.nova.rfid.pattern.behavioral;

import com.nova.rfid.exception.RFIDException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Chain of Responsibility validation handlers.
 *
 * Each handler is tested in isolation (no next handler attached) and
 * also as a complete assembled chain.
 */
class TagValidationHandlersTest {

    // ── EmptyTagHandler ──────────────────────────────────────────────────

    @Test
    void emptyHandler_throwsOnNull() {
        TagValidationHandlers.EmptyTagHandler h = new TagValidationHandlers.EmptyTagHandler();
        assertThrows(RFIDException.InvalidTagFormatException.class, () -> h.handle(null));
    }

    @Test
    void emptyHandler_throwsOnBlankString() {
        TagValidationHandlers.EmptyTagHandler h = new TagValidationHandlers.EmptyTagHandler();
        assertThrows(RFIDException.InvalidTagFormatException.class, () -> h.handle("   "));
    }

    @Test
    void emptyHandler_trimsAndPassesValidTag() throws RFIDException {
        TagValidationHandlers.EmptyTagHandler h = new TagValidationHandlers.EmptyTagHandler();
        // No next handler: passToNext returns the (trimmed) tag
        String result = h.handle("  TAG-1  ");
        assertEquals("TAG-1", result);
    }

    // ── FormatValidationHandler ──────────────────────────────────────────

    @Test
    void formatHandler_throwsOnTooShortTag() {
        TagValidationHandlers.FormatValidationHandler h = new TagValidationHandlers.FormatValidationHandler();
        assertThrows(RFIDException.InvalidTagFormatException.class, () -> h.handle("AB"));
    }

    @Test
    void formatHandler_throwsOnSpecialCharacters() {
        TagValidationHandlers.FormatValidationHandler h = new TagValidationHandlers.FormatValidationHandler();
        assertThrows(RFIDException.InvalidTagFormatException.class, () -> h.handle("TAG!@#"));
    }

    @Test
    void formatHandler_acceptsAlphanumericWithHyphen() throws RFIDException {
        TagValidationHandlers.FormatValidationHandler h = new TagValidationHandlers.FormatValidationHandler();
        String result = h.handle("rfid-001");
        // Should uppercase
        assertEquals("RFID-001", result);
    }

    @Test
    void formatHandler_uppercasesOutput() throws RFIDException {
        TagValidationHandlers.FormatValidationHandler h = new TagValidationHandlers.FormatValidationHandler();
        assertEquals("ABC123", h.handle("abc123"));
    }

    @Test
    void formatHandler_rejectsTooLongTag() {
        TagValidationHandlers.FormatValidationHandler h = new TagValidationHandlers.FormatValidationHandler();
        String longTag = "A".repeat(51);
        assertThrows(RFIDException.InvalidTagFormatException.class, () -> h.handle(longTag));
    }

    @Test
    void formatHandler_accepts50CharTag() throws RFIDException {
        TagValidationHandlers.FormatValidationHandler h = new TagValidationHandlers.FormatValidationHandler();
        String maxTag = "A".repeat(50);
        assertEquals(maxTag, h.handle(maxTag));
    }

    // ── SessionDuplicateHandler ──────────────────────────────────────────

    @Test
    void duplicateHandler_throwsIfTagAlreadyScanned() {
        Set<String> seen = new HashSet<>();
        seen.add("TAG-DUP");
        TagValidationHandlers.SessionDuplicateHandler h =
                new TagValidationHandlers.SessionDuplicateHandler(seen);

        assertThrows(RFIDException.DuplicateRfidScanException.class, () -> h.handle("TAG-DUP"));
    }

    @Test
    void duplicateHandler_passesNewTag() throws RFIDException {
        Set<String> seen = new HashSet<>();
        seen.add("TAG-OLD");
        TagValidationHandlers.SessionDuplicateHandler h =
                new TagValidationHandlers.SessionDuplicateHandler(seen);

        String result = h.handle("TAG-NEW");
        assertEquals("TAG-NEW", result);
    }

    // ── Full chain: Empty → Format → Duplicate ───────────────────────────

    private AbstractTagHandler buildChain(Set<String> sessionTags) {
        AbstractTagHandler empty  = new TagValidationHandlers.EmptyTagHandler();
        AbstractTagHandler format = new TagValidationHandlers.FormatValidationHandler();
        AbstractTagHandler dup    = new TagValidationHandlers.SessionDuplicateHandler(sessionTags);
        empty.setNext(format).setNext(dup);
        return empty;
    }

    @Test
    void chain_normalizes_valid_tag() throws RFIDException {
        Set<String> seen = new HashSet<>();
        AbstractTagHandler chain = buildChain(seen);
        // Input with leading/trailing spaces and lowercase — should normalize
        String result = chain.handle("  rfid-abc  ");
        assertEquals("RFID-ABC", result);
    }

    @Test
    void chain_rejectsNullAtFirstHandler() {
        AbstractTagHandler chain = buildChain(new HashSet<>());
        assertThrows(RFIDException.InvalidTagFormatException.class, () -> chain.handle(null));
    }

    @Test
    void chain_rejectsBadFormatBeforeDuplicateCheck() {
        Set<String> seen = new HashSet<>();
        AbstractTagHandler chain = buildChain(seen);
        assertThrows(RFIDException.InvalidTagFormatException.class, () -> chain.handle("bad tag!"));
    }

    @Test
    void chain_rejectsDuplicateAfterPassingFormat() {
        Set<String> seen = new HashSet<>();
        seen.add("RFID-001");
        AbstractTagHandler chain = buildChain(seen);
        // "rfid-001" normalizes to "RFID-001", which is in seen
        assertThrows(RFIDException.DuplicateRfidScanException.class, () -> chain.handle("RFID-001"));
    }
}
