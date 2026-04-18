-- ============================================================
-- SUBSYSTEM 11: BARCODE READER & RFID TRACKER
-- Team NOVA — Database Setup Script
--
-- INTEGRATION NOTE:
--   This script sets up the LOCAL FALLBACK database (scm_rfid_db).
--   When running with the shared OOAD database (schema.sql from
--   the DB team), the barcode_rfid_events and subsystem_exceptions
--   tables already exist in the OOAD schema.
--
--   Run this script ONLY for standalone/demo mode.
--   For full integration, run the DB team's schema.sql first.
-- ============================================================

-- ── FALLBACK DB (standalone / demo mode) ─────────────────────────
CREATE DATABASE IF NOT EXISTS scm_rfid_db;
USE scm_rfid_db;

-- ============================================================
-- TABLE: products  (READ by Subsystem 11 — legacy fallback)
-- Shared OOAD equivalent: products (Inventory subsystem)
-- ============================================================
CREATE TABLE IF NOT EXISTS products (
    product_id   INT PRIMARY KEY AUTO_INCREMENT,
    rfid_tag     VARCHAR(50)  NOT NULL UNIQUE,
    product_name VARCHAR(100) NOT NULL,
    category     VARCHAR(50)  NOT NULL,
    description  TEXT
);

-- ============================================================
-- TABLE: scan_transactions  (legacy fallback)
-- Shared OOAD equivalent: barcode_rfid_events
-- ============================================================
CREATE TABLE IF NOT EXISTS scan_transactions (
    transaction_id INT PRIMARY KEY AUTO_INCREMENT,
    rfid_tag       VARCHAR(50)  NOT NULL,
    timestamp      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status         VARCHAR(30)  NOT NULL,   -- OK | Unknown | Duplicate | Failed
    source         VARCHAR(20)  NOT NULL    -- RFID | Barcode | Manual
);

