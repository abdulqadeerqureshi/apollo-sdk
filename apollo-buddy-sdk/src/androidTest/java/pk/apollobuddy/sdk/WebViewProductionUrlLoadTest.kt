package pk.apollobuddy.sdk

import android.annotation.SuppressLint
import android.net.http.SslError
import android.os.Build
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Proves a [WebView] can load the SDK-built URL with the long sample token (same shape as
 * production). Uses a **local HTTPS [MockWebServer]** so the test does not depend on live network
 * or `apollo.digitalmiles.org` latency or GET size limits.
 */
@RunWith(AndroidJUnit4::class)
class WebViewProductionUrlLoadTest {

    @Test
    fun webView_loads_production_apollo_digitalmiles_url_with_long_token() {
        val heldCertificate =
            HeldCertificate.Builder()
                .addSubjectAlternativeName("127.0.0.1")
                .build()
        val handshakeCertificates =
            HandshakeCertificates.Builder()
                .heldCertificate(heldCertificate)
                .build()
        val server = MockWebServer()
        server.useHttps(handshakeCertificates.sslSocketFactory(), false)
        server.enqueue(
            MockResponse()
                .setBody("<!DOCTYPE html><html><head><meta charset=\"utf-8\"><title>t</title></head><body>ok</body></html>")
                .setHeader("Content-Type", "text/html; charset=utf-8"),
        )
        server.start()

        try {
            val baseUrl = "https://127.0.0.1:${server.port}/"
            val params =
                ApolloBuddyInitParams.Builder()
                    .setEloadNumber("03454729269")
                    .setImsi("5fd446fa70b4109")
                    .setProfileId(13)
                    .setRegionId(1L)
                    .setUserCode("dummy")
                    .setToken(LongSampleTokenFixture.RAW)
                    .setWebURL(baseUrl)
                    .build()
            val url = params.buildUrl()
            assertTrue(UrlValidator.isValid(url))

            val latch = CountDownLatch(1)
            val errorRef = AtomicReference<String?>(null)
            val inst = InstrumentationRegistry.getInstrumentation()
            val wvHolder = arrayOfNulls<WebView>(1)

            inst.runOnMainSync {
                val wv = WebView(inst.targetContext)
                wvHolder[0] = wv
                wv.settings.javaScriptEnabled = true
                wv.webViewClient =
                    object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, finishedUrl: String?) {
                            if (errorRef.get() == null) {
                                latch.countDown()
                            }
                        }

                        /** Local mock uses a self-signed cert; trust it for this test only. */
                        @SuppressLint("WebViewClientOnReceivedSslError")
                        override fun onReceivedSslError(
                            view: WebView?,
                            handler: SslErrorHandler?,
                            error: SslError?,
                        ) {
                            handler?.proceed()
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?,
                        ) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                                request?.isForMainFrame == true
                            ) {
                                errorRef.set("code=${error?.errorCode} desc=${error?.description}")
                                latch.countDown()
                            }
                        }

                        @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
                        override fun onReceivedError(
                            view: WebView?,
                            errorCode: Int,
                            description: String?,
                            failingUrl: String?,
                        ) {
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                                errorRef.set("code=$errorCode desc=$description url=$failingUrl")
                                latch.countDown()
                            }
                        }
                    }
                wv.loadUrl(url)
            }

            try {
                assertTrue(
                    "onPageFinished should fire within 30s (local MockWebServer)",
                    latch.await(30, TimeUnit.SECONDS),
                )
                assertNull(
                    "Main frame error: ${errorRef.get()}",
                    errorRef.get(),
                )
            } finally {
                inst.runOnMainSync {
                    wvHolder[0]?.destroy()
                }
            }
        } finally {
            server.shutdown()
        }
    }
}
