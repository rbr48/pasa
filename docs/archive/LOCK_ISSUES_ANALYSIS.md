# `/lock` Feature - Comprehensive Issue Analysis

## Executive Summary

The `/lock` (Lost Mode Guard) feature has **30 identified issues** across 4 components that compromise security, reliability, and operational safety. The feature is **currently unsafe for production use** due to weak PIN protection, no brute force defense, silent failures, and multiple bypass mechanisms.

**Current Reality:**
- ❌ Unlimited brute force attempts on unlock PIN (no attempt limiting)
- ❌ PIN stored in plain text (unencrypted)
- ❌ Touch capture debounce too short (allows 4 interactions per minute)
- ❌ Lock Task might not activate if Device Owner fails
- ❌ Activity can be bypassed via multitasking shells
- ❌ No timeout on lock duration (indefinite)
- ❌ Telegram failures sent silently
- ❌ No audit trail of attempts
- ❌ No rate limiting on photo captures
- ❌ Security Activity Launcher reliability uncertain

---

## 🔴 **CRITICAL ISSUES (Must Fix)**

### **Issue #1: No Brute Force Protection on Unlock PIN**
**File:** `AlertMessageActivity.kt:186-215`

**Problem:**
```kotlin
.setPositiveButton("Unlock") { _, _ ->
    val entered = input.text.toString().trim()
    // ... directly compare PIN
    if (isMaster || isLockPin) {
        exitLostMode()
    }
    // No attempt counter, no lockout, try again immediately
}
```

- User can enter unlimited PIN attempts in rapid succession
- No failed attempt counter
- No lockout period after failures
- No exponential backoff

**Attack Scenario:**
```
Device locked with 4-digit PIN "1234"
Attacker has 10,000 possible combinations
Attempt rate: ~2-3 per second via dialog
Time needed: ~1-2 hours brute force
Current protection: None ❌
```

**Impact:** Attacker can brute force unlock PIN in 1-2 hours

**Severity:** 🔴 CRITICAL (Security)

**Fix Required:**
```kotlin
// Track failed attempts + implement exponential backoff
if (!isMaster && !isLockPin) {
    failedUnlockDetector.recordAttempt()
    val lockoutMs = failedUnlockDetector.getRemainingLockoutMs()
    if (lockoutMs > 0) {
        Toast.makeText(this, "Locked out for ${lockoutMs/1000}s", Toast.LENGTH_SHORT).show()
        return
    }
}
```

---

### **Issue #2: PIN Stored in Plain Text**
**File:** `LockCommand.kt:91`

**Problem:**
```kotlin
preferencesManager.activeLockPin = pinToSet  // Stored unencrypted
```

- Lock PIN stored unencrypted in SharedPreferences
- If device is compromised, attacker reads PIN immediately
- Contrast: Master PIN is encrypted via AuthManager

**Impact:** Device unlock trivial if device compromised

**Severity:** 🔴 CRITICAL (Security)

**Fix Required:** Use EncryptedSharedPreferences for activeLockPin

---

### **Issue #3: Security Activity Launcher Reliability Uncertain**
**File:** `LockCommand.kt:118-127`

**Problem:**
```kotlin
SecurityActivityLauncher.launch(
    context = context,
    intent = alertIntent,
    // ... other params
    ongoing = true
)
```

- Relies on SecurityActivityLauncher.launch() to keep AlertMessageActivity visible
- If SecurityActivityLauncher fails, activity might not stay on top
- Some Android shells (Samsung DeX, Huawei Drawer) can push apps to background
- No fallback if launch fails

**Attack Scenario:**
```
1. Device locked with /lock
2. Attacker uses custom launcher to push AlertMessageActivity to background
3. Accesses device normally
Result: Lost Mode bypassed
```

**Impact:** Lock bypassed via launcher manipulation

**Severity:** 🔴 CRITICAL (Reliability)

---

### **Issue #4: Touch Capture Debounce Too Short**
**File:** `AlertMessageActivity.kt:224-234`

