package com.invoiceflow.billing.ui.screens.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.invoiceflow.billing.model.Product
import java.text.NumberFormat

@Composable
fun AddEditProductDialog(
    product: Product?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Double, Int, String, String, Double) -> Unit
) {
    var name by remember { mutableStateOf(product?.name ?: "") }
    var barcode by remember { mutableStateOf(product?.barcode ?: "") }
    var sku by remember { mutableStateOf(product?.sku ?: "") }
    var priceStr by remember { mutableStateOf(product?.price?.toString() ?: "") }
    var stockQtyStr by remember { mutableStateOf(product?.stockQty?.toString() ?: "") }
    var description by remember { mutableStateOf(product?.description ?: "") }
    var category by remember { mutableStateOf(product?.category ?: "") }
    var gstRateStr by remember { mutableStateOf(product?.gstRate?.toString() ?: "18.0") }
    
    var nameError by remember { mutableStateOf<String?>(null) }
    var priceError by remember { mutableStateOf<String?>(null) }
    var stockError by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (product == null) "Add Product" else "Edit Product",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        nameError = null
                    },
                    label = { Text("Product Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it) } }
                )
                
                // Barcode/SKU
                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text("Barcode/SKU") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Price
                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { 
                        priceStr = it
                        priceError = null
                    },
                    label = { Text("Price (₹) *") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = priceError != null,
                    supportingText = priceError?.let { { Text(it) } }
                )
                
                // Stock Quantity
                OutlinedTextField(
                    value = stockQtyStr,
                    onValueChange = { 
                        stockQtyStr = it
                        stockError = null
                    },
                    label = { Text("Stock Quantity *") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = stockError != null,
                    supportingText = stockError?.let { { Text(it) } }
                )
                
                // Category
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                
                // GST Rate
                OutlinedTextField(
                    value = gstRateStr,
                    onValueChange = { gstRateStr = it },
                    label = { Text("GST Rate (%)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    supportingText = { Text("Default: 18%") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Validate
                    var hasError = false
                    
                    if (name.isBlank()) {
                        nameError = "Name is required"
                        hasError = true
                    }
                    
                    val price = priceStr.toDoubleOrNull()
                    if (price == null || price < 0) {
                        priceError = "Invalid price"
                        hasError = true
                    }
                    
                    val stockQty = stockQtyStr.toIntOrNull()
                    if (stockQty == null || stockQty < 0) {
                        stockError = "Invalid stock quantity"
                        hasError = true
                    }
                    
                    if (!hasError) {
                        onSave(
                            name,
                            barcode,
                            sku,
                            price!!,
                            stockQty!!,
                            description,
                            category,
                            gstRateStr.toDoubleOrNull() ?: 18.0
                        )
                    }
                },
                enabled = name.isNotBlank() && priceStr.isNotBlank() && stockQtyStr.isNotBlank()
            ) {
                Text(if (product == null) "Add" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
