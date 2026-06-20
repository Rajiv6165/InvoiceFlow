package com.invoiceflow.billing.util

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Build
import androidx.annotation.RequiresApi
import com.invoiceflow.billing.model.Invoice
import com.invoiceflow.billing.model.Store
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility class for generating PDF invoices
 */
object PdfGeneratorUtil {
    
    // PDF dimensions for 80mm thermal receipt (in points)
    private const val RECEIPT_WIDTH_POINTS = 227f // 80mm ≈ 227 points
    private const val RECEIPT_HEIGHT_POINTS = 800f // Variable, max height
    
    // A4 dimensions (in points at 72 DPI)
    private const val A4_WIDTH_POINTS = 595f
    private const val A4_HEIGHT_POINTS = 842f
    
    /**
     * Generate PDF invoice and save to cache directory
     */
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun generateInvoicePdf(
        context: Context,
        invoice: Invoice,
        store: Store?
    ): File? {
        return try {
            // Create PDF document
            val pdfDocument = PdfDocument()
            
            // Use A4 size for better readability
            val pageInfo = PdfDocument.PageInfo.Builder(
                A4_WIDTH_POINTS.toInt(),
                A4_HEIGHT_POINTS.toInt(),
                1
            ).create()
            
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            
            // Draw content
            drawInvoiceContent(canvas, invoice, store, A4_WIDTH_POINTS)
            
            pdfDocument.finishPage(page)
            
            // Save to file
            val outputFile = createInvoiceFile(context, invoice)
            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }
            
            pdfDocument.close()
            
            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun drawInvoiceContent(
        canvas: Canvas,
        invoice: Invoice,
        store: Store?,
        width: Float
    ) {
        val paint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
        }
        
        val boldPaint = Paint(paint).apply {
            typeface = Typeface.DEFAULT_BOLD
        }
        
        var yPosition = 50f
        val margin = 40f
        val centerX = width / 2
        
        // Store Header
        store?.let {
            paint.textSize = 18f
            canvas.drawText(it.name, centerX, yPosition, paint.apply { textAlign = Paint.Align.CENTER })
            yPosition += 25f
            
            paint.textSize = 12f
            it.address.takeIf { addr -> addr.isNotBlank() }?.let { addr ->
                canvas.drawText(addr, centerX, yPosition, paint.apply { textAlign = Paint.Align.CENTER })
                yPosition += 18f
            }
            
            if (it.phone.isNotBlank()) {
                canvas.drawText("Phone: ${it.phone}", centerX, yPosition, paint.apply { textAlign = Paint.Align.CENTER })
                yPosition += 18f
            }
            
            yPosition += 20f
        }
        
        // Invoice Title
        paint.textSize = 20f
        canvas.drawText("INVOICE", centerX, yPosition, boldPaint.apply { 
            textAlign = Paint.Align.CENTER 
            color = Color.parseColor("#1976D2")
        })
        yPosition += 35f
        
        // Invoice Details
        paint.textSize = 11f
        paint.color = Color.BLACK
        canvas.drawText("Invoice No: ${invoice.invoiceNumber}", margin, yPosition, paint)
        canvas.drawText("Date: ${formatTimestamp(invoice.timestamp)}", width - margin, yPosition, paint.apply { 
            textAlign = Paint.Align.RIGHT 
        })
        yPosition += 20f
        
        canvas.drawText("Cashier: ${invoice.cashierName}", margin, yPosition, paint)
        yPosition += 30f
        
        // Divider Line
        canvas.drawLine(margin, yPosition, width - margin, yPosition, paint.apply {
            strokeWidth = 2f
        })
        yPosition += 20f
        
        // Table Headers
        boldPaint.textSize = 11f
        val col1X = margin
        val col2X = width * 0.3f
        val col3X = width * 0.55f
        val col4X = width - margin
        
        canvas.drawText("Item", col1X, yPosition, boldPaint)
        canvas.drawText("Qty", col2X, yPosition, boldPaint.apply { textAlign = Paint.Align.CENTER })
        canvas.drawText("Rate", col3X, yPosition, boldPaint.apply { textAlign = Paint.Align.CENTER })
        canvas.drawText("Total", col4X, yPosition, boldPaint.apply { textAlign = Paint.Align.RIGHT })
        yPosition += 18f
        
        // Divider
        canvas.drawLine(margin, yPosition, width - margin, yPosition, paint.apply {
            strokeWidth = 1f
        })
        yPosition += 20f
        
        // Items List
        paint.textSize = 10f
        paint.textAlign = Paint.Align.LEFT
        invoice.items.forEach { item ->
            // Item name
            canvas.drawText(item.productName, col1X, yPosition, paint)
            
            // Quantity
            canvas.drawText("${item.quantity}", col2X, yPosition, paint.apply { 
                textAlign = Paint.Align.CENTER 
            })
            
            // Rate
            canvas.drawText("₹${String.format("%.2f", item.unitPrice)}", col3X, yPosition, paint.apply { 
                textAlign = Paint.Align.CENTER 
            })
            
            // Total
            canvas.drawText("₹${String.format("%.2f", item.totalAmount)}", col4X, yPosition, paint.apply { 
                textAlign = Paint.Align.RIGHT 
            })
            
            yPosition += 18f
            
            // Check if we need a new page
            if (yPosition > A4_HEIGHT_POINTS - 150) {
                // Would need multi-page support here
            }
        }
        
        // Divider
        yPosition += 10f
        canvas.drawLine(margin, yPosition, width - margin, yPosition, paint.apply {
            strokeWidth = 1f
        })
        yPosition += 25f
        
        // Totals Section
        val totalSectionWidth = width * 0.6f
        val totalSectionX = width - margin - totalSectionWidth
        
        // Subtotal
        paint.textSize = 11f
        canvas.drawText("Subtotal:", totalSectionX, yPosition, paint)
        canvas.drawText("₹${String.format("%.2f", invoice.subtotal)}", width - margin, yPosition, paint.apply {
            textAlign = Paint.Align.RIGHT
        })
        yPosition += 20f
        
        // Tax
        canvas.drawText("Tax (GST):", totalSectionX, yPosition, paint)
        canvas.drawText("₹${String.format("%.2f", invoice.taxAmount)}", width - margin, yPosition, paint.apply {
            textAlign = Paint.Align.RIGHT
        })
        yPosition += 20f
        
        // Grand Total (Bold and Larger)
        boldPaint.textSize = 14f
        canvas.drawText("Grand Total:", totalSectionX, yPosition, boldPaint)
        canvas.drawText("₹${String.format("%.2f", invoice.grandTotal)}", width - margin, yPosition, boldPaint.apply {
            textAlign = Paint.Align.RIGHT
            color = Color.parseColor("#1976D2")
        })
        yPosition += 40f
        
        // Footer
        paint.textSize = 10f
        paint.color = Color.GRAY
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("Thank you for your business!", centerX, yPosition, paint)
        yPosition += 15f
        canvas.drawText("Terms & Conditions Apply", centerX, yPosition, paint)
    }
    
