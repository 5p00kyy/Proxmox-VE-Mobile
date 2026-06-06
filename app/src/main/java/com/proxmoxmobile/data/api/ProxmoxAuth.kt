package com.proxmoxmobile.data.api

sealed class ProxmoxAuth {
    data object None : ProxmoxAuth()

    data class Ticket(
        val ticket: String,
        val csrfToken: String?
    ) : ProxmoxAuth()

    data class ApiToken(
        val value: String
    ) : ProxmoxAuth()
}
