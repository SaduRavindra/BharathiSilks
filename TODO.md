# TODO — Single-Store Enhancements Backlog (Textile/Saree Retail)

_Last curated: 2026-05-28 (UTC)_

This backlog is distilled from feature analysis of textile-focused POS products (VasyERP, Line Focus, Reckon Sales, Karni Retail, Ginesys, RetailGraph) plus general tools used by Indian retailers (TallyPrime, Marg, Zoho Inventory/POS, Paytm, Vyapar, Sellbii).

## Priority A (high impact for **single-store** rollout)

- [x] **Variant matrix inventory (fabric → design → color → size) with one parent style** — _shipped_  
  Why: Saree and apparel SKUs are variant-heavy; matrix entry reduces duplicate product maintenance and speeds billing/search.
- [x] **Image-assisted billing tiles in POS** — _shipped (POS + storefront)_  
  Why: Visual lookup improves cashier speed for non-barcode/visually similar items.
- [ ] **Alteration / job-work workflow** (fall/pico, blouse stitching, dyeing, embroidery) with due date + status + charges + vendor tracking.  
  Why: Common textile add-on services currently not modeled in billing lifecycle.
- [ ] **Advanced barcode tooling** (label templates, bulk print presets, reprint by last N receipts, handheld stock-verify mode).
- [ ] **Role and permissions v2** (OWNER, MANAGER, CASHIER) with screen-level + action-level ACL (e.g., reset, delete sale, edit price, refund).
- [ ] **Returns exchange assistant** (return-to-wallet / return-and-exchange / size-color swap) with audit trail.
- [ ] **Offline-first billing queue + sync reconciliation**  
  Why: Keep counter billing alive on intermittent internet and sync reliably after reconnect.
- [ ] **Smart discount engine** (slab offers, category offers, combo offers, coupon codes, festive campaigns).

## Priority B (business control + shrinkage reduction)

- [ ] **Stock audit mode** (PDT/mobile scan count vs system stock, discrepancy posting, variance approval).
- [ ] **Reorder suggestions** using sell-through, age, and min/max levels.
- [ ] **Dead-stock liquidation cockpit** with markdown recommendations and campaign tagging.
- [ ] **Salesperson attribution & commissions** per bill/line item.
- [ ] **Customer segmentation + campaign lists** (high value, dormant, festival buyers, first-time buyers).
- [ ] **WhatsApp automation** for invoice share, order-ready, alteration-ready, loyalty reminders.
- [ ] **Dynamic QR payment integration** from POS amount screen (UPI collect + status polling).
- [ ] **Day-end closure workflow** (cash denomination entry, expected vs actual, variance reason codes).

## Priority C (finance + compliance + ops maturity)

- [ ] **Accounting export connectors** (TallyPrime/Marg-ready sales, purchase, ledger, GST mappings).
- [ ] **GST ops enhancements** (e-Invoice/e-Way export where applicable, validation dashboards, filing support views).
- [ ] **Multi-price lists** (retail, wholesale, VIP, event), with guardrails and approval for manual override.
- [ ] **Supplier purchase intelligence** (vendor-wise margin, late delivery score, purchase trend).
- [ ] **Consignment / memo-out support** for exhibition or partner counters.
- [ ] **Serialized high-value stock controls** (where relevant) and enhanced inventory traceability.
- [ ] **Data backup/restore UX** with scheduled encrypted backups.

## UX / Product polish

- [ ] **Quick keyboard billing mode** (scan-first, minimal mouse/touch, lightning counter flow).
- [ ] **Saved bill drafts + parked bills + split/merge bill** for peak-hour operations.
- [ ] **Customer-facing display mode** (line items, savings, payable, QR).
- [ ] **Receipt template studio** (logo, policy notes, WhatsApp link, bilingual footer).
- [ ] **Local language pack** (Tamil + English labels in key billing screens).

## Suggested implementation sequence for this repo

