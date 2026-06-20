package com.invoiceflow.billing.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.invoiceflow.billing.model.Product
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * PagingSource for fetching paginated Products list from Firestore
 */
class ProductsPagingSource(
    private val firestore: FirebaseFirestore,
    private val storeId: String
) : PagingSource<DocumentSnapshot, Product>() {
    
    override fun getRefreshKey(state: PagingState<DocumentSnapshot, Product>): DocumentSnapshot? {
        // Return null to anchor refreshing back to page index 0
        return null
    }

    override suspend fun load(params: LoadParams<DocumentSnapshot>): LoadResult<DocumentSnapshot, Product> {
        return try {
            val key = params.key
            
            var query = firestore.collection(Product.COLLECTION_NAME)
                .whereEqualTo("storeId", storeId)
                .whereEqualTo("isActive", true)
                .orderBy("name")
                .limit(params.loadSize.toLong())
                
            if (key != null) {
                query = query.startAfter(key)
            }
            
            val snapshot = query.get().await()
            val products = snapshot.documents.mapNotNull { it.toObject(Product::class.java) }
            val lastVisibleDoc = snapshot.documents.lastOrNull()
            
            LoadResult.Page(
                data = products,
                prevKey = null, // Forward direction only
                nextKey = if (snapshot.size() < params.loadSize) null else lastVisibleDoc
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
