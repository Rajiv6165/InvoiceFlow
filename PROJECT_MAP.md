# 🗺️ InvoiceFlow Project Map

## 📁 Complete Directory Tree

```
Billing System/
│
├── 📄 README.md                          # Main documentation
├── 📄 QUICKSTART.md                      # Quick setup guide
├── 📄 PHASE1_SUMMARY.md                  # Phase 1 completion summary
├── 📄 PROJECT_MAP.md                     # This file
├── 📄 firestore.rules                    # Firestore security rules
├── 📄 firestore.indexes.json             # Firestore indexes
├── 📄 .gitignore                         # Git ignore rules
├── 📄 build.gradle.kts                   # Root build config
├── 📄 settings.gradle.kts                # Project settings
│
└── 📱 app/
    │
    ├── 📄 build.gradle.kts               # App dependencies
    ├── 📄 proguard-rules.pro             # ProGuard config
    ├── 📄 google-services.json           # Firebase credentials ⚠️ REPLACE
    │
    ├── 📂 src/main/
    │   │
    │   ├── 📄 AndroidManifest.xml        # App manifest
    │   │
    │   ├── 📂 java/com/InvoiceFlow/billing/
    │   │   │
    │   │   ├── 📄 BillingApplication.kt          # Application class
    │   │   │
    │   │   ├── 📂 di/                            # Dependency Injection
    │   │   │   ├── FirebaseModule.kt             # Firebase providers
    │   │   │   └── AppModule.kt                  # App-level providers
    │   │   │
    │   │   ├── 📂 model/                         # Data Models
    │   │   │   ├── Role.kt                       # OWNER | CASHIER
    │   │   │   ├── Store.kt                      # Store document
    │   │   │   └── User.kt                       # User document
    │   │   │
    │   │   ├── 📂 repository/                    # Repository Layer
    │   │   │   ├── BaseRepository.kt             # Base repository
    │   │   │   └── AuthRepository.kt             # Auth operations
    │   │   │
    │   │   ├── 📂 viewmodel/                     # ViewModel Layer
    │   │   │   ├── BaseViewModel.kt              # Base ViewModel
    │   │   │   └── AuthViewModel.kt              # Auth state management
    │   │   │
    │   │   ├── 📂 ui/                            # UI Layer (Jetpack Compose)
    │   │   │   │
    │   │   │   ├── MainActivity.kt               # Main entry + Navigation
    │   │   │   │
    │   │   │   ├── 📂 theme/                     # Theme & Styling
    │   │   │   │   ├── Color.kt                  # Brand colors
    │   │   │   │   ├── Type.kt                   # Typography
    │   │   │   │   └── Theme.kt                  # Material 3 theme
    │   │   │   │
    │   │   │   └── 📂 screens/                   # App Screens
    │   │   │       ├── 📂 login/
    │   │   │       │   └── LoginScreen.kt        # Login UI
    │   │   │       ├── 📂 register/
    │   │   │       │   └── RegistrationScreen.kt # Registration UI
    │   │   │       └── 📂 home/
    │   │   │           └── HomeScreen.kt         # Home/Dashboard UI
    │   │   │
    │   │   └── 📂 util/                          # Utilities
    │   │       ├── Constants.kt                  # App constants
    │   │       ├── Result.kt                     # Sealed class for results
    │   │       ├── LocalDataStore.kt             # DataStore wrapper
    │   │       └── AppUtil.kt                    # App utility
    │   │
    │   └── 📂 res/                               # Resources
    │       │
    │       ├── 📂 values/
    │       │   ├── strings.xml                   # String resources
    │       │   ├── colors.xml                    # Color definitions
    │       │   └── themes.xml                    # Theme config
    │       │
    │       └── 📂 xml/
    │           ├── backup_rules.xml              # Backup rules
    │           └── data_extraction_rules.xml     # Data extraction rules
    │
    └── 📂 build/                                 # Generated (gitignored)
```

---

## 🔀 Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                        USER INTERACTION                      │
│                    (Login/Register Screens)                  │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                    Jetpack Compose UI                        │
│                  (MainActivity.kt)                           │
│              - Navigation Host                               │
│              - Screen Transitions                            │
│              - Auth State Observer                           │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                      AuthViewModel                           │
│              - UI State Management                           │
│              - Business Logic                                │
│              - Validation                                    │
│              - Error Handling                                │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                     AuthRepository                           │
│              - Firebase Integration                          │
│              - Data Operations                               │
│              - Offline Support                               │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
         ┌───────────┴───────────┐
         │                       │
         ▼                       ▼
