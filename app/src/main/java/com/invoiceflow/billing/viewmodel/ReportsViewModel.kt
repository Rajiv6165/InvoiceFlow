package com.invoiceflow.billing.viewmodel

import androidx.lifecycle.viewModelScope
import com.invoiceflow.billing.model.*
import com.invoiceflow.billing.repository.ReportsRepository
import com.invoiceflow.billing.repository.ProductRepository
import com.invoiceflow.billing.repository.AuthRepository
import com.invoiceflow.billing.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

/**
 * Period options for analytics reporting
 */
sealed class AnalyticsPeriod {
    object Today : AnalyticsPeriod()
    object Yesterday : AnalyticsPeriod()
    object ThisWeek : AnalyticsPeriod()
    object ThisMonth : AnalyticsPeriod()
    object Last3Months : AnalyticsPeriod()
    object ThisYear : AnalyticsPeriod()
    data class Custom(val startDate: Date, val endDate: Date) : AnalyticsPeriod()
    
    fun getLabel(): String = when (this) {
        Today -> "Today"
        Yesterday -> "Yesterday"
        ThisWeek -> "This Week"
        ThisMonth -> "This Month"
        Last3Months -> "Last 3 Months"
        ThisYear -> "This Year"
        is Custom -> "Custom"
    }
}

/**
 * ViewModel managing the business command center (Home) and reports dashboards
 */
