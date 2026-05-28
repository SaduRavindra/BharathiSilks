# Bharathi Silks

Point-of-sale and inventory console for a silk/saree retail store, backed by a
Spring Boot + Java REST API. The single-page admin UI (`index.html`) runs against
the API, which owns all the business rules: SKU minting, GST, discounts, loyalty
points, stock movements and returns.

## Stack

- Java 21, Spring Boot 3.4 (Web + Data JPA)
- H2 in-memory database (seeded with demo data on startup; resets on restart)
- Maven build; static UI served by the same server

## Run

```bash
mvn spring-boot:run
```

Then open <http://localhost:8080/>. The H2 console is at
<http://localhost:8080/h2-console> (JDBC URL `jdbc:h2:mem:bharathi`, user `sa`, no
password).

Build a runnable jar instead:

```bash
mvn clean package
java -jar target/bharathi-silks-0.0.1-SNAPSHOT.jar
```

Run tests:

```bash
mvn test
```

## Business rules

- **SKU**: auto-minted per category, e.g. `BS-SAR-0007` (`SAR/LEH/DRS/KUR/BLO/OTH`).
- **GST**: 5% up to ₹1000, 12% above (Indian apparel).
- **Loyalty**: 1 point per ₹200 of bill total; points reverse on return.
- **Totals**: subtotal, discount (% or ₹), GST and point redemption are all priced
  server-side from live product records — the client only sends SKUs and quantities.

## API

| Method | Path | Purpose |
| ------ | ---- | ------- |
| GET | `/api/state` | Full snapshot (products, sales, purchases, customers, counters) |
| GET | `/api/products` | List products |
| POST | `/api/products` | Create product (server assigns SKU + GST) |
| PUT | `/api/products/{sku}` | Update price / stock |
| DELETE | `/api/products/{sku}` | Delete product |
| GET / POST | `/api/purchases` | List / record incoming stock |
| GET / POST | `/api/sales` | List / complete a sale |
| POST | `/api/sales/{inv}/return` | Return a bill (restock + reverse loyalty) |
| GET | `/api/customers` | List loyalty members |
| GET | `/api/customers/{phone}` | One customer |
| GET | `/api/reports?days=30` | Revenue, profit, best sellers, payment mix, dead stock |
| POST | `/api/admin/reset` | Wipe and reseed demo data (OWNER role required) |

## Layout

```
src/main/java/com/bharathisilks
├── domain      JPA entities (Product, Sale, SaleItem, Purchase, Customer, Counter)
├── repo        Spring Data repositories
├── service     Business logic (pricing, billing, loyalty, reports, seeding)
├── web         REST controllers + request/response DTOs
├── error       Exception handling
└── config      Startup demo-data seeder
src/main/resources/static/index.html   Admin UI (calls the API)
```
