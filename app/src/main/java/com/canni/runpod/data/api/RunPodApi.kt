package com.canni.runpod.data.api

import com.canni.runpod.data.api.dto.CreateNetworkVolumeRequest
import com.canni.runpod.data.api.dto.CreatePodRequest
import com.canni.runpod.data.api.dto.ListDataCentersResponse
import com.canni.runpod.data.api.dto.ListGpuTypesResponse
import com.canni.runpod.data.api.dto.ListNetworkVolumesResponse
import com.canni.runpod.data.api.dto.ListPodsResponse
import com.canni.runpod.data.api.dto.ListTemplatesResponse
import com.canni.runpod.data.api.dto.NetworkVolume
import com.canni.runpod.data.api.dto.Template
import com.canni.runpod.data.api.dto.TemplateRequest
import com.canni.runpod.data.api.dto.CreateEndpointRequest
import com.canni.runpod.data.api.dto.ListCpuTypesResponse
import com.canni.runpod.data.api.dto.ListEndpointReleasesResponse
import com.canni.runpod.data.api.dto.ListEndpointWorkersResponse
import com.canni.runpod.data.api.dto.ListEndpointsResponse
import com.canni.runpod.data.api.dto.Pod
import com.canni.runpod.data.api.dto.ServerlessEndpoint
import com.canni.runpod.data.api.dto.SshKeys
import com.canni.runpod.data.api.dto.UpdateEndpointRequest
import com.canni.runpod.data.api.dto.UpdateNetworkVolumeRequest
import com.canni.runpod.data.api.dto.UpdateSshKeysRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface RunPodApi {

    @GET("pods")
    suspend fun listPods(
        @Query("includeClusterPods") includeClusterPods: Boolean = false,
    ): Response<ListPodsResponse>

    @GET("pods/{id}")
    suspend fun getPod(
        @Path("id") id: String,
    ): Response<Pod>

    @POST("pods/{id}/action")
    suspend fun podAction(
        @Path("id") id: String,
        @Body action: PodActionRequest,
    ): Response<Pod>

    @PATCH("pods/{id}")
    suspend fun updatePod(
        @Path("id") id: String,
        @Body body: com.canni.runpod.data.api.dto.UpdatePodLockedRequest,
    ): Response<Pod>

    @POST("pods")
    suspend fun createPod(
        @Body body: CreatePodRequest,
    ): Response<Pod>

    @GET("catalog/gpus")
    suspend fun listGpuTypes(
        @Query("include") include: String? = "AVAILABILITY",
        @Query("product") product: String? = "POD",
    ): Response<ListGpuTypesResponse>

    @GET("catalog/cpus")
    suspend fun listCpuTypes(
        @Query("include") include: String? = null,
        @Query("product") product: String? = null,
    ): Response<ListCpuTypesResponse>

    @GET("catalog/datacenters")
    suspend fun listDataCenters(): Response<ListDataCentersResponse>

    @GET("catalog/templates")
    suspend fun listPublicTemplates(
        @Query("source") source: String? = null,
    ): Response<ListTemplatesResponse>

    @GET("templates")
    suspend fun listTemplates(): Response<ListTemplatesResponse>

    @GET("templates/{id}")
    suspend fun getTemplate(
        @Path("id") id: String,
    ): Response<Template>

    @POST("templates")
    suspend fun createTemplate(
        @Body body: TemplateRequest,
    ): Response<Template>

    @PATCH("templates/{id}")
    suspend fun updateTemplate(
        @Path("id") id: String,
        @Body body: TemplateRequest,
    ): Response<Template>

    @DELETE("templates/{id}")
    suspend fun deleteTemplate(
        @Path("id") id: String,
    ): Response<Unit>

    @GET("network-volumes")
    suspend fun listNetworkVolumes(): Response<ListNetworkVolumesResponse>

    @POST("network-volumes")
    suspend fun createNetworkVolume(
        @Body body: CreateNetworkVolumeRequest,
    ): Response<NetworkVolume>

    @PATCH("network-volumes/{id}")
    suspend fun updateNetworkVolume(
        @Path("id") id: String,
        @Body body: UpdateNetworkVolumeRequest,
    ): Response<NetworkVolume>

    @DELETE("network-volumes/{id}")
    suspend fun deleteNetworkVolume(
        @Path("id") id: String,
    ): Response<Unit>

    @GET("billing")
    suspend fun billing(
        @Query("startTime") startTime: String? = null,
        @Query("endTime") endTime: String? = null,
        @Query("bucketSize") bucketSize: String? = null,
        @Query("lastN") lastN: Int? = null,
    ): Response<com.canni.runpod.data.api.dto.ListBillingResponse>

    @GET("account/ssh-keys")
    suspend fun listSshKeys(): Response<SshKeys>

    @PUT("account/ssh-keys")
    suspend fun replaceSshKeys(
        @Body body: UpdateSshKeysRequest,
    ): Response<SshKeys>

    @GET("billing/pods")
    suspend fun podBilling(
        @Query("startTime") startTime: String? = null,
        @Query("endTime") endTime: String? = null,
        @Query("bucketSize") bucketSize: String? = null,
        @Query("lastN") lastN: Int? = null,
        @Query("podId") podId: String? = null,
    ): Response<com.canni.runpod.data.api.dto.ListPodBillingResponse>

    @GET("serverless")
    suspend fun listEndpoints(): Response<ListEndpointsResponse>

    @GET("serverless/{id}")
    suspend fun getEndpoint(
        @Path("id") id: String,
    ): Response<ServerlessEndpoint>

    @POST("serverless")
    suspend fun createEndpoint(
        @Body body: CreateEndpointRequest,
    ): Response<ServerlessEndpoint>

    @PATCH("serverless/{id}")
    suspend fun updateEndpoint(
        @Path("id") id: String,
        @Body body: UpdateEndpointRequest,
    ): Response<ServerlessEndpoint>

    @DELETE("serverless/{id}")
    suspend fun deleteEndpoint(
        @Path("id") id: String,
    ): Response<Unit>

    @GET("serverless/{id}/releases")
    suspend fun listEndpointReleases(
        @Path("id") id: String,
    ): Response<ListEndpointReleasesResponse>

    @GET("serverless/{id}/workers")
    suspend fun listEndpointWorkers(
        @Path("id") id: String,
    ): Response<ListEndpointWorkersResponse>

    @GET("billing/serverless")
    suspend fun serverlessBilling(
        @Query("startTime") startTime: String? = null,
        @Query("endTime") endTime: String? = null,
        @Query("bucketSize") bucketSize: String? = null,
        @Query("lastN") lastN: Int? = null,
        @Query("serverlessId") serverlessId: String? = null,
    ): Response<com.canni.runpod.data.api.dto.ListServerlessBillingResponse>
}
