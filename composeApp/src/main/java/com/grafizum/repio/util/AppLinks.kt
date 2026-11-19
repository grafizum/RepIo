package com.grafizum.repio.util

/**
 * Public URLs the app is required to expose.
 *
 * Play's User Data policy asks for the privacy policy to be reachable from inside
 * the app as well as from the Privacy Policy field in Play Console, for every app,
 * even one that stores everything locally. The pages are deployed from `site/` in
 * this repository and served on the branded host `repio.grafizum.com`; this
 * constant is the one place the address lives, so moving the page is a one-line
 * change here plus the Console field.
 */
object AppLinks {
    /** The same host also serves `/terms` and `/legal-notice`. */
    const val PRIVACY_POLICY = "https://repio.grafizum.com/privacy"
}
