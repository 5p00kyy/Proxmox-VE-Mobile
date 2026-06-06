package com.proxmoxmobile.data.model

import com.google.gson.annotations.SerializedName

// Authentication Models
data class LoginRequest(
    val username: String,
    val password: String,
    val realm: String = "pam"
)

data class LoginResponse(
    val data: LoginData
)

data class LoginData(
    @SerializedName("ticket")
    val ticket: String,
    @SerializedName("CSRFPreventionToken")
    val csrfToken: String,
    @SerializedName("username")
    val username: String
)

// Node Models
data class Node(
    val node: String,
    val status: String,
    val cpu: Double,
    val level: String,
    val maxcpu: Int,
    val maxmem: Long,
    val mem: Long,
    val ssl_fingerprint: String,
    val uptime: Long
)

data class NodeStatus(
    val node: String,
    val status: String,
    val cpu: Double,
    val maxcpu: Int,
    val mem: Long,
    val maxmem: Long,
    val uptime: Long,
    val loadavg: List<Double>,
    val kversion: String,
    val pveversion: String,
    val rootfs: RootFS,
    val swap: Swap,
    val idle: Int
)

data class RootFS(
    val avail: Long,
    val total: Long,
    val used: Long,
    val free: Long
)

data class Swap(
    val free: Long,
    val total: Long,
    val used: Long
)

// VM Models
data class VirtualMachine(
    val vmid: Int,
    val name: String,
    val status: String,
    val cpu: Double,
    val maxcpu: Int,
    val mem: Long,
    val maxmem: Long,
    val uptime: Long,
    val template: Boolean,
    val cpus: Int,
    val disk: Long,
    val diskread: Long,
    val diskwrite: Long,
    val netin: Long,
    val netout: Long,
    val qmpstatus: String,
    val running_machine: String?,
    val running_qemu: String?,
    val tags: String?
)

data class VMCreateRequest(
    val vmid: Int,
    val name: String,
    val cores: Int = 1,
    val memory: Int = 512,
    val ostype: String = "l26",
    val scsi0: String = "local-lvm:32",
    val net0: String = "virtio,bridge=vmbr0"
)

data class VMActionRequest(
    val action: String // start, stop, shutdown, reset, resume, suspend
)

data class VmSnapshot(
    val name: String,
    val description: String?,
    val snaptime: Long?,
    val vmstate: Int?,
    val parent: String?
)

// Container Models
data class Container(
    val vmid: Int,
    val name: String,
    val status: String,
    val cpu: Double,
    val maxcpu: Int,
    val mem: Long,
    val maxmem: Long,
    val uptime: Long,
    val template: Boolean,
    val cpus: Int,
    val disk: Long,
    val diskread: Long,
    val diskwrite: Long,
    val netin: Long,
    val netout: Long,
    val tags: String?
)

data class ContainerCreateRequest(
    val vmid: Int,
    val hostname: String,
    val ostemplate: String,
    val cores: Int = 1,
    val memory: Int = 512,
    val rootfs: String = "local-lvm:8",
    val net0: String = "name=eth0,bridge=vmbr0,ip=dhcp"
)

data class LxcSnapshot(
    val name: String,
    val description: String?,
    val snaptime: Long?,
    val vmstate: Int?,
    val parent: String?
)

// Storage Models
data class Storage(
    val storage: String,
    val type: String,
    val content: List<String>,
    val nodes: List<String>?,
    val shared: Boolean,
    val active: Boolean,
    val available: Long,
    val used: Long,
    val total: Long
)

data class StorageContent(
    val volid: String,
    val content: String,
    val size: Long?,
    val format: String?,
    val ctime: Long?,
    val notes: String?,
    val vmid: Int?,
    val used: Long?,
    val parent: String?,
    @SerializedName("protected")
    val protectedContent: Boolean?
)

// Network Models
data class NetworkInterface(
    val iface: String,
    val type: String,
    val method: String,
    val address: String?,
    val netmask: String?,
    val gateway: String?,
    val active: Boolean,
    val autostart: Boolean,
    val exists: Boolean,
    val families: List<String>
)

// User Models
data class User(
    val userid: String,
    val enable: Boolean,
    val expire: Long?,
    val firstname: String?,
    val lastname: String?,
    val email: String?,
    val comment: String?
)

data class UserCreateRequest(
    val userid: String,
    val password: String,
    val comment: String? = null,
    val email: String? = null,
    val firstname: String? = null,
    val lastname: String? = null,
    val enable: Boolean = true
)

// Task Models
data class Task(
    val upid: String?,
    val id: String,
    val node: String,
    val pid: Int,
    val pstart: Long,
    val type: String,
    val status: String,
    val exitstatus: String?,
    val starttime: Long,
    val endtime: Long?,
    val user: String,
    val saved: Boolean
)

data class TaskLogEntry(
    @SerializedName("n")
    val lineNumber: Int,
    @SerializedName("t")
    val text: String
)

// Backup Models
data class Backup(
    val volid: String,
    val size: Long,
    val format: String,
    val ctime: Long,
    val content: String,
    val notes: String?
)

data class BackupCreateRequest(
    val storage: String,
    val compress: String = "lz4",
    val mode: String = "snapshot",
    val notes: String? = null
)

// Cluster Models
data class ClusterNode(
    val node: String,
    val nodeid: Int,
    val ip: String,
    val local: Boolean,
    val name: String,
    val online: Boolean,
    val level: String,
    val votes: Int
)

data class ClusterStatus(
    val quorate: Boolean,
    val nodes: Int,
    val votes: Int,
    val expected_votes: Int,
    val highest_expected: Int,
    val total_height: Int,
    val name: String,
    val version: Int,
    val type: String
)

data class ClusterStatusEntry(
    val type: String?,
    val name: String?,
    val nodeid: Int?,
    val ip: String?,
    val local: Int?,
    val online: Int?,
    val level: String?,
    val quorate: Int?,
    val nodes: Int?,
    val votes: Int?,
    val expected_votes: Int?
)

// API Response Models
data class ApiResponse<T>(
    val data: T,
    val success: Boolean = true
)

data class ApiError(
    val data: String?,
    val errors: Map<String, String>?,
    val message: String
)

// Server Configuration
data class ServerConfig(
    val host: String,
    val port: Int = 8006,
    val username: String,
    val password: String? = null,
    val apiToken: String? = null,
    val realm: String = "pam",
    val useHttps: Boolean = true,
    val verifySsl: Boolean = true,
    val certificateFingerprint: String? = null
)

// Resource Usage Models
data class ResourceUsage(
    val cpu: Double,
    val memory: MemoryUsage,
    val disk: DiskUsage,
    val network: NetworkUsage
)

data class MemoryUsage(
    val used: Long,
    val total: Long,
    val percentage: Double
)

data class DiskUsage(
    val used: Long,
    val total: Long,
    val percentage: Double
)

data class NetworkUsage(
    val bytesIn: Long,
    val bytesOut: Long,
    val packetsIn: Long,
    val packetsOut: Long
)
