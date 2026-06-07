package com.proxmoxmobile.data.security

import com.proxmoxmobile.data.model.ServerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

data class ServerCertificateInfo(
    val host: String,
    val port: Int,
    val sha256Fingerprint: String,
    val subject: String,
    val issuer: String,
    val notBefore: String,
    val notAfter: String
)

class UntrustedServerCertificateException(
    val certificateInfo: ServerCertificateInfo
) : Exception("TLS certificate is not trusted. Review and trust this certificate fingerprint to continue.")

object ServerCertificateProbe {
    suspend fun fetch(serverConfig: ServerConfig): ServerCertificateInfo = withContext(Dispatchers.IO) {
        require(serverConfig.useHttps) { "Certificate trust is only available for HTTPS servers." }

        val trustManager = createProbeTrustManager()
        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf<TrustManager>(trustManager), null)
        }

        val socket = sslContext.socketFactory.createSocket() as SSLSocket
        socket.use { sslSocket ->
            sslSocket.connect(InetSocketAddress(serverConfig.host, serverConfig.port), 10_000)
            sslSocket.soTimeout = 10_000
            sslSocket.startHandshake()

            val certificate = sslSocket.session.peerCertificates.firstOrNull() as? X509Certificate
                ?: throw CertificateException("Server certificate chain is empty")

            ServerCertificateInfo(
                host = serverConfig.host,
                port = serverConfig.port,
                sha256Fingerprint = CertificateFingerprint.sha256(certificate),
                subject = certificate.subjectX500Principal.name,
                issuer = certificate.issuerX500Principal.name,
                notBefore = certificate.notBefore.toString(),
                notAfter = certificate.notAfter.toString()
            )
        }
    }

    suspend fun toTrustPromptException(
        exception: Exception,
        serverConfig: ServerConfig,
        fallback: Exception
    ): Exception {
        if (
            !serverConfig.useHttps ||
            CertificateFingerprint.normalize(serverConfig.certificateFingerprint) != null ||
            !exception.isCertificateTrustFailure()
        ) {
            return fallback
        }

        return runCatching {
            UntrustedServerCertificateException(fetch(serverConfig))
        }.getOrElse {
            fallback
        }
    }

    private fun createProbeTrustManager(): X509TrustManager {
        return object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) = Unit
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }
    }

    private fun Throwable.isCertificateTrustFailure(): Boolean {
        var current: Throwable? = this
        while (current != null) {
            if (current is SSLHandshakeException || current is CertificateException) {
                return true
            }
            current = current.cause
        }
        return false
    }
}
