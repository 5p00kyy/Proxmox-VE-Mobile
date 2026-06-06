package com.proxmoxmobile.data.security

import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.util.Locale

object CertificateFingerprint {
    fun normalize(value: String?): String? {
        val normalized = value
            ?.replace("sha256/", "", ignoreCase = true)
            ?.filter { it.isLetterOrDigit() }
            ?.uppercase(Locale.US)
            ?.takeIf { it.isNotBlank() }

        return normalized?.takeIf { it.length == 64 && it.all { char -> char in '0'..'9' || char in 'A'..'F' } }
    }

    fun sha256(certificate: X509Certificate): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(certificate.encoded)
        return digest.joinToString(separator = "") { byte -> "%02X".format(byte) }
    }

    fun matches(certificate: X509Certificate, expectedFingerprint: String): Boolean {
        val normalizedExpected = normalize(expectedFingerprint) ?: return false
        return sha256(certificate) == normalizedExpected
    }
}
