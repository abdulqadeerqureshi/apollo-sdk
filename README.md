# Apollo Buddy SDK for Android

The Apollo Buddy SDK provides a robust, drop-in web view integration built specifically to support
authentication flow seamlessly.

The SDK exposes an easy-to-use Builder pattern for configurations, handles modern Activity Result
APIs natively, and performs rigorous validation securely before ever attempting to load the target
URL.

---

## 🛠 Prerequisites

- **Minimum SDK**: 23 (Android 6.0)
- **Target SDK**: 34 (Android 14)

---

## 📦 Installation

*(Assuming you are using Maven/JitPack or a local repository to distribute your SDK)*

Add the repository and dependency to your app's `build.gradle.kts` / `build.gradle`:

```kotlin
dependencies {
    implementation("com.github.abdulqadeerqureshi:apollo-sdk:v1.0.0")
}
```

---

## 🚀 Step 1: Initialize the SDK

You **must** initialize the SDK exactly once before trying to launch it. The best place to do this
is in your `Application` class inside `onCreate()`.

The SDK mandates certain parameters to guarantee valid authentication flows.

### Building the Initialization Parameters

The `ApolloBuddyInitParams` requires exact parameter matching and validates itself safely:

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // 1. Build your required initialization parameters
        val initParams = ApolloBuddyInitParams.Builder()
            .setEloadNumber("031234567890") // Required: Must not be blank
            .setImsi("a410t010y5689")       // Required: Must not be blank
            .setProfileId(15)               // Required: Must be > 0
            .setRegionId(1L)                // Required: Long, must be > 0 (matches Apollo)
            .setToken("t293f4XXXXXXXXXX")   // Required: Must not be blank

            // Conditional: You MUST provide either userCode (for Non-AD) OR employeeId (for AD)
            .setUserCode("APOL-1112")
            // .setEmployeeId(1456)

           // Optional: override the default base URL (see "Web URL patterns" below)
           // .setWebURL("https://your-api.example.com/")
            .build()

        // 2. Initialize the SDK
        ApolloBuddySdk.init(applicationContext, initParams)
    }
}
```

### Web URL patterns (`setWebURL`)

The SDK builds the page URL by taking a **base URL** and appending query parameters (`eloadNumber`,
`imsi`, `token`, etc.). You can set the base with
**`ApolloBuddyInitParams.Builder.setWebURL(String)`**. If you omit it, the default base is
**`https://apollo.digitalmiles.org/`**.

`setWebURL` **normalizes** the string so common copy-paste mistakes still produce a loadable URL.
You do not need to hand-perfect every slash; follow the rules below.

#### Normalization rules

1. **Trim** — Leading and trailing whitespace is removed.
2. **Scheme** — If the string does not contain `://`:
   - **`https://`** is prefixed for typical hostnames (e.g. `www.example.com` →
     `https://www.example.com`).
   - **Scheme-relative** URLs that start with **`//`** get **`https:`** only (e.g.
     `//api.example.com/path` → `https://api.example.com/path`).
   - **`host:port`** forms (e.g. `api.example.com:443`, `localhost:8080`) get **`https://`** in
     front of the **whole** string.
   - **Opaque** schemes without `://` (`mailto:…`, `tel:…`, `data:…`) are left as-is — do **not**
     use these as the web base URL.
3. **`http://` upgrade** — Explicit **`http://`** hierarchical bases are rewritten to **`https://`**
   before load (in addition to missing-scheme handling above).
4. **Path** — For a normal hierarchical URL (`scheme://host…`):
   - If there is **no path** (e.g. `https://example.com` or `https://example.com?ref=1`), a **root
     path `/`** is inserted so queries append correctly (`https://example.com/…`).
   - If a path **already exists** (including a lone `/` or `/app/...`), the string is **not**
     altered to add extra slashes at the end.

#### Examples

| You pass                  | After normalization (typical) |
|---------------------------|-------------------------------|
| `www.example.com`         | `https://www.example.com/`    |
| `www.example.com/`        | `https://www.example.com/`    |
| `https://example.com`     | `https://example.com/`        |
| `https://example.com/app` | `https://example.com/app`     |
| `https://example.com?x=1` | `https://example.com/?x=1`    |
| `http://example.com`      | `https://example.com/`        |
| `//cdn.example.com/foo`   | `https://cdn.example.com/foo` |
| `localhost:8080`          | `https://localhost:8080/`     |

#### Query string encoding