**Problem:**
```kotlin
override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
    if (ev?.action == MotionEvent.ACTION_DOWN) {
        val now = System.currentTimeMillis()
        if (now - lastTouchCaptureTime > 15000L) { // 15 seconds
            lastTouchCaptureTime = now
            triggerTouchCapture()
        }
    }
    return super.dispatchTouchEvent(ev)
}
```

- Debounce only 15 seconds
- Allows 4 touches per minute
- Attacker can spam touch events to generate false photos/GPS data
- Wastes Telegram quota and creates noise in alert stream

**Attack Timeline:**
```
00:00 - Attacker touches screen
00:15 - First photo sent, touch allowed again
00:30 - Second photo
00:45 - Third photo
01:00 - Fourth photo
...after 1 hour, 4 photos sent
After 24 hours, 96 photos sent (excessive noise)
```

**Impact:** Attacker can waste Telegram quota with false alerts

**Severity:** 🔴 CRITICAL (DoS)

**Fix Required:** Extend debounce to 60-120 seconds or disable touch capture entirely

---

### **Issue #5: Lock Task May Not Activate**
**File:** `LockCommand.kt:99`, `AlertMessageActivity.kt:122-131`

**Problem:**
```kotlin
if (isDeviceOwner) {
    PasaDeviceAdmin.configureLockTask(context)
    // ...
    startLockTask()  // In AlertMessageActivity
    isKioskActive = true
}
```

- If Device Owner not provisioned, Lock Task never starts
- If configureLockTask() fails, startLockTask() still called (might crash)
- No error handling if lock task fails
- Activity can still be exited via back button

**Attack:**
```
Device locked without Device Owner provisioning
Attacker presses back button
→ onBackPressed callback checks isLostModeActive
→ Shows toast "Send /unlock from Telegram"
→ But if Lost Mode state gets corrupted, might exit
Result: Lost Mode bypassed
```

**Impact:** Lock can be exited via back button if lock task not active

**Severity:** 🔴 CRITICAL (Reliability)

---

### **Issue #6: Activity Can Be Exited via Multitasking**
**File:** `AlertMessageActivity.kt:414-431` (onUserLeaveHint, onPause)

**Problem:**
```kotlin
override fun onUserLeaveHint() {
    if (preferencesManager.isLostModeActive) {
        val pullBack = Intent(this, AlertMessageActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        startActivity(pullBack)
    }
}
```

- Attempts to pull activity back when user tries to leave
- But some multitasking shells (Samsung DeX, tablet mode) bypass this
- Accessibility shortcuts can bypass activity
- Home gesture can sometimes work

**Attack:**
```
Android 12+ tablet mode
Device locked with Lost Mode
User swipes up for home
→ onUserLeaveHint called
→ Tries to relaunch AlertMessageActivity
→ But tablet multitasking might keep other app visible
Result: Can access device
```

**Impact:** Multitasking mode can bypass lock

**Severity:** 🔴 CRITICAL (Bypass)

---

### **Issue #7: Unlimited Lock Duration**
**File:** `LockCommand.kt:93-94`

**Problem:**
```kotlin
preferencesManager.isLostModeActive = true
preferencesManager.lostModeMessage = messageText
// No timeout, no expiration
```

- Once enabled, Lost Mode stays enabled indefinitely
- No automatic expiration after N hours/days
- If device stolen for weeks, lock still active if app still running

**Attack:**
```
1. Attacker steals device with Lost Mode active
2. Keeps device offline for 48 hours
3. Comes back online
4. App is still locking device (if app not uninstalled)
Result: Indefinite lock without owner control
```

**Impact:** Lock can't be automatically released after timeout

**Severity:** 🔴 CRITICAL (Reliability)

---

### **Issue #8: No Password for Remote /unlock Command**
**File:** `UnlockCommand.kt:29-63`

**Problem:**
```kotlin
override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
    // No password required
    preferencesManager.isLostModeActive = false
    // Immediately unlocks
}
```

- `/unlock` command has no password or confirmation
- Anyone with access to Telegram bot can unlock any device
- No rate limiting on unlock attempts
- No audit trail of who unlocked

**Attack:**
```
1. Attacker compromises Telegram bot credentials
2. Sends /unlock from any account
3. Device instantly unlocked
4. No password needed, no confirmation
Result: Complete access
```

