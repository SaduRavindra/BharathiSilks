# Bharathi Silks — Product Requirements Document

| | |
|---|---|
| **Product** | Bharathi Silks — Retail storefront + POS & admin console |
| **Status** | Living document (reflects the current implementation) |
| **Last updated** | 2026-05-28 |
| **Repository** | `SaduRavindra/BharathiSilks` |

---

## 1. Overview

Bharathi Silks is a single-shop retail application for a silk/apparel boutique. It
combines three surfaces in one Spring Boot app:

1. **Public storefront** (`/`) — a catalogue customers can browse, with retail
   prices but no cost/margin data exposed.
2. **Admin console** (`/admin`) — an authenticated single-page app for running the
   shop: inventory, point-of-sale billing, stock purchases, customers & loyalty,
   barcode labels, and reports.
3. **JSON API** (`/api/**`) — the backend the two UIs talk to.

The goal is to let a small shop run day-to-day operations (sell, restock, track
customers, print labels, read reports) from a phone, tablet, or desktop without
any third-party POS subscription.

---

## 2. Goals & Non-Goals

### Goals
- Run a complete sale (cart → discount/loyalty → payment → receipt) in seconds.
- Keep live stock counts that decrement on sale and restore on return.
- Give the owner at-a-glance business health (revenue, best-sellers, dead stock).
- Hide cost/margin from any customer-facing surface.
- Work on a phone at the counter and on a desktop in the back office.
- Be deployable as a single self-contained JAR.

### Non-Goals (today)
- Multi-store / multi-branch inventory.
- Real SMS/email delivery (OTP is dev-mode; WhatsApp opens a pre-filled chat).
- Online payments / payment-gateway settlement.
- Granular role-based permissions (the `role` field exists but is not yet enforced).
- Durable production database (current persistence is in-memory H2 — see §11).

---

## 3. Personas

| Persona | Needs |
|---|---|
| **Shop owner** | Full view of inventory, sales, customers, reports; manage products and stock. |
| **Counter staff** | Fast billing, barcode scan, accept Cash/UPI/Card, print receipt. |
| **Walk-in customer** | Browse the public catalogue; optionally enrol in loyalty by phone at billing. |

---

## 4. System Architecture

- **Backend:** Java 17, Spring Boot (Web, Security, Data JPA), Lombok.
- **Persistence:** Spring Data JPA over **H2 in-memory** (`create-drop`, seeded on
  startup). Swappable for a persistent datasource without code changes (see §11).
- **Auth:** Stateless JWT (HMAC-signed). Two sign-in methods: Google OAuth2
  (server-side redirect) and phone OTP.
- **Frontend:** Two self-contained HTML pages served as static resources
  (`index.html` storefront, `admin.html` console) — vanilla HTML/CSS/JS, no build step.
  Barcodes rendered client-side (Code-128).
- **Packaging:** Single executable Spring Boot JAR; static UI bundled inside.

```
Browser ──/ , /admin──▶ Static HTML/CSS/JS
   │
   └──/api/**──▶ Controllers ─▶ Services (RetailRules) ─▶ JPA Repositories ─▶ H2
                    │
                JwtAuthFilter (validates Bearer token, except public routes)
```

---

## 5. Authentication & Authorization

- **JWT:** HMAC-signed, 24h TTL (`jwt.ttl-ms`). Sent as `Authorization: Bearer <token>`.
  Secret overridable via `JWT_SECRET` (must be ≥ 32 chars).
- **Google sign-in:** Server-side OAuth2 redirect. Flow:
  1. Browser → `/oauth2/authorization/google` → Google consent.
  2. Google → `/login/oauth2/code/google`; `OAuth2SuccessHandler` upserts the
     `AppUser`, mints a JWT, and stores a **single-use, short-lived login code**
     (`auth.login-code.ttl-seconds`, default 120s).
  3. Redirect to `/admin#code=<code>`; the SPA calls `POST /api/auth/exchange`
     to swap the code for the JWT (code is consumed on first use).
  - Registered only when `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` are set;
    the app boots normally without them and hides the Google button.
