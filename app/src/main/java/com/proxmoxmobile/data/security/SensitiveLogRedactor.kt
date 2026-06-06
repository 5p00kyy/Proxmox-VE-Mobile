package com.proxmoxmobile.data.security

object SensitiveLogRedactor {
    private val authCookiePattern = Regex("PVEAuthCookie=[^;\\s]+")
    private val apiTokenPattern = Regex("PVEAPIToken=[^\\s]+")
    private val csrfTokenPattern = Regex("CSRFPreventionToken\\s*[:=]\\s*[^\\s]+")
    private val secretKeyPattern = Regex(
        pattern = "\\b(password|token|secret)\\s*[:=]\\s*[^&\\s]+",
        option = RegexOption.IGNORE_CASE
    )

    fun redact(message: String): String {
        return message
            .replace(authCookiePattern, "PVEAuthCookie=<redacted>")
            .replace(apiTokenPattern, "PVEAPIToken=<redacted>")
            .replace(csrfTokenPattern, "CSRFPreventionToken=<redacted>")
            .replace(secretKeyPattern) { match ->
                "${match.groupValues[1]}=<redacted>"
            }
    }
}
