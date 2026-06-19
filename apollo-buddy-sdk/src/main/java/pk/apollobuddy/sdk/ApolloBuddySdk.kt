package pk.apollobuddy.sdk

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.content.IntentCompat
import pk.apollobuddy.sdk.ApolloBuddySdk.pendingLaunchUrl

/**
 * Main entry point for the Apollo Buddy SDK.
 */
object ApolloBuddySdk {

    /**
     * Intents have a Binder size limit (~1MB). Very long query strings (e.g. large tokens) can exceed
     * it and prevent [ApolloBuddyWebViewActivity] from receiving [ApolloBuddyWebViewActivity.EXTRA_URL].
     * URLs longer than this are passed out-of-band via [pendingLaunchUrl] instead.
     */
    internal const val MAX_URL_INTENT_EXTRA_LENGTH: Int = 80_000

    @Volatile
    private var isInitialized = false

    private var applicationContext: Context? = null

    @Volatile
    internal var initParams: ApolloBuddyInitParams? = null

    @Volatile
    internal var pendingLaunchUrl: String? = null
        private set

    internal fun setPendingLaunchUrl(url: String) {
        pendingLaunchUrl = url
    }

    internal fun takePendingLaunchUrl(): String? {
        val u = pendingLaunchUrl
        pendingLaunchUrl = null
        return u
    }

    /**
     * Initializes or refreshes the SDK parameters. Host apps should call this again with a fresh
     * token before launching when their authentication token changes.
     */
    @Synchronized
    fun init(context: Context, params: ApolloBuddyInitParams) {
        applicationContext = context.applicationContext
        updateParams(params)
    }

    /**
     * Refreshes the parameters used to build the next launch URL.
     */
    @Synchronized
    fun updateParams(params: ApolloBuddyInitParams) {
        initParams = params
        isInitialized = true
    }

    /**
     * Interface for legacy callback-style integration.
     */
    interface Callback {
        fun onResult(result: ApolloBuddyResult)
    }

    /**
     * Launches the ApolloBuddy Web Flow.
     *
     * @param context Context to launch the Activity from.
     * @param config Optional configuration for the WebView.
     * @param callback Optional callback for results (legacy style). 
     *                 For Activity Result API, use the [Contract].
     */
    fun launch(
        context: Context,
        config: ApolloBuddyConfig = ApolloBuddyConfig(),
        callback: Callback? = null,
    ) {
        if (!isInitialized || initParams == null) {
            throw IllegalStateException("ApolloBuddySdk must be initialized with params before launching")
        }

        val url = initParams!!.buildUrl()

        if (!UrlValidator.isValid(url)) {
            callback?.onResult(
                ApolloBuddyResult.Failure(
                    reason = "Generated URL is invalid or not HTTPS (check base URL in init params)",
                    code = 400,
                ),
            )
            return
        }

        val intent = Intent(context, ApolloBuddyWebViewActivity::class.java).apply {
            putLaunchUrlExtra(url)
            putExtra(ApolloBuddyWebViewActivity.EXTRA_CONFIG, config)
        }

        if (callback != null) {
            ActiveCallbackHolder.callback = callback
        }

        if (context !is android.app.Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)
    }

    /**
     * Activity Result Contract for modern integration.
     * Use with registerForActivityResult.
     */
    class Contract : ActivityResultContract<ApolloBuddyLaunchInput, ApolloBuddyResult>() {
        override fun createIntent(context: Context, input: ApolloBuddyLaunchInput): Intent {
            val url = initParams?.buildUrl() ?: throw IllegalStateException("ApolloBuddySdk not initialized with params")
            return Intent(context, ApolloBuddyWebViewActivity::class.java).apply {
                putLaunchUrlExtra(url)
                putExtra(ApolloBuddyWebViewActivity.EXTRA_CONFIG, input.config)
            }
        }

        override fun parseResult(resultCode: Int, intent: Intent?): ApolloBuddyResult {
            return intent?.let {
                IntentCompat.getParcelableExtra(
                    it,
                    ApolloBuddyWebViewActivity.EXTRA_RESULT,
                    ApolloBuddyResult::class.java,
                )
            } ?: ApolloBuddyResult.Cancelled()
        }
    }

    private fun Intent.putLaunchUrlExtra(url: String) {
        if (url.length <= MAX_URL_INTENT_EXTRA_LENGTH) {
            putExtra(ApolloBuddyWebViewActivity.EXTRA_URL, url)
        } else {
            setPendingLaunchUrl(url)
        }
    }
}

data class ApolloBuddyLaunchInput(
    val config: ApolloBuddyConfig = ApolloBuddyConfig(),
)

internal object ActiveCallbackHolder {
    var callback: ApolloBuddySdk.Callback? = null
}
