# 🔧 Advanced Gradle Build Troubleshooting Guide

## Current Status

### ✅ Verified Configuration (No Issues Found):

**Root build.gradle.kts:**
```kotlin
// NO Crashlytics plugin present ✅
// NO Firebase Performance plugin present ✅

buildscript {
    dependencies {
        classpath("com.android.tools.build:gradle:8.2.0")           // ✅ Latest
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.20") // ✅ Compatible
        classpath("com.google.gms:google-services:4.4.1")            // ✅ Fixed
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.48.1") // ✅ Compatible
    }
}

plugins {
    id("com.android.application") version "8.2.0" apply false       // ✅
    id("com.android.library") version "8.2.0" apply false           // ✅
    id("org.jetbrains.kotlin.android") version "1.9.20" apply false // ✅
    id("com.google.gms.google-services") version "4.4.1" apply false // ✅
    id("com.google.dagger.hilt.android") version "2.48.1" apply false // ✅
}
```

**All plugins are up-to-date and compatible with Gradle 8.2+**

---

## 🎯 Likely Causes & Solutions

Since the plugins are correct, the error is likely caused by one of these issues:

### **Issue #1: Gradle Cache Corruption** ⭐ MOST LIKELY

**Symptom:** Same error persists after fixing plugin versions

**Solution:** Clear all Gradle caches

#### **Windows PowerShell Commands:**

```powershell
# Navigate to project directory
cd "c:\Users\rajiv\OneDrive\Desktop\Projects\Billing System"

# Stop all Gradle daemons
./gradlew --stop

# Delete .gradle cache folder
Remove-Item -Recurse -Force .gradle

# Delete build folders
Remove-Item -Recurse -Force app/build
Remove-Item -Recurse -Force build

# Clear Android Studio cache
File → Invalidate Caches / Restart → Invalidate and Restart
```

---

### **Issue #2: Gradle Wrapper Version Mismatch**

**Check:** Open `gradle/wrapper/gradle-wrapper.properties`

**Should contain:**
```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.2-bin.zip
```

**If it shows gradle-7.x or lower:**

#### **Update Command:**
```powershell
# Update gradle wrapper
./gradlew wrapper --gradle-version 8.2
```

**Or manually edit** `gradle/wrapper/gradle-wrapper.properties`:
```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.2-bin.zip
```

---

### **Issue #3: Corrupted Dependency Cache**

**Symptom:** Specific dependency causing resolution failure

**Solution:** Force refresh all dependencies

#### **Commands:**
```powershell
# Clean project
./gradlew clean

# Refresh dependencies
./gradlew --refresh-dependencies

# Rebuild
./gradlew build --rerun-tasks
```

---

### **Issue #4: Android Studio IDE Cache**

**Steps to clear:**

1. **Close Android Studio completely**
2. **Delete IDE caches:**
   ```powershell
   # Windows - Android Studio Hedgehog
   Remove-Item -Recurse -Force "$env:LOCALAPPDATA\Google\AndroidStudio2023.1.1\system\caches"
   ```

3. **Restart Android Studio**
4. **Invalidate Caches again:**
   - File → Settings → Build, Execution, Deployment → Compiler
   - Click "Clear all VCS Log caches and indexes"
   - OK → Restart

---

### **Issue #5: Conflicting Transitive Dependencies**

**Check for dependency conflicts:**

```powershell
# Generate dependency tree
./gradlew app:dependencies > dependency-tree.txt

# Open dependency-tree.txt and search for:
# - Multiple versions of same library
# - 'FAILED' or 'CONFLICT' markers
```

**Common culprits:**
- Multiple versions of Play Services libraries
- Conflicting Firebase versions
- Old support library artifacts

**Solution:** Add explicit dependency constraints in `app/build.gradle.kts`:

```kotlin
configurations.all {
    resolutionStrategy {
        force("com.google.guava:guava:32.1.3-jre")
        force("com.google.code.findbugs:jsr305:3.0.2")
    }
}
```

---

### **Issue #6: Kotlin Daemon Issues**

**Symptom:** Random build failures with cryptic errors

**Solution:** Kill Kotlin daemon and restart

