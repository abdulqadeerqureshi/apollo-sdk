package pk.apollobuddy.sdk

import android.net.Uri
import android.webkit.URLUtil
import androidx.core.net.toUri

object UrlValidator {

    fun isValid(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        if (!URLUtil.isNetworkUrl(url)) return false

        val uri: Uri = url.toUri()
        if (uri.scheme?.lowercase() !in listOf("http", "https")) return false
        if (uri.host.isNullOrBlank()) return false

        return true
    }
}
