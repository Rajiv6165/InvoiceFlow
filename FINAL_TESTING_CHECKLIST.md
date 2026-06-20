# InvoiceFlow Final Pre-Flight Testing Checklist

This checklist contains all critical verification steps to perform on **InvoiceFlow** prior to releasing the app to public production. Conduct these tests on a physical device using the `release` build variant to ensure proper obfuscation and security configuration.

---

## 🔒 1. Firebase & Security Configurations

Verify that cloud services are hardened and restrict access to authorized business tenants only.

- [ ] **Package Name Alignment**:
  - Check that the `google-services.json` contains `com.invoiceflow.billing` as the package name.
  - Verify that the SHA-1 and SHA-256 fingerprints of both your debug and production release keys are added to the Firebase Console settings.
- [ ] **Authentication Configuration**:
  - Verify that Email/Password and Phone authentication methods are enabled in the Firebase Console.
  - Test login with non-existent accounts to confirm error handling.
- [ ] **Firestore Database Rules**:
  - Confirm that the production rules in [firestore.rules](file:///C:/Users/rajiv/OneDrive/Desktop/Projects/Main%20Projects/Billing%20System/firestore.rules) have been deployed using `firebase deploy --only firestore`.
  - Validate that users cannot query collections outside of their `businessId` path.
  - Verify that cashier accounts cannot edit critical subscription or business settings.
- [ ] **Network Security Profile**:
  - Verify that HTTP traffic is blocked and only HTTPS connections are allowed (as per [network_security_config.xml](file:///C:/Users/rajiv/OneDrive/Desktop/Projects/Main%20Projects/Billing%20System/app/src/main/res/xml/network_security_config.xml)).
  - Attempt an HTTP connection to a test endpoint to ensure it fails securely.

---

## 🛒 2. Core POS & Billing Flows

Verify that the main business engine compiles invoices and completes checkout cycles accurately.

- [ ] **High-Volume Catalog Setup**:
  - Populate the catalog with at least 10 products with varying prices, GST rates (0%, 5%, 12%, 18%, 28%), and stock counts.
- [ ] **Barcode Scanning**:
  - Use the built-in device camera to scan a product barcode (or type the barcode value).
  - Verify that the correct item is instantly retrieved and added to the cart.
- [ ] **Cart Manipulation**:
  - Add 10 items to a single transaction cart.
  - Modify quantities, apply discounts (flat amount and percentage), and check that subtotal, tax computations, and final totals update correctly without rounding issues.
- [ ] **Checkout Transaction**:
  - Complete a sale transaction using cash, card, and digital UPI.
  - Verify that stock levels decrement immediately upon successful checkout.
- [ ] **Receipt Printing & Sharing**:
  - Generate the invoice PDF and verify the layout (logo alignment, itemized taxes, terms).
  - Test print commands for 2-inch and 3-inch thermal printers.
  - Verify that the WhatsApp share dialog opens with the generated PDF attached.

---

## 👥 3. Staff Management & Cashier Shifting

Verify role-based access control and session audit logs.

- [ ] **Shift Initiation**:
  - Start a cashier shift by inputting the opening drawer balance.
  - Verify that a session audit log entry is created with the exact timestamp and opening cash.
- [ ] **Shift Sales Tracking**:
  - Perform sales under the cashier account and check that cash and credit totals accumulate correctly in the shift summary.
- [ ] **Shift Closure & Reconciliation**:
  - End the cashier shift by inputting the closing cash amount.
  - Verify that a final shift report is generated showing drawer variance, total sales count, and cash drawer discrepancies.
- [ ] **Role Restrictions**:
  - Log in as a Cashier and attempt to access owner-only reports, analytics, or subscription billing settings. Confirm that access is blocked and redirects to permission warnings.

---

## 💳 4. Subscription & Trial Banner Display

Verify SaaS monetization gates and user warnings.

- [ ] **Trial Banner Display**:
  - Register a new business account and verify that the 14-day free trial banner is visible on the home dashboard.
  - Confirm that the banner accurately shows the remaining days left in the trial.
- [ ] **Trial Expiration Locking**:
  - Manually change a test business account's registration date in Firestore to 15 days ago (simulating trial expiration).
  - Confirm that the checkout flow is locked and displays a prominent screen asking the user to upgrade to a premium plan.
- [ ] **Premium Upgrade Activation**:
  - Toggle the subscription status in Firestore to `active` (premium status).
  - Confirm that the lock is lifted, the trial banner disappears, and all premium features are fully unlocked.

---

## 🔌 5. Offline Transitions & WorkManager Queue

Verify that InvoiceFlow operates seamlessly in poor connectivity environments (Kirana stores).

- [ ] **Offline Checkout (Airplane Mode)**:
  - Turn on Airplane mode on the test device.
  - Add items to the cart and perform a checkout.
  - Verify that the checkout succeeds locally, generates a PDF receipt, and adds the transaction to the local database cache.
- [ ] **WorkManager Queue Upload**:
  - While offline, check that the transaction is queued for sync.
  - Disable Airplane mode (restore internet connectivity).
  - Confirm that the WorkManager background task runs automatically, uploads the queued transaction to Firestore, and clears the local queue without duplicate records.

---

## ⚡ 6. Performance & Paging Latency

Verify that the UI remains fluid and responsive under load.

- [ ] **Database Stress Test**:
  - Populate the local database with 100+ product items and 50+ past invoices.
- [ ] **Paging List Smoothness**:
  - Scroll rapidly through the product inventory list and past sales log.
  - Verify that the list uses Paging 3 to load items dynamically, maintaining 60 FPS scrolling speeds without visual stutters or memory leaks.
- [ ] **Memory & Resource Utilization**:
  - Monitor memory usage in Android Studio Profiler during barcode scans and PDF generation. Ensure memory returns to baseline levels.

---

## 📱 7. Responsive UI & Form Factors

Verify visual fidelity across device form factors.

- [ ] **Small Screen Optimization**:
  - Test the layout on a low-resolution device (e.g., 5-inch screen or custom emulator). Ensure text does not clip and buttons remain clickable.
- [ ] **Orientation Switching**:
  - Rotate the device between Portrait and Landscape modes.
  - Verify that screen layouts adapt, scroll view positions are preserved, and no activity crash occurs due to configuration changes.
