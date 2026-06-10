# Aivira Frontend

React/Vite frontend for the Aivira single-vendor bookstore. The frontend talks to the Spring Boot API through `/api/v1` by default and keeps bookstore wording while backend routes remain stable.

## Prerequisites

- Node.js 20 or newer. Development was verified with Node `22.20.0`.
- npm 10 or newer.
- Optional: local backend running at `http://localhost:8080`.

## Install

```powershell
npm ci
```

## Environment

Copy `.env.example` to `.env.local` for local overrides:

```powershell
Copy-Item .env.example .env.local
```

Variables:

- `VITE_API_BASE_URL`: frontend API base URL. Keep `/api/v1` for same-origin local dev or reverse-proxy production.
- `VITE_DEV_PROXY_TARGET`: Vite dev proxy target for `/api/**`, default `http://localhost:8080`.

Examples:

```env
# Local backend through Vite proxy
VITE_API_BASE_URL=/api/v1
VITE_DEV_PROXY_TARGET=http://localhost:8080

# Split frontend/backend domains
VITE_API_BASE_URL=https://api.example.com/api/v1
```

Do not put secrets in `VITE_*` variables. Vite exposes them to the browser bundle.

## Local Development

Run the backend on `localhost:8080`, then:

```powershell
npm run dev
```

Open `http://localhost:5173`.

If the backend uses a different local port:

```powershell
$env:VITE_DEV_PROXY_TARGET="http://localhost:9090"
npm run dev
```

## Build And Preview

```powershell
npm run build
npm run preview
```

Preview runs on `http://localhost:4173`.

## Tests And Quality

```powershell
npm run test
npm run test:e2e
npm run quality
npm run lint
npm run format:check
npm run ci
```

Notes:

- Unit/component tests use Vitest, React Testing Library, and MSW.
- E2E smoke tests use Playwright with mocked `/api/v1/**` responses, so they do not require MySQL or the backend.
- If Playwright browsers are missing, run:

```powershell
npx playwright install chromium
```

## Deployment

### Same-Origin Reverse Proxy

Recommended when frontend and backend share one public domain:

- Build static assets from `frontend/dist`.
- Serve the SPA from the web server.
- Proxy `/api/v1/**` to the Spring Boot backend.
- Keep `VITE_API_BASE_URL=/api/v1`.

### Split Frontend And Backend Domains

Use this when the static frontend and API are deployed on separate domains:

- Set `VITE_API_BASE_URL=https://api-domain.example.com/api/v1` before build.
- Configure backend CORS to allow the frontend origin.
- Configure refresh cookie `secure`, `sameSite`, and `domain` for the deployment domain model.

### Payment Return URLs

- VNPay/MoMo browser return URLs should land on the frontend `/payment-result` route.
- Provider callback/IPN URLs remain backend URLs.

## Troubleshooting

- `401` after login or refresh: verify backend CORS allows credentials and refresh cookie settings match the frontend origin.
- Checkout payment return opens a blank or wrong page: verify provider return URL points to `/payment-result`.
- Local API calls go to the wrong backend: check `VITE_DEV_PROXY_TARGET` and restart Vite.
- Playwright cannot launch a browser: run `npx playwright install chromium`.