- **Phone OTP:** `POST /api/auth/otp/request` issues a code (TTL `otp.ttl-seconds`,
  default 300s). No SMS gateway is wired, so in dev (`otp.expose-code=true`) the code
  is returned/logged. `POST /api/auth/otp/verify` returns the JWT.
- **Authorization model:** Any authenticated principal has full console access today.
  `AppUser.role` is persisted for future RBAC but is not yet enforced.
- **Gating:** `/api/public/**`, the auth endpoints, and static assets are open;
  every other `/api/**` route requires a valid JWT (`RestAuthEntryPoint` returns 401 JSON).

---

## 6. Functional Requirements

### 6.1 Public Storefront (`/`)
- Landing page with brand presentation and a product catalogue.
- Lists products via `GET /api/public/products`, which returns a **cost-free**
  projection (`PublicProductView`) — name, SKU, category, size, colour, price, stock.
- No authentication required; never exposes cost or margin.

### 6.2 Admin Console (`/admin`)
- Authenticated SPA with a left nav (collapses to a scrollable top bar on mobile).
- Shows the signed-in user (name, avatar, role) and offers sign-out.
- **Dashboard:** KPI cards (revenue, products, units, customers) and a
  "Low & out of stock" panel.

### 6.3 Inventory / Products
- List, create, update, delete products.
- A product has: name, category, size, colour, cost, price, stock, GST%, SKU.
- **SKU is server-assigned** from a category prefix (e.g. `SAR`, `LEH`) + sequence.
- **GST is server-assigned** by price band (see §8).
- Filter by category; stock status badges (in stock / low / out).
- API: `GET/POST /api/products`, `PUT /api/products/{sku}`, `DELETE /api/products/{sku}`.

### 6.4 Billing / Point of Sale
- Build a cart (quick-add tiles or barcode/SKU scan), set quantities.
- Apply a discount (percentage or flat — `discType`).
- Optional customer by phone; optionally **redeem loyalty points** (1 point = ₹1,
  capped at the discounted subtotal).
- GST is computed per line and added to the total.
- Choose payment mode: **Cash / UPI / Card**.
- On submit (`POST /api/sales`): decrements stock, generates an invoice number,
  records the sale, and updates the customer's loyalty.
- Prints a receipt.
- **Returns:** `POST /api/sales/{inv}/return` restocks items and reverses the
  loyalty points earned on that bill.
- API: `GET/POST /api/sales`, `POST /api/sales/{inv}/return`.

### 6.5 Purchases / Stock-in
- Record incoming stock for a product (qty, cost/unit, supplier); increments stock.
- Shows recent stock-ins.
- API: `GET/POST /api/purchases`.

### 6.6 Customers & Loyalty
- Auto-enrolled at billing when a phone number is supplied.
- Tracks visits, lifetime spend, and points.
- **Earn rate:** 1 point per ₹200 spent (`POINTS_PER`).
- Per-customer **WhatsApp** action opens a pre-filled `wa.me` chat (no API send).
- API: `GET /api/customers`, `GET /api/customers/{phone}`.

### 6.7 Labels & Barcodes
- Choose copies per product and print a sheet of labels.
- Each label carries a real **Code-128 barcode** of the SKU, scannable at billing.

### 6.8 Reports & Analytics
- Selectable window (7 / 30 / 90 days / all time).
- Revenue, best-sellers, payment-mode mix, dead stock (tied-up value), recent invoices.
- API: `GET /api/reports`.

### 6.9 Admin utilities
- **Reset demo data:** `POST /api/admin/reset` re-seeds the sample catalogue.
- **State snapshot:** `GET /api/state` returns a combined snapshot the SPA hydrates from.

---

