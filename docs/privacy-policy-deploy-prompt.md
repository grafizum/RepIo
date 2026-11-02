# Privacy policy hosting — status and remaining handoff

## What is live now

| | |
|---|---|
| URL | **https://repio-legal.matrix-gian.workers.dev/privacy** |
| Served by | Cloudflare Worker `repio-legal`, account `3212d3605683b29e0f8f18635c210ee7` |
| Source | `site/privacy/index.html` in this repository |
| Verified | HTTP 200, `content-type: text/html; charset=utf-8`, body byte-identical to the file, TLS valid (Google Trust Services), `/` → 200, unknown paths → 404, no cookies, no scripts |
| In the app | Settings → Privacy policy, via `AppLinks.PRIVACY_POLICY` |

That URL is what should go into **Play Console → Policy → App content → Privacy
policy**. It is public, permanent and needs no further work.

## What is not live: the branded host

`repio.grafizum.com` is attached to the same Worker as a custom domain, but it does
**not resolve**. This is not a propagation delay.

Evidence gathered on 2026-10-19:

- A test TXT record (`_repio-check.grafizum.com`) created through the API and
  listed by the API was **NXDOMAIN** on both authoritative nameservers
  (`lakas.ns.cloudflare.com`, `veda.ns.cloudflare.com`) ten seconds and ten minutes
  after creation.
- The same happened to a CNAME for `repio`, and to a Cloudflare Pages project's own
  `*.pages.dev` hostname, which never provisioned either.
- Existing records in the same zone (`dashboard`, `mcp`, DKIM) resolve normally.

So the zone copy held by this Cloudflare account (`aad8020aa097fd939679a640cbf0c910`,
`grafizum.com`, NS `lakas`/`veda`) accepts API writes but is **not the zone that
answers DNS**. The account that actually serves the domain is different, or the
zone was moved and this copy was left behind.

## The remaining task

Give this to an agent (or a person) with access to the Cloudflare account that
**actually serves DNS** for `grafizum.com`:

> Publish the RepIo privacy policy at `https://repio.grafizum.com/privacy`.
>
> The page already exists and is live at
> `https://repio-legal.matrix-gian.workers.dev/privacy`; its source is
> `site/privacy/index.html` in the RepIo repository. It is plain HTML with no
> scripts, no cookies and no external assets.
>
> 1. Confirm you are in the account that answers DNS: `dig +short
>    repio.grafizum.com @lakas.ns.cloudflare.com` must reflect a record you just
>    created. If your API writes do not appear there either, stop and report — do
>    not keep creating records.
> 2. Create a Worker (or Cloudflare Pages project) in that account that serves
>    `site/` so `/privacy` returns the page and `/` returns it too. Deploying
>    `site/` to Pages is enough; no build step exists.
> 3. Attach `repio.grafizum.com` as a custom domain for it. Cloudflare should
>    create the DNS record itself; if not, create a proxied CNAME `repio` →
>    `<project>.pages.dev`.
> 4. Verify publicly, not in the dashboard preview:
>    - `dig +short repio.grafizum.com @lakas.ns.cloudflare.com` returns a value
>    - `curl -sSI https://repio.grafizum.com/privacy` → 200, `text/html`
>    - body contains `RepIo — Privacy Policy` and `privacy@grafizum.com`
>    - certificate valid, no browser warning, no Cloudflare Access policy
> 5. Report the URL and the HTTP status.
>
> Constraints: do not touch the apex site, `dashboard.grafizum.com`,
> `mcp.grafizum.com` or any existing record. Do not add analytics, cookies or
> external fonts. Keep the hostname live permanently — Play requires an active
> URL. Confirm `privacy@grafizum.com` exists and is monitored before publishing;
> the page names it as the contact for data requests.

## After the subdomain works

Change one line — `AppLinks.PRIVACY_POLICY` in
`composeApp/src/main/java/com/grafizum/repio/util/AppLinks.kt` — and the Play
Console field to `https://repio.grafizum.com/privacy`. Nothing else references the
address.

Until then, the `workers.dev` URL is a legitimate, publicly reachable policy URL
and should be used as-is.
