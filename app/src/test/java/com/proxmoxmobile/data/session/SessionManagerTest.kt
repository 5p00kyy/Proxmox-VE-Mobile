package com.proxmoxmobile.data.session

import com.proxmoxmobile.data.api.ProxmoxApiService
import com.proxmoxmobile.data.api.ProxmoxApiServiceFactory
import com.proxmoxmobile.data.api.ProxmoxAuthenticationService
import com.proxmoxmobile.data.api.ProxmoxAuth
import com.proxmoxmobile.data.model.ApiResponse
import com.proxmoxmobile.data.model.LoginData
import com.proxmoxmobile.data.model.LoginResponse
import com.proxmoxmobile.data.model.ServerConfig
import com.proxmoxmobile.data.security.TlsPolicy
import java.lang.reflect.Proxy
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionManagerTest {
    @Test
    fun authenticate_withPasswordStoresTicketSessionAndStripsSecrets() = runBlocking {
        val factory = FakeApiServiceFactory()
        val manager = SessionManager(
            apiFactory = factory,
            authenticationService = FakeAuthenticationService()
        )

        val result = manager.authenticate(
            serverConfig(
                password = "secret"
            )
        )

        assertTrue(result.isSuccess)
        val session = result.getOrThrow()
        assertEquals("tester@pam", session.username)
        assertEquals("ticket-value", session.authToken)
        assertEquals("csrf-value", session.csrfToken)
        assertNull(session.serverConfig.password)
        assertNull(session.serverConfig.apiToken)
        assertTrue(session.auth is ProxmoxAuth.Ticket)
        assertSame(session, manager.currentSession())

        manager.createApiService()
        assertEquals(1, factory.authRequests.size)
        assertTrue(factory.authRequests.single() is ProxmoxAuth.Ticket)
    }

    @Test
    fun authenticate_withApiTokenPingsVersionStoresTokenSessionAndStripsSecrets() = runBlocking {
        val factory = FakeApiServiceFactory()
        val manager = SessionManager(
            apiFactory = factory,
            authenticationService = FakeAuthenticationService()
        )

        val result = manager.authenticate(
            serverConfig(
                password = null,
                apiToken = "tester@pam!mobile=token-secret"
            )
        )

        assertTrue(result.isSuccess)
        val session = result.getOrThrow()
        assertEquals("tester", session.username)
        assertNull(session.authToken)
        assertNull(session.csrfToken)
        assertNull(session.serverConfig.password)
        assertNull(session.serverConfig.apiToken)
        assertTrue(session.auth is ProxmoxAuth.ApiToken)
        assertEquals("tester@pam!mobile=token-secret", (session.auth as ProxmoxAuth.ApiToken).value)
        assertEquals(1, factory.versionChecks)

        manager.createApiService()
        assertTrue(factory.authRequests.first() is ProxmoxAuth.ApiToken)
        assertTrue(factory.authRequests.last() is ProxmoxAuth.ApiToken)
    }

    @Test
    fun authenticate_withInvalidApiTokenConfigFailsWithoutActiveSession() = runBlocking {
        val manager = SessionManager(
            apiFactory = FakeApiServiceFactory(),
            authenticationService = FakeAuthenticationService()
        )

        val result = manager.authenticate(
            serverConfig(host = "", password = null, apiToken = "tester@pam!mobile=token-secret")
        )

        assertTrue(result.isFailure)
        assertEquals("Host cannot be empty", result.exceptionOrNull()?.message)
        assertNull(manager.currentSession())
        assertNull(manager.createApiService())
    }

    @Test
    fun authenticate_withFailedPasswordLoginClearsPreviousSession() = runBlocking {
        val manager = SessionManager(
            apiFactory = FakeApiServiceFactory(),
            authenticationService = FakeAuthenticationService()
        )

        manager.authenticate(serverConfig(password = "secret")).getOrThrow()

        val result = manager.authenticate(serverConfig(host = "", password = "secret"))

        assertTrue(result.isFailure)
        assertEquals("Host cannot be empty", result.exceptionOrNull()?.message)
        assertNull(manager.currentSession())
        assertNull(manager.createApiService())
    }

    @Test
    fun authenticate_withFailedApiTokenPingClearsPreviousSessionAndMapsMessage() = runBlocking {
        val factory = FakeApiServiceFactory(
            versionException = RuntimeException("Invalid username, password, API token, or TFA challenge")
        )
        val manager = SessionManager(
            apiFactory = factory,
            authenticationService = FakeAuthenticationService()
        )

        manager.authenticate(serverConfig(password = "secret")).getOrThrow()

        val result = manager.authenticate(
            serverConfig(
                password = null,
                apiToken = "tester@pam!mobile=bad-token-secret"
            )
        )

        assertTrue(result.isFailure)
        assertEquals("Invalid username, password, API token, or TFA challenge", result.exceptionOrNull()?.message)
        assertNull(manager.currentSession())
        assertNull(manager.createApiService())
    }

    @Test
    fun authenticate_withApiTokenRejectsReleaseInsecureTlsPolicyFailure() = runBlocking {
        val factory = FakeApiServiceFactory(
            createException = IllegalArgumentException(TlsPolicy.RELEASE_INSECURE_TLS_MESSAGE)
        )
        val manager = SessionManager(
            apiFactory = factory,
            authenticationService = FakeAuthenticationService()
        )

        val result = manager.authenticate(
            serverConfig(
                password = null,
                apiToken = "tester@pam!mobile=token-secret"
            ).copy(verifySsl = false)
        )

        assertTrue(result.isFailure)
        assertEquals(TlsPolicy.RELEASE_INSECURE_TLS_MESSAGE, result.exceptionOrNull()?.message)
        assertNull(manager.currentSession())
    }

    @Test
    fun logoutClearsActiveSession() = runBlocking {
        val manager = SessionManager(
            apiFactory = FakeApiServiceFactory(),
            authenticationService = FakeAuthenticationService()
        )

        manager.authenticate(serverConfig(password = "secret")).getOrThrow()
        manager.logout()

        assertNull(manager.currentSession())
        assertNull(manager.createApiService())
    }

    private class FakeApiServiceFactory(
        private val createException: RuntimeException? = null,
        private val versionException: RuntimeException? = null
    ) : ProxmoxApiServiceFactory {
        val authRequests = mutableListOf<ProxmoxAuth>()
        var versionChecks = 0
            private set

        override fun createApiService(
            serverConfig: ServerConfig,
            auth: ProxmoxAuth
        ): ProxmoxApiService {
            createException?.let { throw it }
            authRequests += auth
            return Proxy.newProxyInstance(
                ProxmoxApiService::class.java.classLoader,
                arrayOf(ProxmoxApiService::class.java)
            ) { _, method, _ ->
                when (method.name) {
                    "login" -> LoginResponse(
                        LoginData(
                            ticket = "ticket-value",
                            csrfToken = "csrf-value",
                            username = "${serverConfig.username}@${serverConfig.realm}"
                        )
                    )
                    "getVersion" -> {
                        versionChecks += 1
                        versionException?.let { throw it }
                        ApiResponse(mapOf("version" to "8.2.0"))
                    }
                    "toString" -> "FakeProxmoxApiService"
                    "hashCode" -> System.identityHashCode(this)
                    else -> error("Unexpected API call in session test: ${method.name}")
                }
            } as ProxmoxApiService
        }
    }

    private class FakeAuthenticationService : ProxmoxAuthenticationService {
        override suspend fun authenticate(serverConfig: ServerConfig): Result<LoginResponse> {
            require(serverConfig.host.isNotBlank()) { "Host cannot be empty" }
            require(serverConfig.username.isNotBlank()) { "Username cannot be empty" }
            require(!serverConfig.password.isNullOrBlank()) { "Password cannot be empty" }

            return Result.success(
                LoginResponse(
                    LoginData(
                        ticket = "ticket-value",
                        csrfToken = "csrf-value",
                        username = "${serverConfig.username}@${serverConfig.realm}"
                    )
                )
            )
        }

        override fun toUserFacingException(e: Exception): Exception {
            return e
        }
    }

    private fun serverConfig(
        host: String = "example.test",
        password: String? = "secret",
        apiToken: String? = null
    ): ServerConfig {
        return ServerConfig(
            host = host,
            username = "tester",
            password = password,
            apiToken = apiToken,
            realm = "pam",
            useHttps = true,
            verifySsl = true
        )
    }
}