## 7. API Reference (summary)

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/api/public/products` | Public | Cost-free catalogue for the storefront |
| GET | `/api/auth/config` | Public | Which sign-in methods are enabled |
| POST | `/api/auth/otp/request` | Public | Request a phone OTP |
| POST | `/api/auth/otp/verify` | Public | Verify OTP → JWT |
| POST | `/api/auth/exchange` | Public | Swap one-time Google code → JWT |
| GET | `/api/auth/me` | JWT | Current signed-in user |
| POST | `/api/auth/logout` | JWT | Client-side session end |
| GET / POST | `/api/products` | JWT | List / create products |
| PUT / DELETE | `/api/products/{sku}` | JWT | Update / delete a product |
| GET / POST | `/api/sales` | JWT | List sales / create a bill |
| POST | `/api/sales/{inv}/return` | JWT | Return a bill (restock + reverse points) |
| GET / POST | `/api/purchases` | JWT | List / record stock-in |
| GET | `/api/customers` | JWT | List loyalty customers |
| GET | `/api/customers/{phone}` | JWT | Look up a customer |
| GET | `/api/reports` | JWT | Sales/inventory analytics |
| GET | `/api/state` | JWT | Combined state snapshot |
| POST | `/api/admin/reset` | JWT | Re-seed demo data |

---

## 8. Business Rules

- **GST:** 5% for price ≤ ₹1000, 12% above ₹1000 (`RetailRules.gstRate`).
- **Loyalty earn:** 1 point per ₹200 spent; redeemable at 1 point = ₹1, capped at
  the discounted subtotal. Returns reverse points proportionally to the bill total.
- **Low-stock threshold:** stock ≤ 3 is flagged "low"; 0 is "out".
- **SKU:** `<category-prefix><sequence>`; prefixes — Sarees `SAR`, Lehengas `LEH`,
  Dresses `DRS`, Kurtis `KUR`, Blouses `BLO`, Other `OTH`.
- **Categories:** Sarees, Lehengas, Dresses, Kurtis, Blouses, Other.

---

## 9. Data Model

| Entity | Key fields |
|---|---|
| **Product** | sku (unique), name, category, size, colour, cost, price, stock, gst, created |
| **AppUser** | subject (unique, e.g. `google:<sub>` / `phone:<digits>`), name, email, phone, picture, provider, role, created |
| **Customer** | phone, name, visits, spend, points |
| **Sale** | invoice no., date, items, customer, payment mode, discount, redeem, total |
| **SaleItem** | sku, name, qty, price, gst |
| **Purchase** | date, sku/name, qty, cost, supplier |
| **Counter** | named sequence source for invoice numbers / SKUs |

---

## 10. Non-Functional Requirements

- **Security:** Cost/margin never leaves a customer-facing endpoint; all business
  routes JWT-gated; single-use OAuth codes; configurable JWT secret.
- **Responsive:** Console and storefront usable on phone, tablet, and desktop;
  wide tables scroll within their panel on small screens.
- **Resilience of UX:** Empty states for every list (no products, no sales, etc.).
- **Portability:** One JAR, no external services required to boot (Google optional).
- **Performance:** In-memory data; single-shop scale.

---

## 11. Current Limitations

- **Persistence is in-memory H2** (`create-drop`): all data resets on restart and is
  re-seeded. Suitable for demo/dev, **not** production.
- **OTP is not delivered** (no SMS gateway); the code is exposed in dev mode.
- **No enforced roles** — every signed-in user has full console access.
- **WhatsApp** opens a chat draft; it does not send automatically.

---

## 12. Configuration

| Property / env | Default | Purpose |
|---|---|---|
| `server.port` | 8080 | HTTP port |
| `JWT_SECRET` | dev secret | HMAC signing key (set in prod, ≥ 32 chars) |
| `jwt.ttl-ms` | 86400000 | JWT lifetime (24h) |
| `auth.login-code.ttl-seconds` | 120 | One-time Google code lifetime |
| `otp.ttl-seconds` | 300 | OTP lifetime |
| `otp.expose-code` | true | Dev only — return/log the OTP |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | empty | Enable Google sign-in |
| `app.oauth2.redirect` | `/admin` | Post-login landing page |

> Google redirect URI to register: `http://<host>/login/oauth2/code/google`.

---

## 13. Roadmap / Future Enhancements

1. **Durable database** — switch H2 to file-backed or Postgres so data survives
   restarts (config-only change; JPA layer already in place).
2. **Role-based access** — enforce `AppUser.role` (owner vs. counter staff).
3. **Real OTP/SMS delivery** and WhatsApp Business API sends.
4. **Online payments** (UPI/Razorpay) and settlement reconciliation.
5. **Multi-store** inventory and transfers.
6. **Audit log** for stock and price changes.
