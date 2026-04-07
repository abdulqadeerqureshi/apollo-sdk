package pk.apollobuddy.sdk

import androidx.core.net.toUri

/**
 * Helpers to redact sensitive values before logging or analytics. The SDK does not log raw URLs or
 * credentials; use these when your app logs [ApolloBuddyResult] or URLs.
 */
object SensitiveDataRedaction {

    /**
     * Returns a URL safe to log: scheme and host only, path replaced with `/…` if present,
     * query and fragment stripped.
     */
    fun redactUrl(url: String?): String? {
        if (url.isNullOrBlank()) return url
        return try {
            val u = url.toUri()
            val scheme = u.scheme ?: return "[invalid]"
            val host = u.host ?: return "[invalid]"
            val path = u.path
            val pathPart = if (path.isNullOrEmpty() || path == "/") "" else "/…"
            "$scheme://$host$pathPart"
        } catch (_: Exception) {
            "[redacted]"
        }
    }

    /**
     * Removes likely query parameters from a message string (best-effort; for debug lines only).
     */
    fun redactQueryParamsInMessage(message: String): String {
        return message.replace(Regex("[?&](token|access_token|code|state|session)=[^&\\s]+"), "?$1=[redacted]")
    }
}
