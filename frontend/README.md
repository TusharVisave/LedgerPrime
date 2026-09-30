# LedgerPrime — Frontend

React 18 + TypeScript + Vite SPA for the LedgerPrime (Java full-stack conversion).
Pairs with the Spring Boot backend in `../backend/`. The backend is the source of truth
for the API contract; this app implements it exactly.

## Stack

- React 18 + TypeScript + Vite
- Tailwind CSS v3.4 (postcss + autoprefixer)
- axios (`withCredentials: true` — cookie-based JWT, no tokens in localStorage)
- react-router-dom v6, recharts, lucide-react, react-hot-toast
- vite-plugin-pwa (service worker + web manifest; see "PWA" below)

## Prerequisites

- Node 24 / npm 10
- The backend running (default: `http://localhost:8080`)

## Run

```bash
npm install
npm run dev        # http://localhost:5173
npm run build      # typecheck + production build
npm run preview    # serve the production build
```

## Configure

Copy `.env.example` to `.env` and point `VITE_API_URL` at the backend:

```
VITE_API_URL=http://localhost:8080/api
```

## Structure

```
src/
├── main.tsx                 # entry: AuthProvider, Toaster, styles
├── App.tsx                  # routes + Protected wrapper
├── api/
│   ├── client.ts            # axios instance (withCredentials), 401 handling, blob download
│   ├── types.ts             # DTO types mirroring the backend contract
│   ├── auth.ts              # register/login/refresh/logout/me
│   ├── transactions.ts      # list (filters+pagination) / create / update / delete /
│   │                        # parse (NL quick-add), receipts scan/attach/view, CSV import
│   ├── goals.ts             # goals CRUD + allocate
│   ├── subscriptions.ts     # recurring-reminder subscriptions + monthly totals
│   ├── categories.ts        # list / create / update / delete
│   ├── budgets.ts           # get / upsert (optional category + rollover) / category budgets
│   ├── dashboard.ts         # dashboard summary endpoint (incl. category budgets)
│   ├── reminders.ts         # list / upcoming / create / update / delete
│   ├── insights.ts          # monthly insights (incl. anomalies) + AI report
│   └── exports.ts           # PDF + Excel blob downloads
├── context/
│   └── AuthContext.tsx      # {user} from GET /api/auth/me; login/register/logout
├── components/
│   ├── Layout.tsx           # sidebar (md+) + mobile top bar + bottom tab bar
│   ├── TransactionModal.tsx # add/edit transaction with validation, receipt attach/view
│   ├── GoalModal.tsx        # create/edit savings goal
│   ├── AllocateModal.tsx    # allocate money to a goal (amount + note + date)
│   ├── CategoryModal.tsx    # add/edit category
│   ├── ReminderModal.tsx    # add/edit reminder (toggles, recurrence)
│   ├── Modal.tsx            # generic dialog shell
│   ├── ConfirmDialog.tsx    # delete confirmation
│   ├── Pagination.tsx       # compact page controls
│   ├── Skeleton.tsx         # animate-pulse loading blocks
│   └── EmptyState.tsx       # icon + text + CTA
├── pages/
│   ├── AuthPage.tsx         # /login — split-screen sign in / sign up
│   ├── DashboardPage.tsx    # / — summary cards, donut, 6-month trend, budget, insights, recent
│   ├── TransactionsPage.tsx # /transactions — filters, table, modal, exports,
│   │                        # NL quick-add bar, receipt scan, CSV import
│   ├── GoalsPage.tsx        # /goals — goal cards with progress bars, allocate
│   ├── SubscriptionsPage.tsx# /subscriptions — recurring costs + monthly total
│   ├── BudgetsPage.tsx      # /budgets — monthly budget + per-category budgets
│   │                        # (optional category + rollover checkbox)
│   ├── ProfilePage.tsx      # /profile — account, change password, log out all devices
│   ├── CategoriesPage.tsx   # /categories — card grid, default-delete disabled
│   └── RemindersPage.tsx    # /reminders — due-date status styling
└── utils/
    └── format.ts            # INR formatting (en-IN), date/month labels, helpers
```

## Auth model

- JWT lives in httpOnly cookies set by the backend. The axios client sends
  `withCredentials: true` on every request and never reads/writes tokens to
  localStorage.
- `AuthContext` probes `GET /api/auth/me` on load. Any 401 from another call
  clears the user and the `Protected` wrapper redirects to `/login`.

## Notes

- Amounts arrive from the API as decimal strings (e.g. `"1250.50"`). They are
  parsed with `Number()` **only for display** via `Intl.NumberFormat("en-IN",
  { style: "currency", currency: "INR", maximumFractionDigits: 2 })`.
- PDF/Excel exports download via `responseType: "blob"` + object URL.
- Multipart uploads (receipt images, CSV import) use `FormData` with a
  per-request `Content-Type` override — the axios default `application/json`
  header is bypassed for those calls only.
- Password policy hint: minimum 8 characters, with at least one letter and one
  digit (shown under the password field on registration and change-password).
- Login may return 429 ("Account locked…") after 5 failed attempts; the backend
  message is surfaced verbatim in a toast.

## PWA

The app is installable as a PWA:

- `vite-plugin-pwa` generates `dist/sw.js` (Workbox, `generateSW`) and
  `dist/manifest.webmanifest` at build time.
- Manifest: `name: "LedgerPrime"`, `short_name: "Expenses"`,
  `theme_color`/`background_color: #09090b` (matches the dark theme),
  icons in `public/` (`pwa-192.svg`, `pwa-512.svg` + maskable variants).
- The service worker precaches the app shell (`index.html` + hashed assets)
  with a navigation fallback to `index.html`.
- `/api` requests use a **NetworkOnly** runtime-caching rule — API calls are
  never served offline-first (the JWT lives in cookies and data must always
  come from the backend).
- Registration happens in `src/main.tsx` via `virtual:pwa-register`.

Note: SVG icons keep the install footprint tiny; Chrome's installability
check accepts them, but if the Play Store / TWA route is needed later, swap in
192/512 PNGs with `purpose: "any maskable"`.
