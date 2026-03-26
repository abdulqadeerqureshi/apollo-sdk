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
    implementation("pk.apollobuddy.sdk:apollo-buddy-sdk:1.0.0")
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
            .build()

        // 2. Initialize the SDK
        ApolloBuddySdk.init(applicationContext, initParams)
    }
}
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