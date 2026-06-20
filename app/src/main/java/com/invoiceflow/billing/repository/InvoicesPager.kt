package com.invoiceflow.billing.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.invoiceflow.billing.model.Invoice
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/**
 * PagingSource for fetching paginated Invoices list from Firestore
 */
class InvoicesPagingSource(
    private val firestore: FirebaseFirestore,
    private val storeId: String
) : PagingSource<DocumentSnapshot, Invoice>() {
    
    override fun getRefreshKey(state: PagingState<DocumentSnapshot, Invoice>): DocumentSnapshot? {
        return null
    }

    override suspend fun load(params: LoadParams<DocumentSnapshot>): LoadResult<DocumentSnapshot, Invoice> {
        return try {
            val key = params.key
            
            var query = firestore.collection(Invoice.COLLECTION_NAME)
                .whereEqualTo("storeId", storeId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(params.loadSize.toLong())
                
            if (key != null) {
                query = query.startAfter(key)
            }
            
            val snapshot = query.get().await()
            val invoices = snapshot.documents.mapNotNull { it.toObject(Invoice::class.java) }
            val lastVisibleDoc = snapshot.documents.lastOrNull()
            
            LoadResult.Page(
                data = invoices,
                prevKey = null,
                nextKey = if (snapshot.size() < params.loadSize) null else lastVisibleDoc
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
