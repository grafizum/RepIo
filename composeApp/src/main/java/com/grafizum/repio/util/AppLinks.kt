package com.grafizum.repio.util

/**
 * Public URLs the app is required to expose.
 *
 * The privacy policy has to be reachable from inside the app as well as from the
 * Privacy Policy field in Play Console — Play's User Data policy asks for both,
 * for every app, even one that stores everything locally. The page itself is
 * deployed from `site/` in this repository; this constant is the one place the
 * address lives, so moving the page is a one-line change here plus the Console
 * field.
 */
object AppLinks {
    /**
     * Served by the Cloudflare Worker `repio-legal` (source: `site/` in this
     * repository). The branded host `repio.grafizum.com` is already attached to
     * that Worker, but the zone held by this project's Cloudflare account does not
     * publish new records, so the subdomain does not resolve yet. When it does,
     * switch this constant — nothing else refers to the address.
     */
    const val PRIVACY_POLICY = "https://repio-legal.matrix-gian.workers.dev/privacy"
}
