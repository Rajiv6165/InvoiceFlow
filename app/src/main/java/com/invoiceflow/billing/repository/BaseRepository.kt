package com.invoiceflow.billing.repository

import com.invoiceflow.billing.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Base repository providing common Firebase operations
 */
abstract class BaseRepository {
    
    /**
     * Execute a suspend function and wrap it in a Result
     */
    protected suspend fun <T> safeApiCall(
        apiCall: suspend () -> T
    ): Flow<Result<T>> = flow {
        emit(Result.Loading)
        try {
            val result = apiCall()
            emit(Result.Success(result))
        } catch (e: Exception) {
            emit(Result.Error(e, e.message ?: "An error occurred"))
        }
    }
    
    /**
     * Check if email is valid format
     */
    protected fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}
