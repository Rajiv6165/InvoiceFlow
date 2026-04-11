# 🚀 Quick Start Guide - InvoiceFlow Phase 1

## ⚡ Fast Setup (5 Minutes)

### 1. Firebase Configuration
```bash
# Step 1: Create Firebase Project
→ https://console.firebase.google.com/
→ "Add Project" → Name: "InvoiceFlow-pos"

# Step 2: Download google-services.json
→ Add Android App
→ Package: com.InvoiceFlow.billing
→ Download google-services.json
→ Replace: app/google-services.json (delete placeholder, add real file)

# Step 3: Enable Authentication
→ Authentication → Get Started
→ Sign-in method → Email/Password → Enable → Save

# Step 4: Create Firestore Database
→ Firestore Database → Create Database
→ Start in test mode
→ Choose location (closest to you)
→ Enable
```

### 2. Deploy Security Rules
```javascript
// Go to: Firestore Rules tab
// Copy entire content from: firestore.rules
// Paste and Publish

// OR use Firebase CLI:
npm install -g firebase-tools
firebase login
firebase init firestore
firebase deploy --only firestore:rules
```

### 3. Build & Run
```bash
# Open in Android Studio
→ File → Open → Select project folder
→ Sync Gradle Files (Auto-sync on open)
→ Wait for "BUILD SUCCESSFUL"

# Run App
→ Connect device or start emulator (API 24+)
→ Shift+F10 (Run)
→ App launches with splash screen
```

---

## 🧪 First Test Registration

### Test Account Creation
```
Launch App → Register Link

Fill Form:
- Name: Rajiv Kothari
- Email: owner@kotharistore.com
- Password: owner123
- Confirm: owner123
- Store Name: Kothari Provision Store (or leave default)

→ Tap "Create Account"

Expected Result:
✅ Account created
✅ Navigate to Home screen
✅ Shows user info card with:
   - Name: Rajiv Kothari
   - Email: owner@kotharistore.com
   - Role: OWNER
   - Store ID: auto_generated_id
```

### Verify in Firebase Console
```
Firestore Database → Check collections:

Stores/{storeId}:
  {
    "name": "Kothari Provision Store",
    "gstRate": 18.0,
    "currency": "INR",
    "isActive": true
  }

Users/{userId}:
  {
    "name": "Rajiv Kothari",
    "email": "owner@kotharistore.com",
    "role": "OWNER",
    "storeId": "{same_as_above}"
  }
```

---

## 🔧 Common Issues & Quick Fixes

### Issue: Build Error - google-services.json
```
Error: google-services.json not found
Fix: Delete placeholder file, add real one from Firebase Console
```

### Issue: Permission Denied in Firestore
```
Error: PERMISSION_DENIED at /Stores/...
Fix: 
1. Go to Firestore Rules
2. Copy content from firestore.rules file
3. Paste and Publish
```

### Issue: Login Fails - User Not Found
```
Error: The user record was not found
Fix: 
1. Check if user exists in Firebase Console → Authentication
2. If not, register new account first
3. Ensure Email/Password sign-in is enabled
```

### Issue: App Crashes on Launch
```
Check Logcat for:
- Firebase initialization errors
- Missing dependencies
- Version conflicts

Fix:
1. Sync Gradle files
2. Clean & Rebuild (Build → Clean Project → Rebuild)
3. Check internet connection
```

---

## 📱 Testing Offline Mode

### Test Scenario
```
1. Login successfully with network
2. Turn on Airplane mode
3. Restart app
4. Should still see cached user data
5. Sign out (requires network)
```

**Expected**: App works offline for viewing cached data

---

## 🎯 Validation Checklist

Before moving to Phase 2, verify:

- [ ] ✅ App builds without errors
- [ ] ✅ google-services.json replaced with real config
- [ ] ✅ Firestore rules deployed
- [ ] ✅ Can register new account
- [ ] ✅ Auto-creates store document
- [ ] ✅ User role set to OWNER
- [ ] ✅ Can login after registration
- [ ] ✅ Can sign out
- [ ] ✅ Offline mode shows cached data
- [ ] ✅ Firestore shows correct data structure

---

## 📂 Important Files Reference

| File | Purpose |
|------|---------|
| `firestore.rules` | Multi-tenant security rules |
| `app/build.gradle.kts` | Dependencies configuration |
| `google-services.json` | Firebase credentials (REPLACE THIS) |
| `README.md` | Complete documentation |
| `MainActivity.kt` | App entry point + navigation |
| `AuthViewModel.kt` | Authentication logic |

---

## 🎨 Default Credentials (After Registration)

Since this is the initial setup, there are no default credentials.
**You must register the first account**, which will:
- Create your user as OWNER
- Auto-generate unique storeId
- Create "Kothari Provision Store" (or custom name)

---

## 📞 Next Steps After Phase 1

Once Phase 1 is working:

1. **Test thoroughly**: Try multiple registrations, logins, signouts
2. **Verify Firestore**: Check data structure matches requirements
3. **Test offline**: Ensure caching works
4. **Review security**: Confirm rules prevent cross-store access

Then proceed to **Phase 2**: Inventory Management & POS UI

---

## 🔗 Useful Links

- **Firebase Console**: https://console.firebase.google.com/
- **Firestore Documentation**: https://firebase.google.com/docs/firestore
- **Android Studio**: https://developer.android.com/studio
- **Jetpack Compose**: https://developer.android.com/jetpack/compose

---

**Estimated Setup Time**: 5-10 minutes  
**Difficulty Level**: Beginner-friendly  

*If you encounter any issues, check the main README.md for detailed troubleshooting.*
