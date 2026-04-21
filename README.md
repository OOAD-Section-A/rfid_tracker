# SCM SUBSYSTEM 11 — BARCODE READER & RFID TRACKER
## Team NOVA | Java Swing + MySQL

---

## WHAT CHANGED (Integration with DB Team)

### Files Modified
| File | What Changed |
|------|-------------|
| `RFIDException.java` | Exception codes aligned to shared exception list (IDs 307, 408–413, 51). Added `exceptionListId` field on every exception. |
| `DatabaseManager.java` | `insertToSharedEventsTable()` now uses the **actual** `barcode_rfid_events` columns from `schema.sql` (`rfid_tag`, `product_name`, `category`, `description`, `transaction_id`, `warehouse_id`, `status`, `source`). `logExceptionToSharedTable()` now maps to **all columns** of `subsystem_exceptions` including `exception_name`, `error_code`, `stack_trace`, `inner_exception`, `handling_plan`, `retry_count`. |
| `pom.xml` | `mysql-connector-j` bumped from `8.3.0` → `9.3.0` to match DB team. Added `slf4j-simple` and `exec-maven-plugin`. |
| `database_setup.sql` | Kept as local fallback. Added OOAD seed block (commented out). Changed inserts to `INSERT IGNORE` to prevent re-run errors. |

### Files Unchanged
`Product.java`, `ScanRecord.java`, `RFIDSystemFacade.java`, `CSVExportAdapter.java`,
`AbstractTagHandler.java`, `TagValidationHandlers.java`, `ScanCommand.java`,
`ScanRecordIterator.java`, `ScanDashboardPanel.java`, `ScanLogPanel.java`,
`MainAppWindow.java`, `UIDialogHelper.java`, `RFIDTrackerApp.java`

---

## PROJECT STRUCTURE

```
rfid_tracker/
├── database_setup.sql                          ← Local fallback DB setup
├── pom.xml                                     ← Maven build (mysql-connector-j 9.3.0)
└── src/main/java/com/nova/rfid/
    ├── RFIDTrackerApp.java                     ← ENTRY POINT
    ├── model/
    │   ├── Product.java
    │   └── ScanRecord.java
    ├── exception/
    │   └── RFIDException.java                  ← ★ MODIFIED — aligned to exception list
    ├── db/
    │   └── DatabaseManager.java                ← ★ MODIFIED — correct OOAD schema columns
    ├── pattern/
    │   ├── structural/
    │   │   ├── RFIDSystemFacade.java
    │   │   └── CSVExportAdapter.java
    │   └── behavioral/
    │       ├── AbstractTagHandler.java
    │       ├── TagValidationHandlers.java
    │       ├── ScanCommand.java
    │       └── ScanRecordIterator.java
    └── ui/
        ├── MainAppWindow.java
        ├── ScanDashboardPanel.java
        ├── ScanLogPanel.java
        └── UIDialogHelper.java
```

---

## EXCEPTION LIST ALIGNMENT

Exceptions used by Subsystem 11 (from `Exception-List-Final.xlsx`):

| ID  | Code | Severity | Description |
|-----|------|----------|-------------|
| 51  | DB_CONNECTION_FAILED | MAJOR | Cannot connect to database |
| 307 | BARCODE_DUPLICATE | MINOR | Duplicate barcode in system records |
| 408 | UNKNOWN_RFID_TAG | WARNING | Tag not registered in system |
| 409 | DUPLICATE_RFID_SCAN | WARNING | Same tag scanned twice at checkpoint |
| 410 | MISSING_RFID_FIELD / INVALID_TAG_FORMAT | MAJOR/MINOR | Tag missing required field |
| 411 | FILE_NOT_FOUND | MAJOR | RFID data file not found |
| 412 | FILE_FORMAT_INVALID | MAJOR | RFID file in wrong format |
| 413 | PRODUCT_LOOKUP_FAILED | MAJOR | Tag cannot be matched to product |

---

## HOW TO RUN

### Prerequisites
- Java 17+
- Maven 3.8+
- MySQL 8+ running on localhost:3306

---

### OPTION A — With the shared OOAD database (full integration)

**Step 1 — Run the DB team's schema.sql first (one time only)**
```cmd
mysql -u root -p < path\to\database_module\schema.sql
```

**Step 2 — Verify the RFID tables exist**
```cmd
mysql -u root -p -e "USE OOAD; SHOW TABLES LIKE 'barcode%'; SHOW TABLES LIKE 'subsystem%';"
```
You should see `barcode_rfid_events` and `subsystem_exceptions`.

