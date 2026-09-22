# `/screenrecord` Feature - Comprehensive Issue Analysis

## Executive Summary

The `/screenrecord` feature has **15 critical/high-priority issues** across 3 components that severely limit video quality, cause potential crashes, waste memory, and create garbage files.

**Current Reality:**
- ❌ Videos are extremely low quality (2 FPS, 4-40 frames)
- ❌ Resolution doesn't match device (hardcoded 720x1280)
- ❌ Memory leaks and OOM crashes possible
- ❌ Bitrate mismatches (8Mbps command vs 2Mbps encoder)
- ❌ No error recovery or cleanup
- ❌ Blocks on encoding (can timeout)

---

## 🔴 **CRITICAL ISSUES (Must Fix)**

### **Issue #1: Extremely Low Video Quality (2 FPS)**
**File:** `ScreenRecordCommand.kt:104`
```kotlin
val fps = 2 // 2 frames per second
```

**Problem:**
- 2 FPS is unwatchable - results in choppy, slideshow-like video
- For 15-second recording: only 30 frames captured
- For 60-second recording: only 120 frames captured
- Professional minimum is 24 FPS; standard is 30-60 FPS

**Impact:** Video is completely unusable for identifying intruders or actions
**Severity:** 🔴 CRITICAL

**Fix Required:**
```kotlin
val fps = 15  // Minimum 15 FPS for watchable video
```

---

### **Issue #2: Frame Count Severely Limited**
**File:** `ScreenRecordCommand.kt:105`
```kotlin
val totalFramesTarget = (durationSeconds * fps).coerceIn(4, 40)
```

**Problem:**
- Only 4-40 frames regardless of duration
- 60-second recording gets max 40 frames = 0.66 FPS effective!
- The `coerceIn(4, 40)` cap makes longer recordings worse
- Math: 60 seconds * 2fps = 120, but coerced down to max 40

**Calculation Matrix:**
| Duration | Expected Frames | Actual Frames | Effective FPS |
|----------|-----------------|---------------|---------------|
| 5 sec    | 10              | 10            | 2.0           |
| 15 sec   | 30              | 30            | 2.0           |
| 30 sec   | 60              | 40            | 1.3           |
| 60 sec   | 120             | 40            | 0.66          |

**Impact:** Longer recordings become worse quality
**Severity:** 🔴 CRITICAL

**Fix Required:**
```kotlin
val totalFramesTarget = (durationSeconds * fps)  // Remove artificial cap
```

---

### **Issue #3: Hardcoded Screen Resolution (720x1280)**
**File:** `ScreenRecordCommand.kt:171`
```kotlin
"screenrecord --size 720x1280 --bit-rate ${BITRATE_KBPS * 1000} " +
```

**Problem:**
- Device might be 1080x2400, 1440x3120, or 360x800
- Videos always stretched/squashed or have black bars
- Ignores device's actual DisplayMetrics

**Device Reality Check:**
```kotlin
val displayMetrics = context.resources.displayMetrics
val width = displayMetrics.widthPixels  // Could be anything
val height = displayMetrics.heightPixels // Could be anything
```

**Impact:** All videos are distorted or have wrong aspect ratio
**Severity:** 🔴 CRITICAL

**Fix Required:**
```kotlin
val displayMetrics = context.resources.displayMetrics
val screenWidth = displayMetrics.widthPixels
val screenHeight = displayMetrics.heightPixels
val command = "screenrecord --size ${screenWidth}x${screenHeight} ..."
```

---

### **Issue #4: Bitrate Mismatch (8 Mbps vs 2 Mbps)**
**File:** `ScreenRecordCommand.kt:170` vs `ScreenVideoEncoder.kt:40`
```kotlin
// Command line: 8Mbps
"--bit-rate ${BITRATE_KBPS * 1000}"  // 8000 * 1000 = 8,000,000 bps = 8 Mbps

// Encoder: 2Mbps
setInteger(MediaFormat.KEY_BIT_RATE, 2_000_000) // 2 Mbps
```

**Problem:**
- Hardware encoder told to use 8 Mbps
- Software encoder told to use 2 Mbps
- Accessibility engine (software) always outputs 2 Mbps videos
- File size varies wildly (expected 8Mbps, gets 2Mbps)

**Impact:** 
- Compression artifacts at 2Mbps
- File size is 4x smaller than expected
- Quality degradation

**Severity:** 🔴 CRITICAL