1. Variant matrix inventory + POS variant selection
2. Job-work/alteration module
3. Role/permission v2 and audit trail hardening
4. Offline billing queue + sync
5. Dynamic QR integration + day-end closure
6. Accounting export pack (Tally/Marg)

## Research notes / source anchors

- Textile-focused:  
  VasyERP: https://vasyerp.com/  
  Line Focus Textile Billing: https://linefocus.com/textiles-and-silk-sarees-shop-billing-software.php  
  Karni Retail (RBW): https://www.rbw.in/software/saree-retail-software/  
  Ginesys retail billing: https://www.ginesys.in/retail-shop-billing-software
- General Indian retail stack inspiration:  
  Tally retail accounting: https://tallysolutions.com/accounting-software/retail/  
  Marg features: https://margcompusoft.com/key_features.html  
  Zoho inventory feature list: https://www.zoho.com/us/inventory/kb/general-overview/zom-feature-list.html  
  Paytm POS docs: https://business.paytm.com/docs/pos-introduction-to-paytm-pos/  
  Vyapar: https://vyaparapp.in/pricing-detail  
  Sellbii: https://sellbii.com/
# Bharathi Silks — Pending Items

_Updated 2026-05-29. Several roadmap items have shipped; remaining work and new
order-management follow-ups are tracked below._

## Shipped recently
- [x] **Role enforcement** — OWNER/STAFF gated in backend + console + demo.
- [x] **Config hardening** — `otp.expose-code` defaults false (dev profile re-enables); `JWT_SECRET` length validated at startup.
- [x] **Pluggable SMS sender** — `SmsSender` seam (logs in dev; a provider drops in later).
- [x] **Categories management** — entity + CRUD, dynamic SKU prefixes, auto-add on import.
- [x] **Invoice import** — paste/CSV with remembered per-supplier mapping, plus AI image/PDF parse (behind `ANTHROPIC_API_KEY`).
- [x] **Audit log** — in-memory trail of catalogue/stock/sale/category/order changes (owner-only).
- [x] **Variant matrix** product creation + **image-assisted** POS and storefront tiles.
- [x] **Online order management** — storefront ordering, tracking by ref + timeline, and a staff Orders tab (PLACED → CONFIRMED → PACKED → SHIPPED → DELIVERED, + CANCELLED).

## Production blockers
- [ ] **Durable database** — swap in-memory H2 (`create-drop`, resets every restart)
  for file-backed H2 or Postgres. Now also affects **orders, the audit log and
  imported products** (all reset on restart). Config-only; JPA layer already in place.
- [ ] **Set prod secrets in deployment** — supply a real `JWT_SECRET` and keep
  `otp.expose-code=false` (defaults are safe; the dev profile opts back in).

## Order management — future
- [ ] **Online payments at checkout** — UPI / Razorpay with order ↔ payment status.
- [ ] **Order → sale on fulfilment** — convert a delivered order into a billed sale that decrements stock (today placing an order does not touch inventory).
- [ ] **Stock reservation** when an order is placed/confirmed.
- [ ] **Order notifications** — WhatsApp/SMS on placed / confirmed / shipped, with a tracking link.
- [ ] **Customer accounts** — login + order history (track without needing the ref).
- [ ] **Returns & exchange from orders** — size/colour swap with audit trail.
- [ ] **Delivery / courier integration** — shipping labels + live courier tracking.

## Auth & access — future
- [ ] **Roles v2 granularity** — OWNER/STAFF shipped; add MANAGER/CASHIER + screen/action-level ACL.
- [ ] **Real OTP/SMS delivery** — implement an `SmsSender` provider (MSG91/Twilio); the seam is ready.
- [ ] **WhatsApp Business API** — auto-send (today opens a chat draft).
- [ ] **Multi-store** inventory and transfers.

## Manual verification
- [ ] Browser pass: storefront ordering/tracking, console Orders tab, role gating, mobile layouts.
- [ ] Live Google consent round-trip with a real client id/secret.
- [ ] Live AI invoice parse with a real `ANTHROPIC_API_KEY`.