**Step 3 — Build the project**
```cmd
cd rfid_tracker
mvn clean package -DskipTests
```

**Step 4 — Run**
```cmd
java -jar target\rfid-tracker-jar-with-dependencies.jar
```

Or run directly without packaging:
```cmd
mvn exec:java
```

---

### OPTION B — Standalone / fallback mode (no OOAD DB needed)

**Step 1 — Run local fallback setup**
```cmd
mysql -u root -p < database_setup.sql
```

**Step 2 — Build and run**
```cmd
mvn clean package -DskipTests
java -jar target\rfid-tracker-jar-with-dependencies.jar
```
The app will try OOAD first, then automatically fall back to `scm_rfid_db`.
The sidebar will show `● scm_rfid_db (local)`.

---

### Change DB credentials (if your MySQL password is not "root")

Edit `DatabaseManager.java` lines:
```java
private static final String DB_USER     = "root";
private static final String DB_PASSWORD = "root";
```

---

## CMD PROMPT LINES — QUICK REFERENCE

```cmd
REM ── BUILD ─────────────────────────────────────────────────────
cd rfid_tracker
mvn clean package -DskipTests

REM ── RUN (fat jar) ─────────────────────────────────────────────
java -jar target\rfid-tracker-jar-with-dependencies.jar

REM ── RUN (directly via Maven) ──────────────────────────────────
mvn exec:java

REM ── COMPILE ONLY (check for errors) ──────────────────────────
mvn compile

REM ── RUN TESTS (if tests added) ────────────────────────────────
mvn test

REM ── VERIFY OOAD DB TABLES ─────────────────────────────────────
mysql -u root -p -e "USE OOAD; DESCRIBE barcode_rfid_events; DESCRIBE subsystem_exceptions;"

REM ── CHECK SCAN EVENTS WRITTEN ─────────────────────────────────
mysql -u root -p -e "USE OOAD; SELECT * FROM barcode_rfid_events ORDER BY event_timestamp DESC LIMIT 10;"

REM ── CHECK EXCEPTIONS LOGGED ───────────────────────────────────
mysql -u root -p -e "USE OOAD; SELECT exception_id, exception_name, severity, exception_message FROM subsystem_exceptions WHERE subsystem_name='SUBSYSTEM_11_RFID_BARCODE' ORDER BY timestamp_utc DESC LIMIT 10;"

REM ── SETUP LOCAL FALLBACK DB ONLY ──────────────────────────────
mysql -u root -p < database_setup.sql

REM ── CLEAN BUILD ARTIFACTS ─────────────────────────────────────
mvn clean
```

---

## INTEGRATION POINTS WITH OTHER SUBSYSTEMS

### Tables we READ (owned by other teams)
| Table | Owner | Columns we read |
|-------|-------|-----------------|
| `products` | Inventory subsystem | `product_id`, `product_name`, `category`, `sku` |

### Tables we WRITE (owned by Subsystem 11)
| Table | What we write |
|-------|--------------|
| `barcode_rfid_events` | `event_id`, `product_id`, `rfid_tag`, `product_name`, `category`, `description`, `transaction_id`, `warehouse_id`, `event_timestamp`, `status`, `source` |
| `subsystem_exceptions` | All columns — exception details logged on every RFIDException |

### Facade API (what other subsystems call)
```java
RFIDSystemFacade facade = RFIDSystemFacade.getInstance();
facade.submitScan(rfidTag, "RFID");     // Sub.2 WMS, Sub.4 Orders
facade.getTodaySummary();               // Sub.3 Reporting
facade.getAllScanLogs();                 // Sub.3 Reporting
facade.getRecentScans(10);              // Sub.3 Reporting
```

---

## TROUBLESHOOTING

| Problem | Fix |
|---------|-----|
| `DB_CONNECTION_FAILED` on startup | MySQL not running, or wrong credentials in `DatabaseManager.java` |
| `Table 'OOAD.barcode_rfid_events' doesn't exist` | Run the DB team's `schema.sql` first |
| Falls back to `scm_rfid_db` | OOAD DB not reachable; run `database_setup.sql` for local fallback |
| `Column 'event_type' doesn't exist` | Old version of `DatabaseManager.java` — replace with the updated one |
| Build fails on Java version | Ensure `JAVA_HOME` points to Java 17+; run `java -version` to verify |