@HiltViewModel
class ReportsViewModel @Inject constructor(
    val reportsRepository: ReportsRepository,
    private val productRepository: ProductRepository,
    private val authRepository: AuthRepository
) : BaseViewModel() {

    private var currentStoreId: String = ""

    // Current store details state
    private val _currentStore = MutableStateFlow<Store?>(null)
    val currentStore: StateFlow<Store?> = _currentStore.asStateFlow()

    // Selected period state
    private val _selectedPeriod = MutableStateFlow<AnalyticsPeriod>(AnalyticsPeriod.Today)
    val selectedPeriod: StateFlow<AnalyticsPeriod> = _selectedPeriod.asStateFlow()

    // Active product sort tab state
    val activeProductsSort = MutableStateFlow("quantity")

    // Home / Dashboard Metrics States
    private val _activeProductsCount = MutableStateFlow(0)
    val activeProductsCount: StateFlow<Int> = _activeProductsCount.asStateFlow()

    private val _lowStockProductsCount = MutableStateFlow(0)
    val lowStockProductsCount: StateFlow<Int> = _lowStockProductsCount.asStateFlow()

    private val _todaysSalesState = MutableStateFlow<Result<TodaysSalesModel>>(Result.Loading)
    val todaysSalesState: StateFlow<Result<TodaysSalesModel>> = _todaysSalesState.asStateFlow()

    private val _last7DaysTrendState = MutableStateFlow<Result<List<DailyRevenue>>>(Result.Loading)
    val last7DaysTrendState: StateFlow<Result<List<DailyRevenue>>> = _last7DaysTrendState.asStateFlow()

    private val _recentTransactionsState = MutableStateFlow<Result<List<Invoice>>>(Result.Loading)
    val recentTransactionsState: StateFlow<Result<List<Invoice>>> = _recentTransactionsState.asStateFlow()

    // Analytics / Period Reports States
    private val _salesPeriodState = MutableStateFlow<Result<SalesPeriodModel>>(Result.Loading)
    val salesPeriodState: StateFlow<Result<SalesPeriodModel>> = _salesPeriodState.asStateFlow()

    private val _dailyRevenueTrendState = MutableStateFlow<Result<List<DailyRevenue>>>(Result.Loading)
    val dailyRevenueTrendState: StateFlow<Result<List<DailyRevenue>>> = _dailyRevenueTrendState.asStateFlow()

    private val _categoryBreakdownState = MutableStateFlow<Result<List<CategoryRevenue>>>(Result.Loading)
    val categoryBreakdownState: StateFlow<Result<List<CategoryRevenue>>> = _categoryBreakdownState.asStateFlow()

    private val _topProductsState = MutableStateFlow<Result<List<TopProductModel>>>(Result.Loading)
    val topProductsState: StateFlow<Result<List<TopProductModel>>> = _topProductsState.asStateFlow()

    // In-Memory Caches to avoid duplicate Firestore queries
    private val salesPeriodCache = mutableMapOf<String, SalesPeriodModel>()
    private val dailyRevenueCache = mutableMapOf<String, List<DailyRevenue>>()
    private val topProductsCache = mutableMapOf<String, MutableMap<String, List<TopProductModel>>>()
    private val categoryBreakdownCache = mutableMapOf<String, List<CategoryRevenue>>()

    init {
        // Automatically trigger reload when the selected period changes
        selectedPeriod
            .onEach { period ->
                if (currentStoreId.isNotEmpty()) {
                    loadPeriodSales(period, force = false)
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Configure store ID and start real-time feeds
     */
    fun setStoreId(storeId: String) {
        if (currentStoreId == storeId) return
        currentStoreId = storeId
        
        // Fetch current store details
        viewModelScope.launch {
            _currentStore.value = authRepository.getStoreById(storeId)
        }
        
        // Observe products and transaction streams
        observeRealTimeFeeds()
        
        // Trigger initial data load
        loadPeriodSales(selectedPeriod.value, force = true)
    }

    /**
     * Set the selected period
     */
    fun setPeriod(period: AnalyticsPeriod) {
        _selectedPeriod.value = period
    }

    /**
     * Set active product ranking sorting rule
     */
    fun setProductsSort(sortBy: String) {
        activeProductsSort.value = sortBy
        val dates = getDatesForPeriod(selectedPeriod.value)
        viewModelScope.launch {
            loadTopProducts(selectedPeriod.value, dates.first, dates.second, sortBy, force = false)
        }
    }

    /**
     * Refresh all dashboard and report details
     */
    fun refreshData(force: Boolean = true) {
        if (currentStoreId.isEmpty()) return
        
        if (force) {
            clearAllCaches()
        }
        
        loadTodaysSales()
        loadLast7DaysTrend()
        loadPeriodSales(selectedPeriod.value, force = force)
    }

    /**
     * Load today's stats specifically for greeting metrics
     */
    fun loadTodaysSales() {
        viewModelScope.launch {
            _todaysSalesState.value = Result.Loading
            _todaysSalesState.value = reportsRepository.getTodaysSales(currentStoreId)
        }
    }

    /**
     * Load last 7 days daily sales trend specifically for sparklines
     */
    fun loadLast7DaysTrend() {
        viewModelScope.launch {
            _last7DaysTrendState.value = Result.Loading
            _last7DaysTrendState.value = reportsRepository.getDailyRevenueTrend(currentStoreId, 7)
        }
    }

    /**
     * Start observers for live feeds
     */
    private fun observeRealTimeFeeds() {
        viewModelScope.launch {
            productRepository.getProductsByStoreId(currentStoreId)
                .collect { result ->
                    if (result is Result.Success) {
                        _activeProductsCount.value = result.data.size
                    }
                }
        }

        viewModelScope.launch {
            productRepository.getLowStockProducts(currentStoreId)
                .collect { result ->
                    if (result is Result.Success) {
                        _lowStockProductsCount.value = result.data.size
                    }
                }
        }

        viewModelScope.launch {
            reportsRepository.getRecentTransactions(currentStoreId)
                .collect { result ->
                    _recentTransactionsState.value = result
                }
        }

        loadTodaysSales()
        loadLast7DaysTrend()
    }

    /**
     * Core loader for period-based statistics
     */
    private fun loadPeriodSales(period: AnalyticsPeriod, force: Boolean) {
        val dates = getDatesForPeriod(period)
        val startDate = dates.first
        val endDate = dates.second
        val cacheKey = getPeriodCacheKey(period, startDate, endDate)

        viewModelScope.launch {
            if (force) {
                clearCacheForPeriodKey(cacheKey)
            }

            // 1. SalesPeriodModel details
            if (salesPeriodCache.containsKey(cacheKey)) {
                _salesPeriodState.value = Result.Success(salesPeriodCache[cacheKey]!!)
            } else {
                _salesPeriodState.value = Result.Loading
                val result = reportsRepository.getSalesForPeriod(currentStoreId, startDate, endDate)
                if (result is Result.Success) {
                    salesPeriodCache[cacheKey] = result.data
                    _salesPeriodState.value = result
                } else {
                    _salesPeriodState.value = result
                }
            }

            // 2. Daily revenue line chart data
            val days = getDaysCountForPeriod(period, startDate, endDate)
            val trendKey = "${cacheKey}_trend_$days"
            if (dailyRevenueCache.containsKey(trendKey)) {
                _dailyRevenueTrendState.value = Result.Success(dailyRevenueCache[trendKey]!!)
            } else {
                _dailyRevenueTrendState.value = Result.Loading
                val result = reportsRepository.getDailyRevenueTrend(currentStoreId, days)
                if (result is Result.Success) {
                    dailyRevenueCache[trendKey] = result.data
                    _dailyRevenueTrendState.value = result
                } else {
                    _dailyRevenueTrendState.value = result
                }
            }

            // 3. Category Breakdown donut chart data
            if (categoryBreakdownCache.containsKey(cacheKey)) {
                _categoryBreakdownState.value = Result.Success(categoryBreakdownCache[cacheKey]!!)
            } else {
                _categoryBreakdownState.value = Result.Loading
                val result = reportsRepository.getCategoryBreakdown(currentStoreId, startDate, endDate)
                if (result is Result.Success) {
                    categoryBreakdownCache[cacheKey] = result.data
                    _categoryBreakdownState.value = result
                } else {
                    _categoryBreakdownState.value = result
                }
            }

            // 4. Top Sold Products list
            loadTopProducts(period, startDate, endDate, activeProductsSort.value, force)
        }
    }

    /**
     * Load products details with support for tabs and caching
     */
    private suspend fun loadTopProducts(
        period: AnalyticsPeriod,
        startDate: Date,
        endDate: Date,
        sortBy: String,
        force: Boolean
    ) {
        val cacheKey = getPeriodCacheKey(period, startDate, endDate)
        val periodCache = topProductsCache.getOrPut(cacheKey) { mutableMapOf() }

        if (!force && periodCache.containsKey(sortBy)) {
            _topProductsState.value = Result.Success(periodCache[sortBy]!!)
        } else {
            _topProductsState.value = Result.Loading
            val result = reportsRepository.getTopProducts(
                storeId = currentStoreId,
                startDate = startDate,
                endDate = endDate,
                limit = 50,
                sortBy = sortBy
            )
            if (result is Result.Success) {
                periodCache[sortBy] = result.data
                _topProductsState.value = result
            } else {
                _topProductsState.value = result
            }
        }
    }

    /**
     * Clear specific cache key entries
     */
    private fun clearCacheForPeriodKey(key: String) {
        salesPeriodCache.remove(key)
        categoryBreakdownCache.remove(key)
        topProductsCache.remove(key)
        dailyRevenueCache.keys.filter { it.startsWith(key) }.forEach { dailyRevenueCache.remove(it) }
    }

    /**
     * Wipe all in-memory caches
     */
    private fun clearAllCaches() {
        salesPeriodCache.clear()
        dailyRevenueCache.clear()
        categoryBreakdownCache.clear()
        topProductsCache.clear()
    }

    /**
     * Generate cache key identifier string
     */
    private fun getPeriodCacheKey(period: AnalyticsPeriod, start: Date, end: Date): String {
        return when (period) {
            is AnalyticsPeriod.Custom -> "Custom_${start.time}_${end.time}"
            else -> period.getLabel()
        }
    }

    /**
     * Calculate days in date range
     */
    private fun getDaysCountForPeriod(period: AnalyticsPeriod, start: Date, end: Date): Int {
        return when (period) {
            AnalyticsPeriod.Today -> 1
            AnalyticsPeriod.Yesterday -> 1
            AnalyticsPeriod.ThisWeek -> 7
            AnalyticsPeriod.ThisMonth -> {
                val cal = Calendar.getInstance()
                cal.time = start
                cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            }
            AnalyticsPeriod.Last3Months -> 90
            AnalyticsPeriod.ThisYear -> 365
            is AnalyticsPeriod.Custom -> {
                val diff = end.time - start.time
                val days = (diff / (1000 * 60 * 60 * 24)).toInt()
                if (days <= 0) 1 else days
            }
        }
    }

    /**
     * Determine dates range based on selected period option
     */
    fun getDatesForPeriod(period: AnalyticsPeriod): Pair<Date, Date> {
        val calendar = Calendar.getInstance()
        val now = Date()
        
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfToday = calendar.time
        
        when (period) {
            AnalyticsPeriod.Today -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                return Pair(calendar.time, endOfToday)
            }
            AnalyticsPeriod.Yesterday -> {
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfYesterday = calendar.time
                
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val endOfYesterday = calendar.time
                return Pair(startOfYesterday, endOfYesterday)
            }
            AnalyticsPeriod.ThisWeek -> {
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                val daysToSubtract = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
                calendar.add(Calendar.DAY_OF_YEAR, -daysToSubtract)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                return Pair(calendar.time, endOfToday)
            }
            AnalyticsPeriod.ThisMonth -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                return Pair(calendar.time, endOfToday)
            }
            AnalyticsPeriod.Last3Months -> {
                calendar.add(Calendar.MONTH, -3)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                return Pair(calendar.time, endOfToday)
            }
            AnalyticsPeriod.ThisYear -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                return Pair(calendar.time, endOfToday)
            }
            is AnalyticsPeriod.Custom -> {
                return Pair(period.startDate, period.endDate)
            }
        }
    }
}
