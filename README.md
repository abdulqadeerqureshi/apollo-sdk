# Hisaab SDK for Android

The Hisaab SDK provides a robust, drop-in web view integration built specifically to support
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
    implementation("pk.myhisaab.sdk:hisaab-sdk:1.0.0")
}
```

---

## 🚀 Step 1: Initialize the SDK

You **must** initialize the SDK exactly once before trying to launch it. The best place to do this
is in your `Application` class inside `onCreate()`.

The SDK mandates certain parameters to guarantee valid authentication flows.

### Building the Initialization Parameters

The `HisaabInitParams` requires exact parameter matching and validates itself safely:

```kotlin
import android.app.Application
import pk.myhisaab.sdk.HisaabSdk
import pk.myhisaab.sdk.HisaabInitParams

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // 1. Build your required initialization parameters
        val initParams = HisaabInitParams.Builder()
            .setEloadNumber("031234567890") // Required: Must not be blank
            .setImsi("a410t010y5689")       // Required: Must not be blank
            .setProfileId(15)               // Required: Must be > 0
            .setRegionId(1)                 // Required: Must be > 0
            .setToken("t293f4XXXXXXXXXX")   // Required: Must not be blank

            // Conditional: You MUST provide either userCode (for Non-AD) OR employeeId (for AD)
            .setUserCode("APOL-1112")
            // .setEmployeeId(1456)            

            // Optional: The default URL is already https://apollo.digitalmiles.org/
            // .setWebURL("https://apollo.digitalmiles.org/") 
            .build()

        // 2. Initialize the SDK
        HisaabSdk.init(applicationContext, initParams)
    }
}
```

---

## 🕹 Step 2: Configure Web View Options (Optional)

The SDK utilizes `HSConfig` to manipulate how the built-in WebView behaves. You can optionally use
this during launch to tailor the SDK to your needs.

```kotlin
import pk.myhisaab.sdk.HSConfig

val webConfig = HSConfig.Builder()
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

Since the SDK dynamically generates the secure URL internally based on the `HisaabInitParams`
provided during initialization, launching the SDK requires no URL input!

### Method A: The Modern Way (Activity Result API) - Recommended 🌟

In your `Activity` or `Fragment`, register a modern launcher contract:

```kotlin
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import pk.myhisaab.sdk.HisaabSdk
import pk.myhisaab.sdk.HSLaunchInput
import pk.myhisaab.sdk.HSResult

class MainActivity : AppCompatActivity() {

    // Define the contract
    private val hisaabLauncher = registerForActivityResult(HisaabSdk.Contract()) { result ->
        when (result) {
            is HSResult.Success -> {
                // Success criteria met!
                Toast.makeText(this, "Success: ${result.data}", Toast.LENGTH_LONG).show()
            }
            is HSResult.Failure -> {
                // Error handled gracefully
                Toast.makeText(this, "Failed: ${result.reason}", Toast.LENGTH_LONG).show()
            }
            is HSResult.Cancelled -> {
                // User pressed back button / closed SDK
                Toast.makeText(this, "Dismissed by user", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Launch it whenever needed
    fun openHisaab() {
        hisaabLauncher.launch(
            HSLaunchInput(config = webConfig) // the webConfig from Step 2
        )
    }
}
```

### Method B: The Legacy Way (Callbacks)

If your architecture does not easily support modern Activity Result Contracts, use the legacy
wrapper:

```kotlin
HisaabSdk.launch(
    context = this,
    config = webConfig,
    callback = object : HisaabSdk.Callback {
        override fun onResult(result: HSResult) {
            when (result) {
                is HSResult.Success -> { /* Handle success */
                }
                is HSResult.Failure -> { /* Handle failure */
                }
                is HSResult.Cancelled -> { /* Handle cancel */
                }
            }
        }
    }
)
```

---

## ⚙️ How URL Construction Works Under-The-Hood

The SDK automatically serializes your `HisaabInitParams` directly to the `webURL` using safe query
components:

```
https://apollo.digitalmiles.org/?eloadNumber=031234567890&imsi=a410...&profileId=15&regionId=1&userCode=APOL-1112&token=...
```

Therefore, you don't have to worry about manual string formatting or query encoding. The SDK takes
care of it natively.