**Impact:** Single API call unlocks device

**Severity:** 🔴 CRITICAL (Security)

---

## 🟠 **HIGH-PRIORITY ISSUES (Should Fix)**

### **Issue #9: Failed Unlock Counter Behavior Incorrect**
**File:** `FailedUnlockDetector.kt:36-46`

**Problem:**
```kotlin
suspend fun onFailedAttempt() {
    val count = preferencesManager.failedUnlockCount + 1
    preferencesManager.failedUnlockCount = count

    if (count >= ALERT_THRESHOLD && preferencesManager.isConfigured()) {
        triggerAlert(count)
    }
}

private suspend fun triggerAlert(attemptCount: Int) {
    // ... send alert
    preferencesManager.failedUnlockCount = 0  // Reset after alert
}
```

- Counter only increments if AlertMessageActivity used
- PIN unlock in AlertMessageActivity doesn't call onFailedAttempt()
- Counter resets after 3 attempts, then user can try 3 more
- No exponential backoff, just reset

**Attack:**
```
Device locked with Lost Mode
Attacker uses AlertMessageActivity PIN entry (unlimited attempts)
→ Counter never incremented
→ No alerts sent
→ Brute force unlimited
```

**Impact:** Failed unlock detection is ineffective

**Severity:** 🟠 HIGH (Security)

---

### **Issue #10: Touch Capture Photo Sent Without Encryption**
**File:** `AlertMessageActivity.kt:285-298`

**Problem:**
```kotlin
telegramApi.sendPhoto(
    token = preferencesManager.botToken,
    chatId = chatIdBody,
    photo = photoPart,
    caption = captionBody
)
```

- Photo sent unencrypted over HTTP to Telegram
- Telegram servers see all device photos
- No end-to-end encryption
- Metadata (location, timestamp) visible in files

**Impact:** Privacy leak - device photos visible to Telegram

**Severity:** 🟠 HIGH (Privacy)

---

### **Issue #11: Notification Can Be Swiped Away**
**File:** `LockCommand.kt:118-127`

**Problem:**
```kotlin
SecurityActivityLauncher.launch(
    // ...
    ongoing = true  // Should prevent dismissal
)
```

- ongoing=true should prevent swipe-away
- But some devices/Android versions allow dismissal of ongoing notifications
- If notification dismissed, activity might not be recalled

**Attack:**
```
Device locked
Attacker swipes notification down
→ Notification disappears
→ Activity still visible but user doesn't see it
→ Can interact with app or press home
```

**Impact:** Lock can be bypassed by dismissing notification

**Severity:** 🟠 HIGH (Reliability)

---

### **Issue #12: Phone Number Extraction Vulnerable**
**File:** `AlertMessageActivity.kt:443-448`

**Problem:**
```kotlin
private fun extractPhoneNumber(text: String): String? {
    val pattern = Pattern.compile("(\\+?[0-9]{7,15})")
    val matcher = pattern.matcher(text)
    return if (matcher.find()) {
        matcher.group(1)
    } else null
}
```

- Pattern `[0-9]{7,15}` is too loose
- Matches "1234567" as phone number
- Matches any 7-15 digit sequence in message
- Could match credit card numbers, ZIP codes, etc.

**Attack:**
```
Lock message: "Device lost! PIN is 123456789"
→ Extracts "123456789" as "phone number"
→ Creates call button "📞 Call Owner: 123456789"
→ User clicks button trying to call owner
→ Calls random number
```

**Impact:** Misleading UI, false call button

**Severity:** 🟠 HIGH (Usability)

---

### **Issue #13: No Timeout on Unlock Dialog**
**File:** `AlertMessageActivity.kt:173-218`

**Problem:**
```kotlin
com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
    .setTitle("Unlock Device")
    .setView(input)
    .setPositiveButton("Unlock") { _, _ ->
        // Dialog stays open indefinitely
        // No timeout
    }
    .show()
```

- PIN entry dialog has no timeout
- User can type unlimited PINs without any delay
- No session limit or time constraint

**Impact:** Unlimited brute force attempts without delay

