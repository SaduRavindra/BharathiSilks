# Bharathi Silks — Pending Items

Drawn from the PRD's limitations + roadmap and the unchecked manual-verification
items from PR #3. None of these are started yet.

## Production blockers
- [ ] **Durable database** — swap in-memory H2 (`create-drop`, resets every
  restart) for file-backed H2 or Postgres. Config-only; JPA layer already in place.
- [ ] **Set prod secrets** — real `JWT_SECRET` (≥32 chars) and
  `otp.expose-code=false` so OTPs aren't returned/logged.

## Auth & access
- [ ] **Enforce roles** — `AppUser.role` exists but isn't checked; every signed-in
  user has full console access. Gate owner-only vs. counter-staff actions.
- [ ] **Real OTP delivery** — wire an SMS gateway; today the code is only exposed
  in dev mode, never sent.

## Features (roadmap)
- [ ] **Online payments** — UPI/Razorpay + settlement reconciliation.
- [ ] **WhatsApp auto-send** — currently only opens a chat draft; move to WhatsApp
  Business API.
- [ ] **Multi-store** inventory and transfers.
- [ ] **Audit log** for stock and price changes.

## Manual verification (left unchecked on PR #3)
- [ ] Browser pass: storefront, login, and mobile console layout.
- [ ] Live Google consent round-trip with a real client id/secret on localhost.