**Fix Required:**
```kotlin
// Align both to same bitrate
companion object {
    private const val BITRATE_KBPS = 4000  // 4 Mbps = good balance
}
// Update encoder:
setInteger(MediaFormat.KEY_BIT_RATE, 4_000_000) // Match command
```

---

### **Issue #5: Memory Leak - Unbounded Bitmap Storage**
**File:** `ScreenRecordCommand.kt:103-162`
```kotlin
val frames = mutableListOf<Bitmap>()  // No size limit!

while (System.currentTimeMillis() < endTime && processedCount < totalFramesTarget) {
    val batch = mutableListOf<Bitmap>()
    repeat(batchSize.coerceAtMost(...)) {
        val bmp = BitmapFactory.decodeFile(shotFile.absolutePath)
        if (bmp != null) batch.add(bmp)  // Add raw bitmap
    }
    frames.addAll(batch)  // All stored in memory
}
```

**Problem:**
- Each bitmap for 1080x2400 screen = ~10 MB (ARGB_8888)
- Storing 40 frames = 400 MB RAM used
- No cleanup on memory pressure
- Exception in encoding = bitmaps never recycled

**Worst Case OOM Scenario:**
```
Device RAM: 4 GB
PASA overhead: 500 MB
Available: 3.5 GB

40 frames × 10 MB/frame = 400 MB stored
Peak during encoding: 400 MB + 100 MB working = 500 MB
Encoding working set: 200 MB
TOTAL: ~700 MB → OOM possible on lower-end devices
```

**Impact:** App crash on mid-range devices during screen record
**Severity:** 🔴 CRITICAL

**Fix Required:**
```kotlin
// Encode frames immediately instead of storing all in memory
for (i in 0 until totalFramesTarget) {
    val bmp = screenshotManager.captureScreenshot()
    if (bmp != null) {
        encodeFrameToMp4(bmp, encoder, ...)
        bmp.recycle()  // Free immediately
    }
}
```

---

### **Issue #6: No Disk Space Validation**
**File:** `ScreenRecordCommand.kt:48`
```kotlin
val outputFile = File(context.cacheDir, "screenrecord_${System.currentTimeMillis()}.mp4")
// No check: Do we have space?
```

**Problem:**
- For 60-second 1080p: ~300-500 MB file needed
- Writing to full disk = corrupted partial file
- No cleanup of partial file
- cacheDir might be on shared storage with low space

**Failure Mode:**
```
1. Request /screenrecord 60
2. Device has 200 MB free, needs 500 MB
3. Starts writing...
4. Disk full after 100 MB
5. Partial corrupt MP4 left behind
6. User sees "failed" but file exists
```

**Severity:** 🔴 CRITICAL

**Fix Required:**
```kotlin
// Check available space before recording
val statFs = StatFs(context.cacheDir.absolutePath)
val availableBytes = statFs.availableBytes
val estimatedBytes = durationSeconds * 8_000_000 / 8  // ~1MB/sec at 8Mbps
if (availableBytes < estimatedBytes) {
    return CommandResult(false, "❌ Not enough storage: need ${estimatedBytes/1MB}MB, have ${availableBytes/1MB}MB")
}
```

---

### **Issue #7: Cache Directory Deletion**
**File:** `ScreenRecordCommand.kt:48`
```kotlin
val outputFile = File(context.cacheDir, "screenrecord_...")
```

**Problem:**
- Android can clear cacheDir without warning
- Device storage pressure triggers auto-cleanup
- User tries to retrieve recorded video - file gone

**Severity:** 🔴 CRITICAL

**Fix Required:**
```kotlin
// Use getFilesDir() instead - permanent storage
val outputFile = File(context.filesDir, "recordings/screenrecord_${System.currentTimeMillis()}.mp4")
```

---

## 🟠 **HIGH-PRIORITY ISSUES (Should Fix)**

### **Issue #8: Synchronous Encoding Can Timeout**
**File:** `ScreenRecordCommand.kt:103-162`
```kotlin
val encoded = ScreenVideoEncoder.encodeBitmapsToMp4(frames, outputFile, fps = fps)
// This blocks the entire command execution!
```

**Problem:**
- Encoding 40 frames can take 5-10 seconds
- Command execution timeout might occur
- No progress feedback
- User thinks it failed when it's still running

**Severity:** 🟠 HIGH

