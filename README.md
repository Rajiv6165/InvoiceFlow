# InvoiceFlow - Multi-Tenant POS & Billing System

## Phase 1: Foundation Setup ✅ COMPLETE

### Overview
InvoiceFlow is an offline-first Point of Sale (POS) and Billing system tailored for retail shops. This is Phase 1 of the project, establishing the foundational architecture with Firebase backend integration.

---

## 📋 What's Implemented in Phase 1

### ✅ Core Features
- **Multi-Tenant Architecture**: Complete store-based data isolation in Firestore
- **Firebase Authentication**: Email/password authentication with automatic user-store linking
- **Offline-First Setup**: Firebase persistence enabled for offline operations
- **Role-Based Access Control**: Owner and Cashier roles defined
- **Default Tenant Seeding**: "Kothari Provision Store" configured as default test tenant

### ✅ Technical Stack
- **Frontend**: Android (Kotlin with Jetpack Compose)
- **Architecture**: MVVM with Clean Architecture principles
- **Backend**: Firebase Auth + Cloud Firestore + Firebase Storage
- **Dependency Injection**: Hilt
- **Async Operations**: Kotlin Coroutines + Flow

### ✅ Security
- **Firestore Security Rules**: Multi-tenant isolation enforced at database level
- **Store ID Validation**: Every document must contain storeId
- **Role-Based Permissions**: Different access levels for Owner vs Cashier

---

## 🚀 Getting Started

### Prerequisites
1. **Android Studio**: Hedgehog (2023.1.1) or newer
2. **JDK**: Version 17 or higher
3. **Android SDK**: API 34 (targetSdkVersion)
4. **Firebase Account**: Create a Firebase project

### Step 1: Clone/Setup Project
```bash
cd "c:\Users\rajiv\OneDrive\Desktop\Projects\Billing System"
```

### Step 2: Configure Firebase

#### 2.1 Create Firebase Project
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Add Project"
3. Enter project name (e.g., "InvoiceFlow-pos")
4. Enable Google Analytics (optional)
5. Click "Create Project"

#### 2.2 Add Android App to Firebase
1. In Firebase Console, click "Add app" → Select Android icon
2. Enter package name: `com.InvoiceFlow.billing`
3. Download `google-services.json`
4. **Replace** the placeholder file at:
   ```
   app/google-services.json
   ```
   with your downloaded file

#### 2.3 Enable Firebase Authentication
1. In Firebase Console → Authentication → Get Started
2. Click "Sign-in method" tab
3. Enable "Email/Password"
4. Click "Save"

