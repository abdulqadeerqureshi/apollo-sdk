package pk.apollobuddy.sdk

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.MenuItem
import android.view.View
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.net.toUri
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.color.MaterialColors
import pk.apollobuddy.sdk.databinding.ActivityApolloBuddyWebviewBinding

class ApolloBuddyWebViewActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_URL = "pk.apollobuddy.sdk.EXTRA_URL"
        const val EXTRA_CONFIG = "pk.apollobuddy.sdk.EXTRA_CONFIG"
        const val EXTRA_RESULT = "pk.apollobuddy.sdk.EXTRA_RESULT"
    }

    private lateinit var binding: ActivityApolloBuddyWebviewBinding
    private var config: ApolloBuddyConfig? = null
    private var sessionCleared: Boolean = false
    private var resultDelivered: Boolean = false

    /** Origin string for [WebStorage.deleteOrigin] (e.g. `https://example.com`). */
    private var flowStorageOrigin: String? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityApolloBuddyWebviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val url = intent.getStringExtra(EXTRA_URL) ?: ApolloBuddySdk.takePendingLaunchUrl()
        config = intent.getParcelableExtra(EXTRA_CONFIG)

        if (url == null || !UrlValidator.isValid(url)) {
            finishWithResult(
                ApolloBuddyResult.Failure(
                    reason = "Invalid URL: loading address is missing or not HTTPS",
                    code = 400,
                ),
            )
            return
        }

        val localConfig = config ?: ApolloBuddyConfig()
        val uri = url.toUri()
        if (localConfig.enforceTrustedHostNavigation) {
            if (localConfig.trustedHosts.isEmpty() ||
                !TrustedHostPolicy.isHostTrusted(uri, localConfig.trustedHosts)
            ) {
                finishWithResult(
                    ApolloBuddyResult.Failure(
                        reason = "URL host is not on the trusted host allowlist",
                        code = 403,
                    ),
                )
                return
            }
        }

        flowStorageOrigin = webStorageOriginForUrl(url)

        if (localConfig.showToolbar) {
            // Host apps often use Theme.*.DarkActionBar; that supplies a window action bar which
            // conflicts with setSupportActionBar(Toolbar). Hide the window bar and use our toolbar only.
            supportActionBar?.hide()
            binding.toolbar.visibility = View.VISIBLE
            setupStandaloneToolbar(binding.toolbar, localConfig.themeColor)
            binding.toolbar.setNavigationOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }
        } else {
            binding.toolbar.visibility = View.GONE
        }

        setupWebViewSettings()
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (binding.webview.canGoBack()) {
                        binding.webview.goBack()
                    } else {
                        finishWithResult(ApolloBuddyResult.Cancelled(binding.webview.url))
                    }
                }
            },
        )
        setupWebViewClient()
        setupWebChromeClient()

        binding.webview.loadUrl(url)
    }

    private fun setupWebViewSettings() {
        val settings = binding.webview.settings
        val localConfig = config ?: ApolloBuddyConfig()

        settings.javaScriptEnabled = localConfig.enableJs
        settings.domStorageEnabled = localConfig.enableDomStorage

        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW

        settings.allowFileAccess = false
        settings.allowContentAccess = false
        @Suppress("DEPRECATION")
        settings.allowFileAccessFromFileURLs = false
        @Suppress("DEPRECATION")
        settings.allowUniversalAccessFromFileURLs = false

        if (localConfig.customUserAgent != null) {
            settings.userAgentString = localConfig.customUserAgent
        }

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(binding.webview, localConfig.allowThirdPartyCookies)
    }

    private fun setupWebViewClient() {
        binding.webview.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val reqUrl = request?.url?.toString() ?: return false
                val reqUri = request?.url ?: return false
                if (!allowNavigationTo(reqUri)) {
                    finishWithResult(
                        ApolloBuddyResult.Failure(
                            reason = "Navigation blocked: URL is not on the trusted host allowlist",
                            code = 403,
                        ),
                    )
                    return true
                }
                return handleUrl(reqUrl)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                binding.progressBar.visibility = View.VISIBLE
                binding.progressBar.progress = 0

                url?.let { u ->
                    val uri = u.toUri()
                    if (!allowNavigationTo(uri)) {
                        finishWithResult(
                            ApolloBuddyResult.Failure(
                                reason = "Navigation blocked: URL is not on the trusted host allowlist",
                                code = 403,
                            ),
                        )
                        return
                    }
                    handleUrl(u)
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                binding.progressBar.visibility = View.GONE
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && request?.isForMainFrame != true) {
                    return
                }
                finishWithResult(
                    ApolloBuddyResult.Failure(
                        reason = "WebView load error (code ${error?.errorCode ?: 500})",
                        code = error?.errorCode ?: 500,
                        finalUrl = request?.url?.toString(),
                    ),
                )
            }

            @SuppressLint("WebViewClientOnReceivedSslError")
            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: android.net.http.SslError?) {
                val localConfig = config ?: ApolloBuddyConfig()
                if (localConfig.ignoreSslErrors && isHostAppDebuggable()) {
                    handler?.proceed()
                } else {
                    super.onReceivedSslError(view, handler, error)
                    finishWithResult(
                        ApolloBuddyResult.Failure(
                            reason = "SSL certificate error",
                            code = -202,
                            finalUrl = view?.url,
                        ),
                    )
                }
            }
        }
    }

    private fun setupWebChromeClient() {
        binding.webview.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                binding.progressBar.progress = newProgress
                if (newProgress == 100) {
                    binding.progressBar.visibility = View.GONE
                } else {
                    binding.progressBar.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun allowNavigationTo(uri: android.net.Uri): Boolean {
        val local = config ?: return true
        if (!local.enforceTrustedHostNavigation) return true
        return TrustedHostPolicy.isHostTrusted(uri, local.trustedHosts)
    }

    private fun callbacksAllowedOnHost(uri: android.net.Uri): Boolean {
        val local = config ?: return true
        if (!local.enforceTrustedHostNavigation) return true
        return TrustedHostPolicy.isHostTrusted(uri, local.trustedHosts)
    }

    private fun handleUrl(url: String): Boolean {
        val localConfig = config ?: return false
        val uri = url.toUri()
        if (!callbacksAllowedOnHost(uri)) return false

        if (!localConfig.statusQueryParam.isNullOrBlank()) {
            val status = uri.getQueryParameter(localConfig.statusQueryParam)
            if (status != null) {
                if (status.equals(localConfig.successQueryValue, ignoreCase = true)) {
                    finishWithResult(ApolloBuddyResult.Success(url))
                    return true
                } else if (status.equals(localConfig.failureQueryValue, ignoreCase = true)) {
                    finishWithResult(ApolloBuddyResult.Failure("Payment failed: status query", 200, url))
                    return true
                }
            }
        }

        if (!localConfig.successPathPrefix.isNullOrBlank()) {
            if (pathMatchesPrefix(uri.path, localConfig.successPathPrefix)) {
                finishWithResult(ApolloBuddyResult.Success(url))
                return true
            }
        } else if (!localConfig.successUrlPattern.isNullOrBlank() && url.contains(localConfig.successUrlPattern)) {
            finishWithResult(ApolloBuddyResult.Success(url))
            return true
        }

        if (!localConfig.failurePathPrefix.isNullOrBlank()) {
            if (pathMatchesPrefix(uri.path, localConfig.failurePathPrefix)) {
                finishWithResult(ApolloBuddyResult.Failure("Payment failed or cancelled (failure path)", 200, url))
                return true
            }
        } else if (!localConfig.failureUrlPattern.isNullOrBlank() && url.contains(localConfig.failureUrlPattern)) {
            finishWithResult(ApolloBuddyResult.Failure("Payment failed or cancelled via URL pattern", 200, url))
            return true
        }

        return false
    }

    private fun isHostAppDebuggable(): Boolean =
        (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    private fun finishWithResult(result: ApolloBuddyResult) {
        if (resultDelivered || isFinishing) return
        resultDelivered = true
        clearSessionBeforeFinish {
            val data = Intent()
            data.putExtra(EXTRA_RESULT, result)
            setResult(RESULT_OK, data)

            ActiveCallbackHolder.callback?.onResult(result)
            ActiveCallbackHolder.callback = null

            finish()
        }
    }

    private fun clearSessionBeforeFinish(done: () -> Unit) {
        if (sessionCleared) {
            done()
            return
        }
        sessionCleared = true
        if (::binding.isInitialized) {
            binding.webview.clearCache(true)
            binding.webview.clearHistory()
        }
        flowStorageOrigin?.let { origin ->
            try {
                WebStorage.getInstance().deleteOrigin(origin)
            } catch (_: Exception) {
            }
        }
        CookieManager.getInstance().removeSessionCookies {
            CookieManager.getInstance().flush()
            runOnUiThread(done)
        }
    }

    override fun onDestroy() {
        if (!sessionCleared && ::binding.isInitialized) {
            sessionCleared = true
            binding.webview.clearCache(true)
            flowStorageOrigin?.let { origin ->
                try {
                    WebStorage.getInstance().deleteOrigin(origin)
                } catch (_: Exception) {
                }
            }
            CookieManager.getInstance().removeSessionCookies { CookieManager.getInstance().flush() }
        }
        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Toolbar as a standalone widget (no [setSupportActionBar]) so the activity works with host
     * themes that already provide a window action bar ([AppCompatDelegate] would otherwise throw).
     * Up icon comes from [androidx.appcompat.R.attr.homeAsUpIndicator] so it matches the app theme.
     */
    private fun setupStandaloneToolbar(toolbar: MaterialToolbar, themeColor: Int?) {
        val tv = TypedValue()
        if (theme.resolveAttribute(androidx.appcompat.R.attr.homeAsUpIndicator, tv, true) && tv.resourceId != 0) {
            toolbar.navigationIcon = AppCompatResources.getDrawable(this, tv.resourceId)
        }
        applyToolbarColors(toolbar, themeColor)
    }

    /**
     * When [ApolloBuddyConfig.themeColor] is set, tint navigation icon and title for contrast.
     * Otherwise the toolbar uses [android.R.attr.actionBarTheme] from the host application theme.
     */
    private fun applyToolbarColors(toolbar: MaterialToolbar, themeColor: Int?) {
        if (themeColor == null) return
        toolbar.setBackgroundColor(themeColor)
        val useDarkForeground = MaterialColors.isColorLight(themeColor)
        val on = if (useDarkForeground) Color.BLACK else Color.WHITE
        toolbar.setNavigationIconTint(on)
        toolbar.setTitleTextColor(on)
    }
}

/** Builds an origin string suitable for [WebStorage.deleteOrigin]. */
internal fun webStorageOriginForUrl(url: String): String? {
    return try {
        val u = url.toUri()
        val scheme = u.scheme ?: return null
        val host = u.host ?: return null
        val port = u.port
        if (port == -1 || (scheme.equals("https", true) && port == 443) || (scheme.equals("http", true) && port == 80)) {
            "$scheme://$host"
        } else {
            "$scheme://$host:$port"
        }
    } catch (_: Exception) {
        null
    }
}