┌─────────────────┐     ┌─────────────────┐
│  Firebase Auth  │     │ Cloud Firestore │
│                 │     │                 │
│ - Email/Pass    │     │ - Users         │
│ - Sign In       │     │ - Stores        │
│ - Sign Up       │     │ - Products      │
│ - Sign Out      │     │ - Invoices      │
└─────────────────┘     └─────────────────┘
                                │
                                │
                                ▼
                    ┌───────────────────────┐
                    │  Offline Persistence  │
                    │  (Auto-sync on reconnect) │
                    └───────────────────────┘
```

---

## 🔄 Authentication Flow

### Registration Flow
```
User opens app
      ↓
Splash Screen (1.5s)
      ↓
Check if logged in? → No
      ↓
Login Screen
      ↓
Tap "Register" link
      ↓
Registration Screen
      ↓
Fill form + Tap "Create Account"
      ↓
AuthViewModel.validate()
      ↓
AuthRepository.signUp()
      ↓
Firebase Auth.createUser()
      ↓
Generate storeId
      ↓
Create Store document
      ↓
Create User document (role=OWNER)
      ↓
Navigate to Home Screen
      ↓
Show user info card ✅
```

### Login Flow
```
User opens app
      ↓
Splash Screen (1.5s)
      ↓
Check if logged in? → No
      ↓
Login Screen
      ↓
Enter credentials
      ↓
Tap "Login"
      ↓
AuthViewModel.validate()
      ↓
AuthRepository.signIn()
      ↓
Firebase Auth.signInWithEmailAndPassword()
      ↓
Fetch User from Firestore
      ↓
Update lastLoginAt
      ↓
Navigate to Home Screen
      ↓
Show user info card ✅
```

### Sign Out Flow
```
Home Screen
      ↓
Tap Sign Out button
      ↓
AuthViewModel.signOut()
      ↓
AuthRepository.signOut()
      ↓
Firebase Auth.signOut()
      ↓
Clear DataStore session
      ↓
Navigate to Login Screen
      ↓
