package com.proxmoxmobile.data.api

import com.proxmoxmobile.data.model.*
import retrofit2.http.*

interface ProxmoxApiService {

    // Version check (no auth required)
    @GET("api2/json/version")
    suspend fun getVersion(): ApiResponse<Map<String, String>>

    // Authentication
    @POST("api2/json/access/ticket")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    // Nodes
    @GET("api2/json/nodes")
    suspend fun getNodes(): ApiResponse<List<Node>>

    @GET("api2/json/nodes/{node}/status")
    suspend fun getNodeStatus(@Path("node") node: String): ApiResponse<NodeStatus>

    @GET("api2/json/nodes/{node}/rrd")
    suspend fun getNodeRRD(
        @Path("node") node: String,
        @Query("timeframe") timeframe: String = "hour"
    ): ApiResponse<Map<String, Any>>

    // Virtual Machines
    @GET("api2/json/nodes/{node}/qemu")
    suspend fun getVirtualMachines(@Path("node") node: String): ApiResponse<List<VirtualMachine>>

    @GET("api2/json/nodes/{node}/qemu/{vmid}/status/current")
    suspend fun getVMStatus(
        @Path("node") node: String,
        @Path("vmid") vmid: Int
    ): ApiResponse<VirtualMachine>

    @GET("api2/json/nodes/{node}/qemu/{vmid}/config")
    suspend fun getVMConfig(
        @Path("node") node: String,
        @Path("vmid") vmid: Int
    ): ApiResponse<Map<String, Any?>>

    @POST("api2/json/nodes/{node}/qemu")
    suspend fun createVM(
        @Path("node") node: String,
        @Body request: VMCreateRequest
    ): ApiResponse<Map<String, String>>

    @POST("api2/json/nodes/{node}/qemu/{vmid}/status/{action}")
    suspend fun performVMAction(
        @Path("node") node: String,
        @Path("vmid") vmid: Int,
        @Path("action") action: String
    ): ApiResponse<String>

    @DELETE("api2/json/nodes/{node}/qemu/{vmid}")
    suspend fun deleteVM(
        @Path("node") node: String,
        @Path("vmid") vmid: Int
    ): ApiResponse<String>

    @GET("api2/json/nodes/{node}/qemu/{vmid}/rrd")
    suspend fun getVMRRD(
        @Path("node") node: String,
        @Path("vmid") vmid: Int,
        @Query("timeframe") timeframe: String = "hour"
    ): ApiResponse<Map<String, Any>>

    // Containers
    @GET("api2/json/nodes/{node}/lxc")
    suspend fun getContainers(@Path("node") node: String): ApiResponse<List<Container>>

    @GET("api2/json/nodes/{node}/lxc/{vmid}/status/current")
    suspend fun getContainerStatus(
        @Path("node") node: String,
        @Path("vmid") vmid: Int
    ): ApiResponse<Container>

    @POST("api2/json/nodes/{node}/lxc")
    suspend fun createContainer(
        @Path("node") node: String,
        @Body request: ContainerCreateRequest
    ): ApiResponse<Map<String, String>>

    @POST("api2/json/nodes/{node}/lxc/{vmid}/status/{action}")
    suspend fun performContainerAction(
        @Path("node") node: String,
        @Path("vmid") vmid: Int,
        @Path("action") action: String
    ): ApiResponse<String>

    @DELETE("api2/json/nodes/{node}/lxc/{vmid}")
    suspend fun deleteContainer(
        @Path("node") node: String,
        @Path("vmid") vmid: Int
    ): ApiResponse<String>

    @GET("api2/json/nodes/{node}/lxc/{vmid}/rrd")
    suspend fun getContainerRRD(
        @Path("node") node: String,
        @Path("vmid") vmid: Int,
        @Query("timeframe") timeframe: String = "hour"
    ): ApiResponse<Map<String, Any>>

    @GET("api2/json/nodes/{node}/lxc/{vmid}/snapshot")
    suspend fun getLXCSnapshots(
        @Path("node") node: String,
        @Path("vmid") vmid: Int
    ): ApiResponse<List<LxcSnapshot>>

    // Storage
    @GET("api2/json/nodes/{node}/storage")
    suspend fun getStorages(@Path("node") node: String): ApiResponse<List<Storage>>

    @GET("api2/json/nodes/{node}/storage/{storage}/content")
    suspend fun getStorageContent(
        @Path("node") node: String,
        @Path("storage") storage: String
    ): ApiResponse<List<StorageContent>>

    @GET("api2/json/nodes/{node}/storage/{storage}/rrd")
    suspend fun getStorageRRD(
        @Path("node") node: String,
        @Path("storage") storage: String,
        @Query("timeframe") timeframe: String = "hour"
    ): ApiResponse<Map<String, Any>>

    // Network
    @GET("api2/json/nodes/{node}/network")
    suspend fun getNetworkInterfaces(@Path("node") node: String): ApiResponse<List<NetworkInterface>>

    @GET("api2/json/nodes/{node}/network/{iface}")
    suspend fun getNetworkInterface(
        @Path("node") node: String,
        @Path("iface") iface: String
    ): ApiResponse<NetworkInterface>

    // Users
    @GET("api2/json/access/users")
    suspend fun getUsers(): ApiResponse<List<User>>

