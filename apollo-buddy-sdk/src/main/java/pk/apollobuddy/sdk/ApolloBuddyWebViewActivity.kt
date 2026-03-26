package pk.apollobuddy.sdk

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import pk.apollobuddy.sdk.databinding.ActivityApolloBuddyWebviewBinding

class ApolloBuddyWebViewActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_URL = "pk.apollobuddy.sdk.EXTRA_URL"
        const val EXTRA_CONFIG = "pk.apollobuddy.sdk.EXTRA_CONFIG"
        const val EXTRA_RESULT = "pk.apollobuddy.sdk.EXTRA_RESULT"
    }

    private lateinit var binding: ActivityApolloBuddyWebviewBinding
    private var config: ApolloBuddyConfig? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityApolloBuddyWebviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val url = intent.getStringExtra(EXTRA_URL)
        config = intent.getParcelableExtra(EXTRA_CONFIG)

        if (url == null || !UrlValidator.isValid(url)) {
            finishWithResult(ApolloBuddyResult.Failure("Invalid URL: $url", 400))
            return
        }

        if (config?.showToolbar == true) {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = "ApolloBuddy"
        } else {
            supportActionBar?.hide()
        }

        setupWebViewSettings()
        setupWebViewClient()
        setupWebChromeClient()

        binding.webview.loadUrl(url)
    }

    private fun setupWebViewSettings() {
        val settings = binding.webview.settings
        val localConfig = config ?: ApolloBuddyConfig()

        settings.javaScriptEnabled = localConfig.enableJs
        settings.domStorageEnabled = localConfig.enableDomStorage

        if (localConfig.customUserAgent != null) {
            settings.userAgentString = localConfig.customUserAgent
        }

        // Mixed content mode
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

        // Third party cookies
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(binding.webview, localConfig.allowThirdPartyCookies)
    }

    private fun setupWebViewClient() {
        binding.webview.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                return handleUrl(url)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                binding.progressBar.visibility = View.VISIBLE
                binding.progressBar.progress = 0

                // Check if the start URL is already a result
                url?.let { handleUrl(it) }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                binding.progressBar.visibility = View.GONE
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                // Optional: Finish with failure if main frame error? 
                // For now, let's keep it open to show the error page, 
                // but maybe we can just log it or Toast.
                finishWithResult(ApolloBuddyResult.Failure("WebView Error: ${error?.description}", error?.errorCode ?: 500, request?.url?.toString()))
            }

            @SuppressLint("WebViewClientOnReceivedSslError")
            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: android.net.http.SslError?) {
                val localConfig = config ?: ApolloBuddyConfig()
                if (localConfig.ignoreSslErrors) {
                    handler?.proceed()
                } else {
                    super.onReceivedSslError(view, handler, error)
                    // We can also notify the user why it failed
                    finishWithResult(ApolloBuddyResult.Failure("SSL Error: ${error?.primaryError}", -202, view?.url))
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

    private fun handleUrl(url: String): Boolean {
        val localConfig = config ?: return false
        val uri = url.toUri()

        // 1. Check Query Param Logic (Robust)
        if (!localConfig.statusQueryParam.isNullOrBlank()) {
            val status = uri.getQueryParameter(localConfig.statusQueryParam)
            if (status != null) {
                if (status.equals(localConfig.successQueryValue, ignoreCase = true)) {
                    finishWithResult(ApolloBuddyResult.Success(url))
                    return true
                } else if (status.equals(localConfig.failureQueryValue, ignoreCase = true)) {
                    finishWithResult(ApolloBuddyResult.Failure("Payment failed: $status", 200, url))
                    return true
                }
            }
        }

        // 2. Check Success Pattern (String match)
        if (!localConfig.successUrlPattern.isNullOrBlank() && url.contains(localConfig.successUrlPattern)) {
            finishWithResult(ApolloBuddyResult.Success(url))
            return true
        }

        // 3. Check Failure Pattern
        if (!localConfig.failureUrlPattern.isNullOrBlank() && url.contains(localConfig.failureUrlPattern)) {
            finishWithResult(ApolloBuddyResult.Failure("Payment failed or cancelled via parsing", 200, url))
            return true
        }

        return false
    }

    private fun finishWithResult(result: ApolloBuddyResult) {
        val data = Intent()
        data.putExtra(EXTRA_RESULT, result)
        setResult(RESULT_OK, data)

        ActiveCallbackHolder.callback?.onResult(result)
        ActiveCallbackHolder.callback = null

        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    @Deprecated("Deprecated in Java", ReplaceWith("if (webView.canGoBack()) webView.goBack() else super.onBackPressed()"))
    override fun onBackPressed() {
        super.onBackPressed()
        if (binding.webview.canGoBack()) {
            binding.webview.goBack()
        } else {
            finishWithResult(ApolloBuddyResult.Cancelled(binding.webview.url))
        }
    }
}
