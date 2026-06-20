package com.invoiceflow.billing.model

/**
 * Subscription status sealed class to represent the current state of a store's subscription
 */
sealed class SubscriptionStatus {
    object Active : SubscriptionStatus()
    object Trial : SubscriptionStatus()
    data class Expired(val daysSinceExpiry: Int) : SubscriptionStatus()
    data class Suspended(val reason: String) : SubscriptionStatus()
    object Error : SubscriptionStatus()
}
