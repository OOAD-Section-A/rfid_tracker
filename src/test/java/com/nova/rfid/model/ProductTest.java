package com.nova.rfid.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Product model and its Builder.
 */
class ProductTest {

    @Test
    void builder_setsAllStringFields() {
        Product p = new Product.Builder()
                .productId("P-001")
                .rfidTag("RFID-ABC")
                .productName("Widget A")
                .category("Electronics")
                .sku("SKU-999")
                .description("A sample widget")
                .build();

        assertEquals("P-001",        p.getProductId());
        assertEquals("RFID-ABC",     p.getRfidTag());
        assertEquals("Widget A",     p.getProductName());
        assertEquals("Electronics",  p.getCategory());
        assertEquals("SKU-999",      p.getSku());
        assertEquals("A sample widget", p.getDescription());
    }

    @Test
    void builder_intProductId_convertsToString() {
        Product p = new Product.Builder()
                .productId(42)
                .productName("Legacy Item")
                .build();

        assertEquals("42", p.getProductId());
        assertEquals(42,   p.getProductIdInt());
    }

    @Test
    void getProductIdInt_returnsMinusOneForNonNumericId() {
        Product p = new Product.Builder()
                .productId("P-ALPHA")
                .build();

        assertEquals(-1, p.getProductIdInt());
    }

    @Test
    void builder_skuAndDescription_defaultToEmptyString() {
        Product p = new Product.Builder()
                .productId("P-002")
                .build();

        assertEquals("", p.getSku());
        assertEquals("", p.getDescription());
    }

    @Test
    void toString_containsKeyFields() {
        Product p = new Product.Builder()
                .productId("P-003")
                .sku("S-1")
                .rfidTag("TAG1")
                .productName("Gadget")
                .category("Tools")
                .build();

        String s = p.toString();
        assertTrue(s.contains("P-003"));
        assertTrue(s.contains("S-1"));
        assertTrue(s.contains("TAG1"));
        assertTrue(s.contains("Gadget"));
        assertTrue(s.contains("Tools"));
    }
}
