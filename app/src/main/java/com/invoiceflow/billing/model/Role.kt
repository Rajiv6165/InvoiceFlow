package com.invoiceflow.billing.model

/**
 * User roles in the InvoiceFlow system
 */
enum class Role {
    OWNER,      // Full CRUD access to inventory, reports, and store settings
    CASHIER     // Can only read inventory and write to Invoices collection
}
