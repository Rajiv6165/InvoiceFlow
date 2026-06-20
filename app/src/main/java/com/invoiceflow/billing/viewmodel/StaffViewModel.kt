package com.invoiceflow.billing.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.invoiceflow.billing.model.*
import com.invoiceflow.billing.repository.ActivityLogRepository
import com.invoiceflow.billing.repository.AuthRepository
import com.invoiceflow.billing.repository.ReportsRepository
import com.invoiceflow.billing.util.Result
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class StaffViewModel @Inject constructor(
    private val application: Application,
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository,
    private val reportsRepository: ReportsRepository,
    private val activityLogRepository: ActivityLogRepository,
    private val subscriptionRepository: com.invoiceflow.billing.repository.SubscriptionRepository
) : BaseViewModel() {

    companion object {
        private const val TAG = "StaffViewModel"
    }

    private var currentStoreId: String = ""

    // UI States
    private val _staffListState = MutableStateFlow<Result<List<User>>>(Result.Loading)
    val staffListState: StateFlow<Result<List<User>>> = _staffListState.asStateFlow()

    private val _logsState = MutableStateFlow<Result<List<ActivityLog>>>(Result.Loading)
    val logsState: StateFlow<Result<List<ActivityLog>>> = _logsState.asStateFlow()

    private val _shiftsState = MutableStateFlow<Result<List<Shift>>>(Result.Loading)
    val shiftsState: StateFlow<Result<List<Shift>>> = _shiftsState.asStateFlow()

    private val _activeShiftState = MutableStateFlow<Shift?>(null)
    val activeShiftState: StateFlow<Shift?> = _activeShiftState.asStateFlow()

    private val _cashierPerformanceState = MutableStateFlow<Result<CashierPerformance>>(Result.Loading)
    val cashierPerformanceState: StateFlow<Result<CashierPerformance>> = _cashierPerformanceState.asStateFlow()

    private val _cashierLogsState = MutableStateFlow<List<ActivityLog>>(emptyList())
    val cashierLogsState: StateFlow<List<ActivityLog>> = _cashierLogsState.asStateFlow()

    // Filter states
    val userFilter = MutableStateFlow<String?>(null)
    val actionFilter = MutableStateFlow<String?>(null)
    val dateRangeFilter = MutableStateFlow<Pair<Date, Date>?>(null)

    // Store state
    private val _currentStore = MutableStateFlow<Store?>(null)
    val currentStore: StateFlow<Store?> = _currentStore.asStateFlow()

    init {
        // Combine filters for real-time logs loading
        combine(userFilter, actionFilter, dateRangeFilter) { user, action, range ->
            Triple(user, action, range)
        }
        .onEach { (user, action, range) ->
            if (currentStoreId.isNotEmpty()) {
                observeLogs(currentStoreId, user, action, range?.first, range?.second)
            }
        }
        .launchIn(viewModelScope)
    }

    /**
     * Set storeId and fetch initial data
     */
    fun setStoreId(storeId: String) {
        if (currentStoreId == storeId) return
        currentStoreId = storeId

        viewModelScope.launch {
            _currentStore.value = authRepository.getStoreById(storeId)
        }

        observeStaffMembers()
        observeShifts()
        observeLogs(storeId, userFilter.value, actionFilter.value, dateRangeFilter.value?.first, dateRangeFilter.value?.second)
    }

    /**
     * Stream staff members belonging to the current store
     */
    private fun observeStaffMembers() {
        viewModelScope.launch {
            _staffListState.value = Result.Loading
            firestore.collection(User.COLLECTION_NAME)
                .whereEqualTo("storeId", currentStoreId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        _staffListState.value = Result.Error(error, error.message ?: "Failed to stream staff members")
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val users = snapshot.documents.mapNotNull { it.toObject(User::class.java) }
                        _staffListState.value = Result.Success(users.sortedBy { it.role.name })
                    }
                }
        }
    }

    /**
     * Stream active and closed shifts for this store
     */
    private fun observeShifts() {
        viewModelScope.launch {
            _shiftsState.value = Result.Loading
            firestore.collection("Shifts")
                .whereEqualTo("storeId", currentStoreId)
                .orderBy("startTime", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        _shiftsState.value = Result.Error(error, error.message ?: "Failed to stream shifts")
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val shifts = snapshot.documents.mapNotNull { it.toObject(Shift::class.java) }
                        _shiftsState.value = Result.Success(shifts)
                    }
                }
        }
    }

    /**
     * Stream activity logs with in-memory filtering
     */
    private fun observeLogs(
        storeId: String,
        userId: String?,
        action: String?,
        start: Date?,
        end: Date?
    ) {
        viewModelScope.launch {
            _logsState.value = Result.Loading
            activityLogRepository.getActivityLogs(storeId, userId, action, start, end)
                .collect {
                    _logsState.value = it
                }
        }
    }

    /**
     * Log an event to the security/audit trail
     */
    fun logEvent(
        userId: String,
        userName: String,
        userEmail: String,
        actionType: String,
        details: Map<String, String> = emptyMap()
    ) {
        viewModelScope.launch {
            val buildDetails = details.toMutableMap()
            buildDetails["os"] = "Android"
            buildDetails["brand"] = android.os.Build.BRAND
            buildDetails["device"] = android.os.Build.MODEL
            
            activityLogRepository.logEvent(
                storeId = currentStoreId,
                userId = userId,
                userName = userName,
                userEmail = userEmail,
                actionType = actionType,
                details = buildDetails,
                deviceInfo = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (SDK ${android.os.Build.VERSION.SDK_INT})"
            )
        }
    }

    /**
     * Programmatically create a cashier account via secondary Firebase app instantiation
     */
    fun createCashier(
        name: String,
        email: String,
        password: String,
        phone: String,
        shift: String,
        onSuccess: (User) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            // Check staff limit first
            val isWithinLimit = subscriptionRepository.isWithinStaffLimit(currentStoreId)
            if (!isWithinLimit) {
                onError("Staff account limit reached. Please upgrade to a higher plan.")
                return@launch
            }

            val result = withContext(Dispatchers.IO) {
                try {
                    val currentApp = FirebaseApp.getInstance()
                    val options = currentApp.options
                    val secondaryAppName = "SecondaryApp_${System.currentTimeMillis()}"

                    // Create secondary app context
                    val secondaryApp = FirebaseApp.initializeApp(application, options, secondaryAppName)
                    val secondaryAuth = FirebaseAuth.getInstance(secondaryApp)

                    // Create auth credentials
                    val authResult = secondaryAuth.createUserWithEmailAndPassword(email, password).await()
                    val firebaseUser = authResult.user ?: throw IllegalStateException("Secondary Auth User creation returned null")
                    val cashierUid = firebaseUser.uid

                    // Immediately sign out and delete app so it doesn't linger
                    secondaryAuth.signOut()
                    secondaryApp.delete()

                    // Create cashier document
                    val newUser = User(
                        userId = cashierUid,
                        name = name,
                        email = email,
                        role = Role.CASHIER,
                        storeId = currentStoreId,
                        phone = phone,
                        createdAt = Timestamp.now(),
                        lastSeenAt = null,
                        shift = shift,
                        isActive = true
                    )

                    firestore.collection(User.COLLECTION_NAME)
                        .document(cashierUid)
                        .set(newUser)
                        .await()

                    // Log event
                    logEvent(
                        userId = cashierUid,
                        userName = name,
                        userEmail = email,
                        actionType = "USER_CREATED",
                        details = mapOf("created_by" to "Owner", "role" to "CASHIER", "shift" to shift)
                    )

                    Result.Success(newUser)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in secondary Auth creation", e)
                    Result.Error(e, e.message ?: "Auth error during cashier creation")
                }
            }

            when (result) {
                is Result.Success -> onSuccess(result.data)
                is Result.Error -> onError(result.message ?: "Failed to create cashier")
                else -> {}
            }
        }
    }

    /**
     * Change active status of cashier (Deactivate / Reactivate)
     */
    fun toggleUserStatus(userId: String, isActive: Boolean, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                firestore.collection(User.COLLECTION_NAME)
                    .document(userId)
                    .update("isActive", isActive)
                    .await()

                // If deactivating, force close any active shift
                if (!isActive) {
                    closeActiveShiftForUser(userId)
                }

                // Log event
                logEvent(
                    userId = userId,
                    userName = "System",
                    userEmail = "",
                    actionType = if (isActive) "USER_REACTIVATED" else "USER_DEACTIVATED",
                    details = mapOf("target_userId" to userId)
                )

                onSuccess()
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling status for user: $userId", e)
                onError(e.message ?: "Failed to change user status")
            }
        }
    }

    /**
     * Load metrics and timeline specifically for cashier profile views
     */
    fun loadCashierProfileData(cashierId: String, cashierName: String) {
        viewModelScope.launch {
            _cashierPerformanceState.value = Result.Loading
            
            try {
                // Fetch invoices created by cashier
                val invoicesSnapshot = firestore.collection(Invoice.COLLECTION_NAME)
                    .whereEqualTo("storeId", currentStoreId)
                    .whereEqualTo("cashierId", cashierId)
                    .get()
                    .await()

                val invoices = invoicesSnapshot.documents.mapNotNull { it.toObject(Invoice::class.java) }

                val totalBills = invoices.size
                val totalRevenue = invoices.sumOf { it.grandTotal }
                val averageBill = if (totalBills > 0) totalRevenue / totalBills else 0.0

                // Today's tallies
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfToday = calendar.time

                val todayInvoices = invoices.filter { it.timestamp.toDate().after(startOfToday) }
                val billsToday = todayInvoices.size
                val revenueToday = todayInvoices.sumOf { it.grandTotal }

                // Best performing day
                val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                val dayRevenueMap = invoices.groupBy { sdf.format(it.timestamp.toDate()) }
                    .mapValues { entry -> entry.value.sumOf { it.grandTotal } }

                val bestDay = dayRevenueMap.maxByOrNull { it.value }?.key ?: "N/A"

                _cashierPerformanceState.value = Result.Success(
                    CashierPerformance(
                        totalBills = totalBills,
                        totalRevenue = totalRevenue,
                        averageBillValue = averageBill,
                        billsToday = billsToday,
                        revenueToday = revenueToday,
                        bestPerformingDay = bestDay
                    )
                )

                // Stream timeline events
                firestore.collection("ActivityLogs")
                    .document(currentStoreId)
                    .collection("logs")
                    .whereEqualTo("userId", cashierId)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(50)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        val logs = querySnapshot.documents.mapNotNull { it.toObject(ActivityLog::class.java) }
                        _cashierLogsState.value = logs
                    }

            } catch (e: Exception) {
                Log.e(TAG, "Error building cashier profile stats", e)
                _cashierPerformanceState.value = Result.Error(e, e.message ?: "Failed to fetch cashier stats")
            }
        }
    }

    /**
     * Start cashier work shift (call on Login success)
     */
    fun startShift(userId: String, userName: String) {
        viewModelScope.launch {
            try {
                // Check if shift is already open
                val activeShiftSnapshot = firestore.collection("Shifts")
                    .whereEqualTo("storeId", currentStoreId)
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("status", "OPEN")
                    .limit(1)
                    .get()
                    .await()

                if (!activeShiftSnapshot.isEmpty) {
                    val activeShift = activeShiftSnapshot.documents.first().toObject(Shift::class.java)
                    _activeShiftState.value = activeShift
                    Log.d(TAG, "Shift already open for user: $userId")
                    return@launch
                }

                // Create new shift document
                val shiftRef = firestore.collection("Shifts").document()
                val newShift = Shift(
                    shiftId = shiftRef.id,
                    userId = userId,
                    userName = userName,
                    storeId = currentStoreId,
                    startTime = Timestamp.now(),
                    endTime = null,
                    totalRevenue = 0.0,
                    totalBills = 0,
                    status = "OPEN"
                )

                shiftRef.set(newShift.toMap()).await()
                _activeShiftState.value = newShift

                // Update user last seen
                updateUserLastSeen(userId)

                // Log login
                logEvent(
                    userId = userId,
                    userName = userName,
                    userEmail = "",
                    actionType = "USER_LOGIN",
                    details = mapOf("shiftId" to shiftRef.id)
                )

                Log.d(TAG, "Shift started successfully: ${shiftRef.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting shift for: $userId", e)
            }
        }
    }

    /**
     * Complete cashier work shift (call on Logout or Owner override)
     */
    fun closeShift(userId: String, userName: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val activeShiftSnapshot = firestore.collection("Shifts")
                    .whereEqualTo("storeId", currentStoreId)
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("status", "OPEN")
                    .limit(1)
                    .get()
                    .await()

                if (activeShiftSnapshot.isEmpty) {
                    Log.d(TAG, "No active shift to close for user: $userId")
                    onComplete()
                    return@launch
                }

                val shiftDoc = activeShiftSnapshot.documents.first()
                val shift = shiftDoc.toObject(Shift::class.java)!!

                // Fetch invoices created by cashier since shift started
                val invoicesSnapshot = firestore.collection(Invoice.COLLECTION_NAME)
                    .whereEqualTo("storeId", currentStoreId)
                    .whereEqualTo("cashierId", userId)
                    .whereGreaterThanOrEqualTo("timestamp", shift.startTime)
                    .get()
                    .await()

                val invoices = invoicesSnapshot.documents.mapNotNull { it.toObject(Invoice::class.java) }
                val totalRevenue = invoices.sumOf { it.grandTotal }
                val totalBills = invoices.size

                // Update shift details
                val closedShift = shift.copy(
                    endTime = Timestamp.now(),
                    totalRevenue = totalRevenue,
                    totalBills = totalBills,
                    status = "CLOSED"
                )

                firestore.collection("Shifts")
                    .document(shift.shiftId)
                    .set(closedShift.toMap())
                    .await()

                if (userId == authRepository.getCurrentFirebaseUser()?.uid) {
                    _activeShiftState.value = null
                }

                // Log logout
                logEvent(
                    userId = userId,
                    userName = userName,
                    userEmail = "",
                    actionType = "USER_LOGOUT",
                    details = mapOf("shiftId" to shift.shiftId, "revenue" to "$totalRevenue", "bills" to "$totalBills")
                )

                Log.d(TAG, "Shift closed: ${shift.shiftId}")
                onComplete()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing shift for: $userId", e)
                onComplete()
            }
        }
    }

    /**
     * Helper to close shift for a user without knowing name (like in deactivation flow)
     */
    private suspend fun closeActiveShiftForUser(userId: String) {
        try {
            val activeShiftSnapshot = firestore.collection("Shifts")
                .whereEqualTo("storeId", currentStoreId)
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "OPEN")
                .limit(1)
                .get()
                .await()

            if (!activeShiftSnapshot.isEmpty) {
                val shiftDoc = activeShiftSnapshot.documents.first()
                val shift = shiftDoc.toObject(Shift::class.java)!!
                
                val invoicesSnapshot = firestore.collection(Invoice.COLLECTION_NAME)
                    .whereEqualTo("storeId", currentStoreId)
                    .whereEqualTo("cashierId", userId)
                    .whereGreaterThanOrEqualTo("timestamp", shift.startTime)
                    .get()
                    .await()

                val invoices = invoicesSnapshot.documents.mapNotNull { it.toObject(Invoice::class.java) }
                val totalRevenue = invoices.sumOf { it.grandTotal }
                val totalBills = invoices.size

                val closedShift = shift.copy(
                    endTime = Timestamp.now(),
                    totalRevenue = totalRevenue,
                    totalBills = totalBills,
                    status = "CLOSED"
                )

                firestore.collection("Shifts")
                    .document(shift.shiftId)
                    .set(closedShift.toMap())
                    .await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in force closing shift", e)
        }
    }

    /**
     * Update cashier last seen timestamp
     */
    fun updateUserLastSeen(userId: String) {
        viewModelScope.launch {
            try {
                firestore.collection(User.COLLECTION_NAME)
                    .document(userId)
                    .update("lastSeenAt", Timestamp.now())
                    .await()
            } catch (e: Exception) {
                Log.e(TAG, "Error updating user last seen", e)
            }
        }
    }
}