Parameter values are encoded with Android’s `Uri.Builder.appendQueryParameter` (same idea as a
browser `URL` + `searchParams.set` in JavaScript): characters such as `/`, `+`, and `=` in a long
**token** become `%2F`, `%2B`, `%3D`, etc. Pass **raw** strings to `setToken` and other setters—do
not pre-encode the full query yourself.

#### What to avoid

- **Relative** URLs (`foo/bar`, `/only/path`) — prefer a full **absolute** base URL; relative bases
  are not supported the same way as in a browser.
- **Non-HTTP(S) opaque URIs** as the base — use an `https://` web URL your WebView should load.
  Plain `http://` bases are **upgraded to `https://`** during normalization.

#### Verifying behavior

Automated **unit** tests (JVM, Robolectric where `android.net.Uri` is required) include:

| Area                                  | Test class                          |
|---------------------------------------|-------------------------------------|
| `normalizeWebUrl` / `setWebURL`       | `NormalizeWebUrlTest`               |
| `buildUrl()` encoding vs. long tokens | `ApolloBuddyInitParamsBuildUrlTest` |
| Production base URL + `UrlValidator`  | `ProductionApolloUrlTest`           |
| HTTPS validation                      | `UrlValidatorTest`                  |
| Trusted host policy                   | `TrustedHostPolicyTest`             |

Run unit tests:

```bash
./gradlew :apollo-buddy-sdk:testDebugUnitTest
```

**Instrumentation** tests (`src/androidTest`) include a WebView load against a **local HTTPS
MockWebServer** (OkHttp) so CI does not depend on live network. Run on a device or emulator:

```bash
./gradlew :apollo-buddy-sdk:connectedDebugAndroidTest
```

---

## Security defaults

The SDK applies a strict baseline (HTTPS-only loads, mixed content blocked, file access disabled,
third-party cookies off by default). Highlights:

| Topic                      | Behavior                                                                                                                                                                                                                                                  |
|----------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Transport**              | Only **`https://`** URLs load. `http://` in `setWebURL` is normalized to **`https://`**.                                                                                                                                                                  |
| **Mixed content**          | Insecure subresources on HTTPS pages are **not** loaded (`MIXED_CONTENT_NEVER_ALLOW`).                                                                                                                                                                    |
| **SSL bypass**             | `ignoreSslErrors` is honored **only** when the **host app is debuggable** (`ApplicationInfo.FLAG_DEBUGGABLE`). Release production builds must not ship debuggable; misconfiguration cannot bypass TLS in a normal release APK.                            |
| **Third-party cookies**    | Default **off**. Enable only if your identity flow **requires** it.                                                                                                                                                                                       |
| **Trusted hosts**          | Optional `setTrustedHosts` + `setEnforceTrustedHostNavigation(true)` restrict the initial URL, redirects, and callback handling to approved hosts (subdomains of a listed host are allowed).                                                              |
| **Callbacks**              | Prefer **`setSuccessPathPrefix` / `setFailurePathPrefix`** (path prefix match) over loose `setSuccessUrlPattern` substring checks. Query-param rules remain supported.                                                                                    |
| **Session cleanup**        | Session cookies, WebView cache/history, and HTML5 storage for the flow origin are cleared when the flow finishes. `CookieManager` is process-wide; avoid enabling third-party cookies unless necessary.                                                   |
| **Logging**                | Use **`SensitiveDataRedaction.redactUrl`** before logging any URL from `ApolloBuddyResult`. Do not log tokens, IMSI, or user identifiers from SDK parameters.                                                                                             |
| **Signed `state` / nonce** | Strong assurance against forged redirect URLs requires your **authorization server** to issue and verify a `state` or nonce (OAuth-style). The SDK does not implement server-side signing; add app-side verification once your backend contract is fixed. |

---

## 🕹 Step 2: Configure Web View Options (Optional)

The SDK utilizes `ApolloBuddyConfig` to manipulate how the built-in WebView behaves. You can
optionally use
this during launch to tailor the SDK to your needs.

```kotlin

val webConfig = ApolloBuddyConfig.Builder()
    .setEnableJs(true)
    .setShowToolbar(true)
    // Leave third-party cookies false unless your IdP requires them
    .setAllowThirdPartyCookies(false)
    // SSL bypass only applies to debuggable builds; keep false
    .setIgnoreSslErrors(false)

    // Optional: when your production domain is known, enforce allowlist + path prefixes
    // .setTrustedHosts(listOf("apollo.example.com"))
    // .setEnforceTrustedHostNavigation(true)
    // .setSuccessPathPrefix("/payment/complete")

    // Define success criteria based on query parameters
    .setStatusQueryParam(
        param = "status",
        successValue = "success",
        failureValue = "failed"
    )
    // Legacy substring match on full URL (prefer setSuccessPathPrefix when possible)
    // .setSuccessUrlPattern("payment-successful")
    .build()
```