**Fix Required:**
```kotlin
// Run encoding in background thread
CoroutineScope(Dispatchers.Default).launch {
    encodeFramesAsync(frames, outputFile)
}
return CommandResult(true, "Recording... encoding in progress. Video will be ready in ~30s")
```

---

### **Issue #9: UID Root Check (Security Risk Pattern)**
**File:** `ScreenRecordCommand.kt:55`
```kotlin
if (isOwner || android.os.Process.myUid() == 2000 || android.os.Process.myUid() == 0) {
```

**Problem:**
- Checking for UID == 0 (root) is bad pattern
- Should never rely on process UID as security measure
- Open to spoofing on rooted devices
- Creates false sense of security

**Severity:** 🟠 HIGH (Security)

**Fix Required:**
```kotlin
// Remove UID checks, only rely on DeviceOwner
if (isDeviceOwner()) {
    // Try hardware screenrecord
}
```

---

### **Issue #10: No Garbage Cleanup on Failure**
**File:** `ScreenRecordCommand.kt:48, 98-100`
```kotlin
val outputFile = File(context.cacheDir, "screenrecord_...")
// If command fails at line 99:
return CommandResult(false, message)
// outputFile is never deleted!
```

**Problem:**
- Failed recordings leave garbage files
- After 10 failed recordings: 1+ GB wasted
- No automatic cleanup mechanism

**Impact:** Disk space gradually fills up
**Severity:** 🟠 HIGH

**Fix Required:**
```kotlin
if (!success) {
    outputFile.delete()  // Clean up before returning
    return CommandResult(false, message)
}
```

---

### **Issue #11: No Screenshot Rate Limiting**
**File:** `ScreenRecordCommand.kt:117-129`
```kotlin
repeat(batchSize.coerceAtMost(...)) {
    val shotFile = screenshotManager.captureScreenshot()
    ...
    delay(intervalMs)  // Only 500ms between captures at 2fps
}
```

**Problem:**
- Taking screenshots rapidly causes AccessibilityService lag
- System can't keep up
- Screenshots come out black/blank
- No backpressure handling

**Severity:** 🟠 HIGH

**Fix Required:**
```kotlin
// Add adaptive rate limiting
if (captureFailures > 3) {
    delay(1000)  // Back off if service is lagging
}
```

---

### **Issue #12: Service Instance Polling (CPU Waste)**
**File:** `ScreenshotManager.kt:53-59`
```kotlin
var a11yService = AccessibilityScreenCaptureService.instance
var attempts = 0
while (a11yService == null && attempts < 10) {
    kotlinx.coroutines.delay(200L)  // Poll every 200ms
    a11yService = AccessibilityScreenCaptureService.instance
    attempts++
}
```

**Problem:**
- Polling is wasteful - wakes up every 200ms
- Should use callback/listener instead
- Adds 2000ms latency in worst case (10 attempts × 200ms)
- Unnecessary CPU usage

**Severity:** 🟠 HIGH

**Fix Required:**
```kotlin
// Use callback instead of polling
val a11yService = AccessibilityScreenCaptureService.getInstance(timeout = 5000L)
```

---

### **Issue #13: Hardcoded Color Format**
**File:** `ScreenVideoEncoder.kt:39`
```kotlin
setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar)
```

**Problem:**
- Not all devices support NV12 (YUV420SemiPlanar)
- Different chipsets support different formats
- No fallback if format not supported
- Could cause encoder creation to fail silently

**Severity:** 🟠 HIGH

**Fix Required:**
```kotlin
// Query supported formats instead
val codec = MediaCodec.createEncoderByType(MIME_TYPE)
val caps = codec.codecInfo.getCapabilitiesForType(MIME_TYPE)
val supportedFormats = caps.colorFormats
val selectedFormat = supportedFormats.firstOrNull() ?: COLOR_FormatYUV420SemiPlanar
```

---

### **Issue #14: No Automatic Screenshot Cleanup**
**File:** `ScreenshotManager.kt:174-186`
```kotlin
fun cleanupOldScreenshots() {
    // Method exists but is never called!
}
```

**Problem:**
- cleanup() method defined but never invoked
- Old screenshots accumulate
- Can fill up storage over time
- No automatic housekeeping

**Impact:** Storage slowly fills with old recordings
**Severity:** 🟠 HIGH

**Fix Required:**
```kotlin
// Call cleanup on app startup
override fun onCreate() {
    super.onCreate()
    screenshotManager.cleanupOldScreenshots()  // Add this
}
```