    private fun createInvoiceFile(context: Context, invoice: Invoice): File {
        val cacheDir = context.cacheDir
        val fileName = "Invoice_${invoice.invoiceNumber}_${System.currentTimeMillis()}.pdf"
        return File(cacheDir, fileName)
    }
    
    private fun formatTimestamp(timestamp: com.google.firebase.Timestamp): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(timestamp.toDate())
    }

    /**
     * Generate PDF daily business summary and save to cache directory
     */
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun generateDailySummaryPdf(
        context: Context,
        store: Store?,
        revenue: Double,
        billCount: Int,
        avgBillValue: Double,
        topProducts: List<com.invoiceflow.billing.model.TopProductModel>,
        categoryRevenue: List<com.invoiceflow.billing.model.CategoryRevenue>
    ): File? {
        return try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(
                A4_WIDTH_POINTS.toInt(),
                A4_HEIGHT_POINTS.toInt(),
                1
            ).create()
            
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            
            drawDailySummaryContent(
                canvas = canvas,
                store = store,
                revenue = revenue,
                billCount = billCount,
                avgBillValue = avgBillValue,
                topProducts = topProducts,
                categoryRevenue = categoryRevenue,
                width = A4_WIDTH_POINTS
            )
            
            pdfDocument.finishPage(page)
            
            val cacheDir = context.cacheDir
            val dateStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
            val fileName = "DailySummary_${dateStr}_${System.currentTimeMillis()}.pdf"
            val outputFile = File(cacheDir, fileName)
            
            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()
            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun drawDailySummaryContent(
        canvas: Canvas,
        store: Store?,
        revenue: Double,
        billCount: Int,
        avgBillValue: Double,
        topProducts: List<com.invoiceflow.billing.model.TopProductModel>,
        categoryRevenue: List<com.invoiceflow.billing.model.CategoryRevenue>,
        width: Float
    ) {
        val paint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
        }
        
        val boldPaint = Paint(paint).apply {
            typeface = Typeface.DEFAULT_BOLD
        }
        
        var yPosition = 50f
        val margin = 40f
        val centerX = width / 2
        
        // Store Header
        store?.let {
            paint.textSize = 18f
            canvas.drawText(it.name, centerX, yPosition, paint.apply { textAlign = Paint.Align.CENTER })
            yPosition += 22f
            
            paint.textSize = 11f
            if (it.address.isNotBlank()) {
                canvas.drawText(it.address, centerX, yPosition, paint.apply { textAlign = Paint.Align.CENTER })
                yPosition += 16f
            }
            yPosition += 10f
        }
        
        // Title
        paint.textSize = 20f
        canvas.drawText("DAILY BUSINESS SUMMARY", centerX, yPosition, boldPaint.apply { 
            textAlign = Paint.Align.CENTER 
            color = Color.parseColor("#2E7D32") // Green color for daily summary
        })
        yPosition += 35f
        
        // Date
        paint.textSize = 11f
        paint.color = Color.BLACK
        val todayStr = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date())
        canvas.drawText("Report Date: $todayStr", margin, yPosition, paint.apply { textAlign = Paint.Align.LEFT })
        yPosition += 25f
        
        // Divider
        canvas.drawLine(margin, yPosition, width - margin, yPosition, paint.apply {
            strokeWidth = 2f
            color = Color.BLACK
        })
        yPosition += 25f
        
        // Key Performance Indicators Section
        canvas.drawText("KEY PERFORMANCE INDICATORS (KPIs)", margin, yPosition, boldPaint.apply { 
            textSize = 13f 
            color = Color.parseColor("#1976D2")
            textAlign = Paint.Align.LEFT
        })
        yPosition += 20f
        
        paint.textSize = 11f
        paint.color = Color.BLACK
        canvas.drawText("Today's Total Revenue:", margin + 10, yPosition, paint.apply { textAlign = Paint.Align.LEFT })
        canvas.drawText("₹${String.format("%.2f", revenue)}", width - margin - 10, yPosition, boldPaint.apply { textAlign = Paint.Align.RIGHT })
        yPosition += 18f
        
        canvas.drawText("Total Bills/Transactions Count:", margin + 10, yPosition, paint.apply { textAlign = Paint.Align.LEFT })
        canvas.drawText("$billCount", width - margin - 10, yPosition, boldPaint.apply { textAlign = Paint.Align.RIGHT })
        yPosition += 18f
        
        canvas.drawText("Average Transaction Value:", margin + 10, yPosition, paint.apply { textAlign = Paint.Align.LEFT })
        canvas.drawText("₹${String.format("%.2f", avgBillValue)}", width - margin - 10, yPosition, boldPaint.apply { textAlign = Paint.Align.RIGHT })
        yPosition += 30f
        
        // Category Performance Section
        if (categoryRevenue.isNotEmpty()) {
            canvas.drawText("REVENUE BY PRODUCT CATEGORY", margin, yPosition, boldPaint.apply { 
                textSize = 13f 
                color = Color.parseColor("#1976D2")
                textAlign = Paint.Align.LEFT
            })
            yPosition += 20f
            
            // Draw table header
            boldPaint.textSize = 10f
            canvas.drawText("Category", margin + 10, yPosition, boldPaint.apply { textAlign = Paint.Align.LEFT })
            canvas.drawText("Revenue", width * 0.6f, yPosition, boldPaint.apply { textAlign = Paint.Align.RIGHT })
            canvas.drawText("Contribution", width - margin - 10, yPosition, boldPaint.apply { textAlign = Paint.Align.RIGHT })
            yPosition += 15f
            
            canvas.drawLine(margin + 10, yPosition, width - margin - 10, yPosition, paint.apply { strokeWidth = 1f; color = Color.BLACK })
            yPosition += 15f
            
            paint.textSize = 10f
            categoryRevenue.forEach { cat ->
                canvas.drawText(cat.category.ifBlank { "General" }, margin + 10, yPosition, paint.apply { textAlign = Paint.Align.LEFT })
                canvas.drawText("₹${String.format("%.2f", cat.revenue)}", width * 0.6f, yPosition, paint.apply { textAlign = Paint.Align.RIGHT })
                canvas.drawText("${String.format("%.1f", cat.percentage)}%", width - margin - 10, yPosition, paint.apply { textAlign = Paint.Align.RIGHT })
                yPosition += 15f
            }
            yPosition += 20f
        }
        
        // Top Products Section
        if (topProducts.isNotEmpty()) {
            canvas.drawText("TOP SOLD PRODUCTS", margin, yPosition, boldPaint.apply { 
                textSize = 13f 
                color = Color.parseColor("#1976D2")
                textAlign = Paint.Align.LEFT
            })
            yPosition += 20f
            
            // Table Header
            boldPaint.textSize = 10f
            canvas.drawText("Rank & Name", margin + 10, yPosition, boldPaint.apply { textAlign = Paint.Align.LEFT })
            canvas.drawText("SKU/Barcode", width * 0.5f, yPosition, boldPaint.apply { textAlign = Paint.Align.LEFT })
            canvas.drawText("Qty Sold", width * 0.75f, yPosition, boldPaint.apply { textAlign = Paint.Align.RIGHT })
            canvas.drawText("Revenue", width - margin - 10, yPosition, boldPaint.apply { textAlign = Paint.Align.RIGHT })
            yPosition += 15f
            
            canvas.drawLine(margin + 10, yPosition, width - margin - 10, yPosition, paint.apply { strokeWidth = 1f; color = Color.BLACK })
            yPosition += 15f
            
            paint.textSize = 10f
            topProducts.take(10).forEachIndexed { index, prod ->
                canvas.drawText("#${index + 1}  ${prod.name}", margin + 10, yPosition, paint.apply { textAlign = Paint.Align.LEFT })
                canvas.drawText(prod.sku, width * 0.5f, yPosition, paint.apply { textAlign = Paint.Align.LEFT })
                canvas.drawText("${prod.quantitySold}", width * 0.75f, yPosition, paint.apply { textAlign = Paint.Align.RIGHT })
                canvas.drawText("₹${String.format("%.2f", prod.revenue)}", width - margin - 10, yPosition, paint.apply { textAlign = Paint.Align.RIGHT })
                yPosition += 15f
                
                if (yPosition > A4_HEIGHT_POINTS - 100) {
                    // Stop drawing to fit single A4 page
                    return@forEachIndexed
                }
            }
            yPosition += 20f
        }
        
        // Footer
        yPosition = A4_HEIGHT_POINTS - 60f
        canvas.drawLine(margin, yPosition, width - margin, yPosition, paint.apply { strokeWidth = 1f; color = Color.LTGRAY })
        yPosition += 20f
        
        paint.textSize = 9f
        paint.color = Color.GRAY
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("InvoiceFlow Analytics Engine © 2026", centerX, yPosition, paint)
    }
}
