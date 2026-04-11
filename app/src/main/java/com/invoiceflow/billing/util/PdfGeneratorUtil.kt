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
}