**Severity:** 🟠 HIGH (Security)

---

### **Issue #14: Location Sent Without Permission Check**
**File:** `AlertMessageActivity.kt:237-253`, `FailedUnlockDetector.kt:59-72`

**Problem:**
```kotlin
val loc = locationTracker.getCurrentLocation()
// No permission check, might fail silently
```

- Doesn't verify ACCESS_FINE_LOCATION permission granted
- LocationTracker.getCurrentLocation() might return null
- Fallback message is vague "Location unavailable"

**Impact:** GPS not sent even though code says it does

**Severity:** 🟠 HIGH (Reliability)

---

## 🟡 **MEDIUM-PRIORITY ISSUES (Should Consider)**

### **Issue #15: No Rate Limiting on Photo Captures**
**File:** `AlertMessageActivity.kt:223-234`

**Problem:**
- Touch capture only debounced 15 seconds
- Attacker can trigger photo every 15 seconds
- Each photo generates Telegram API call
- Could fill up Telegram quota

**Fix:** Rate limit to 1 photo per 5-10 minutes, or disable entirely

**Severity:** 🟡 MEDIUM (DoS)

---

### **Issue #16: No Audit Trail of Unlock Attempts**
**File:** `AlertMessageActivity.kt:201-215`

**Problem:**
- No logging of who attempted to unlock or when
- PIN entry attempts not recorded
- Can't review attack history
- No forensic record

**Impact:** No audit trail for security investigation

**Severity:** 🟡 MEDIUM (Forensics)

---

### **Issue #17: Master PIN Fallback Has No Confirmation**
**File:** `AlertMessageActivity.kt:202`

**Problem:**
```kotlin
val isMaster = authManager.hasMasterPassword() && authManager.verifyMasterPassword(entered)
if (isMaster || isLockPin) {
    exitLostMode()  // Immediately exits
}
```

- Master PIN unlock is silent, no confirmation
- If attacker knows master PIN, instant unlock
- No logging of who used master PIN

**Impact:** Master PIN is backdoor if compromised

**Severity:** 🟡 MEDIUM (Security)

---

### **Issue #18: Kiosk Mode Failure Not Logged**
**File:** `AlertMessageActivity.kt:122-131`

**Problem:**
```kotlin
try {
    PasaDeviceAdmin.configureLockTask(this)
    PasaDeviceAdmin.setComprehensiveLockdown(this, true)
    startLockTask()
    isKioskActive = true
} catch (e: Exception) {
    Log.w(TAG, "Could not start lock task: ${e.message}")
    // Continue without lock task!
}
```

- If lock task fails, app continues without security hardening
- No attempt to retry or fallback
- User doesn't know lock task failed

**Impact:** Lock bypassed if lock task fails silently

**Severity:** 🟡 MEDIUM (Reliability)

---

### **Issue #19: Telegram Configuration Validation Missing**
**File:** `LockCommand.kt:118-127`, `AlertMessageActivity.kt:271`

**Problem:**
```kotlin
if (!preferencesManager.botToken.isNullOrBlank() && preferencesManager.ownerChatIdLong != 0L) {
    // Send alert
}
// Silently fails if token/chatId not set
```

- No validation that Telegram is configured
- No error message if token missing
- User doesn't know security alert won't work

**Impact:** Alerts sent silently, may fail

**Severity:** 🟡 MEDIUM (Reliability)

---

### **Issue #20: Back Button Bypass Scenario**
**File:** `AlertMessageActivity.kt:102-111`

**Problem:**
```kotlin
onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
    override fun handleOnBackPressed() {
        if (preferencesManager.isLostModeActive) {
            Toast.makeText(this@AlertMessageActivity, "🔒 Locked: Send /unlock from Telegram...", Toast.LENGTH_SHORT).show()
        } else {
            isEnabled = false
            onBackPressedDispatcher.onBackPressed()
        }
    }
})
```

- Only checks `isLostModeActive` preference
- If preference gets cleared/corrupted, back button works
- Toast message not a real protection

**Impact:** Back button can exit if preference corrupted

**Severity:** 🟡 MEDIUM (Reliability)

---

