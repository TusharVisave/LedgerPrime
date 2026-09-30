# LedgerPrime — Java Full-Stack Expense Tracker

A complete Java 21 + Spring Boot 3 backend and React + TypeScript + Tailwind frontend,
converted from the original MERN (MongoDB / Express / React / Node) Expense Tracker.

## What it does

Personal finance tracking: JWT-secured multi-user accounts, income/expense
transactions with tags and search, monthly budgets (overall + per-category,
with rollover) and limit alerts, custom categories, bill reminders with
recurring-expense automation and optional email, a subscriptions view, savings
goals with allocations, receipt scanning (OCR) + CSV import + natural-language
quick entry, a dashboard (summary, category donut, 6-month trend, daily trend,
recent transactions, per-category budget bars), rule-based spending insights
with anomaly alerts, an optional AI monthly report, and PDF/Excel exports.

## Architecture

```
ledgerprime/
├── docker-compose.yml          # postgres + backend, one-command local run
├── backend/                    # Spring Boot 3.2, Java 21, Maven
│   ├── src/main/java/com/expensetracker/
│   │   ├── controller/         # Auth, Transaction, Category, Budget, Dashboard,
│   │   │                       # Reminder, Insight, Export, Health (REST)
│   │   ├── service/            # Business logic, scheduled recurring-reminder job,
│   │   │                       # SMTP / no-op mail notifier
│   │   ├── entity/             # JPA entities: User, Category, Transaction,
│   │   │                       # Budget, Reminder, RefreshToken
│   │   ├── repository/         # Spring Data JPA, JPQL filtered search
│   │   ├── dto/                # Request DTOs (Bean Validation) + response DTOs
│   │   ├── security/           # Cookie JWT filter, refresh rotation,
│   │   │                       # auth rate-limit filter, CORS whitelist config
│   │   ├── exception/          # @RestControllerAdvice → standardized error JSON
│   │   └── util/               # MoneyUtil (paise math), MonthUtil, CookieUtil
│   ├── src/main/resources/
│   │   ├── application.yml     # env-driven config (DB, JWT, CORS, mail)
│   │   └── db/migration/V1__init.sql   # Flyway schema
│   └── src/test/               # H2-backed tests (see below)
└── frontend/                   # React 18 + TypeScript + Vite + Tailwind
    └── src/
        ├── api/                # axios client (withCredentials, cookie auth)
        ├── context/            # AuthContext
        ├── pages/              # Auth, Dashboard, Transactions, Budgets,
        │                       # Categories, Reminders
        └── components/         # cards, charts, tables, modals, skeletons
```

### Key design decisions (correctness fixes over the original)

- **Money as `long` paise** in the DB; the API accepts/returns decimal strings
  (`"1250.50"`) converted via `BigDecimal` — never `float`/`double`.
- **Bean Validation on every request DTO** — bad amounts, missing fields and
  bad emails are rejected with `400` + `fieldErrors` JSON.
- **Cookie JWT auth**: 15-minute access token + rotating refresh tokens in
  `httpOnly` cookies (`SameSite=Lax` by default, configurable via
  `app.security.cookie-same-site`; `Secure` flag via `app.security.cookie-secure`).
  Nothing in `localStorage`.
- **Password policy**: minimum 8 characters, at least one letter and one digit
  (Bean Validation, clear `400` messages on register and change-password).
- **Failed-login lockout**: 5 bad attempts lock the account for 15 minutes;
  further attempts get `429` with a `Retry-After` header and a retry message.
- **Refresh-token reuse detection**: presenting a rotated-out refresh token
  revokes *all* sessions for that user (theft precaution). Plus explicit
  `POST /api/auth/change-password` (requires current password) and
  `POST /api/auth/logout-all` endpoints.
- **Security headers**: `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`,
  `X-XSS-Protection: 1; mode=block`, HSTS (effective once served over TLS).
- **CORS whitelist** from `app.cors.allowed-origins` (comma-separated) —
  never reflect-all-origins.
- **Rate limiting** on `/api/auth/**` (in-memory token bucket, configurable
  via `app.auth.rate-limit-per-minute`).