---

### **Issue #15: No OOM Protection**
**File:** `ScreenRecordCommand.kt:103-162`
```kotlin
// If OOM occurs:
catch (e: Throwable) {
    Log.e(TAG, "Error capturing frames (possibly OOM)", e)
    // continues without cleanup!
}
```

**Problem:**
- No graceful degradation on OOM
- Bitmaps might not be recycled
- App state left inconsistent
- User doesn't know what happened

**Severity:** 🟠 HIGH

**Fix Required:**
```kotlin
catch (e: OutOfMemoryError) {
    frames.forEach { it.recycle() }
    return CommandResult(false, "❌ Out of memory: device doesn't have enough RAM for video recording")
}
```

---

## 🟡 **MEDIUM-PRIORITY ISSUES (Should Consider)**

### **Issue #16: Bitrate Too Low (2 Mbps)**
**File:** `ScreenVideoEncoder.kt:40`
```kotlin
setInteger(MediaFormat.KEY_BIT_RATE, 2_000_000) // 2 Mbps
```

**Problem:**
- 2 Mbps for 1080p is very compressed
- Visible artifacts and blockiness
- Text becomes hard to read
- 4 Mbps would be much better

**Severity:** 🟡 MEDIUM

---

### **Issue #17: Fixed I-Frame Interval**
**File:** `ScreenVideoEncoder.kt:42`
```kotlin
setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)  // Every frame is keyframe!
```

**Problem:**
- I-frame every 1 second creates MASSIVE files
- Should be 3-5 seconds for reasonable compression
- At 15 FPS: that's 15 keyframes/sec = inefficient

**Severity:** 🟡 MEDIUM

---

## 📊 **Issue Summary**

| Category | Count | Impact |
|----------|-------|--------|
| **Critical** | 7 | App crashes, unusable videos, data loss |
| **High** | 8 | Unreliable operation, resource leaks |
| **Medium** | 2 | Poor quality, inefficient |
| **TOTAL** | **17** | **Major overhaul needed** |

---

## 🔧 **Recommended Fix Priority**

### **Phase 1: Stability (Do First)**
1. ✅ Add disk space check (Issue #6)
2. ✅ Add memory cleanup on failure (Issue #10)
3. ✅ Fix memory leak with bitmap storage (Issue #5)
4. ✅ Add OOM protection (Issue #15)
5. ✅ Use filesDir instead of cacheDir (Issue #7)

### **Phase 2: Quality (Do Second)**
6. ✅ Increase FPS from 2 to 15 (Issue #1)
7. ✅ Remove frame count cap (Issue #2)
8. ✅ Get actual device resolution (Issue #3)
9. ✅ Align bitrates (Issue #4)

### **Phase 3: Reliability (Do Third)**
10. ✅ Add service instance callback (Issue #12)
11. ✅ Add rate limiting (Issue #11)
12. ✅ Auto-call cleanup (Issue #14)
13. ✅ Fix color format detection (Issue #13)
14. ✅ Make encoding async (Issue #8)

### **Phase 4: Polish (Nice to Have)**
15. ✅ Remove UID root check (Issue #9)
16. ✅ Optimize I-frame interval (Issue #17)
17. ✅ Increase bitrate (Issue #16)

---

## 💡 **Example: Before vs After**

### **Before (Broken)**
```
/screenrecord 60
→ 2 FPS, 40 frames max
→ Hardcoded 720x1280
→ 8Mbps vs 2Mbps mismatch
→ 400MB RAM used (potential OOM)
→ No disk space check
→ File in cache (can be deleted)
→ Garbage files on failure
→ Video completely unwatchable
Result: ❌ Useless
```

### **After (Fixed)**
```
/screenrecord 60
→ 15 FPS, 900 frames
→ 1080x2400 (actual device)
→ 4Mbps consistent
→ Streaming encoding (50MB peak RAM)
→ Disk space validated
→ File in permanent storage
→ Auto-cleanup on failure
→ Clear, watchable video
Result: ✅ Functional
```

---

## 🚨 **Conclusion**

The `/screenrecord` feature is **currently broken and unsuitable for production use**. It requires significant fixes across 3 components to be reliable. The combination of low FPS, hardcoded resolution, memory leaks, and storage issues makes it unusable for its intended purpose of covert evidence collection.

**Recommended Action:** Complete Phase 1 (Stability) immediately before the next deployment.