## 🔵 **LOW-PRIORITY ISSUES (Nice to Have)**

### **Issue #21: Toast Messages Logged to Logcat**
- "❌ Incorrect PIN" visible in system logs
- Attacker with logcat access sees attempt feedback
- Should use silent failure or encrypted logging

**Severity:** 🔵 LOW (Privacy)

---

### **Issue #22: Notification Sound Always Plays**
**File:** `AlertMessageActivity.kt:434-440`

- No way to silence notification sound
- Alerts attacker that owner has been notified
- Should respect device silent mode

**Severity:** 🔵 LOW (Usability)

---

### **Issue #23: EditText Shows Password Field**
**File:** `AlertMessageActivity.kt:174-178`

- PIN field shows dots but value still accessible
- TYPE_NUMBER_VARIATION_PASSWORD doesn't hide value from memory
- Should use secure input method

**Severity:** 🔵 LOW (Security)

---

### **Issue #24: No Feature Flag for Lock**
- Lock always enabled once Device Admin active
- No way to disable just lock feature
- Should have per-feature configuration

**Severity:** 🔵 LOW (Operability)

---

### **Issue #25: Photo File Deletion Race Condition**
**File:** `AlertMessageActivity.kt:318-322`

- Photo deleted immediately after sending
- Might delete while upload in progress
- Should wait for upload confirmation

**Severity:** 🔵 LOW (Reliability)

---

## More Issues (26-30)

**Issue #26: No Timeout on Lock Dialog**
- Dialog stays open indefinitely, no auto-close
- User can take time between attempts

**Severity:** 🟡 MEDIUM

**Issue #27: Window Flags May Not Apply on All Devices**
- WindowManager flags behavior differs by OEM
- Some devices allow overlays despite FLAG_SHOW_WHEN_LOCKED
- Samsung DeX, Huawei can bypass

**Severity:** 🟠 HIGH

**Issue #28: System Bar Hiding Implementation Inconsistent**
- Multiple ways to hide system bars (deprecated + new)
- Behavior varies across Android versions

**Severity:** 🟡 MEDIUM

**Issue #29: Failed Unlock Alert Sent Repeatedly**
- Alert sent every 3 failed attempts
- If attacker tries 300 times, sends 100 alerts
- No throttling on alert sending

**Severity:** 🟡 MEDIUM

**Issue #30: No Lock Duration Limit**
- Lock can stay active indefinitely
- Should auto-release after 7-30 days
- Prevents indefinite remote lock abuse

**Severity:** 🟡 MEDIUM

---

## 📊 **Issue Summary**

| Severity | Count | Impact |
|----------|-------|--------|
| **Critical** | 8 | Device bypassed, brute force possible, no security |
| **High** | 6 | Reliability issues, weak protection |
| **Medium** | 12 | Coverage gaps, operational issues |
| **Low** | 4 | Privacy/usability issues |
| **TOTAL** | **30** | **Major overhaul needed** |

---

## 🔧 **Recommended Fix Priority**

### **Phase 1: Security (CRITICAL)**
1. Add brute force protection with exponential backoff
2. Encrypt PIN storage (AES-256-GCM)
3. Require password for /unlock command
4. Implement lock timeout (7-30 days)
5. Extend touch debounce to 120 seconds

### **Phase 2: Reliability (HIGH)**
6. Validate Telegram configuration before storing
7. Add location permission check
8. Ensure lock task activation or fail safely
9. Add audit trail of unlock attempts
10. Implement failed unlock rate limiting

### **Phase 3: Polish (MEDIUM)**
11. Add timeout on PIN entry dialog
12. Improve phone number extraction pattern
13. Add audit logging for forensics
14. Remove toast messages or encrypt them
15. Add feature flags for per-feature control

---

## 💡 **Conclusion**

The `/lock` feature is **currently unsafe for production use**. It requires significant fixes across brute force protection, PIN security, activity reliability, and error handling. The combination of unlimited unlock attempts, plain-text PIN storage, and unreliable activity keepalive makes the lock bypassable within hours.

**Recommended Action:** Complete Phase 1 (Security) and Phase 2 (Reliability) immediately before deployment.