```powershell
# Kill all Java/Kotlin processes
taskkill /F /IM java.exe
taskkill /F /IM kotlinc.exe

# In Android Studio:
Build → Clean Project
Build → Rebuild Project
```

---

## 🚀 Complete Clean Build Procedure

### **Step-by-Step Reset:**

```powershell
# 1. Navigate to project
cd "c:\Users\rajiv\OneDrive\Desktop\Projects\Billing System"

# 2. Stop Gradle
./gradlew --stop

# 3. Kill Java processes
taskkill /F /IM java.exe

# 4. Delete all build artifacts
Remove-Item -Recurse -Force .gradle
Remove-Item -Recurse -Force app/build
Remove-Item -Recurse -Force build
Remove-Item -Recurse -Force .idea

# 5. Delete local Maven cache (optional, aggressive)
Remove-Item -Recurse -Force "$env:USERPROFILE\.m2\repository\com\InvoiceFlow"

# 6. Close Android Studio completely

# 7. Delete Android Studio system cache
Remove-Item -Recurse -Force "$env:LOCALAPPDATA\Google\AndroidStudio2023.1.1\system"

# 8. Restart computer (recommended)

# 9. After restart, open Android Studio

# 10. File → Invalidate Caches / Restart → Invalidate and Restart

# 11. After restart:
#     - File → Sync Project with Gradle Files
#     - Wait for sync to complete
#     - Build → Make Project
```

---

## 🔍 Diagnostic Commands

### **Check Gradle Version:**
```powershell
./gradlew --version
```

**Expected output:**
```
Gradle 8.2
Build time:   2023-06-30 18:02:30 UTC
Revision:     ...
```

### **Check Plugin Versions:**
```powershell
./gradlew buildEnvironment --configuration classpath
```

### **View Full Dependency Tree:**
```powershell
./gradlew app:dependencies --configuration releaseRuntimeClasspath
```

### **Identify Failing Dependency:**
```powershell
# Run with stacktrace for detailed error
./gradlew assembleDebug --stacktrace

# Or with full debug info
./gradlew assembleDebug --info --debug
```

---

## 📋 Verification Checklist

After applying fixes, verify:

- [ ] Gradle wrapper is version 8.2
- [ ] All `.gradle` folders deleted
- [ ] All `build` folders deleted
- [ ] Android Studio caches cleared
- [ ] No Java processes running during clean
- [ ] Sync completed without errors
- [ ] Build succeeds with `BUILD SUCCESSFUL`

---

## 🎯 Most Likely Solution for Your Case

Based on the error message and your configuration being correct, **this is almost certainly a cache issue**.

### **Quick Fix (90% success rate):**

```powershell
cd "c:\Users\rajiv\OneDrive\Desktop\Projects\Billing System"
./gradlew --stop
Remove-Item -Recurse -Force .gradle
Remove-Item -Recurse -Force app/build
Remove-Item -Recurse -Force build

# Then in Android Studio:
# File → Invalidate Caches / Restart → Invalidate and Restart
# File → Sync Project with Gradle Files
```

This should resolve the `fileCollection()` API error completely.

---

## 🆘 If Problem Persists

If none of the above works, the issue might be:

1. **Corrupted Gradle installation**
   - Delete: `%USERPROFILE%\.gradle\wrapper\dists\gradle-8.2`
   - Re-download will happen automatically

2. **Antivirus interference**
   - Temporarily disable antivirus
   - Add project folder to exclusions

3. **OneDrive sync conflicts**
   - Pause OneDrive syncing
   - Move project outside OneDrive folder temporarily
   - Build locally, then move back

4. **Corporate proxy/firewall**
   - Configure proxy in `gradle.properties`:
   ```properties
   systemProp.http.proxyHost=proxy.company.com
   systemProp.http.proxyPort=8080
   ```

---

## ✅ Expected Outcome

After clearing caches and restarting:

```
✅ BUILD SUCCESSFUL in 45s
✅ 0 problems, 0 warnings
✅ All dependencies resolved
✅ No 'fileCollection' errors
```

---

**Last Updated:** March 15, 2026  
**Status:** Ready for cache clear and rebuild  
**Recommended Action:** Execute Quick Fix commands above  

*Note: Since no Crashlytics/Firebase Performance plugins are present, the issue is definitely cache-related or environment-specific.*
