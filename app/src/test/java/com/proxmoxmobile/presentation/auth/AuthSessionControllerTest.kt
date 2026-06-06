package com.proxmoxmobile.presentation.auth

import com.proxmoxmobile.data.api.ProxmoxApiService
import com.proxmoxmobile.data.api.ProxmoxAuth
import com.proxmoxmobile.data.model.ServerConfig
import com.proxmoxmobile.data.session.AuthSessionService
import com.proxmoxmobile.data.session.AuthenticatedSession
import java.lang.reflect.Proxy
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthSessionControllerTest {
    @Test
    fun authenticate_successStoresSessionState() = runBlocking {
        val serverConfig = serverConfig(password = "secret")
        val service = FakeAuthSessionService(
            result = Result.success(ticketSession(serverConfig))
        )
        val controller = AuthSessionController(service)

        controller.authenticate(serverConfig)

        assertTrue(controller.isAuthenticated.value)
        assertEquals(serverConfig.copy(password = null), controller.currentServer.value)
        assertEquals("ticket-value", controller.authToken.value)
        assertEquals("csrf-value", controller.csrfToken.value)
        assertEquals("\u2705 Authentication successful!", controller.errorMessage.value)
        assertFalse(controller.isLoading.value)
        assertEquals(0, service.logoutCalls)
    }

    @Test
    fun authenticate_failureClearsSessionStateAndLogsOut() = runBlocking {
        val service = FakeAuthSessionService(
            result = Result.failure(Exception("Invalid credentials"))
        )
        val controller = AuthSessionController(service)
        controller.setAuthenticated(true)

        controller.authenticate(serverConfig(password = "bad-secret"))

        assertFalse(controller.isAuthenticated.value)
        assertNull(controller.authToken.value)
        assertNull(controller.csrfToken.value)
        assertEquals("\u274c Invalid credentials", controller.errorMessage.value)
        assertFalse(controller.isLoading.value)
        assertEquals(1, service.logoutCalls)
    }

    @Test
    fun createApiService_returnsServiceOnlyWhenActiveSessionExists() {
        val serverConfig = serverConfig(password = "secret").copy(password = null)
        val service = FakeAuthSessionService(
            result = Result.success(ticketSession(serverConfig)),
            activeSession = ticketSession(serverConfig)
        )
        val controller = AuthSessionController(service)

        assertNotNull(controller.createApiService())
        assertEquals(1, service.apiServiceCalls)

        service.activeSession = null

        assertNull(controller.createApiService())
        assertEquals(1, service.apiServiceCalls)
    }

    @Test
    fun logoutClearsAuthStateAndDelegatesToSessionService() {
        val controller = AuthSessionController(FakeAuthSessionService())
        controller.setAuthenticated(true)
        controller.setCurrentServer(serverConfig(password = null))
        controller.setErrorMessage("Some error")

        controller.logout()

        assertFalse(controller.isAuthenticated.value)
        assertNull(controller.currentServer.value)
        assertNull(controller.authToken.value)
        assertNull(controller.csrfToken.value)
        assertNull(controller.errorMessage.value)
    }

    private class FakeAuthSessionService(
        private val result: Result<AuthenticatedSession> = Result.failure(Exception("Not configured")),
        var activeSession: AuthenticatedSession? = null
    ) : AuthSessionService {
        var logoutCalls = 0
            private set
        var apiServiceCalls = 0
            private set

        override suspend fun authenticate(serverConfig: ServerConfig): Result<AuthenticatedSession> {
            result.onSuccess { activeSession = it }
            return result
        }

        override fun createApiService(): ProxmoxApiService? {
            apiServiceCalls += 1
            return fakeApiService()
        }

        override fun currentSession(): AuthenticatedSession? {
            return activeSession
        }

        override fun logout() {
            logoutCalls += 1
            activeSession = null
        }
    }

    companion object {
        private fun ticketSession(serverConfig: ServerConfig): AuthenticatedSession {
            return AuthenticatedSession(
                serverConfig = serverConfig.copy(password = null, apiToken = null),
                auth = ProxmoxAuth.Ticket(
                    ticket = "ticket-value",
                    csrfToken = "csrf-value"
                ),
                authToken = "ticket-value",
                csrfToken = "csrf-value",
                username = "${serverConfig.username}@${serverConfig.realm}"
            )
        }

        private fun serverConfig(password: String?): ServerConfig {
            return ServerConfig(
                host = "pve.local",
                username = "root",
                password = password,
                realm = "pam",
                useHttps = true,
                verifySsl = true
            )
        }

        private fun fakeApiService(): ProxmoxApiService {
            return Proxy.newProxyInstance(
                ProxmoxApiService::class.java.classLoader,
                arrayOf(ProxmoxApiService::class.java)
            ) { _, method, _ ->
                when (method.name) {
                    "toString" -> "FakeProxmoxApiService"
                    "hashCode" -> 1
                    else -> error("Unexpected API call in auth controller test: ${method.name}")
                }
            } as ProxmoxApiService
        }
    }
}
