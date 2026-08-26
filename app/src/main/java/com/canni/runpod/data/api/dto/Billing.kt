package com.canni.runpod.data.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class ListBillingResponse(
    val records: List<BillingRecord> = emptyList(),
    val metadata: BillingMetadata? = null,
)

data class AccountBalance(
    val clientBalance: Double? = null,
    val currentSpendPerHr: Double? = null,
    val spendLimit: Double? = null,
)

@Serializable
data class BillingRecord(
    val startTime: String? = null,
    val endTime: String? = null,
    val totalAmount: Double = 0.0,
    val podGpuAmount: Double = 0.0,
    val podCpuAmount: Double = 0.0,
    val podDiskAmount: Double = 0.0,
    val serverlessGpuAmount: Double = 0.0,
    val serverlessCpuAmount: Double = 0.0,
    val serverlessDiskAmount: Double = 0.0,
    val serverlessFeeAmount: Double = 0.0,
    val storageStandardAmount: Double = 0.0,
    val storageHighPerformanceAmount: Double = 0.0,
    val endpointAmount: Double = 0.0,
    val clusterGpuAmount: Double = 0.0,
    val clusterDiskAmount: Double = 0.0,
    val clusterNetworkingAmount: Double = 0.0,
)

@Serializable
data class BillingMetadata(
    val recordCount: Int = 0,
    val totals: BillingAmounts? = null,
)

@Serializable
data class BillingAmounts(
    val totalAmount: Double = 0.0,
    val podGpuAmount: Double = 0.0,
    val podCpuAmount: Double = 0.0,
    val podDiskAmount: Double = 0.0,
    val serverlessGpuAmount: Double = 0.0,
    val serverlessCpuAmount: Double = 0.0,
    val serverlessDiskAmount: Double = 0.0,
    val serverlessFeeAmount: Double = 0.0,
    val storageStandardAmount: Double = 0.0,
    val storageHighPerformanceAmount: Double = 0.0,
    val endpointAmount: Double = 0.0,
    val clusterGpuAmount: Double = 0.0,
    val clusterDiskAmount: Double = 0.0,
    val clusterNetworkingAmount: Double = 0.0,
)

@Serializable
data class ListPodBillingResponse(
    val records: List<PodBillingRecord> = emptyList(),
    val metadata: PodBillingMetadata? = null,
)

@Serializable
data class PodBillingRecord(
    val podId: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val totalAmount: Double = 0.0,
    val gpuAmount: Double = 0.0,
    val cpuAmount: Double = 0.0,
    val diskAmount: Double = 0.0,
)

@Serializable
data class PodBillingMetadata(
    val recordCount: Int = 0,
    val uniquePodCount: Int = 0,
    val totals: PodTotals? = null,
)

@Serializable
data class PodTotals(
    val totalAmount: Double = 0.0,
    val gpuAmount: Double = 0.0,
    val cpuAmount: Double = 0.0,
    val diskAmount: Double = 0.0,
)
