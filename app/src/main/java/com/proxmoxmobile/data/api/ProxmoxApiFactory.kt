package com.proxmoxmobile.data.api

import android.util.Log
import com.proxmoxmobile.BuildConfig
import com.proxmoxmobile.data.model.ServerConfig
import com.proxmoxmobile.data.security.CertificateFingerprint
import com.proxmoxmobile.data.security.SensitiveLogRedactor
import com.proxmoxmobile.data.security.TlsPolicy
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLPeerUnverifiedException
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

interface ProxmoxApiServiceFactory {
    fun createApiService(
        serverConfig: ServerConfig,
        auth: ProxmoxAuth = ProxmoxAuth.None
    ): ProxmoxApiService
}

class ProxmoxApiFactory : ProxmoxApiServiceFactory {
    companion object {
        private const val TAG = "ProxmoxApiFactory"
        private const val USER_AGENT = "ProxmoxVEMobile/1.0"
    }

    override fun createApiService(
        serverConfig: ServerConfig,
        auth: ProxmoxAuth
    ): ProxmoxApiService {
        val retrofit = Retrofit.Builder()
            .baseUrl(serverConfig.baseUrl())
            .client(createOkHttpClient(serverConfig, auth))
            .addConverterFactory(GsonConverterFactory.create(ProxmoxGsonFactory.create()))
            .build()

        return retrofit.create(ProxmoxApiService::class.java)
    }

    private fun createOkHttpClient(
        serverConfig: ServerConfig,
        auth: ProxmoxAuth
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .addInterceptor(createLoggingInterceptor())
            .addInterceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "application/json")

                when (auth) {
                    ProxmoxAuth.None -> Unit
                    is ProxmoxAuth.Ticket -> {
                        requestBuilder.header("Cookie", "PVEAuthCookie=${auth.ticket}")
                        auth.csrfToken
                            ?.takeIf { it.isNotBlank() }
                            ?.let { requestBuilder.header("CSRFPreventionToken", it) }
                    }
                    is ProxmoxAuth.ApiToken -> {
                        requestBuilder.header("Authorization", "PVEAPIToken=${auth.value}")
                    }
                }

                chain.proceed(requestBuilder.method(original.method, original.body).build())
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)

        val pinnedFingerprint = CertificateFingerprint.normalize(serverConfig.certificateFingerprint)
        if (serverConfig.useHttps && pinnedFingerprint != null) {
            Log.i(TAG, "Using pinned certificate fingerprint for configured HTTPS server")
            val trustManager = createPinnedTrustManager(pinnedFingerprint)
            val sslContext = SSLContext.getInstance("TLS").apply {
                init(null, arrayOf<TrustManager>(trustManager), null)
            }

            builder
                .sslSocketFactory(sslContext.socketFactory, trustManager)
                .hostnameVerifier { _, session ->
                    try {
                        val certificate = session.peerCertificates.firstOrNull() as? X509Certificate
                        certificate != null && CertificateFingerprint.matches(certificate, pinnedFingerprint)
                    } catch (e: SSLPeerUnverifiedException) {
                        false
                    }
                }
        } else if (serverConfig.useHttps && !serverConfig.verifySsl) {
            TlsPolicy.requireInsecureTlsAllowed(BuildConfig.ALLOW_INSECURE_TLS)
            Log.w(TAG, "SSL verification disabled for configured HTTPS server; debug-only trusted lab mode")
            val trustManager = createInsecureTrustManager()
            val sslContext = SSLContext.getInstance("TLS").apply {
                init(null, arrayOf<TrustManager>(trustManager), null)
            }

            builder
                .sslSocketFactory(sslContext.socketFactory, trustManager)
                .hostnameVerifier { _, _ -> true }
        }

        return builder.build()
    }

    private fun createLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor { message ->
            Log.d(TAG, "HTTP: ${SensitiveLogRedactor.redact(message)}")
        }.apply {
            level = HttpLoggingInterceptor.Level.BASIC
            redactHeader("Authorization")
            redactHeader("Cookie")
            redactHeader("CSRFPreventionToken")
        }
    }

    private fun createPinnedTrustManager(expectedFingerprint: String): X509TrustManager {
        return object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit

            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                val leafCertificate = chain.firstOrNull()
                    ?: throw CertificateException("Server certificate chain is empty")

                if (!CertificateFingerprint.matches(leafCertificate, expectedFingerprint)) {
                    throw CertificateException("Server certificate fingerprint did not match the configured pin")
                }
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }
    }

    private fun createInsecureTrustManager(): X509TrustManager {
        return object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) = Unit
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }
    }

    private fun ServerConfig.baseUrl(): String {
        val protocol = if (useHttps) "https" else "http"
        return "$protocol://$host:$port/"
    }

}
