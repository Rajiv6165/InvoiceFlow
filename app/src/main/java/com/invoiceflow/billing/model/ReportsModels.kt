package com.invoiceflow.billing.model

import java.util.Date

/**
2.  * Data model for today's sales metrics compared to yesterday
3.  */
data class TodaysSalesModel(
    val totalRevenue: Double = 0.0,
    val billCount: Int = 0,
    val averageValue: Double = 0.0,
    val revenueTrendPercent: Double = 0.0, // Percentage change vs yesterday
    val revenueTrendUp: Boolean = true,
    val billsTrendPercent: Double = 0.0,    // Percentage change vs yesterday
    val billsTrendUp: Boolean = true
)

/**
 * Data model for period-based sales analytics metrics
 */
data class SalesPeriodModel(
    val totalRevenue: Double = 0.0,
    val transactionCount: Int = 0,
    val averageTransactionValue: Double = 0.0,
    val highestTransactionValue: Double = 0.0,
    val lowestTransactionValue: Double = 0.0,
    val peakHour: Int = 0,
    val peakDayOfWeek: Int = 1, // Calendar.SUNDAY = 1
    val cashRevenue: Double = 0.0,
    val digitalRevenue: Double = 0.0,
    val transactionSuccessRate: Double = 100.0,
    val hourlyDistribution: Map<Int, Int> = emptyMap(),
    val dayOfWeekDistribution: Map<Int, Int> = emptyMap(),
    val comparisonRevenuePercentage: Double = 0.0, // vs previous period of same length
    val comparisonRevenueUp: Boolean = true
)

/**
 * Daily revenue record for trend line charts
 */
data class DailyRevenue(
    val dateStr: String = "", // e.g. "yyyy-MM-dd" or "dd MMM"
    val date: Date = Date(),
    val revenue: Double = 0.0,
    val billCount: Int = 0
)

/**
 * Product performance details for analytics rank
 */
data class TopProductModel(
    val productId: String = "",
    val name: String = "",
    val sku: String = "",
    val category: String = "",
    val quantitySold: Int = 0,
    val revenue: Double = 0.0,
    val percentageOfTotalRevenue: Double = 0.0,
    val stockQty: Int = 0,
    val minStockLevel: Int = 0
)

/**
 * Category revenue breakdown for donut charts
 */
data class CategoryRevenue(
    val category: String = "",
    val revenue: Double = 0.0,
    val percentage: Double = 0.0
)
