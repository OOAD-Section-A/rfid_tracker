package com.nova.rfid.model;

/**
 * MODEL: Product
 *
 * GRASP - Information Expert: Holds and exposes all product data.
 * SOLID - SRP: Only represents product entity; no business logic.
 *
 * ── INTEGRATION CHANGE ────────────────────────────────────────────
 * The shared OOAD schema `products` table uses:
 *   product_id  VARCHAR(50)  — was INT in the legacy scm_rfid_db
 *   sku         VARCHAR(50)  — new field added by Inventory subsystem
 *
 * The Builder has been updated accordingly.
 * Callers using the old productId(int) overload will still compile
 * via the convenience overload — it just stringifies the int.
 *
 * Integrates with: Subsystem 1 (Inventory), Subsystem 2 (Warehouse)
 * They READ product_id, product_name, category, sku, description.
 * ─────────────────────────────────────────────────────────────────
 */
public class Product {

    private String productId;   // VARCHAR(50) in shared schema
    private String rfidTag;     // the scanned tag that resolved to this product
    private String productName;
    private String category;
    private String sku;         // from shared products table
    private String description;

    // ── Builder Pattern (Creational) ──────────────────────────────────────
    private Product() {}

    public static class Builder {
        private String productId;
        private String rfidTag;
        private String productName;
        private String category;
        private String sku         = "";
        private String description = "";

        /** Set product_id from the shared OOAD schema (VARCHAR). */
        public Builder productId(String id)         { this.productId   = id;   return this; }

        /**
         * Convenience overload for legacy scm_rfid_db where product_id is INT.
         * Converts to String so the rest of the codebase stays uniform.
         */
        public Builder productId(int id)            { this.productId   = String.valueOf(id); return this; }

        public Builder rfidTag(String tag)          { this.rfidTag     = tag;  return this; }
        public Builder productName(String name)     { this.productName = name; return this; }
        public Builder category(String cat)         { this.category    = cat;  return this; }

        /** SKU from the shared products table. Optional — defaults to "". */
        public Builder sku(String sku)              { this.sku         = sku;  return this; }

        public Builder description(String desc)     { this.description = desc; return this; }

        public Product build() {
            Product p = new Product();
            p.productId   = this.productId;
            p.rfidTag     = this.rfidTag;
            p.productName = this.productName;
            p.category    = this.category;
            p.sku         = this.sku;
            p.description = this.description;
            return p;
        }
    }

    // ── Getters ───────────────────────────────────────────────────────────
    /** Returns the product_id as a String (works for both INT legacy and VARCHAR shared). */
    public String getProductId()   { return productId;   }

    /** Returns the product_id as an int for legacy code that still needs it. */
    public int    getProductIdInt() {
        try { return Integer.parseInt(productId); }
        catch (NumberFormatException e) { return -1; }
    }

    public String getRfidTag()     { return rfidTag;     }
    public String getProductName() { return productName; }
    public String getCategory()    { return category;    }
    public String getSku()         { return sku;         }
    public String getDescription() { return description; }

    @Override
    public String toString() {
        return "Product{id=" + productId + ", sku=" + sku + ", tag=" + rfidTag
                + ", name=" + productName + ", category=" + category + "}";
    }
}