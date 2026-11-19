# Privacy policy hosting — status and Play Console handoff

## What is live now (verified 5 November 2026)

| | |
|---|---|
| URL | **https://repio.grafizum.com/privacy** |
| Also served | `https://repio-legal.matrix-gian.workers.dev/privacy` (same bytes) |
| Legacy copy | `https://repio-8b15b.web.app/` — the pre-Cloudflare Firebase Hosting policy, last updated 10 August 2026 (root path only; `/privacy` 404s there). Superseded by the page above, kept as a mirror. |
| Served by | Cloudflare Worker `repio-legal` |
| Source | `site/privacy/index.html`, `site/terms/index.html`, `site/legal-notice/index.html`, `site/index.html` |
| Verified | HTTP 200, `content-type: text/html; charset=utf-8`, body sha256 `3ba1f7de…db22` identical to the repo file, `server: cloudflare`, TLS valid, no cookies, no scripts |
| Hub | `/` returns 200; unknown paths return 404 |

The earlier blocker — `repio.grafizum.com` not resolving because the API-visible
account was not the one answering DNS — is **resolved**. The host now resolves to
Cloudflare and serves the Worker. `docs/` notes from 2026-10-19 describing that
failure are obsolete.

## Live routes vs authored routes

| Route | Source | Status |
|---|---|---|
| `/privacy` | `site/privacy/index.html` | **Live**, 200 |
| `/` | `site/index.html` | **Live**, 200 (hub now links all three documents) |
| `/terms` | `site/terms/index.html` | Authored, **404 today** — needs a Worker redeploy |
| `/legal-notice` | `site/legal-notice/index.html` | Authored, **404 today** — needs a Worker redeploy |

New routes appear only after the `repio-legal` Worker is redeployed with the
updated `site/` tree. Nothing else is required: the pages are plain HTML with no
build step, no scripts and no external assets.

## In the app

`AppLinks.PRIVACY_POLICY` in
`composeApp/src/main/java/com/grafizum/repio/util/AppLinks.kt` now points at
`https://repio.grafizum.com/privacy`. Nothing else in the codebase refers to the
address.

## Play Console

Policy → App content → Privacy policy → paste
`https://repio.grafizum.com/privacy` → Save. The URL must stay publicly
reachable, must not sit behind a login or Cloudflare Access, and must be a
web page rather than a PDF.

## Constraints

Do not touch the apex site, `dashboard.grafizum.com` or `mcp.grafizum.com`. Keep
`privacy@grafizum.com` monitored — the policy names it as the contact for data
requests.