-- ============================================================
-- SEED: 100 products with RFID tags
-- ============================================================
INSERT IGNORE INTO products (rfid_tag, product_name, category, description) VALUES
('RFID-0001','Laptop Pro 15','Electronics','High-performance laptop with 16GB RAM'),
('RFID-0002','Wireless Mouse','Electronics','Ergonomic wireless mouse, 2.4GHz'),
('RFID-0003','Mechanical Keyboard','Electronics','Tactile mechanical keyboard, backlit'),
('RFID-0004','USB-C Hub 7-in-1','Electronics','Multi-port USB-C hub with HDMI'),
('RFID-0005','27-inch Monitor','Electronics','4K UHD IPS display monitor'),
('RFID-0006','Noise Cancelling Headphones','Electronics','Over-ear ANC headphones'),
('RFID-0007','Webcam HD 1080p','Electronics','Full HD webcam with auto-focus'),
('RFID-0008','External SSD 1TB','Electronics','Portable SSD, USB 3.2 Gen 2'),
('RFID-0009','Smart Speaker','Electronics','Voice-controlled smart speaker'),
('RFID-0010','Graphic Tablet','Electronics','Digital drawing tablet with stylus'),
('RFID-0011','Office Chair Pro','Furniture','Ergonomic mesh office chair'),
('RFID-0012','Standing Desk','Furniture','Electric height-adjustable desk'),
('RFID-0013','Bookshelf 5-Tier','Furniture','Solid wood 5-tier bookshelf'),
('RFID-0014','Filing Cabinet','Furniture','3-drawer steel filing cabinet'),
('RFID-0015','Whiteboard 4x3','Furniture','Magnetic dry-erase whiteboard'),
('RFID-0016','Conference Table','Furniture','Oval conference table, 10-seater'),
('RFID-0017','Visitor Chair','Furniture','Cushioned visitor chair with armrests'),
('RFID-0018','Monitor Stand','Furniture','Adjustable dual monitor stand'),
('RFID-0019','Cable Management Tray','Furniture','Under-desk cable management'),
('RFID-0020','Desk Organizer','Furniture','Bamboo desk organizer set'),
('RFID-0021','Safety Helmet','Safety','ANSI-certified hard hat, class E'),
('RFID-0022','Safety Gloves L','Safety','Cut-resistant level 5 gloves'),
('RFID-0023','Fire Extinguisher 5kg','Safety','ABC dry powder extinguisher'),
('RFID-0024','First Aid Kit Pro','Safety','100-piece professional first aid kit'),
('RFID-0025','Safety Vest','Safety','Hi-vis reflective safety vest'),
('RFID-0026','Safety Goggles','Safety','Anti-fog polycarbonate goggles'),
('RFID-0027','Ear Protection Muffs','Safety','SNR 30dB ear defenders'),
('RFID-0028','Fall Arrest Harness','Safety','Full-body safety harness, EN361'),
('RFID-0029','Steel-toe Boots Size 9','Safety','S3-rated steel-toe safety boots'),
('RFID-0030','Respirator Mask N95','Safety','N95 particulate respirator'),
('RFID-0031','Forklift Battery','Machinery','48V lithium forklift battery'),
('RFID-0032','Pallet Jack 2T','Machinery','2-tonne hydraulic pallet jack'),
('RFID-0033','Conveyor Belt 5m','Machinery','Modular conveyor belt section'),
('RFID-0034','Barcode Scanner Gun','Machinery','Wireless 1D/2D barcode scanner'),
('RFID-0035','Label Printer ZT420','Machinery','Industrial thermal label printer'),
('RFID-0036','Stretch Wrap Machine','Machinery','Semi-auto pallet stretch wrapper'),
('RFID-0037','Electric Stacker','Machinery','1.5T electric walkie stacker'),
('RFID-0038','Industrial Scale 500kg','Machinery','Platform scale with indicator'),
('RFID-0039','Dock Leveler','Machinery','Hydraulic dock leveler 6T'),
('RFID-0040','Air Compressor 50L','Machinery','Oil-free air compressor'),
('RFID-0041','Printer Paper A4 Ream','Office Supplies','500 sheets 80gsm A4 paper'),
('RFID-0042','Ballpoint Pens Box','Office Supplies','Box of 50 blue ballpoint pens'),
('RFID-0043','Sticky Notes Pack','Office Supplies','Pack of 12 sticky note pads'),
('RFID-0044','Stapler Heavy Duty','Office Supplies','Heavy-duty 100-sheet stapler'),
('RFID-0045','Whiteboard Markers','Office Supplies','Set of 8 dry-erase markers'),
('RFID-0046','Binder Clips Large','Office Supplies','Box of 24 large binder clips'),
('RFID-0047','Scotch Tape Dispenser','Office Supplies','Desktop tape dispenser + 3 rolls'),
('RFID-0048','File Folders A4','Office Supplies','Pack of 50 manilla file folders'),
('RFID-0049','Highlighter Set','Office Supplies','Pack of 6 fluorescent highlighters'),
('RFID-0050','Shredder Cross-Cut','Office Supplies','10-sheet cross-cut paper shredder'),
('RFID-0051','Cold Chain Sensor A1','Sensors','Temperature/humidity IoT sensor'),
('RFID-0052','Cold Chain Sensor A2','Sensors','Temperature/humidity IoT sensor'),
('RFID-0053','RFID Gate Reader','Sensors','UHF RFID portal gate reader'),
('RFID-0054','Smart Lock Unit','Sensors','Bluetooth smart lock for cabinets'),
('RFID-0055','Asset Tracker T1','Sensors','BLE asset tracking beacon'),
('RFID-0056','Motion Sensor PIR','Sensors','Passive infrared motion sensor'),
('RFID-0057','Door Contact Sensor','Sensors','Magnetic door open/close sensor'),
('RFID-0058','GPS Tracker G5','Sensors','4G LTE real-time GPS tracker'),
('RFID-0059','Weight Sensor 50kg','Sensors','Load cell weight sensor module'),
('RFID-0060','Vibration Sensor V2','Sensors','Industrial vibration alert sensor'),
('RFID-0061','Cardboard Boxes Small','Packaging','Pack of 20 small shipping boxes'),
('RFID-0062','Bubble Wrap Roll','Packaging','10m x 1.5m bubble wrap roll'),
('RFID-0063','Packing Peanuts 1 CuFt','Packaging','Biodegradable packing peanuts'),
('RFID-0064','Stretch Film 500m','Packaging','Hand stretch wrap 20 micron'),
('RFID-0065','Fragile Sticker Roll','Packaging','Roll of 500 fragile stickers'),
('RFID-0066','Thermal Labels 100x150','Packaging','Roll of 500 thermal labels'),
('RFID-0067','Packing Tape 48mm','Packaging','Pack of 6 clear packing tape rolls'),
('RFID-0068','Padded Mailers A4','Packaging','Pack of 25 bubble mailers'),
('RFID-0069','Wooden Pallets EUR','Packaging','EUR-standard wooden pallet'),
('RFID-0070','Plastic Pallet','Packaging','HDPE rackable plastic pallet'),
('RFID-0071','Server Rack 42U','IT Infrastructure','42U open-frame server rack'),
('RFID-0072','Network Switch 24P','IT Infrastructure','Managed 24-port Gigabit switch'),
('RFID-0073','UPS 1500VA','IT Infrastructure','Online UPS 1500VA/900W'),
('RFID-0074','Ethernet Cable 100m','IT Infrastructure','Cat6 ethernet cable drum'),
('RFID-0075','Patch Panel 24P','IT Infrastructure','24-port cat6 patch panel'),
('RFID-0076','Wi-Fi Access Point','IT Infrastructure','802.11ax dual-band AP'),
('RFID-0077','Fiber Patch Cable 10m','IT Infrastructure','LC-LC OM3 fiber patch cable'),
('RFID-0078','KVM Switch 8P','IT Infrastructure','8-port HDMI KVM switch'),
('RFID-0079','Rack PDU 16A','IT Infrastructure','Metered rack power distribution'),
('RFID-0080','NAS Drive 8TB','IT Infrastructure','Network attached storage 8TB'),
('RFID-0081','Vitamin C 1000mg','Pharmaceuticals','Bottle of 60 vitamin C tablets'),
('RFID-0082','Paracetamol 500mg','Pharmaceuticals','Strip of 10 paracetamol tablets'),
('RFID-0083','Antiseptic Solution 500ml','Pharmaceuticals','Isopropyl antiseptic solution'),
('RFID-0084','Surgical Gloves M','Pharmaceuticals','Box of 100 latex surgical gloves'),
('RFID-0085','Disposable Masks Box','Pharmaceuticals','Box of 50 surgical face masks'),
('RFID-0086','Instant Cold Pack','Pharmaceuticals','Single-use instant cold compress'),
('RFID-0087','Elastic Bandage 5cm','Pharmaceuticals','5cm x 4.5m elastic bandage'),
('RFID-0088','Sterile Gauze Pad','Pharmaceuticals','Pack of 10 sterile gauze pads'),
('RFID-0089','Thermometer Digital','Pharmaceuticals','Fast-read digital thermometer'),
('RFID-0090','Eye Wash Station','Pharmaceuticals','1L emergency eye wash station'),
('RFID-0091','Bottled Water 1L Case','Consumables','Case of 24 x 1L water bottles'),
('RFID-0092','Coffee Beans 1kg','Consumables','Premium arabica coffee beans'),
('RFID-0093','Paper Cups 200ml','Consumables','Pack of 100 disposable cups'),
('RFID-0094','Hand Sanitizer 500ml','Consumables','70% alcohol hand gel'),
('RFID-0095','Cleaning Spray 750ml','Consumables','Multi-surface disinfectant spray'),
('RFID-0096','Trash Bags 60L Box','Consumables','Box of 50 heavy-duty trash bags'),
('RFID-0097','Toilet Paper Roll 48P','Consumables','Pack of 48 toilet paper rolls'),
('RFID-0098','Dish Soap 1L','Consumables','Industrial dish washing liquid'),
('RFID-0099','Mop and Bucket Set','Consumables','Spin mop with wringer bucket'),
('RFID-0100','Microfiber Cloths Pack','Consumables','Pack of 10 microfiber cloths');