#### 2.4 Create Cloud Firestore Database
1. In Firebase Console → Firestore Database → Create Database
2. Start in **test mode** (we'll update rules next)
3. Choose a location closest to your users
4. Click "Enable"

#### 2.5 Deploy Firestore Security Rules
1. Go to Firestore Rules tab
2. Copy the entire content from `firestore.rules` file in this project
3. Paste into the Firebase Console rules editor
4. Click "Publish"

**Alternative**: Use Firebase CLI
```bash
firebase deploy --only firestore:rules
```

### Step 3: Build Configuration

The project is already configured with:
- Kotlin 1.9.20
- Jetpack Compose BOM 2023.10.01
- Firebase BoM 32.6.0
- Hilt 2.48.1
- Navigation Compose 2.7.5

No additional configuration needed unless you want to customize versions.

### Step 4: Sync and Build

1. Open project in Android Studio
2. Click "Sync Project with Gradle Files"
3. Wait for dependencies to download
4. Build → Make Project (Ctrl+F9)

### Step 5: Run the App

1. Connect an Android device or start an emulator (API 24+)
2. Run → Run 'app' (Shift+F10)
3. App will show splash screen → Login screen

---

## 🧪 Testing Phase 1

### Test Registration
1. Launch app → Tap "Register" link
2. Fill in details:
   - **Name**: Your name
   - **Email**: Valid email (e.g., owner@test.com)
   - **Password**: Minimum 6 characters
   - **Confirm Password**: Same as password
   - **Store Name**: Optional (defaults to "Kothari Provision Store")
3. Tap "Create Account"
4. **Expected Result**: 
   - Account created successfully
   - Navigate to Home screen
   - User info card shows: Name, Email, Role=OWNER, Store ID (auto-generated)

### Test Login
1. Sign out from Home screen
2. Enter registered email and password
3. Tap "Login"
4. **Expected Result**: Successfully logged in

### Test Offline Mode
1. Login successfully
2. Turn off network (airplane mode)
3. Restart app
4. **Expected Result**: App still works, can view cached user data

### Verify Firestore Data
1. Go to Firebase Console → Firestore Database
2. Check collections:
   - **Stores/**: Should have one document with your store
   - **Users/**: Should have one document with your user
3. Verify structure matches models:
   ```json
   Stores/{storeId}: {
     "storeId": "auto_generated_id",
     "name": "Kothari Provision Store",
     "gstRate": 18.0,
     "currency": "INR",
     "createdAt": Timestamp(...),
     "isActive": true
   }
   
   Users/{userId}: {
     "userId": "firebase_auth_uid",
     "name": "Your Name",
     "email": "your@email.com",
     "role": "OWNER",
     "storeId": "same_as_above",
     "createdAt": Timestamp(...)
   }
   ```

---

## 📁 Project Structure

```
app/src/main/java/com/InvoiceFlow/billing/
├── di/                          # Dependency Injection (Hilt modules)
│   ├── FirebaseModule.kt
│   └── AppModule.kt
├── model/                       # Data Models
│   ├── Role.kt                  # OWNER | CASHIER
│   ├── Store.kt                 # Store document
│   └── User.kt                  # User document
├── repository/                  # Repository Layer
│   ├── BaseRepository.kt
│   └── AuthRepository.kt        # Auth operations
├── ui/                          # UI Layer (Jetpack Compose)
│   ├── screens/
│   │   ├── login/
│   │   │   └── LoginScreen.kt
│   │   ├── register/
│   │   │   └── RegistrationScreen.kt
│   │   └── home/
│   │       └── HomeScreen.kt
│   ├── theme/
│   │   ├── Color.kt
│   │   ├── Type.kt
│   │   └── Theme.kt
│   └── MainActivity.kt          # Main entry point
├── util/                        # Utilities
│   ├── Constants.kt
│   ├── Result.kt                # Sealed class for results
│   ├── LocalDataStore.kt        # DataStore wrapper
│   └── AppUtil.kt
├── viewmodel/                   # ViewModel Layer
│   ├── BaseViewModel.kt
│   └── AuthViewModel.kt         # Auth state management
└── BillingApplication.kt        # Application class
```

**Resource Files:**
```
app/src/main/res/
├── values/
│   ├── strings.xml
│   ├── colors.xml
│   └── themes.xml
└── xml/
    ├── backup_rules.xml
    └── data_extraction_rules.xml
```

---

## 🔐 Firestore Security Rules Summary

The deployed rules ensure:

1. **Store Isolation**: Users can only read their own store's data
2. **User Document Access**: Users can only read/update their own profile
3. **Product Access**: Only users from same store can read products
4. **Invoice Creation**: Any authenticated user from store can create invoices
5. **No Cross-Tenant Access**: Impossible to query another store's data

Example rule enforcement:
```javascript
// User cannot read products from different store
match /Products/{productId} {
  allow read: if request.auth != null &&
                 resource.data.storeId == getUserStoreId();
}
```

---

## 🎯 Key Architectural Decisions

### 1. Multi-Tenancy Strategy
- **Approach**: Database-level isolation using storeId field
- **Benefit**: Single database, logical separation, cost-effective
- **Security**: Enforced via Firestore security rules

### 2. Offline-First Design
- Firebase Persistence enabled by default
- Cache size set to UNLIMITED (100MB)
- Automatic sync when network returns

### 3. Auto-Generated Store ID
- Each new registration creates unique storeId
- First user automatically becomes OWNER
- Store document created before user document

### 4. Clean Architecture
- **UI Layer**: Compose screens + ViewModels
- **Domain Layer**: Use cases (to be added in Phase 2)
- **Data Layer**: Repositories + Firebase sources

---

## ⚠️ Known Limitations (Phase 1)

1. **No Email Verification**: Users can login immediately after registration
2. **No Password Reset UI**: Available in repo but not exposed in UI
3. **Single User Per Store**: Phase 2 will add cashier creation by owner
4. **No Store Settings Management**: Basic store creation only

These will be addressed in upcoming phases.

---

## 📦 Next Steps (Phase 2)

Phase 2 will implement:
- ✅ Inventory Management (Product CRUD)
- ✅ Barcode Scanning
- ✅ Shopping Cart & Billing
- ✅ Invoice Generation & Storage
- ✅ PDF Receipt Generation
- ✅ GST Calculation
- ✅ Sales Reports Dashboard

---

## 🐛 Troubleshooting

### Build Errors
**Problem**: "google-services.json not found"
**Solution**: Replace placeholder file with actual Firebase config

**Problem**: "FirebaseAuth cannot be resolved"
**Solution**: Sync Gradle files, check internet connection

### Runtime Errors
**Problem**: "Permission denied" in Firestore
**Solution**: Ensure security rules are published correctly

**Problem**: "Network error" on login
**Solution**: Check internet permission in manifest, verify network connectivity

### Firebase Issues
**Problem**: "App not registered with Firebase"
**Solution**: Verify package name matches in Firebase Console

**Problem**: "Authentication failed"
**Solution**: Enable Email/Password sign-in method in Firebase Auth

---

## 📞 Support

For issues or questions:
1. Check Firebase Console logs
2. Review Logcat output (filter: "InvoiceFlow" or "Firebase")
3. Verify firestore.rules syntax
4. Ensure all dependencies are synced

---

## 📄 License

This project is proprietary software. All rights reserved.

---

## ✅ Phase 1 Completion Checklist

- [x] MVVM project structure created
- [x] Firebase modules configured
- [x] Firestore security rules deployed
- [x] Data models implemented (User, Store, Role)
- [x] AuthRepository with Firebase integration
- [x] AuthViewModel with state management
- [x] Login Screen (Compose)
- [x] Registration Screen (Compose)
- [x] Home Screen (Compose)
- [x] Navigation setup
- [x] Offline persistence enabled
- [x] Default tenant seeding logic
- [x] README documentation

---

**Phase 1 Status**: ✅ COMPLETE  
**Ready for**: Phase 2 - Inventory Management & POS UI

---

*Last Updated: March 15, 2026*