Reset UI state ✅
```

---

## 🏗️ Architecture Layers

### Layer 1: Presentation (UI)
**Files**: `ui/screens/*`, `ui/theme/*`, `MainActivity.kt`
- Jetpack Compose composables
- Material 3 design
- State observation via StateFlow
- User interaction handling

### Layer 2: ViewModel
**Files**: `viewmodel/AuthViewModel.kt`, `BaseViewModel.kt`
- UI state management
- Business logic
- Input validation
- Error/success message handling
- Navigation triggers

### Layer 3: Repository
**Files**: `repository/AuthRepository.kt`, `BaseRepository.kt`
- Firebase API calls
- Data operations
- Error handling
- Result wrapping

### Layer 4: Data Source
**Services**: Firebase Auth, Cloud Firestore, Firebase Storage
- Authentication
- Database operations
- File storage
- Offline persistence

### Layer 5: Dependency Injection
**Files**: `di/FirebaseModule.kt`, `di/AppModule.kt`
- Hilt modules
- Singleton providers
- Firebase initialization
- DataStore setup

---

## 🎯 Key Classes & Responsibilities

| Class | Responsibility | Dependencies |
|-------|---------------|--------------|
| **BillingApplication** | App initialization | Hilt |
| **MainActivity** | Navigation host, Auth observer | NavHost, AuthViewModel |
| **AuthViewModel** | UI state, Validation, Business logic | AuthRepository |
| **AuthRepository** | Firebase operations, Data mapping | FirebaseAuth, Firestore |
| **User Model** | User data structure | Firestore |
| **Store Model** | Store data structure | Firestore |
| **Role Enum** | User role definition | None |
| **LocalDataStore** | Session persistence | DataStore |
| **Result** | Operation result wrapper | None |

---

## 🔐 Security Implementation

### Multi-Tenant Isolation
```
User A (Store X) ──→ Can only access Store X data
                         ├─→ Users with storeId = X
                         ├─→ Products with storeId = X
                         └─→ Invoices with storeId = X

User B (Store Y) ──→ Can only access Store Y data
                         ├─→ Users with storeId = Y
                         ├─→ Products with storeId = Y
                         └─→ Invoices with storeId = Y

❌ Cross-store access BLOCKED by Firestore rules
```

### Rule Enforcement
```javascript
// Example: Product read rule
match /Products/{productId} {
  allow read: if request.auth != null &&
                 resource.data.storeId == getUserStoreId();
}

// Translation: 
// "Allow reading a product ONLY IF:
//  1. User is authenticated
//  2. Product's storeId matches user's storeId"
```

---

## 📊 State Management

### LoginUiState
```kotlin
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val navigateToHome: Boolean = false,
    val navigateToRegister: Boolean = false
)
```

### RegistrationUiState
```kotlin
data class RegistrationUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val storeName: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val navigateToLogin: Boolean = false,
    val navigateToHome: Boolean = false
)
```

---

## 🧩 Component Relationships

```
┌──────────────────┐
│  MainActivity    │
│  (NavHost)       │
└────────┬─────────┘
         │
         ├──────────────────┬──────────────────┐
         │                  │                  │
         ▼                  ▼                  ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────┐
│  LoginScreen    │ │ RegisterScreen  │ │ HomeScreen  │
└────────┬────────┘ └────────┬────────┘ └──────┬──────┘
         │                   │                  │
         └───────────────────┼──────────────────┘
                             │
                             ▼
                   ┌──────────────────┐
                   │  AuthViewModel   │◄───────┐
                   └────────┬─────────┘        │
                            │                  │
                            ▼                  │
                   ┌──────────────────┐        │
                   │ AuthRepository   │────────┘
                   └────────┬─────────┘
                            │
                            │
                   ┌────────┴─────────┐
                   │                  │
                   ▼                  ▼
          ┌─────────────┐    ┌─────────────┐
          │Firebase Auth│    │ Cloud       │
          │             │    │ Firestore   │
          └─────────────┘    └─────────────┘
```

---

## 🎨 Screen Navigation Map

```
┌─────────────┐
│   Splash    │
│  (1.5s)     │
└──────┬──────┘
       │
       ├──────────────┐
       │              │
   Logged In      Not Logged In
       │              │
       ▼              ▼
┌─────────────┐  ┌─────────────┐
│    Home     │  │    Login    │
└──────┬──────┘  └──────┬──────┘
       │                │
       │ Sign Out       │ Tap "Register"
       │                │
       └────────────────┘
                        │
                        ▼
                 ┌─────────────┐
                 │  Register   │
                 └──────┬──────┘
                        │
                        │ Success
                        │
                        ▼
                 ┌─────────────┐
                 │    Home     │
                 └─────────────┘
```

---

## 📦 Build Process

```
Gradle Sync
      ↓
Download Dependencies
      ↓
Process Resources
      ↓
Compile Kotlin
      ↓
Process Manifest
      ↓
Merge Dex
      ↓
Sign APK (Debug)
      ↓
Install on Device
      ↓
Launch App ✅
```

---

## 🔍 Debugging Guide

### Check Logs
```bash
Android Studio → Logcat
Filter: "InvoiceFlow" or "AuthRepository" or "AuthViewModel"
```

### Inspect Firestore
```bash
Firebase Console → Firestore Database
Collections:
  - Stores/{storeId}
  - Users/{userId}
```

### Check Auth Users
```bash
Firebase Console → Authentication
Tab: Users
Shows: All registered users
```

### Test Offline Mode
```bash
1. Login successfully
2. Enable Airplane Mode
3. Restart app
4. Check Logcat for offline errors
5. Verify cached data displays
```

---

## 🚦 Development Workflow

### Adding New Feature (e.g., Product List)
```
1. Create Product model
2. Add ProductRepository methods
3. Create ProductViewModel
4. Build ProductListScreen composable
5. Add navigation route in MainActivity
6. Update Firestore rules for Products
7. Test with multiple stores
```

### Modifying Existing Feature
```
1. Locate relevant file (use this map)
2. Understand current implementation
3. Make changes following MVVM pattern
4. Update tests if applicable
5. Rebuild and test
```

---

## 📈 Next Phase Roadmap

### Phase 2: Inventory & POS
```
Current: Phase 1 ✅
Next: Phase 2 - Inventory Management
Then: Phase 3 - Advanced Features
Future: Phase 4 - Analytics & Reports
```

---

*This map provides complete visibility into the InvoiceFlow Phase 1 codebase.*  
*Use it to navigate, understand, and extend the application.*

**Last Updated**: March 15, 2026  
**Project**: InvoiceFlow POS & Billing System  
**Phase**: 1 - Foundation (COMPLETE)
