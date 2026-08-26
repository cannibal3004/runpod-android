package com.canni.runpod.ui.billing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canni.runpod.data.api.dto.AccountBalance
import com.canni.runpod.data.api.dto.BillingAmounts
import com.canni.runpod.data.api.dto.BillingRecord
import com.canni.runpod.data.api.dto.PodBillingRecord
import com.canni.runpod.data.repo.AccountRepository
import com.canni.runpod.data.repo.BillingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class BillingViewModel @Inject constructor(
    private val billingRepository: BillingRepository,
    private val accountRepository: AccountRepository,
) : ViewModel() {

    enum class Range(val label: String, val bucketSize: String, val lastN: Int) {
        DAY("24h", "hour", 24),
        WEEK("7d", "day", 7),
        MONTH("30d", "day", 30),
    }

    data class PodSpend(
        val podId: String,
        val total: Double,
        val buckets: Int,
    )

    data class UiState(
        val range: Range = Range.WEEK,
        val isLoading: Boolean = true,
        val error: String? = null,
        val balance: AccountBalance? = null,
        val balanceError: String? = null,
        val records: List<BillingRecord> = emptyList(),
        val totals: BillingAmounts? = null,
        val podSpends: List<PodSpend> = emptyList(),
        val podTotal: Double = 0.0,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        load()
    }

    fun setRange(range: Range) {
        if (range == _state.value.range) return
        _state.update { it.copy(range = range) }
        load()
    }

    fun load() {
        val range = _state.value.range
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            coroutineScope {
                val summary = async {
                    runCatching { billingRepository.summary(range.bucketSize, range.lastN) }
                }
                val pods = async {
                    runCatching { billingRepository.pods(range.bucketSize, range.lastN) }
                }
                val balance = async {
                    runCatching { accountRepository.balance() }
                }
                val s = summary.await()
                val p = pods.await()
                val b = balance.await()
                val error = listOfNotNull(s.exceptionOrNull(), p.exceptionOrNull()).firstOrNull()?.message
                _state.update {
                    val sBody = s.getOrNull()
                    val pBody = p.getOrNull()
                    it.copy(
                        isLoading = false,
                        error = error,
                        balance = b.getOrNull(),
                        balanceError = b.exceptionOrNull()?.message,
                        records = sBody?.records ?: emptyList(),
                        totals = sBody?.metadata?.totals,
                        podSpends = aggregatePods(pBody?.records ?: emptyList()),
                        podTotal = pBody?.metadata?.totals?.totalAmount ?: 0.0,
                    )
                }
            }
        }
    }

    private fun aggregatePods(records: List<PodBillingRecord>): List<PodSpend> {
        val byPod = records
            .filter { !it.podId.isNullOrBlank() }
            .groupBy { it.podId!! }
        return byPod
            .map { (podId, recs) ->
                PodSpend(
                    podId = podId,
                    total = recs.sumOf { it.totalAmount },
                    buckets = recs.size,
                )
            }
            .sortedByDescending { it.total }
    }
}
