package com.invoiceflow.billing.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.invoiceflow.billing.util.Result
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * Base ViewModel providing common functionality for all ViewModels
 */
abstract class BaseViewModel : ViewModel() {
    
    /**
     * Launch a coroutine in viewModelScope and handle exceptions
     */
    protected fun launchCoroutine(
        onError: ((Throwable) -> Unit)? = null,
        block: suspend () -> Unit
    ): Job {
        return viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                onError?.invoke(e)
            }
        }
    }
    
    /**
     * Collect a flow and handle loading/success/error states
     */
    protected fun <T> collectFlowWithState(
        stateFlow: MutableStateFlow<Result<T>>,
        flow: Flow<Result<T>>
    ): Job {
        return viewModelScope.launch {
            flow.catch { e ->
                emit(Result.Error(e, e.message ?: "An error occurred"))
            }.collect { result ->
                stateFlow.value = result
            }
        }
    }
}
