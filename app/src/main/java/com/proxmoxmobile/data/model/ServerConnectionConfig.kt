package com.proxmoxmobile.data.model

import java.net.URI

private const val DEFAULT_PROXMOX_PORT = 8006

private val LOCAL_CLEARTEXT_HOSTS = setOf(
    "localhost",
    "127.0.0.1",
    "10.0.2.2",
    "::1",
    "[::1]"
)

fun ServerConfig.normalizedForConnection(): ServerConfig {
    val endpoint = ServerEndpoint.parse(
        rawHost = host,
        fallbackPort = port,
        fallbackUseHttps = useHttps
    )
    val normalized = copy(
        host = endpoint.host,
        port = endpoint.port,
        useHttps = endpoint.useHttps
    )

    normalized.requireAllowedTransport()
    return normalized
}

fun ServerConfig.requireAllowedTransport() {
    require(host.isNotBlank()) { "Host cannot be empty" }
    require(port in 1..65535) { "Port must be between 1 and 65535" }
    require(useHttps || host.isLocalCleartextHost()) {
        "HTTP is only supported for localhost or Android emulator test endpoints. Use HTTPS with port 8006 for Proxmox hosts."
    }
}

private data class ServerEndpoint(
    val host: String,
    val port: Int,
    val useHttps: Boolean
) {
    companion object {
        fun parse(
            rawHost: String,
            fallbackPort: Int,
            fallbackUseHttps: Boolean
        ): ServerEndpoint {
            val trimmed = rawHost.trim()
            require(trimmed.isNotBlank()) { "Host cannot be empty" }

            val hasScheme = trimmed.contains("://")
            val candidate = if (hasScheme) trimmed else "proxmox://$trimmed"
            val uri = URI(candidate)
            val scheme = uri.scheme?.lowercase()
            val useHttps = when (scheme) {
                "https" -> true
                "http" -> fallbackUseHttps
                "proxmox" -> fallbackUseHttps
                else -> throw IllegalArgumentException("Unsupported server URL scheme: $scheme")
            }
            val parsedHost = uri.host
                ?: uri.authority
                    ?.substringBefore('@')
                    ?.substringBefore(':')
                    ?.trim('[', ']')
            require(!parsedHost.isNullOrBlank()) { "Host cannot be empty" }

            return ServerEndpoint(
                host = parsedHost,
                port = uri.port.takeIf { it > 0 } ?: fallbackPort,
                useHttps = useHttps
            )
        }
    }
}

private fun String.isLocalCleartextHost(): Boolean {
    val normalized = trim().trim('[', ']').lowercase()
    return normalized in LOCAL_CLEARTEXT_HOSTS.map { it.trim('[', ']').lowercase() }
}
