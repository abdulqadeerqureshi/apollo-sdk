# Apollo Buddy SDK for Android

The Apollo Buddy SDK provides a robust, drop-in web view integration built specifically to support
authentication flow seamlessly.

The SDK exposes an easy-to-use Builder pattern for configurations, handles modern Activity Result
APIs natively, and performs rigorous validation securely before ever attempting to load the target
URL.

---

## 🛠 Prerequisites

- **Minimum SDK**: 24 (Android 7.0)
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
            .setRegionId(1)                 // Required: Must be > 0
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
`imsi`, `token`, etc.). You can set the base with *
*`ApolloBuddyInitParams.Builder.setWebURL(String)`**. If you omit it, the default base is *
*`https://apollo.digitalmiles.org/`**.

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
3. **Path** — For a normal hierarchical URL (`scheme://host…`):
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
| `//cdn.example.com/foo`   | `https://cdn.example.com/foo` |
| `localhost:8080`          | `https://localhost:8080/`     |

#### What to avoid

- **Relative** URLs (`foo/bar`, `/only/path`) — prefer a full **absolute** base URL; relative bases
  are not supported the same way as in a browser.
- **Non-HTTP(S) opaque URIs** as the base — use an `https://` (or `http://`) web URL your WebView
  should load.

#### Verifying behavior

Automated tests for URL normalization live under
`apollo-buddy-sdk/src/test/java/pk/apollobuddy/sdk/NormalizeWebUrlTest.kt`. Run:

```bash
./gradlew :apollo-buddy-sdk:testDebugUnitTest
```

---

## 🕹 Step 2: Configure Web View Options (Optional)

The SDK utilizes `ApolloBuddyConfig` to manipulate how the built-in WebView behaves. You can
optionally use
this during launch to tailor the SDK to your needs.

```kotlin

val webConfig = ApolloBuddyConfig.Builder()
    .setEnableJs(true)
    .setShowToolbar(true)
    .setAllowThirdPartyCookies(true)
    .setIgnoreSslErrors(false) // Warning: Only use TRUE for development!

    // Define success criteria based on query parameters
    .setStatusQueryParam(
        param = "status",
        successValue = "success",
        failureValue = "failed"
    )
    // Or define success criteria based on URL path matches
    // .setSuccessUrlPattern("payment-successful")
    .build()
```

---

## 🎯 Step 3: Launch the SDK

Since the SDK dynamically generates the secure URL internally based on the `ApolloBuddyInitParams`
provided during initialization, launching the SDK requires no URL input!

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