-- ============================================================
-- SEED: scan_transactions (legacy fallback only)
-- ============================================================
INSERT IGNORE INTO scan_transactions (rfid_tag, timestamp, status, source) VALUES
('RFID-0001','2025-01-05 08:15:22','OK','RFID'),
('RFID-0002','2025-01-05 08:17:44','OK','RFID'),
('RFID-0003','2025-01-05 08:20:10','OK','Manual'),
('RFID-0004','2025-01-06 09:05:33','OK','RFID'),
('RFID-0005','2025-01-06 09:12:01','OK','RFID'),
('RFID-XXXX','2025-01-06 09:14:55','Unknown','RFID'),
('RFID-0006','2025-01-07 10:00:00','OK','Manual'),
('RFID-0007','2025-01-07 10:22:18','OK','RFID'),
('RFID-0008','2025-01-08 11:05:44','OK','RFID'),
('RFID-0009','2025-01-08 11:30:02','OK','RFID'),
('RFID-0010','2025-01-09 13:15:55','OK','Manual'),
('RFID-0011','2025-01-10 08:40:12','OK','RFID'),
('RFID-0012','2025-01-10 09:00:35','OK','RFID'),
('RFID-YYYY','2025-01-10 09:02:11','Unknown','RFID'),
('RFID-0013','2025-01-11 10:10:10','OK','RFID'),
('RFID-0050','2025-02-21 11:40:55','OK','RFID'),
('RFID-0051','2025-03-01 08:05:22','OK','RFID'),
('RFID-BBBB','2025-03-06 13:00:22','Unknown','RFID'),
('RFID-0100','2025-05-10 12:45:33','OK','RFID'),
('RFID-DDDD','2025-05-11 13:00:44','Unknown','RFID');

