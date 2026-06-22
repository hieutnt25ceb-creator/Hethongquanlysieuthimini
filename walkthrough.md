# Mini Mart Management System — Walkthrough

A complete Java Desktop Client/Server application for mini-mart management, built to Grade A+ requirements.

---

## Project Structure

```
d:\MiniMart\
├── pom.xml                           ← Parent Maven POM (multi-module)
├── db/
│   ├── minimart_schema.sql           ← Full database schema + sample data
│   └── update_passwords.sql          ← BCrypt password helper
├── minimart-common/                  ← Shared DTOs & Models
├── minimart-server/                  ← Server application
└── minimart-client/                  ← JavaFX client application
```

---

## Quick Start

### Step 1 — Initialize Database

```sql
-- In MySQL Workbench or MySQL CLI:
source d:/MiniMart/db/minimart_schema.sql
```

> **Note:** The sample BCrypt hashes in the schema are placeholders. Run the server once to auto-generate correct hashes, **or** update them with `update_passwords.sql`.

### Step 2 — Configure Database Password

Edit [ConnectionManager.java](file:///d:/MiniMart/minimart-server/src/main/java/com/minimart/server/config/ConnectionManager.java) or pass JVM args at runtime:

```
-Ddb.password=YOUR_MYSQL_PASSWORD
```

### Step 3 — Build the Project

```bash
cd d:\MiniMart
mvn clean install -DskipTests
```

### Step 4 — Start the Server

```bash
java -jar minimart-server/target/minimart-server.jar
# Override defaults:
java -Dserver.port=9999 -Ddb.password=secret -jar minimart-server/target/minimart-server.jar
```

### Step 5 — Start the Client

```bash
java --module-path "C:\path\to\javafx-sdk\lib" --add-modules javafx.controls,javafx.fxml,javafx.charts \
  -jar minimart-client/target/minimart-client.jar
```

Or run via Maven:
```bash
cd minimart-client
mvn javafx:run
```

### Step 6 — Login

| Role     | Username    | Password   |
|----------|-------------|------------|
| Admin    | `admin`     | `Admin@123`|
| Employee | `nhanvien1` | `Emp@123`  |

---

## Feature Map

| Feature | Location |
|---|---|
| BCrypt password hashing | [BCryptUtil.java](file:///d:/MiniMart/minimart-server/src/main/java/com/minimart/server/security/BCryptUtil.java) |
| AES phone encryption | [AESUtil.java](file:///d:/MiniMart/minimart-server/src/main/java/com/minimart/server/security/AESUtil.java) |
| Session tokens | [TokenManager.java](file:///d:/MiniMart/minimart-server/src/main/java/com/minimart/server/security/TokenManager.java) |
| HikariCP connection pool | [ConnectionManager.java](file:///d:/MiniMart/minimart-server/src/main/java/com/minimart/server/config/ConnectionManager.java) |
| Multi-threaded server | [ServerMain.java](file:///d:/MiniMart/minimart-server/src/main/java/com/minimart/server/ServerMain.java) |
| Client handler (per-thread) | [ClientHandler.java](file:///d:/MiniMart/minimart-server/src/main/java/com/minimart/server/network/ClientHandler.java) |
| Request routing | [RequestDispatcher.java](file:///d:/MiniMart/minimart-server/src/main/java/com/minimart/server/network/RequestDispatcher.java) |
| **DB Transaction (order)** | [OrderService.java](file:///d:/MiniMart/minimart-server/src/main/java/com/minimart/server/service/OrderService.java) |
| JAXB XML export/import | [ProductService.java](file:///d:/MiniMart/minimart-server/src/main/java/com/minimart/server/service/ProductService.java) |
| Structured logging | [StructuredLogger.java](file:///d:/MiniMart/minimart-server/src/main/java/com/minimart/server/log/StructuredLogger.java) |
| TCP socket client | [ServerCommunicator.java](file:///d:/MiniMart/minimart-client/src/main/java/com/minimart/client/network/ServerCommunicator.java) |
| Client service gateway | [ClientService.java](file:///d:/MiniMart/minimart-client/src/main/java/com/minimart/client/service/ClientService.java) |
| Login UI + async Task | [LoginController.java](file:///d:/MiniMart/minimart-client/src/main/java/com/minimart/client/ui/LoginController.java) |
| Admin Dashboard + BarChart | [AdminDashboardController.java](file:///d:/MiniMart/minimart-client/src/main/java/com/minimart/client/ui/AdminDashboardController.java) |
| POS checkout screen | [POSController.java](file:///d:/MiniMart/minimart-client/src/main/java/com/minimart/client/ui/POSController.java) |

---

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│  CLIENT APP (minimart-client)                                       │
│                                                                     │
│  JavaFX UI (LoginController, AdminDashboardController, POSController)│
│       ↕  (JavaFX Task — background thread, non-blocking)           │
│  ClientService.getInstance()                                        │
│       ↕  (synchronized JSON over TCP)                              │
│  ServerCommunicator (singleton Socket)                              │
└─────────────────────────────┬───────────────────────────────────────┘
                              │  TCP/IP : Port 9999
                              │  Protocol: newline-delimited JSON
┌─────────────────────────────▼───────────────────────────────────────┐
│  SERVER APP (minimart-server)                                       │
│                                                                     │
│  ServerMain → ServerSocket → ExecutorService (50 threads)           │
│       → ClientHandler (Runnable, per connection)                    │
│       → RequestDispatcher (routes action → service)                 │
│                                                                     │
│  Services: Auth, Product, Customer, Order, Dashboard, User, Log     │
│  Security: BCryptUtil, AESUtil, TokenManager                        │
│       ↕  (JDBC + PreparedStatement)                                │
│  DAO Layer: UserDAO, ProductDAO, CustomerDAO, OrderDAO, etc.        │
│       ↕  (HikariCP pool)                                           │
│  ConnectionManager → MySQL 8.0                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Key Technical Highlights

### Transactional Order Creation
[OrderService.createOrder()](file:///d:/MiniMart/minimart-server/src/main/java/com/minimart/server/service/OrderService.java#L60-L110) implements a 4-step atomic transaction:
1. **Validate** stock for each item
2. **Insert** order header (`orders`)
3. **Batch-insert** line items (`order_items`)
4. **Decrement** stock for each product (`products`)
→ **COMMIT** on success, **ROLLBACK** on any failure

### AES Phone Encryption
[AESUtil](file:///d:/MiniMart/minimart-server/src/main/java/com/minimart/server/security/AESUtil.java) encrypts phone numbers with AES-128-CBC before DB storage. [CustomerDAO](file:///d:/MiniMart/minimart-server/src/main/java/com/minimart/server/dao/CustomerDAO.java) transparently encrypts on write and decrypts on read.

### Non-blocking UI
All network calls are wrapped in `javafx.concurrent.Task` so the JavaFX Application Thread is never blocked. The `ServerCommunicator` is `synchronized` to prevent concurrent socket interleaving from multiple Tasks.

### XML Import/Export
[ProductService.exportToXml()](file:///d:/MiniMart/minimart-server/src/main/java/com/minimart/server/service/ProductService.java) marshals `ProductListWrapper` (JAXB) to a UTF-8 XML string returned to the client. The client saves it to disk via a `FileChooser`. Import reverses the process with duplicate-code checking.

---

## Customization Points

| Setting | How to change |
|---|---|
| DB host/port | `-Ddb.url=jdbc:mysql://HOST:PORT/minimart_db` |
| DB credentials | `-Ddb.username=X -Ddb.password=Y` |
| Server port | `-Dserver.port=9999` |
| Server threads | `-Dserver.threads=50` |
| AES key/IV | `-DAES_SECRET_KEY=16chars -DAES_IV=16chars` |
| Low-stock threshold | `DashboardService.LOW_STOCK_THRESHOLD` |
| Log file location | `minimart-server/src/main/resources/logback.xml` |
