# InvoiceFlow - Smart POS & Billing System

![Version](https://img.shields.io/badge/version-1.0.0-blue)
![Platform](https://img.shields.io/badge/platform-Android-green)
![Status](https://img.shields.io/badge/status-Production%20Ready-success)

A professional, cloud-native Point of Sale and Billing SaaS application 
built for Indian retail businesses. Powered by Firebase and built with 
modern Android development practices.

---

## ✅ All Phases Complete

| Phase | Feature | Status |
|-------|---------|--------|
| Phase 1 | Foundation, Auth, Firebase Setup | ✅ Complete |
| Phase 2 | Inventory Management & POS Engine | ✅ Complete |
| Phase 3 | Barcode Scanning & PDF Invoices | ✅ Complete |
| Phase 4 | Analytics Dashboard & Reports | ✅ Complete |
| Phase 5 | Staff Management & Activity Logs | ✅ Complete |
| Phase 6 | Subscription System & Super Admin | ✅ Complete |
| Phase 7 | Premium UI Polish & Performance | ✅ Complete |
| Phase 8 | Production Deployment Preparation | ✅ Complete |

---

## 🚀 Key Features

### 💰 Point of Sale
- Lightning fast barcode scanning using CameraX + ML Kit
- Smart cart with real-time GST calculation
- Confetti animation for large transactions
- Offline billing — works without internet
- PDF invoice generation and WhatsApp sharing

### 📦 Inventory Management
- Full product CRUD with barcode/SKU support
- Real-time stock tracking across all devices
- Low stock alerts and notifications
- Category-based product organization
- Bulk CSV export

### 📊 Analytics Dashboard
- Real-time revenue and transaction metrics
- Daily, weekly, monthly sales trends
- Top selling products report
- Staff performance leaderboard
- Interactive charts with date range filters

### 👥 Staff Management
- Owner can create and manage cashier accounts
- Role-based access control (Owner vs Cashier)
- Activity logging for every action
- Shift tracking with performance metrics
- Deactivate/reactivate staff accounts

### 🔐 Subscription System
- 14-day free trial on registration
- Three plans: Starter, Professional, Enterprise
- Beautiful paywall screen with WhatsApp contact
- Trial countdown banner (green → orange → red)
- Feature gating based on plan limits

### 🎨 Premium UI
- Deep Blue and Emerald Green design system
- Android 12+ Splash Screen API
- 4-slide animated onboarding flow
- Skeleton loading animations
- Real-time offline/sync status indicator
- Material Design 3 throughout

---

## 🏗️ Technical Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI Framework | Jetpack Compose |
| Architecture | MVVM + Clean Architecture |
| Dependency Injection | Dagger Hilt |
| Database | Firebase Cloud Firestore |
| Authentication | Firebase Auth |
| Storage | Firebase Storage |
| Async | Kotlin Coroutines + Flow |
| Barcode Scanning | CameraX + ML Kit |
| Background Work | WorkManager |
| Pagination | Paging 3 |
| PDF Generation | Android PdfDocument API |

---

## 🔐 Security Architecture

- **Multi-tenant isolation** — storeId enforced on every Firestore document
- **Role-based Firestore rules** — Owners and Cashiers have different permissions
- **Subscription validation** — expired accounts blocked at database level
- **Network security config** — HTTPS only, no cleartext traffic
- **ProGuard** — code obfuscation enabled for release builds
- **SuperAdmin collection** — completely locked from client apps

---

## 📁 Project Structure
app/src/main/java/com/invoiceflow/billing/

├── di/                    # Hilt dependency injection modules

├── model/                 # Data models

│   ├── User.kt

│   ├── Store.kt

│   ├── Role.kt

│   ├── ReportsModels.kt

│   ├── StaffModels.kt

│   └── SubscriptionStatus.kt

├── repository/            # Data layer

│   ├── AuthRepository.kt

│   ├── ProductRepository.kt

│   ├── ReportsRepository.kt

│   ├── ActivityLogRepository.kt

│   ├── SubscriptionRepository.kt

│   ├── LicenseActivationRepository.kt

│   ├── ProductsPager.kt

│   └── InvoicesPager.kt

├── ui/

│   ├── screens/

│   │   ├── login/         # Login + Onboarding

│   │   ├── register/      # Registration

│   │   ├── home/          # Dashboard

│   │   ├── inventory/     # Product management

│   │   ├── pos/           # POS + Cart

│   │   ├── analytics/     # Reports & charts

│   │   ├── staff/         # Staff management

│   │   ├── profile/       # Settings

│   │   ├── barcode/       # Scanner

│   │   └── main/          # Navigation + Offline banner

│   └── theme/             # Design system

├── util/                  # Helpers

│   ├── PdfGeneratorUtil.kt

│   ├── NetworkMonitor.kt

│   ├── BackgroundSyncWorker.kt

│   └── LocalDataStore.kt

└── viewmodel/             # ViewModels for all screens

---

## ⚙️ Setup & Installation

### Prerequisites
- Android Studio Hedgehog or newer
- JDK 17+
- Android SDK API 34
- Firebase Account

### Step 1: Clone Repository
```bash
git clone https://github.com/Rajiv6165/InvoiceFlow.git
cd InvoiceFlow
```

### Step 2: Configure Firebase
1. Go to [Firebase Console](https://console.firebase.google.com)
2. Create new project
3. Add Android app with package: `com.invoiceflow.billing`
4. Download `google-services.json`
5. Place in `app/` folder

### Step 3: Enable Firebase Services
- Authentication → Email/Password
- Firestore Database → Create database
- Deploy `firestore.rules` from this repo

### Step 4: Build & Run
```bash
# Sync dependencies
File → Sync Project with Gradle Files

# Run on device
Run → Run 'app'
```

---

## 🧪 Testing

See `FINAL_TESTING_CHECKLIST.md` for the complete pre-launch testing guide.

Key test areas:
- Register new store and verify Firestore data
- Complete full POS transaction with barcode scan
- Test PDF invoice generation and WhatsApp share
- Verify analytics show correct data
- Create cashier and test role restrictions
- Test offline mode with airplane mode
- Verify subscription trial countdown

---

## 🚀 Deployment

See `DEPLOYMENT_GUIDE.md` for complete Play Store submission guide.

Key steps:
1. Generate signing keystore
2. Configure `local.properties` with signing details
3. Build release AAB
4. Submit to Google Play Console

---

## 📄 Legal

- `privacy_policy.html` — Required for Play Store
- `terms_of_service.html` — Terms of use
- See `STORE_LISTING.md` for Play Store metadata

---

## 🏪 Play Store

**App Name:** InvoiceFlow - POS & Billing  
**Package:** com.invoiceflow.billing  
**Version:** 1.0.0  
**Target SDK:** 34  
**Min SDK:** 24 (Android 7.0+)

---

## 📞 Contact & Support

For licensing and business inquiries contact via WhatsApp.

---

*Built with ❤️ for Indian retail businesses*
*© 2026 InvoiceFlow. All rights reserved.*
