# SCM Subsystem 11 — Barcode Reader & RFID Tracker
**Team NOVA** | Java Swing + MySQL

---

## Overview

A Java Swing desktop application for scanning and tracking RFID tags and barcodes. Scan events are logged to the shared OOAD database and displayed in a live dashboard. Exceptions are automatically recorded to the shared `subsystem_exceptions` table.

---

## Prerequisites

- Java 17+
- Maven 3.8+
- MySQL 8+ running on `localhost:3306`

---

## How to Run

### Option A — With the shared OOAD database (recommended)

```cmd
REM 1. Run the DB team's schema (one time only)
mysql -u root -p < path\to\database_module\schema.sql

REM 2. Build
cd rfid_tracker
mvn clean package -DskipTests

REM 3. Run
java -jar target\rfid-tracker-jar-with-dependencies.jar
```

### Option B — Standalone / local fallback

```cmd
REM 1. Set up local DB
mysql -u root -p < database_setup.sql

REM 2. Build and run
mvn clean package -DskipTests
java -jar target\rfid-tracker-jar-with-dependencies.jar
```

The app tries the OOAD database first, then falls back to `scm_rfid_db` automatically. The status bar will show `● scm_rfid_db (local)` when in fallback mode.

### Changing DB credentials

Edit these lines in `DatabaseManager.java` if your MySQL password isn't `root`:

```java
private static final String DB_USER     = "root";
private static final String DB_PASSWORD = "root";
```

---

## Quick Command Reference

```cmd
REM Build
mvn clean package -DskipTests

REM Run (fat jar)
java -jar target\rfid-tracker-jar-with-dependencies.jar

REM Run via Maven
mvn exec:java

REM Compile only
mvn compile

REM Clean build artifacts
mvn clean
```

---

## Database Tables

| Table | Access | Purpose |
|-------|--------|---------|
| `barcode_rfid_events` | Write | Logs every scan event |
| `subsystem_exceptions` | Write | Logs every RFIDException |
| `products` | Read | Product lookup by tag (owned by Inventory subsystem) |

---

## Facade API

Other subsystems can integrate via `RFIDSystemFacade`:

```java
RFIDSystemFacade facade = RFIDSystemFacade.getInstance();
facade.submitScan(rfidTag, "RFID");  // Submit a scan
facade.getTodaySummary();            // Today's scan summary
facade.getAllScanLogs();             // Full scan history
facade.getRecentScans(10);          // Last N scans
```

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| `DB_CONNECTION_FAILED` on startup | MySQL not running, or wrong credentials in `DatabaseManager.java` |
| `Table 'OOAD.barcode_rfid_events' doesn't exist` | Run the DB team's `schema.sql` first |
| Falls back to `scm_rfid_db` | OOAD DB not reachable — run `database_setup.sql` for local fallback |
| Build fails on Java version | Ensure `JAVA_HOME` points to Java 17+; verify with `java -version` |
