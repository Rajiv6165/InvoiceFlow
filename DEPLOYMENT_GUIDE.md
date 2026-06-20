# InvoiceFlow Production Deployment Guide

This guide details the step-by-step process required to securely compile, sign, package, and deploy **InvoiceFlow** (`com.invoiceflow.billing`) to the Google Play Store.

---

## 🛠️ Part 1: Production Signing Key Generation

Google Play requires all release applications to be cryptographically signed before they can be uploaded to the Play Console. You must generate a secure keystore file (.jks) using the Java Development Kit (JDK) `keytool` command-line utility.

### Step 1: Open Terminal or Command Prompt
Ensure that `keytool` is in your environment PATH (usually bundled within the JDK bin directory, e.g., `%JAVA_HOME%\bin` or Android Studio's JBR path).

### Step 2: Execute Keytool Command
Run the following command to generate a new key pair and keystore. Replace placeholders like `<keystore_name>` and `<key_alias>` with your preferred names:

```bash
keytool -genkey -v -keystore release-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias invoiceflow-key
```

### Step 3: Provide Security Details
When prompted, supply the following:
1. **Keystore Password**: Choose a strong password. Note this down securely!
2. **First & Last Name**: e.g., InvoiceFlow Production
3. **Organizational Unit**: IT / Software
4. **Organization**: InvoiceFlow
5. **City/Locality**: Bangalore
6. **State/Province**: Karnataka
7. **Country Code (2 letters)**: IN
8. **Key Password for Alias**: Press `Enter` to use the same password as the keystore (recommended).

> [!WARNING]
> Keep your `release-keystore.jks` file absolutely secure. If lost, you will not be able to push updates to the same app on the Play Store. Never check the keystore file or passwords into your public or private Git repository.

---

## ⚙️ Part 2: Configuring `local.properties` Securely

The `build.gradle.kts` file of the `app` module is configured to load signing credentials from `local.properties` at the root of the project. This ensures that no sensitive passwords or file paths are checked into version control.

### Step 1: Locate `local.properties`
Navigate to the root directory of your workspace:
[local.properties](file:///C:/Users/rajiv/OneDrive/Desktop/Projects/Main%20Projects/Billing%20System/local.properties)

### Step 2: Append Signing Variables
Open `local.properties` and add the following entries, replacing the values with your actual path and credentials:

```properties
# Google Play Store Release Signing Configurations
KEYSTORE_PATH=C:/Users/rajiv/OneDrive/Desktop/Projects/Main Projects/Billing System/release-keystore.jks
KEYSTORE_PASSWORD=your_strong_keystore_password
KEY_ALIAS=invoiceflow-key
KEY_PASSWORD=your_strong_key_alias_password
```

> [!IMPORTANT]
> - Ensure you use forward slashes `/` in the `KEYSTORE_PATH` to prevent escape character issues in Gradle on Windows.
> - Verify that `local.properties` is in your `.gitignore` file so it is never committed.

---

## 📦 Part 3: Generating the Release AAB Bundle in Android Studio

Google Play requires the **Android App Bundle (.aab)** format for new submissions instead of standard APKs. This allows Google Play to generate optimized APKs specific to each user's device configuration.

### Method A: Build via Android Studio UI (Recommended)
1. Open the **InvoiceFlow** project in Android Studio.
2. Go to the menu bar and select **Build > Generate Signed Bundle / APK...**.
3. Select **Android App Bundle** and click **Next**.
4. Choose the keystore path (`release-keystore.jks`), enter the passwords and alias you created in Part 1, and click **Next**.
5. Select **release** build variant.
6. Choose the destination folder for the `.aab` file and click **Finish**.
7. Once compile completes, Android Studio will display a notification with a link to the location of `app-release.aab`.

### Method B: Build via Gradle Wrapper (Command Line)
If you have configured `local.properties` as described in Part 2, you can build directly from the terminal:
1. Open terminal in the project root folder.
2. Execute the following command:
   ```powershell
   ./gradlew.bat :app:bundleRelease
   ```
3. After the build completes, the signed `.aab` file will be generated at:
   `C:\Users\rajiv\OneDrive\Desktop\Projects\Main Projects\Billing System\app\build\outputs\bundle\release\app-release.aab`

---

## 🚀 Part 4: Google Play Console Setup & Submission

To publish InvoiceFlow, you must create a developer account and register your application.

### Step 1: Create a Google Play Developer Account
1. Visit the [Google Play Console](https://play.google.com/console/signup).
2. Sign in with a Google account.
3. Accept the developer agreement, complete the verification, and pay the one-time **$25 USD registration fee**.

### Step 2: Create Your App
1. Inside the Play Console dashboard, click **Create app**.
2. **App Name**: InvoiceFlow
3. **Default Language**: English (United States) (or target regional languages).
4. **App or Game**: App
5. **Free or Paid**: Free (monetization via in-app SaaS subscriptions).
6. Agree to the Declarations and click **Create app**.

### Step 3: Complete App Setup (Initial Questionnaire)
You must complete all tasks listed under the "Set up your app" section:
- **App Access**: Indicate that parts of the app require authentication (provide dummy credentials for a Cashier and Store Owner for Play Store review).
- **Ads**: Declare that your app contains **no ads**.
- **Content Rating**: Complete the rating questionnaire (Retail/Utility category).
- **Target Audience**: Select age groups (e.g., 18 and older, as it is a commercial POS tool).
- **News Apps**: Declare that your app is **not** a news app.
- **COVID-19 Contact Tracing**: Declare that your app is **not** a COVID-19 app.
- **Data Safety**: Complete the safety form. Disclose that you collect:
  - Personal info (name, email) for account management.
  - Financial/Sales transaction info (sales, products, bills) for accounting features.
  - File collections (for PDF sharing).
  - Declare that all data is transmitted securely over HTTPS and users can request data deletion.
- **Government Apps**: Declare that your app does **not** represent a government entity.
- **Financial Features**: Declare that the app provides business accounting and billing services, but is not a licensed banking/lending app.

### Step 4: Configure Store Listing & Pricing
1. Navigate to **Store presence > Main store listing**.
2. Input the copy from your [STORE_LISTING.md](file:///C:/Users/rajiv/OneDrive/Desktop/Projects/Main%20Projects/Billing%20System/STORE_LISTING.md) (Title, Short Description, Full Description).
3. Upload assets following the [SCREENSHOTS_GUIDE.md](file:///C:/Users/rajiv/OneDrive/Desktop/Projects/Main%20Projects/Billing%20System/SCREENSHOTS_GUIDE.md):
   - **App Icon**: 512x512 PNG, max 1MB.
   - **Feature Graphic**: 1024x500 PNG, max 1MB.
   - **Phone Screenshots**: Upload at least 4-6 high-quality screenshots.
   - **Tablet Screenshots**: Upload 7-inch and 10-inch screenshots if desired (or reuse phone layouts styled cleanly).
4. Set up your **Privacy Policy** link to point to your hosted version of [privacy_policy.html](file:///C:/Users/rajiv/OneDrive/Desktop/Projects/Main%20Projects/Billing%20System/privacy_policy.html).

### Step 5: Start a Release Track
1. Navigate to **Testing > Closed testing** or **Testing > Internal testing**.
   > [!TIP]
   > It is highly recommended to release to **Internal testing** first. This allows up to 100 internal testers to download the build immediately without waiting for Google's full review cycle (which can take 3-7 days for new accounts).
2. Click **Create new release**.
3. Upload `app-release.aab`.
4. Enter release notes (e.g., "Initial production release of InvoiceFlow featuring smart POS billing, analytics, and cashier shifting.").
5. Click **Save** and then **Review release**.
6. Click **Start rollout** to make the build available to your testers.

---

## 📈 Part 5: Post-Release Updates & Log Tracking

Once InvoiceFlow is live, you must monitor its performance and release periodic updates.

### Pushing App Updates
1. Increment the versioning in `app/build.gradle.kts`:
   - `versionCode` must be increased by 1 (e.g., `versionCode = 2`).
   - `versionName` should be updated logically (e.g., `versionName = "1.0.1"`).
2. Generate a new signed `.aab` file using the same signing key.
3. Go to Play Console, create a new release on the production or testing track, upload the new `.aab`, and rollout.

### Production Logging & Analytics
1. **Firebase Crashlytics**:
   - Ensure Crashlytics is integrated. Since we disabled raw console logs in release mode (`ENABLE_LOGGING = false`), all runtime crashes and unhandled exceptions are automatically forwarded to the **Firebase Console > Crashlytics** dashboard.
2. **Google Play Console Vitals**:
   - Monitor the **Android Vitals** tab in the Play Console to track metrics such as crash rates, ANR (App Not Responding) rates, excessive wakeups, and battery drain. Ensure the crash rate remains below the bad behavior threshold of 1.09%.
