# Google Play Developer Security & Data Safety Audit

This audit evaluates the **RepIo** app against the standard requirements of the Google Play Developer Security Center and Data Safety requirements.

## 1. Data Safety Declaration (High Priority)
When submitting to Google Play, you must complete the Data Safety form. Based on the current code, you should declare the following:

| Data Type | Collected? | Shared? | Purpose | Encrypted? |
| :--- | :--- | :--- | :--- | :--- |
| **App Interactions** | Yes | No | App Functionality (Support Tickets) | Yes (HTTPS) |
| **Device ID / Other IDs** | No* | No | N/A | - |
| **Other Data** | Yes | No | Developer Communications (Support Tickets) | Yes (HTTPS) |

> [!NOTE]
> *While you aren't explicitly collecting a "User ID", the `device_model` is sent with support tickets. This is generally considered "collected data" for developer communications.

### Key Data Safety Actions:
- **Encryption in Transit**: All Firebase communication (Firestore) uses HTTPS/TLS by default.
- **Data Deletion**: Google Play requires you to provide a way for users to request data deletion. Since tickets are sent to Firestore without a login, you should add a note in your privacy policy explaining how they can contact you to delete their ticket data (e.g., via the support form itself).

---

## 2. Permissions & Manifest Security
- **Minimalism**: Checked. Only 3 permissions are used (`POST_NOTIFICATIONS`, `VIBRATE`, `SCHEDULE_EXACT_ALARM`). All are justified for a workout timer app.
- **Cleartext Traffic**: Checked. `android:usesCleartextTraffic` is NOT enabled, which is correct. The app defaults to secure HTTPS.
- **Component Exporting**: The `RestTimerReceiver` is correctly set to `android:exported="false"`, preventing other apps from triggering your timer logic.

---

## 3. Network Security
- **Firebase Firestore**: The app uses the official Firebase SDK.
- **API Keys**: No hardcoded Firebase API keys or secrets were found in the source code.
- **HTTPS**: All external traffic (Firebase) is encrypted.

---

## 4. Anti-Spam & Abuse Prevention
- **Client-Side Cooldown**: [x] Implemented (5-minute/Flexible timeout).
- **Server-Side Security (Action Required)**:
  > [!IMPORTANT]
  > Ensure your Firestore Security Rules are updated in the Firebase Console. "Test Mode" rules expire and are insecure.
  > **Recommended Rule:**
  > ```javascript
  > service cloud.firestore {
  >   match /databases/{database}/documents {
  >     match /support_tickets/{ticket} {
  >       allow create: if request.resource.data.subject.size() > 0
  >                     && request.resource.data.message.size() > 0;
  >       allow read, update, delete: if false; // Only you can read tickets
  >     }
  >   }
  > }
  > ```

---

## 5. Potential Improvements for Play Store Approval
1. **Enable Minification (Recommended)**: In `app/build.gradle.kts`, `isMinifyEnabled` is set to `false`. Enable this for your release build to obfuscate your code. This makes it much harder for malicious users to reverse-engineer your app and find your backend endpoints or bypass rate limits.
2. **Auto-Backup (UX vs Privacy)**: `android:allowBackup` is `true`. This means your workout database is backed up to Google Drive. This is great for users who switch phones, but you should ensure no personally identifiable information (PII) is stored in the database without the user knowing.
3. **App Check Implementation**: You have the App Check dependency in your project. To truly stop automated bot spam (from outside the app), you should initialize App Check in your `Application` class. This ensures only your genuine app can talk to Firestore.
4. **Locked Mode Verification**: Since you are using "Production/Locked Mode" in Firestore, ensure you have explicitly granted `create` permissions for the `support_tickets` collection, otherwise the "Send Ticket" feature will fail silently or return an error.

## 6. Advanced Security Checks (Completed)
- [x] **Hardcoded Secrets**: Scanned for API keys, tokens, and secrets. None found.
- [x] **WebView Safety**: Scanned for WebViews. None used (prevents XSS/JavaScript injection risks).
- [x] **Exported Components**: Verified that background receivers are not accessible to other apps.
- [x] **Cleartext Traffic**: Verified that only encrypted HTTPS traffic is allowed.

## Conclusion
The app is in **Good Standing** for Google Play security requirements. Since you are already avoiding "Test Mode," your primary focus should be **Enabling Minification** and potentially setting up **Firebase App Check** if you notice any bot activity in the future.
