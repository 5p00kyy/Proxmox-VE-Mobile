package com.proxmoxmobile.data.security

import java.math.BigInteger
import java.security.Principal
import java.security.PublicKey
import java.security.cert.X509Certificate
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CertificateFingerprintTest {
    @Test
    fun normalize_acceptsColonSeparatedAndSha256PrefixedFingerprints() {
        val fingerprint = "SHA256/ab:cd:ef:12:34:56:78:90:ab:cd:ef:12:34:56:78:90:ab:cd:ef:12:34:56:78:90:ab:cd:ef:12:34:56:78:90"

        assertEquals(
            "ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890",
            CertificateFingerprint.normalize(fingerprint)
        )
    }

    @Test
    fun normalize_rejectsInvalidFingerprints() {
        assertNull(CertificateFingerprint.normalize(""))
        assertNull(CertificateFingerprint.normalize("not-a-fingerprint"))
        assertNull(CertificateFingerprint.normalize("AA"))
    }

    @Test
    fun matches_comparesCertificateSha256() {
        val certificate = FakeCertificate(byteArrayOf(1, 2, 3, 4))
        val fingerprint = CertificateFingerprint.sha256(certificate)

        assertTrue(CertificateFingerprint.matches(certificate, fingerprint))
        assertTrue(CertificateFingerprint.matches(certificate, fingerprint.chunked(2).joinToString(":")))
        assertFalse(CertificateFingerprint.matches(certificate, "ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890"))
    }

    private class FakeCertificate(
        private val encodedBytes: ByteArray
    ) : X509Certificate() {
        override fun getEncoded(): ByteArray = encodedBytes
        override fun verify(key: PublicKey?) = Unit
        override fun verify(key: PublicKey?, sigProvider: String?) = Unit
        override fun toString(): String = "FakeCertificate"
        override fun getPublicKey(): PublicKey? = null
        override fun checkValidity() = Unit
        override fun checkValidity(date: Date?) = Unit
        override fun getVersion(): Int = 3
        override fun getSerialNumber(): BigInteger = BigInteger.ONE
        override fun getIssuerDN(): Principal? = null
        override fun getSubjectDN(): Principal? = null
        override fun getNotBefore(): Date = Date(0)
        override fun getNotAfter(): Date = Date(0)
        override fun getTBSCertificate(): ByteArray = encodedBytes
        override fun getSignature(): ByteArray = byteArrayOf()
        override fun getSigAlgName(): String = "NONE"
        override fun getSigAlgOID(): String = "0.0"
        override fun getSigAlgParams(): ByteArray? = null
        override fun getIssuerUniqueID(): BooleanArray? = null
        override fun getSubjectUniqueID(): BooleanArray? = null
        override fun getKeyUsage(): BooleanArray? = null
        override fun getBasicConstraints(): Int = -1
        override fun hasUnsupportedCriticalExtension(): Boolean = false
        override fun getCriticalExtensionOIDs(): Set<String>? = null
        override fun getNonCriticalExtensionOIDs(): Set<String>? = null
        override fun getExtensionValue(oid: String?): ByteArray? = null
    }
}
