package com.invoiceflow.billing.repository

import android.util.Log
import com.invoiceflow.billing.model.*
import com.invoiceflow.billing.util.Result
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for compiling sales, revenue, and product intelligence reports
 */
@Singleton
class ReportsRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : BaseRepository() {
    
    companion object {
        private const val TAG = "ReportsRepository"
    }

    /**
     * Get today's sales metrics compared to yesterday
     */
    suspend fun getTodaysSales(storeId: String): Result<TodaysSalesModel> {
        return try {
            val calendar = Calendar.getInstance()
            
            // Start and End of Today
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfToday = calendar.time
            
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val endOfToday = calendar.time
            
            // Start and End of Yesterday
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            val endOfYesterday = calendar.time
            
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfYesterday = calendar.time
            
            // Fetch today's invoices
            val todaySnapshot = firestore.collection(Invoice.COLLECTION_NAME)
                .whereEqualTo("storeId", storeId)
                .whereGreaterThanOrEqualTo("timestamp", Timestamp(startOfToday))
                .whereLessThanOrEqualTo("timestamp", Timestamp(endOfToday))
                .get()
                .await()
                
            // Fetch yesterday's invoices
            val yesterdaySnapshot = firestore.collection(Invoice.COLLECTION_NAME)
                .whereEqualTo("storeId", storeId)
                .whereGreaterThanOrEqualTo("timestamp", Timestamp(startOfYesterday))
                .whereLessThanOrEqualTo("timestamp", Timestamp(endOfYesterday))
                .get()
                .await()
                
            val todayInvoices = todaySnapshot.documents.mapNotNull { it.toObject(Invoice::class.java) }
            val yesterdayInvoices = yesterdaySnapshot.documents.mapNotNull { it.toObject(Invoice::class.java) }
            
            val todayRevenue = todayInvoices.sumOf { it.grandTotal }
            val todayBills = todayInvoices.size
            val todayAverage = if (todayBills > 0) todayRevenue / todayBills else 0.0
            
            val yesterdayRevenue = yesterdayInvoices.sumOf { it.grandTotal }
            val yesterdayBills = yesterdayInvoices.size
            
            // Calculate trends
            val revenueTrendPercent = if (yesterdayRevenue > 0) {
                ((todayRevenue - yesterdayRevenue) / yesterdayRevenue) * 100.0
            } else if (todayRevenue > 0) {
                100.0
            } else {
                0.0
            }
            
            val billsTrendPercent = if (yesterdayBills > 0) {
                ((todayBills - yesterdayBills).toDouble() / yesterdayBills) * 100.0
            } else if (todayBills > 0) {
                100.0
            } else {
                0.0
            }
            
            Result.Success(
                TodaysSalesModel(
                    totalRevenue = todayRevenue,
                    billCount = todayBills,
                    averageValue = todayAverage,
                    revenueTrendPercent = Math.abs(revenueTrendPercent),
                    revenueTrendUp = revenueTrendPercent >= 0,
                    billsTrendPercent = Math.abs(billsTrendPercent),
                    billsTrendUp = billsTrendPercent >= 0
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting today's sales", e)
            Result.Error(e, e.message ?: "Error getting today's sales")
        }
    }

    /**
     * Get comprehensive metrics and splits for a specified period
     */
    suspend fun getSalesForPeriod(storeId: String, startDate: Date, endDate: Date): Result<SalesPeriodModel> {
        return try {
            val snapshot = firestore.collection(Invoice.COLLECTION_NAME)
                .whereEqualTo("storeId", storeId)
                .whereGreaterThanOrEqualTo("timestamp", Timestamp(startDate))
                .whereLessThanOrEqualTo("timestamp", Timestamp(endDate))
                .get()
                .await()
                
            val invoices = snapshot.documents.mapNotNull { it.toObject(Invoice::class.java) }
            
            val totalRevenue = invoices.sumOf { it.grandTotal }
            val count = invoices.size
            val avgValue = if (count > 0) totalRevenue / count else 0.0
            
            val highest = invoices.maxOfOrNull { it.grandTotal } ?: 0.0
            val lowest = invoices.minOfOrNull { it.grandTotal } ?: 0.0
            
            // Hourly distribution
            val hourlyDistribution = mutableMapOf<Int, Int>()
            (0..23).forEach { hourlyDistribution[it] = 0 }
            
            // Day of week distribution
            val dayOfWeekDistribution = mutableMapOf<Int, Int>()
            (1..7).forEach { dayOfWeekDistribution[it] = 0 }
            
            val calendar = Calendar.getInstance()
            invoices.forEach { invoice ->
                calendar.time = invoice.timestamp.toDate()
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                val day = calendar.get(Calendar.DAY_OF_WEEK)
                
                hourlyDistribution[hour] = (hourlyDistribution[hour] ?: 0) + 1
                dayOfWeekDistribution[day] = (dayOfWeekDistribution[day] ?: 0) + 1
            }
            
            val peakHour = hourlyDistribution.maxByOrNull { it.value }?.key ?: 0
            val peakDay = dayOfWeekDistribution.maxByOrNull { it.value }?.key ?: 1
            
            var cashRevenue = 0.0
            var digitalRevenue = 0.0
            var paidCount = 0
            
            invoices.forEach { invoice ->
                if (invoice.paymentMethod.uppercase() == "CASH") {
                    cashRevenue += invoice.grandTotal
                } else {
                    digitalRevenue += invoice.grandTotal
                }
                
                if (invoice.paymentStatus.uppercase() == "PAID") {
                    paidCount++
                }
            }
            
            val successRate = if (count > 0) (paidCount.toDouble() / count) * 100.0 else 100.0
            
            // Calculate comparison vs previous period of same length
            val durationMs = endDate.time - startDate.time
            val prevStartDate = Date(startDate.time - durationMs - 1000)
            val prevEndDate = Date(startDate.time - 1000)
            
            val prevSnapshot = firestore.collection(Invoice.COLLECTION_NAME)
                .whereEqualTo("storeId", storeId)
                .whereGreaterThanOrEqualTo("timestamp", Timestamp(prevStartDate))
                .whereLessThanOrEqualTo("timestamp", Timestamp(prevEndDate))
                .get()
                .await()
                
            val prevInvoices = prevSnapshot.documents.mapNotNull { it.toObject(Invoice::class.java) }
            val prevRevenue = prevInvoices.sumOf { it.grandTotal }
            
            val compPercentage = if (prevRevenue > 0) {
                ((totalRevenue - prevRevenue) / prevRevenue) * 100.0
            } else if (totalRevenue > 0) {
                100.0
            } else {
                0.0
            }
            
            Result.Success(
                SalesPeriodModel(
                    totalRevenue = totalRevenue,
                    transactionCount = count,
                    averageTransactionValue = avgValue,
                    highestTransactionValue = highest,
                    lowestTransactionValue = lowest,
                    peakHour = peakHour,
                    peakDayOfWeek = peakDay,
                    cashRevenue = cashRevenue,
                    digitalRevenue = digitalRevenue,
                    transactionSuccessRate = successRate,
                    hourlyDistribution = hourlyDistribution,
                    dayOfWeekDistribution = dayOfWeekDistribution,
                    comparisonRevenuePercentage = Math.abs(compPercentage),
                    comparisonRevenueUp = compPercentage >= 0
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting sales for period", e)
            Result.Error(e, e.message ?: "Error getting sales for period")
        }
    }

    /**
     * Get continuous daily revenue trend points for line chart
     */
    suspend fun getDailyRevenueTrend(storeId: String, numberOfDays: Int): Result<List<DailyRevenue>> {
        return try {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -numberOfDays + 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startDate = calendar.time
            
            val snapshot = firestore.collection(Invoice.COLLECTION_NAME)
                .whereEqualTo("storeId", storeId)
                .whereGreaterThanOrEqualTo("timestamp", Timestamp(startDate))
                .get()
                .await()
                
            val invoices = snapshot.documents.mapNotNull { it.toObject(Invoice::class.java) }
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val displaySdf = SimpleDateFormat("dd MMM", Locale.getDefault())
            
            val grouped = invoices.groupBy { sdf.format(it.timestamp.toDate()) }
            
            val trend = mutableListOf<DailyRevenue>()
            val tempCal = Calendar.getInstance().apply { time = startDate }
            
            for (i in 0 until numberOfDays) {
                val dateKey = sdf.format(tempCal.time)
                val label = displaySdf.format(tempCal.time)
                val dayInvoices = grouped[dateKey] ?: emptyList()
                
                trend.add(
                    DailyRevenue(
                        dateStr = label,
                        date = tempCal.time,
                        revenue = dayInvoices.sumOf { it.grandTotal },
                        billCount = dayInvoices.size
                    )
                )
                
                tempCal.add(Calendar.DAY_OF_YEAR, 1)
            }
            
            Result.Success(trend)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting daily revenue trend", e)
            Result.Error(e, e.message ?: "Error getting trend")
        }
    }

    /**
     * Get product lists grouped and sorted for various product intelligence metrics
     */
    suspend fun getTopProducts(
        storeId: String,
        startDate: Date,
        endDate: Date,
        limit: Int = 10,
        sortBy: String = "quantity"
    ): Result<List<TopProductModel>> {
        return try {
            val invoicesSnapshot = firestore.collection(Invoice.COLLECTION_NAME)
                .whereEqualTo("storeId", storeId)
                .whereGreaterThanOrEqualTo("timestamp", Timestamp(startDate))
                .whereLessThanOrEqualTo("timestamp", Timestamp(endDate))
                .get()
                .await()
                
            val productsSnapshot = firestore.collection(Product.COLLECTION_NAME)
                .whereEqualTo("storeId", storeId)
                .whereEqualTo("isActive", true)
                .get()
                .await()
                
            val invoices = invoicesSnapshot.documents.mapNotNull { it.toObject(Invoice::class.java) }
            val products = productsSnapshot.documents.mapNotNull { it.toObject(Product::class.java) }
            
            val salesMap = mutableMapOf<String, Pair<Int, Double>>() // productId -> (qty, revenue)
            invoices.forEach { invoice ->
                invoice.items.forEach { item ->
                    val current = salesMap[item.productId] ?: Pair(0, 0.0)
                    salesMap[item.productId] = Pair(
                        current.first + item.quantity,
                        current.second + item.totalAmount
                    )
                }
            }
            
            val totalStoreRevenue = invoices.sumOf { it.grandTotal }
            
            val allModels = products.map { product ->
                val sales = salesMap[product.productId] ?: Pair(0, 0.0)
                val percentage = if (totalStoreRevenue > 0) (sales.second / totalStoreRevenue) * 100.0 else 0.0
                TopProductModel(
                    productId = product.productId,
                    name = product.name,
                    sku = product.sku.ifBlank { product.barcode },
                    category = product.category.ifBlank { "General" },
                    quantitySold = sales.first,
                    revenue = sales.second,
                    percentageOfTotalRevenue = percentage,
                    stockQty = product.stockQty,
                    minStockLevel = product.minStockLevel
                )
            }
            
            val filteredSorted = when (sortBy.lowercase()) {
                "quantity" -> {
                    allModels.filter { it.quantitySold > 0 }
                        .sortedByDescending { it.quantitySold }
                }
                "revenue" -> {
                    allModels.filter { it.revenue > 0.0 }
                        .sortedByDescending { it.revenue }
                }
                "slow_moving" -> {
                    allModels.filter { it.quantitySold > 0 }
                        .sortedBy { it.quantitySold }
                }
                "never_sold" -> {
                    allModels.filter { it.quantitySold == 0 }
                        .sortedBy { it.name }
                }
                else -> allModels.sortedByDescending { it.quantitySold }
            }
            
            Result.Success(filteredSorted.take(limit))
        } catch (e: Exception) {
            Log.e(TAG, "Error getting top products", e)
            Result.Error(e, e.message ?: "Error getting top products")
        }
    }

    /**
     * Get hourly distribution of transactions
     */
    suspend fun getHourlyTransactionDistribution(storeId: String): Result<Map<Int, Int>> {
        return try {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -30) // Default to past 30 days
            val startDate = calendar.time
            
            val snapshot = firestore.collection(Invoice.COLLECTION_NAME)
                .whereEqualTo("storeId", storeId)
                .whereGreaterThanOrEqualTo("timestamp", Timestamp(startDate))
                .get()
                .await()
                
            val invoices = snapshot.documents.mapNotNull { it.toObject(Invoice::class.java) }
            val hourlyDistribution = mutableMapOf<Int, Int>()
            (0..23).forEach { hourlyDistribution[it] = 0 }
            
            val tempCal = Calendar.getInstance()
            invoices.forEach { invoice ->
                tempCal.time = invoice.timestamp.toDate()
                val hour = tempCal.get(Calendar.HOUR_OF_DAY)
                hourlyDistribution[hour] = (hourlyDistribution[hour] ?: 0) + 1
            }
            
            Result.Success(hourlyDistribution)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting hourly transaction distribution", e)
            Result.Error(e, e.message ?: "Error getting distribution")
        }
    }

    /**
     * Get revenue breakdown by product category
     */
    suspend fun getCategoryBreakdown(
        storeId: String,
        startDate: Date,
        endDate: Date
    ): Result<List<CategoryRevenue>> {
        return try {
            val invoicesSnapshot = firestore.collection(Invoice.COLLECTION_NAME)
                .whereEqualTo("storeId", storeId)
                .whereGreaterThanOrEqualTo("timestamp", Timestamp(startDate))
                .whereLessThanOrEqualTo("timestamp", Timestamp(endDate))
                .get()
                .await()
                
            val productsSnapshot = firestore.collection(Product.COLLECTION_NAME)
                .whereEqualTo("storeId", storeId)
                .whereEqualTo("isActive", true)
                .get()
                .await()
                
            val invoices = invoicesSnapshot.documents.mapNotNull { it.toObject(Invoice::class.java) }
            val products = productsSnapshot.documents.mapNotNull { it.toObject(Product::class.java) }
            
            val productCategoryMap = products.associate { it.productId to it.category.ifBlank { "Uncategorized" } }
            
            val categorySales = mutableMapOf<String, Double>()
            
            invoices.forEach { invoice ->
                invoice.items.forEach { item ->
                    val category = productCategoryMap[item.productId] ?: "Uncategorized"
                    categorySales[category] = (categorySales[category] ?: 0.0) + item.totalAmount
                }
            }
            
            val totalRevenue = categorySales.values.sum()
            val breakdown = categorySales.map { (category, revenue) ->
                val percentage = if (totalRevenue > 0) (revenue / totalRevenue) * 100.0 else 0.0
                CategoryRevenue(category, revenue, percentage)
            }.sortedByDescending { it.revenue }
            
            Result.Success(breakdown)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting category breakdown", e)
            Result.Error(e, e.message ?: "Error getting category breakdown")
        }
    }

    /**
     * Get recent transactions as real-time Flow
     */
    fun getRecentTransactions(storeId: String, limit: Int = 10): Flow<Result<List<Invoice>>> = callbackFlow {
        try {
            trySend(Result.Loading)
            val registration = firestore.collection(Invoice.COLLECTION_NAME)
                .whereEqualTo("storeId", storeId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(Result.Error(error, error.message ?: "Error listening to recent transactions"))
                        return@addSnapshotListener
                    }
                    
                    if (snapshot != null) {
                        val invoices = snapshot.documents.mapNotNull { it.toObject(Invoice::class.java) }
                        trySend(Result.Success(invoices))
                    }
                }
            
            awaitClose { registration.remove() }
        } catch (e: Exception) {
            Log.e(TAG, "Error in recent transactions callbackFlow", e)
            close(e)
        }
    }
}