- **Keyword search escapes LIKE wildcards** (`%`, `_`, `\`), so user input
  can never act as a wildcard.
- Pagination everywhere lists can grow (`page`/`size`, max 100, default 25).

## Run it

### Option A — Docker Compose (PostgreSQL + backend)

```bash
cd ledgerprime
cp .env.example .env    # configure POSTGRES_PASSWORD and JWT_SECRET
docker compose up --build
# backend → http://localhost:8080  (Flyway migrates the schema automatically)
```

Compose automatically loads `.env` for database credentials, ports, and `JWT_SECRET`.

### Option B — local backend + local Postgres

```bash
# 1. start postgres and create db `expensetracker`
# 2. export POSTGRES_HOST/PORT/DB/USER/PASSWORD, JWT_SECRET, CORS_ORIGINS
cd backend
mvn spring-boot:run
```

### Frontend

```bash
cd frontend
cp .env.example .env   # VITE_API_URL=http://localhost:8080/api
npm install
npm run dev            # → http://localhost:5173
```

### Tests (backend, H2 — no Postgres needed)

```bash
cd backend
mvn test
```

Covers: paise money math, transaction repository filters/pagination/keyword
escaping, full auth flow (register → login → me → refresh rotation → logout),
transaction CRUD + validation failures, pagination limits, and an end-to-end
0.10 + 0.20 = 0.30 dashboard check.

## API summary

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/register` | Register (seeds default categories), sets auth cookies |
| POST | `/api/auth/login` | Login, sets auth cookies |
| POST | `/api/auth/refresh` | Rotate refresh token |
| POST | `/api/auth/logout` | Revoke refresh token, clear cookies |
| POST | `/api/auth/logout-all` | Revoke **all** sessions, clear cookies |
| POST | `/api/auth/change-password` | Change password (needs current password), revokes all sessions |
| GET | `/api/auth/me` | Current user |
| GET | `/api/transactions` | Filter (type, category, keyword, amounts, dates) + paginate + sort |
| POST/PUT/DELETE | `/api/transactions[/{id}]` | Create / update / delete (returns budget status) |
| POST | `/api/transactions/parse` | Natural-language draft: `{text}` → `{type, amount, category, date, note}` |
| POST | `/api/transactions/import` | CSV import (multipart `file`); `{importedCount, failedCount, errors[]}` |
| POST | `/api/transactions/{id}/receipt` | Attach receipt image (multipart `image`) |
| GET | `/api/transactions/{id}/receipt` | Download attached receipt image |
| POST | `/api/receipts/scan` | OCR receipt image → suggested draft (`ocrAvailable=false` when tesseract is absent) |
| GET/POST/PUT/DELETE | `/api/goals[/{id}]` | Savings goals CRUD (progress computed server-side) |
| POST | `/api/goals/{id}/allocate` | Allocate money toward a goal |
| GET | `/api/subscriptions` | Derived from recurring reminders + total monthly cost |
| GET/POST/PUT/DELETE | `/api/categories[/{id}]` | Category CRUD (defaults can't be deleted) |
| GET/PUT | `/api/budgets` | Monthly budget + status (spent, remaining, nearLimit, exceeded) |
| GET | `/api/budgets/categories?month=` | Per-category budget progress bars |
| GET | `/api/dashboard?month=` | Summary, category donut data, 6-month trend, daily trend, recent, per-category budgets |
| GET/POST/PUT/DELETE | `/api/reminders[/{id}]` | Reminders CRUD |
| GET | `/api/reminders/upcoming?days=` | Reminders due within N days |
| GET | `/api/insights?month=` | Rule-based insights + `anomalies` (7-day vs 8-week spikes) |
| GET | `/api/insights/anomalies` | On-demand anomaly check |
| POST | `/api/insights/ai-report` | AI monthly report via OpenAI-compatible API (`501` when no key configured) |
| GET | `/api/exports/transactions.pdf?month=` | PDF report download |
| GET | `/api/exports/transactions.xlsx?month=` | Excel report download |
| GET | `/api/health` | Health check |
| GET | `/swagger-ui.html` | OpenAPI docs |

All amounts in JSON are decimal strings (`"1250.50"`); `amountPaise` is also
exposed for exact clients. Errors use `{timestamp,status,error,message,path,fieldErrors}`.

## Notes / additions vs the original

- **Added** (not in the original): refresh-token rotation + reuse detection +
  logout-all, password policy + 5-strike login lockout, per-endpoint rate
  limiting, security headers, `/api/reminders/upcoming`, structured insight
  objects (`type`/`severity`/`title`/`message`), per-category colors on the
  dashboard, savings goals, per-category budgets with rollover, subscriptions
  derived from recurring reminders, receipt OCR scanning + attachments, CSV
  import, natural-language quick entry, anomaly alerts, optional AI monthly
  report, PWA support, OpenAPI docs, Docker Compose, Flyway migrations, H2
  test suite.
- Email stays **disabled by default** (`MAIL_ENABLED=false`), mirroring the
  original's optional email.
- The recurring job runs daily at 08:00 server time and generates recurring
  expense transactions plus upcoming-reminder emails, like the original cron job.
- A budget rollover job runs at 00:05 on the 1st of each month, carrying
  unused budget into the next month for budgets with `rollover: true`.

### Receipt OCR (Tess4J) — degraded mode

`POST /api/receipts/scan` uses Tess4J, which needs the native **tesseract**
library plus `tessdata` language files at runtime. If they are absent, the
endpoint still accepts and stores the image but returns `ocrAvailable: false`
with empty amount/merchant/date — the UI then falls back to manual entry.
To enable real OCR, install tesseract on the host
(e.g. `apt-get install tesseract-ocr`) and point Tess4J at the tessdata dir.

### AI monthly report (BYOK)

`POST /api/insights/ai-report` calls an OpenAI-compatible chat-completions
endpoint (NVIDIA NIM style). Configure `app.ai.base-url`, `app.ai.model` and
`app.ai.api-key` (env `AI_BASE_URL` / `AI_MODEL` / `AI_API_KEY`). With no key
configured the endpoint returns `501` with a clear message — the rest of the
app works fully without it. The key is never logged or returned in responses.

### PWA

The frontend ships a web app manifest and a service worker (Vite PWA plugin):
installable, with the app shell cached for offline loads. API calls are never
served stale — they use network-only strategies.
