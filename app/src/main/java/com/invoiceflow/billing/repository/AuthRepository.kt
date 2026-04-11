package com.invoiceflow.billing.repository

import android.util.Log
import com.invoiceflow.billing.model.Role
import com.invoiceflow.billing.model.Store
import com.invoiceflow.billing.model.User
import com.invoiceflow.billing.util.Constants
import com.invoiceflow.billing.util.Result
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for handling authentication and user/store data operations
 */
@Singleton
class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : BaseRepository() {
    
    companion object {
        private const val TAG = "AuthRepository"
    }
    
    /**
     * Get current authenticated user from Firebase Auth
     */
    fun getCurrentFirebaseUser(): FirebaseUser? {
        return firebaseAuth.currentUser
    }
    
    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }
    
    /**
     * Sign in with email and password
     */
    suspend fun signIn(email: String, password: String): Flow<Result<User>> = safeApiCall {
        // Validate input
        if (!isValidEmail(email)) {
            throw IllegalArgumentException("Invalid email address")
        }
        
        if (password.length < Constants.MIN_PASSWORD_LENGTH) {
            throw IllegalArgumentException("Password must be at least ${Constants.MIN_PASSWORD_LENGTH} characters")
        }
        
        // Sign in to Firebase Auth
        val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
        val firebaseUser = authResult.user ?: throw IllegalStateException("Firebase user is null")
        
        // Get user data from Firestore
        val userDoc = firestore.collection(User.COLLECTION_NAME)
            .document(firebaseUser.uid)
            .get()
            .await()
        
        if (!userDoc.exists()) {
            throw IllegalStateException("User document not found in Firestore")
        }
        
        val user = userDoc.toObject(User::class.java) 
            ?: throw IllegalStateException("Failed to parse user data")
        
        // Update last login time
        updateUserLastLogin(firebaseUser.uid)
        
        Log.d(TAG, "User signed in: ${user.email}")
        user
    }
    
    /**
     * Register new user with email and password
     * Automatically creates store and sets user as OWNER
     */
    suspend fun signUp(
        name: String,
        email: String,
        password: String,
        storeName: String = Constants.DEFAULT_STORE_NAME
    ): Flow<Result<User>> = safeApiCall {
        // Validate input
        if (!isValidEmail(email)) {
            throw IllegalArgumentException("Invalid email address")
        }
        
        if (password.length < Constants.MIN_PASSWORD_LENGTH) {
            throw IllegalArgumentException("Password must be at least ${Constants.MIN_PASSWORD_LENGTH} characters")
        }
        
        if (name.isBlank()) {
            throw IllegalArgumentException("Name cannot be empty")
        }
        
        // Create Firebase Auth user
        val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        val firebaseUser = authResult.user ?: throw IllegalStateException("Firebase user is null")
        
        // Generate unique store ID
        val storeId = firestore.collection(Store.COLLECTION_NAME).document().id
        
        // Create Store document first
        val store = Store.defaultKothariProvisionStore(storeId).copy(
            name = storeName.ifBlank { Constants.DEFAULT_STORE_NAME }
        )
        
        firestore.collection(Store.COLLECTION_NAME)
            .document(storeId)
            .set(store)
            .await()
        
        Log.d(TAG, "Store created: $storeId")
        
        // Create User document
        val user = User(
            userId = firebaseUser.uid,
            name = name,
            email = email,
            role = Role.OWNER,  // First user is always OWNER
            storeId = storeId,
            createdAt = com.google.firebase.Timestamp.now()
        )
        
        firestore.collection(User.COLLECTION_NAME)
            .document(firebaseUser.uid)
            .set(user)
            .await()
        
        Log.d(TAG, "User registered: ${user.email} with role ${user.role}")
        
        user
    }
    
    /**
     * Sign out current user
     */
    suspend fun signOut(): Flow<Result<Unit>> = safeApiCall {
        val userId = firebaseAuth.currentUser?.uid
        Log.d(TAG, "User signing out: $userId")
        firebaseAuth.signOut()
    }
    
    /**
     * Get current user from Firestore
     */
    suspend fun getCurrentUser(): User? {
        val firebaseUser = firebaseAuth.currentUser ?: return null
        
        return try {
            val userDoc = firestore.collection(User.COLLECTION_NAME)
                .document(firebaseUser.uid)
                .get()
                .await()
            
            if (userDoc.exists()) {
                userDoc.toObject(User::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current user", e)
            null
        }
    }
    
    /**
     * Get user by ID
     */
    suspend fun getUserById(userId: String): User? {
        return try {
            val userDoc = firestore.collection(User.COLLECTION_NAME)
                .document(userId)
                .get()
                .await()
            
            if (userDoc.exists()) {
                userDoc.toObject(User::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user by ID", e)
            null
        }
    }
    
    /**
     * Get store by ID
     */
    suspend fun getStoreById(storeId: String): Store? {
        return try {
            val storeDoc = firestore.collection(Store.COLLECTION_NAME)
                .document(storeId)
                .get()
                .await()
            
            if (storeDoc.exists()) {
                storeDoc.toObject(Store::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting store by ID", e)
            null
        }
    }
    
    /**
     * Get current user's store
     */
    suspend fun getCurrentUserStore(): Store? {
        val currentUser = getCurrentUser() ?: return null
        return getStoreById(currentUser.storeId)
    }
    
    /**
     * Observe authentication state changes
     */
    fun observeAuthState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }
    
    /**
     * Reset password for given email
     */
    suspend fun resetPassword(email: String): Flow<Result<Unit>> = safeApiCall {
        if (!isValidEmail(email)) {
            throw IllegalArgumentException("Invalid email address")
        }
        
        firebaseAuth.sendPasswordResetEmail(email).await()
        Log.d(TAG, "Password reset email sent to: $email")
    }
    
    /**
     * Update user's last login timestamp
     */
    private suspend fun updateUserLastLogin(userId: String) {
        try {
            firestore.collection(User.COLLECTION_NAME)
                .document(userId)
                .update("lastLoginAt", com.google.firebase.Timestamp.now())
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating last login", e)
        }
    }
    
    /**
     * Check if this is the first user (for seeding default store)
     */
    suspend fun isFirstUser(): Boolean {
        return try {
            val snapshot = firestore.collection(User.COLLECTION_NAME)
                .limit(1)
                .get()
                .await()
            
            snapshot.isEmpty
        } catch (e: Exception) {
            Log.e(TAG, "Error checking first user", e)
            false
        }
    }
}