-- ============================================================
-- VIEW: dashboard summary (used by ScanDashboard — legacy fallback)
-- ============================================================
CREATE OR REPLACE VIEW v_scan_summary AS
SELECT
    COUNT(*) AS total_scans,
    SUM(CASE WHEN status = 'OK' THEN 1 ELSE 0 END) AS successful_scans,
    SUM(CASE WHEN status IN ('Unknown','Failed','Duplicate') THEN 1 ELSE 0 END) AS failed_scans,
    DATE(timestamp) AS scan_date
FROM scan_transactions
WHERE DATE(timestamp) = CURDATE()
GROUP BY DATE(timestamp);

-- ============================================================
-- OOAD INTEGRATION: seed barcode_rfid_events in OOAD DB
-- Run this block AFTER the DB team's schema.sql has been applied.
-- Uncomment and run once connected to OOAD:
-- ============================================================
/*
USE OOAD;

INSERT IGNORE INTO barcode_rfid_events
  (event_id, product_id, rfid_tag, product_name, category, description,
   transaction_id, warehouse_id, event_timestamp, status, source)
VALUES
  ('EVT-SEED-0001','RFID-0001','RFID-0001','Laptop Pro 15','Electronics',
   'High-performance laptop','TXN-001',NULL,'2025-01-05 08:15:22','OK','RFID'),
  ('EVT-SEED-0002','RFID-0002','RFID-0002','Wireless Mouse','Electronics',
   'Ergonomic wireless mouse','TXN-002',NULL,'2025-01-05 08:17:44','OK','RFID'),
  ('EVT-SEED-0003','RFID-XXXX','RFID-XXXX','—','',
   '','TXN-003',NULL,'2025-01-06 09:14:55','Unknown','RFID');
*/

COMMIT;