### Toolbar (`setShowToolbar`)

When **`setShowToolbar(true)`** (the default), the flow shows a **Material toolbar** with a centered
**“Apollo Buddy”** title and a **back** control that mirrors the system back behavior (WebView
history first, then finish back to your app).

- **Host theme** — The WebView activity inherits your **`<application android:theme>`** (the library
  does not force a separate activity theme). The toolbar uses **`?attr/colorPrimary`**,
  **`?attr/actionBarTheme`**, and **`?attr/actionBarPopupTheme`** so colors and the up icon follow
  your app. Prefer a **Material** / **AppCompat** theme that defines those attributes.
- **Window action bar** — If your theme uses **`Theme.*.DarkActionBar`** (a window action bar), the
  SDK **hides** that bar and uses its own **MaterialToolbar** in the layout so you do not get a
  duplicate bar or a `setSupportActionBar` crash. Themes that already use **`NoActionBar`** are also
  supported.
- **Custom bar color** — Optional **`ApolloBuddyConfig.Builder.setThemeColor(color: Int)`** tints
  the
  toolbar background and picks a dark or light foreground for the title and navigation icon for
  contrast.

---

## 🎯 Step 3: Launch the SDK

Since the SDK dynamically generates the secure URL internally based on the `ApolloBuddyInitParams`
provided during initialization, launching the SDK requires no URL input!

### Very long URLs (large tokens)

Android limits how much data can be sent in a single `Intent` extra (Binder budget). If the
generated URL is **longer than ~80,000 characters**, the SDK **does not** put it in the intent;
instead it passes the URL through an internal holder and the WebView activity reads it from there.
Shorter URLs continue to use the `Intent` extra as usual. You do not need to change integration
code for this behavior.

### What Success, Failure, and Cancelled mean

The SDK reports exactly one outcome through `ApolloBuddyResult`:

- **Success** — The flow finished in a **successful** state: the loaded URL matched your configured
  success rules (for example `statusQueryParam` / `successQueryValue`, or `successUrlPattern`).
  `data` is the final URL string.
- **Failure** — The flow ended in an **error** or **explicit failure** state: WebView or SSL
  errors, invalid URL, or a URL matched your failure rules (`failureQueryValue`,
  `failureUrlPattern`).
  Includes a human-readable `reason`, an integer `code`, and optionally `finalUrl`.
- **Cancelled** — The user **exited** the WebView without matching success or failure (for example
  back navigation when there is no prior page). Optionally includes `finalUrl` if the WebView had a
  current URL.

### Method A: The Modern Way (Activity Result API) - Recommended 🌟

In your `Activity` or `Fragment`, register a modern launcher contract:

```kotlin
class MainActivity : AppCompatActivity() {

    // Define the contract
    private val apolloBuddyLauncher =
        registerForActivityResult(ApolloBuddySdk.Contract()) { result ->
        when (result) {
            is ApolloBuddyResult.Success -> {
                // Success criteria met!
                Toast.makeText(this, "Success: ${result.data}", Toast.LENGTH_LONG).show()
            }
            is ApolloBuddyResult.Failure -> {
                // Error handled gracefully
                Toast.makeText(this, "Failed: ${result.reason}", Toast.LENGTH_LONG).show()
            }
            is ApolloBuddyResult.Cancelled -> {
                // User pressed back button / closed SDK
                Toast.makeText(this, "Dismissed by user", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Launch it whenever needed
    fun openApolloBuddy() {
        apolloBuddyLauncher.launch(
            ApolloBuddyLaunchInput(config = webConfig) // the webConfig from Step 2
        )
    }
}
```

### Method B: The Legacy Way (Callbacks)

If your architecture does not easily support modern Activity Result Contracts, use the legacy
wrapper:

```kotlin
ApolloBuddySdk.launch(
    context = this,
    config = webConfig,
    callback = object : ApolloBuddySdk.Callback {
        override fun onResult(result: ApolloBuddyResult) {
            when (result) {
                is ApolloBuddyResult.Success -> { /* Handle success */
                }
                is ApolloBuddyResult.Failure -> { /* Handle failure */
                }
                is ApolloBuddyResult.Cancelled -> { /* Handle cancel */
                }
            }
        }
    }
)
```