    @POST("api2/json/access/users")
    suspend fun createUser(@Body request: UserCreateRequest): ApiResponse<Map<String, String>>

    @PUT("api2/json/access/users/{userid}")
    suspend fun updateUser(
        @Path("userid") userid: String,
        @Body request: UserCreateRequest
    ): ApiResponse<Map<String, String>>

    @DELETE("api2/json/access/users/{userid}")
    suspend fun deleteUser(@Path("userid") userid: String): ApiResponse<Map<String, String>>

    // Tasks
    @GET("api2/json/nodes/{node}/tasks")
    suspend fun getTasks(
        @Path("node") node: String,
        @Query("limit") limit: Int = 50,
        @Query("start") start: Int = 0,
        @Query("statusfilter") statusFilter: String? = null,
        @Query("typefilter") typeFilter: String? = null,
        @Query("vmid") vmid: Int? = null
    ): ApiResponse<List<Task>>

    @GET("api2/json/nodes/{node}/tasks/{upid}/status")
    suspend fun getTaskStatus(
        @Path("node") node: String,
        @Path("upid") upid: String
    ): ApiResponse<Task>

    @GET("api2/json/nodes/{node}/tasks/{upid}/log")
    suspend fun getTaskLog(
        @Path("node") node: String,
        @Path("upid") upid: String,
        @Query("start") start: Int = 0,
        @Query("limit") limit: Int = 500
    ): ApiResponse<List<TaskLogEntry>>

    @DELETE("api2/json/nodes/{node}/tasks/{upid}")
    suspend fun deleteTask(
        @Path("node") node: String,
        @Path("upid") upid: String
    ): ApiResponse<Map<String, String>>

    // Backups
    @POST("api2/json/nodes/{node}/qemu/{vmid}/backup")
    suspend fun createBackup(
        @Path("node") node: String,
        @Path("vmid") vmid: Int,
        @Body request: BackupCreateRequest
    ): ApiResponse<Map<String, String>>

    @DELETE("api2/json/nodes/{node}/storage/{storage}/content/{volume}")
    suspend fun deleteBackup(
        @Path("node") node: String,
        @Path("storage") storage: String,
        @Path("volume") volume: String
    ): ApiResponse<Map<String, String>>

    // Cluster
    @GET("api2/json/cluster/status")
    suspend fun getClusterStatus(): ApiResponse<List<ClusterStatusEntry>>

    @GET("api2/json/cluster/resources")
    suspend fun getClusterResources(): ApiResponse<List<Map<String, Any>>>

    // System
    @GET("api2/json/nodes/{node}/system/version")
    suspend fun getSystemVersion(@Path("node") node: String): ApiResponse<Map<String, String>>

    @GET("api2/json/nodes/{node}/system/dns")
    suspend fun getSystemDNS(@Path("node") node: String): ApiResponse<Map<String, String>>

    @GET("api2/json/nodes/{node}/system/time")
    suspend fun getSystemTime(@Path("node") node: String): ApiResponse<Map<String, String>>

    // Firewall
    @GET("api2/json/nodes/{node}/firewall/rules")
    suspend fun getFirewallRules(@Path("node") node: String): ApiResponse<List<Map<String, Any>>>

    @GET("api2/json/nodes/{node}/firewall/aliases")
    suspend fun getFirewallAliases(@Path("node") node: String): ApiResponse<List<Map<String, Any>>>

    // HA (High Availability)
    @GET("api2/json/cluster/ha/status/current")
    suspend fun getHAStatus(): ApiResponse<List<Map<String, Any>>>

    @GET("api2/json/cluster/ha/resources")
    suspend fun getHAResources(): ApiResponse<List<Map<String, Any>>>

    // Replication
    @GET("api2/json/nodes/{node}/replication")
    suspend fun getReplicationJobs(@Path("node") node: String): ApiResponse<List<Map<String, Any>>>

    // Snapshots
    @GET("api2/json/nodes/{node}/qemu/{vmid}/snapshot")
    suspend fun getVMSnapshots(
        @Path("node") node: String,
        @Path("vmid") vmid: Int
    ): ApiResponse<List<VmSnapshot>>

    @POST("api2/json/nodes/{node}/qemu/{vmid}/snapshot")
    suspend fun createVMSnapshot(
        @Path("node") node: String,
        @Path("vmid") vmid: Int,
        @Query("snapname") snapname: String
    ): ApiResponse<Map<String, String>>

    @DELETE("api2/json/nodes/{node}/qemu/{vmid}/snapshot/{snapname}")
    suspend fun deleteVMSnapshot(
        @Path("node") node: String,
        @Path("vmid") vmid: Int,
        @Path("snapname") snapname: String
    ): ApiResponse<Map<String, String>>

    // Console
    @GET("api2/json/nodes/{node}/qemu/{vmid}/vncwebsocket")
    suspend fun getVNCWebSocket(
        @Path("node") node: String,
        @Path("vmid") vmid: Int,
        @Query("ticket") ticket: String,
        @Query("vncticket") vncticket: String
    ): ApiResponse<Map<String, String>>

    // File Browser
    @GET("api2/json/nodes/{node}/storage/{storage}/browse")
    suspend fun browseStorage(
        @Path("node") node: String,
        @Path("storage") storage: String,
        @Query("path") path: String = "/"
    ): ApiResponse<List<Map<String, Any>>>
}
