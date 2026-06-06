package com.proxmoxmobile.data.api

import com.proxmoxmobile.data.model.ServerConfig

@Deprecated(
    message = "Use ProxmoxApiFactory or SessionManager so TLS and auth policy stay centralized.",
    replaceWith = ReplaceWith("ProxmoxApiFactory()")
)
class ProxmoxApiClient(
    private val apiFactory: ProxmoxApiFactory = ProxmoxApiFactory()
) {
    fun createApiService(serverConfig: ServerConfig, authToken: String? = null, csrfToken: String? = null): ProxmoxApiService {
        val auth = if (authToken.isNullOrBlank()) {
            ProxmoxAuth.None
        } else {
            ProxmoxAuth.Ticket(authToken, csrfToken)
        }
        return apiFactory.createApiService(serverConfig, auth)
    }
}
