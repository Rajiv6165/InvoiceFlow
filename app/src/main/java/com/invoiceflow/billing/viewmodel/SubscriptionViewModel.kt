package com.invoiceflow.billing.viewmodel

import androidx.lifecycle.viewModelScope
import com.invoiceflow.billing.model.SubscriptionStatus
import com.invoiceflow.billing.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository
) : BaseViewModel() {

    private val _subscriptionStatus = MutableStateFlow<SubscriptionStatus?>(null)
    val subscriptionStatus: StateFlow<SubscriptionStatus?> = _subscriptionStatus.asStateFlow()

    private val _remainingTrialDays = MutableStateFlow(0)
    val remainingTrialDays: StateFlow<Int> = _remainingTrialDays.asStateFlow()

    private val _isAnalyticsEnabled = MutableStateFlow(true)
    val isAnalyticsEnabled: StateFlow<Boolean> = _isAnalyticsEnabled.asStateFlow()

    private val _storeName = MutableStateFlow("")
    val storeName: StateFlow<String> = _storeName.asStateFlow()

    fun checkSubscriptionStatus(storeId: String, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            val status = subscriptionRepository.checkSubscriptionStatus(storeId, forceRefresh)
            _subscriptionStatus.value = status

            val name = subscriptionRepository.getStoreName(storeId)
            _storeName.value = name

            if (status is SubscriptionStatus.Trial) {
                val remaining = subscriptionRepository.getRemainingTrialDays(storeId)
                _remainingTrialDays.value = remaining
            } else {
                _remainingTrialDays.value = 0
            }

            // Check analytics feature flag
            val hasAnalytics = subscriptionRepository.checkFeatureAccess(storeId, "analytics")
            _isAnalyticsEnabled.value = hasAnalytics
        }
    }
}
