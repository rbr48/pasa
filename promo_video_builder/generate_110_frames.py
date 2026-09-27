# PASA Sentinel - 110 High-Velocity Motion Graphics & Image Frame Generator
# Produces 110 distinct 1920x1080 Full HD frames covering the entire mobile cyber defense arsenal.

import os
from PIL import Image, ImageDraw, ImageFont

OUT_DIR = r"E:\Projects\PrivateApp\promo_video_builder\output\frames"
ASSETS_DIR = r"E:\Projects\PrivateApp\promo_video_builder\assets"
os.makedirs(OUT_DIR, exist_ok=True)

W, H = 1920, 1080

# Fonts
font_giant = ImageFont.truetype("C:/Windows/Fonts/segoeuib.ttf", 72)
font_huge = ImageFont.truetype("C:/Windows/Fonts/segoeuib.ttf", 52)
font_large = ImageFont.truetype("C:/Windows/Fonts/segoeuib.ttf", 40)
font_title = ImageFont.truetype("C:/Windows/Fonts/segoeuib.ttf", 32)
font_sub = ImageFont.truetype("C:/Windows/Fonts/segoeui.ttf", 24)
font_mono = ImageFont.truetype("C:/Windows/Fonts/arialbd.ttf", 22)
font_mono_small = ImageFont.truetype("C:/Windows/Fonts/arialbd.ttf", 16)
font_badge = ImageFont.truetype("C:/Windows/Fonts/arialbd.ttf", 18)

# Palette
C_BG = (6, 10, 18)
C_DARK = (12, 18, 32)
C_CYAN = (6, 182, 212)
C_ROSE = (244, 63, 94)
C_EMERALD = (16, 185, 129)
C_AMBER = (245, 158, 11)
C_WHITE = (255, 255, 255)
C_GRAY = (148, 163, 184)
C_BORDER = (24, 38, 64)

# Load hero images if available
img_snatch = None
img_knox = None
img_cellebrite = None
try:
    img_snatch = Image.open(os.path.join(ASSETS_DIR, "snatch_intercept.jpg")).resize((W, H))
    img_knox = Image.open(os.path.join(ASSETS_DIR, "knox_fortress.jpg")).resize((W, H))
    img_cellebrite = Image.open(os.path.join(ASSETS_DIR, "cellebrite_blocked.jpg")).resize((W, H))
except Exception as e:
    print(f"Warning loading assets: {e}")

def create_base(bg_color=C_BG):
    img = Image.new("RGB", (W, H), bg_color)
    d = ImageDraw.Draw(img)
    # Subtle telemetry grid
    for x in range(0, W, 120):
        d.line([(x, 0), (x, H)], fill=(12, 18, 32), width=1)
    for y in range(0, H, 120):
        d.line([(y, 0), (W, y)], fill=(12, 18, 32), width=1)
    # Corner brackets
    d.line([(40, 40), (80, 40)], fill=C_CYAN, width=2)
    d.line([(40, 40), (40, 80)], fill=C_CYAN, width=2)
    d.line([(W - 40, 40), (W - 80, 40)], fill=C_CYAN, width=2)
    d.line([(W - 40, 40), (W - 40, 80)], fill=C_CYAN, width=2)
    d.line([(40, H - 40), (80, H - 40)], fill=C_CYAN, width=2)
    d.line([(40, H - 40), (40, H - 80)], fill=C_CYAN, width=2)
    d.line([(W - 40, H - 40), (W - 80, H - 40)], fill=C_CYAN, width=2)
    d.line([(W - 40, H - 40), (W - 40, H - 80)], fill=C_CYAN, width=2)
    return img, d

def draw_header(d, frame_idx, topic, badge_color):
    d.text((60, 45), "PASA SENTINEL // SOVEREIGN MOBILE DEFENSE", fill=C_CYAN, font=font_mono_small)
    d.text((W - 360, 45), f"FRAME #{frame_idx:03d} // ACTIVE HUD", fill=badge_color, font=font_mono_small)
    d.line([(60, 75), (W - 60, 75)], fill=C_BORDER, width=1)
    # Topic Badge
    d.rounded_rectangle([60, 95, 450, 135], radius=6, fill=(16, 24, 40), outline=badge_color, width=2)
    d.text((75, 105), topic, fill=badge_color, font=font_badge)

def save_frame(img, frame_idx):
    path = os.path.join(OUT_DIR, f"frame_{frame_idx:03d}.png")
    img.save(path)

print("Generating 110 high-velocity motion graphics frames...")

frame_idx = 1

# ═══════════════════════════════════════════════════════════════
# SECTION 1: THE THREAT MATRIX & THE 8-SECOND COLLAPSE (1 - 14)
# ═══════════════════════════════════════════════════════════════
threats = [
    ("GLOBAL MOBILE THEFT CRISIS", "OVER 10,000,000 PHONES STOLEN YEARLY", "Physical snatches, black-market resale, and instant device isolation.", C_ROSE),
    ("THE 8-SECOND WINDOW", "STANDARD TRACKERS GO BLIND IN 8 SECONDS", "Thieves act before victims can even borrow another phone.", C_ROSE),
    ("ATTACK VECTOR 01: STREET SNATCH", "2.65G HIGH-VELOCITY MOTORCYCLE SNATCH", "Thief grabs phone unlocked, accelerates, and isolates RF.", C_ROSE),
    ("ATTACK VECTOR 02: HARDWARE FORENSICS", "CELLEBRITE & GRAYKEY EXTRACTION BOXES", "Physical USB cables rip private chats, photos, and wallet keys.", C_ROSE),
    ("ATTACK VECTOR 03: BANKING TROJANS", "ACCESSIBILITY OVERLAY MALWARE", "Malicious apps inject invisible screens over banking logins.", C_ROSE),
    ("ATTACK VECTOR 04: IMSI CATCHERS", "ROGUE 2G BASE STATION (STINGRAY)", "Forces cellular downgrade to unencrypted 2G to intercept OTPs.", C_ROSE),
    ("ATTACK VECTOR 05: CRYPTO CLIPPERS", "SYSTEM CLIPBOARD HIJACKING", "Replaces copied Bitcoin, Ethereum, and Solana wallet addresses.", C_ROSE),
    ("ATTACK VECTOR 06: RANSOMWARE LOCKERS", "STORAGE ENCRYPTION THREATS", "Ransomware canary files silently encrypted before notice.", C_ROSE),
    ("ATTACK VECTOR 07: PHYSICAL SIM EJECT", "SIM TRAY COMPROMISE & OTP SWAP", "Thief moves SIM to burner phone to steal 2FA accounts.", C_ROSE),
    ("FAILURE POINT: QUICK SETTINGS PULLDOWN", "AIRPLANE MODE TOGGLED IN 2 SECONDS", "Android allows notification shade pulldown by default.", C_ROSE),
    ("FAILURE POINT: FACTORY RESET", "HARD REBOOT INTO RECOVERY MENU", "Standard devices wipe evidence and lock permanently.", C_ROSE),
    ("FAILURE POINT: GOOGLE FIND MY DEVICE", "OFFLINE IMMEDIATELY WHEN DATA SEVERED", "Zero control once Wi-Fi and mobile data are switched off.", C_ROSE),
    ("THE DILEMMA: CONVENTIONAL APPS FAIL", "SANDBOX APPS CANNOT STOP REAL THIEVES", "Play Store apps lack OS supervisor permissions.", C_ROSE),
    ("THE SOLUTION: ENTER PASA SENTINEL", "SOVEREIGN SYSTEM-LEVEL SUPERVISOR", "Operating directly at Android Enterprise Device Owner layer.", C_CYAN)
]

for t_sub, t_head, t_body, col in threats:
    img, d = create_base()
    draw_header(d, frame_idx, t_sub, col)
    d.text((80, 220), t_head, fill=C_WHITE, font=font_huge)
    d.text((80, 310), t_body, fill=col, font=font_title)
    
    # Visual Box Card
    d.rounded_rectangle([80, 420, W - 80, 850], radius=16, fill=C_DARK, outline=col, width=2)
    d.text((120, 460), "TELEMETRY AUDIT REPORT:", fill=col, font=font_mono)
    d.text((120, 520), f"// TARGET PLATFORM: ANDROID 8.0 - 16 (API 26 - 36)", fill=C_WHITE, font=font_title)
    d.text((120, 590), f"// THREAT LEVEL: CRITICAL ZERO-DAY SURFACE", fill=C_ROSE, font=font_title)
    d.text((120, 660), f"// COUNTERMEASURE STATUS: DEPLOYING SYSTEM SUPERVISOR", fill=C_CYAN, font=font_title)
    d.text((120, 730), f"// GOOGLE PLAY DEPENDENCIES: 0% (AIR-GAPPED SOVEREIGN)", fill=C_EMERALD, font=font_title)
    save_frame(img, frame_idx)
    frame_idx += 1

# ═══════════════════════════════════════════════════════════════
# SECTION 2: KINETIC SNATCH & HARDWARE ACCELEROMETER (15 - 26)
# ═══════════════════════════════════════════════════════════════
snatch_beats = [
    ("KINETIC 2.65G ACCELEROMETER", "CONTINUOUS 3-AXIS VECTOR SURVEILLANCE", "TrapManager monitors sqrt(x^2 + y^2 + z^2) > 26.0 m/s^2.", C_ROSE),
    ("SNATCH TRIGGER ENGAGED", "VIOLENT MOTORCYCLE PULL DETECTED", "Vector magnitude spiked to 27.8 m/s^2. Immediate trap fire.", C_ROSE),
    ("HERO_SNATCH", "HERO_SNATCH", "HERO_SNATCH", C_ROSE),
    ("AUTONOMOUS TRAP ENGAGEMENT", "ACTION TIME: 0.04 SECONDS", "Before the thief can accelerate 5 meters, the device is secured.", C_CYAN),
    ("KNOX KIOSK LOST MODE", "LOCK_TASK_FEATURE_NONE ENGAGED", "Hardware buttons, home gestures, and back navigation locked.", C_CYAN),
    ("SYSTEM UI LOCKOUT", "NOTIFICATION SHADE PERMANENTLY DISABLED", "Thief cannot pull down Quick Settings to turn on Airplane Mode.", C_CYAN),
    ("HEADLESS CAMERAX TRIGGER", "STEALTH FRONT-CAMERA MUGSHOT CAPTURED", "Zero screen blackout, zero preview flicker, zero shutter sound.", C_EMERALD),
    ("HARDWARE GNSS ENFORCEMENT", "FORCING GPS SATELLITE CHIP ACTIVE", "dpm.setLocationEnabled(true) turns on GPS even if user turned it off.", C_EMERALD),
    ("HIGH-PRIORITY TELEGRAM SOS", "EMERGENCY INCIDENT PIN BROADCAST", "Live satellite coordinates + mugshot beamed directly to owner.", C_EMERALD),
    ("POCKET EXTRACTION TRAP", "PROXIMITY SENSOR UNCOVERED TRAP", "Device pulled from pocket while locked requires unlock in 5s.", C_AMBER),
    ("COVERT TACTILE LOCATOR", "PULSE VIBRATION LOCATOR (/vibrate_pulse)", "Locate phone in crowded room without noisy alarms alerting thief.", C_AMBER),
    ("BATTERY HEALTH & DRAIN MONITOR", "RAPID POWER DRAIN ALERT (/battery_alert)", "Alerts if rogue surveillance or charger disconnection detected.", C_AMBER)
]

for t_sub, t_head, t_body, col in snatch_beats:
    if t_sub == "HERO_SNATCH" and img_snatch:
        # Composite cinematic hero image
        overlay = Image.new("RGBA", (W, H), (5, 8, 16, 120))
        img = Image.alpha_composite(img_snatch.convert("RGBA"), overlay).convert("RGB")
        d = ImageDraw.Draw(img)
        draw_header(d, frame_idx, "KINETIC 2.65G ACCELEROMETER SNATCH", C_ROSE)
        d.text((80, 200), "SNATCH VECTOR DETECTED: 2.65G", fill=C_ROSE, font=font_giant)
        d.text((80, 290), "Autonomous trap fires in 40 milliseconds before thief escapes.", fill=C_WHITE, font=font_title)
        d.rounded_rectangle([80, H - 240, W - 80, H - 80], radius=16, fill=(10, 15, 26), outline=C_ROSE, width=2)
        d.text((110, H - 210), "TELEMETRY: sqrt(x^2 + y^2 + z^2) = 27.4 m/s^2 // SATELLITE GPS ARMED", fill=C_WHITE, font=font_title)
        d.text((110, H - 150), "STATUS: KNOX KIOSK LOST MODE ENGAGED • FRONT MUGSHOT STREAMED", fill=C_CYAN, font=font_title)
    else:
        img, d = create_base()
        draw_header(d, frame_idx, t_sub, col)
        d.text((80, 220), t_head, fill=C_WHITE, font=font_huge)
        d.text((80, 310), t_body, fill=col, font=font_title)
        d.rounded_rectangle([80, 420, W - 80, 850], radius=16, fill=C_DARK, outline=col, width=2)
        d.text((120, 470), f"SENSOR LOG // EVENT ID: TRAP_{frame_idx:04d}", fill=col, font=font_mono)
        d.text((120, 540), f"• HARDWARE COMPONENT: STMicroelectronics 3-Axis Gyro/Accel", fill=C_WHITE, font=font_title)
        d.text((120, 610), f"• INTERCEPTION LATENCY: < 50ms (KERNEL DIRECT BROADCAST)", fill=C_EMERALD, font=font_title)
        d.text((120, 680), f"• EXECUTIVE ACTION: DPM LOCKTASK + FOREGROUND CAMERA BURST", fill=C_CYAN, font=font_title)
        d.text((120, 750), f"• ZERO STORAGE FOOTPRINT: DIRECT-TO-TELEGRAM RAM SHREDDER", fill=C_WHITE, font=font_title)
    save_frame(img, frame_idx)
    frame_idx += 1

# ═══════════════════════════════════════════════════════════════
# SECTION 3: KNOX-GRADE DEVICE OWNER PERMANENCE (27 - 38)
# ═══════════════════════════════════════════════════════════════
knox_beats = [
    ("KNOX DEVICE OWNER SUPERVISOR", "PROVISIONED VIA ANDROID ENTERPRISE (DPM)", "App operates at the highest Android system privilege.", C_CYAN),
    ("UNINSTALL PERMANENTLY DISABLED", "THE UNINSTALL BUTTON IS PHYSICALLY GREYED OUT", "Android Settings displays 'Managed by your organization'.", C_CYAN),
    ("HERO_KNOX", "HERO_KNOX", "HERO_KNOX", C_CYAN),
    ("SAFE MODE BOOT BLOCKED", "DISALLOW_SAFE_BOOT ENFORCED", "Thief cannot boot into diagnostic safe mode to bypass security.", C_CYAN),
    ("FACTORY RESET BLOCKED", "DISALLOW_FACTORY_RESET ACTIVE", "Device blocks wipes from Settings, Recovery, or ADB.", C_CYAN),
    ("AIRPLANE MODE LOCKED", "DISALLOW_AIRPLANE_MODE ENFORCED", "Cellular radios cannot be disabled by unauthorized handlers.", C_CYAN),
    ("HARDWARE ESCROW TOKEN", "RESET LOCKSCREEN PIN REMOTELY", "dpm.resetPasswordWithToken resets PIN on Android 8-16.", C_CYAN),
    ("BIOMETRIC COERCION KILLSWITCH", "DISABLE FINGERPRINT & FACE (/biometrics off)", "Forces master passphrase under robbery or duress checkpoints.", C_ROSE),
    ("LOCKSCREEN EMERGENCY BANNER", "RECOVERY INFO PINNED PERMANENTLY (/lockscreen_info)", "Owner contact info pinned directly onto OS lockscreen.", C_CYAN),
    ("INACTIVITY AUTOLOCK POLICY", "AUTOMATIC SYSTEM LOCKOUT (/autolock)", "Custom hardware timeout before device forces re-authentication.", C_CYAN),
    ("SELF-HEALING PERMISSIONS", "SELF-HEALING RUNTIME PERMISSION SOVEREIGNTY", "Camera, Mic, GPS, and SMS permissions permanently locked.", C_EMERALD),
    ("KERNEL SECURITY AUDITING", "LOW-LEVEL LINUX KERNEL AUDIT (/security_audit)", "Reports interactive ADB shell openings and KeyStore tampering.", C_CYAN)
]

for t_sub, t_head, t_body, col in knox_beats:
    if t_sub == "HERO_KNOX" and img_knox:
        overlay = Image.new("RGBA", (W, H), (5, 8, 16, 120))
        img = Image.alpha_composite(img_knox.convert("RGBA"), overlay).convert("RGB")
        d = ImageDraw.Draw(img)
        draw_header(d, frame_idx, "KNOX-GRADE SYSTEM SUPERVISOR", C_CYAN)
        d.text((80, 200), "IMMUTABLE KERNEL PERMANENCE", fill=C_CYAN, font=font_giant)
        d.text((80, 290), "Android itself physically prevents uninstallation and force-stopping.", fill=C_WHITE, font=font_title)
        d.rounded_rectangle([80, H - 240, W - 80, H - 80], radius=16, fill=(10, 15, 26), outline=C_CYAN, width=2)
        d.text((110, H - 210), "SPECIFICATION: DEVICE OWNER DPM // LOCK_TASK_FEATURE_NONE", fill=C_WHITE, font=font_title)
        d.text((110, H - 150), "STATUS: UNINSTALL GREYED OUT • FACTORY RESET DISABLED • SAFE BOOT OFF", fill=C_EMERALD, font=font_title)
    else:
        img, d = create_base()
        draw_header(d, frame_idx, t_sub, col)
        d.text((80, 220), t_head, fill=C_WHITE, font=font_huge)
        d.text((80, 310), t_body, fill=col, font=font_title)
        d.rounded_rectangle([80, 420, W - 80, 850], radius=16, fill=C_DARK, outline=col, width=2)
        d.text((120, 470), f"SECURITY DIRECTIVE // KERNEL HOOK DPM_{frame_idx:04d}", fill=col, font=font_mono)
        d.text((120, 540), f"• PROVISIONING METHOD: adb shell dpm set-device-owner", fill=C_WHITE, font=font_title)
        d.text((120, 610), f"• OS SUPERVISOR PRIVILEGE: ROOT-EQUIVALENT POLICY AUTHORITY", fill=C_CYAN, font=font_title)
        d.text((120, 680), f"• ESCROW CRYPTO TOKENS: 256-BIT TEE HARDWARE GENERATED", fill=C_EMERALD, font=font_title)
        d.text((120, 750), f"• PERMANENCE VERDICT: CANNOT BE UNINSTALLED WITHOUT MASTER AUTH", fill=C_WHITE, font=font_title)
    save_frame(img, frame_idx)
    frame_idx += 1

# ═══════════════════════════════════════════════════════════════
# SECTION 4: CELLEBRITE & FORENSIC HARDWARE BLOCKER (39 - 50)
# ═══════════════════════════════════════════════════════════════
cellebrite_beats = [
    ("HARDWARE FORENSIC EXTRACTION BOXES", "CELLEBRITE UFED & GRAYKEY EXTRACTION BOXES", "Forensic labs connect high-speed cables to rip databases.", C_ROSE),
    ("USB PHYSICAL PIN KILLSWITCH", "SEVERING USB DATA LINES AT KERNEL LEVEL", "Android 12+ dpm.setUsbDataSignalingEnabled(false).", C_ROSE),
    ("HERO_CELLEBRITE", "HERO_CELLEBRITE", "HERO_CELLEBRITE", C_ROSE),
    ("LOCKED-STATE USB AUTOLOCK", "AUTOMATIC KILLSWITCH ON SCREEN LOCK (/usb_autolock)", "When screen locks, USB data pins are immediately disconnected.", C_ROSE),
    ("BADUSB INJECTION DEFENSE", "BLOCKING MALICIOUS HID KEYBOARD EMULATORS", "Rubber Ducky and BadUSB hardware payloads fail instantly.", C_ROSE),
    ("JUICE-JACKING DEFENSE", "PUBLIC CHARGING STATION IMMUNITY", "Charge safely at airports & cafes without risk of data tapping.", C_CYAN),
    ("POWER CHARGING PRESERVED", "VBUS POWER PINS REMAIN ACTIVE", "Fast AC power flows normally; only data signaling lines are cut.", C_EMERALD),
    ("COMMAND INTERACTION", "REMOTE TOGGLE COMMAND: /usb_lock on|off", "Instant 1-tap Telegram inline toggle controls USB state.", C_CYAN),
    ("STATUS INSPECTION", "REAL-TIME BUS TELEMETRY: /usb_lock status", "Reports whether USB data signaling is physically connected or severed.", C_CYAN),
    ("FORENSIC DUMP BLOCKED", "SQLCIPHER ENCRYPTED VAULT INTEGRITY", "Local SQLite database encrypted with 256-bit AES master keys.", C_EMERALD),
    ("REVERSE REPLAY GUARD", "COMMAND VERIFIER WITH ANTI-REPLAY STORE", "Cryptographic Ed25519 signature rejects replayed packets.", C_EMERALD),
    ("ZERO FORENSIC BYTES", "EXTRACTION RESULT: 0 BYTES READ", "Cellebrite and forensic software return 'Device I/O Disconnected'.", C_EMERALD)
]

for t_sub, t_head, t_body, col in cellebrite_beats:
    if t_sub == "HERO_CELLEBRITE" and img_cellebrite:
        overlay = Image.new("RGBA", (W, H), (5, 8, 16, 120))
        img = Image.alpha_composite(img_cellebrite.convert("RGBA"), overlay).convert("RGB")
        d = ImageDraw.Draw(img)
        draw_header(d, frame_idx, "CELLEBRITE & BADUSB HARDWARE BLOCKER", C_ROSE)
        d.text((80, 200), "HARDWARE DATA LINES SEVERED", fill=C_ROSE, font=font_giant)
        d.text((80, 290), "dpm.setUsbDataSignalingEnabled(false) cuts data pins while charging continues.", fill=C_WHITE, font=font_title)
        d.rounded_rectangle([80, H - 240, W - 80, H - 80], radius=16, fill=(10, 15, 26), outline=C_ROSE, width=2)
        d.text((110, H - 210), "HARDWARE STATUS: DATA PINS D+ / D- / TX / RX PHYSICALLY SHUT DOWN", fill=C_WHITE, font=font_title)
        d.text((110, H - 150), "ATTACK BLOCKED: CELLEBRITE UFED, GRAYKEY & BADUSB DUMP FAILED", fill=C_CYAN, font=font_title)
    else:
        img, d = create_base()
        draw_header(d, frame_idx, t_sub, col)
        d.text((80, 220), t_head, fill=C_WHITE, font=font_huge)
        d.text((80, 310), t_body, fill=col, font=font_title)
        d.rounded_rectangle([80, 420, W - 80, 850], radius=16, fill=C_DARK, outline=col, width=2)
        d.text((120, 470), f"HARDWARE USB BUS AUDIT // PIN STATE {frame_idx:04d}", fill=col, font=font_mono)
        d.text((120, 540), f"• USB CONTROLLER: Type-C Hardware Controller Signaling State", fill=C_WHITE, font=font_title)
        d.text((120, 610), f"• COMMAND HOOK: /usb_lock on /usb_autolock enabled", fill=C_CYAN, font=font_title)
        d.text((120, 680), f"• AC VOLTAGE: 9.0V / 2.0A Fast Charge Active (Power Preserved)", fill=C_EMERALD, font=font_title)
        d.text((120, 750), f"• DATA TRANSFER RATE: 0 B/s (FULL HARDWARE ISOLATION)", fill=C_ROSE, font=font_title)
    save_frame(img, frame_idx)
    frame_idx += 1

# ═══════════════════════════════════════════════════════════════
# SECTION 5: BANKING TROJANS & CYBER CRIME SHIELDS (51 - 62)
# ═══════════════════════════════════════════════════════════════
cyber_beats = [
    ("BANKING OVERLAY MALWARE", "ACCESSIBILITY TROJAN OVERLAY SHIELD (/a11y_shield)", "Strips transparent overlays stealing banking credentials.", C_CYAN),
    ("REAL-TIME A11Y WINDOW AUDIT", "ACCESSIBILITY SERVICE HIERARCHY SCAN", "Detects rogue screen listeners attempting auto-clicks or keylogging.", C_CYAN),
    ("OVERLAY PURGE MECHANISM", "GLOBAL_ACTION_BACK & INSTANT WINDOW REMOVAL", "Malicious phishing windows are dismissed before input occurs.", C_CYAN),
    ("CRYPTO CLIPPER TRAP", "CLIPBOARD MONITORING GUARD (/clipper_guard)", "Monitors system clipboard for address replacement malware.", C_EMERALD),
    ("MULTI-CHAIN WALLET DEFENSE", "BTC • ETH • SOLANA • TON ADDRESS PROTECTION", "Matches regex wallet patterns and neutralizes unauthorized swaps.", C_EMERALD),
    ("CLIPBOARD PURGE ON TAMPER", "MALICIOUS ATTACKER WALLET WIPED", "Restores original copied address and alerts user on Telegram.", C_EMERALD),
    ("ANTI-2G / IMSI CATCHER SHIELD", "MODEM 2G DOWNGRADE PREVENTION (/anti_2g)", "Stops rogue Stingray base stations from forcing unencrypted 2G.", C_AMBER),
    ("BASEBAND RADIO HARDENING", "TELEPHONY REGISTRY CELLULAR MONITOR", "Locks radio to LTE/5G encrypted cellular channels.", C_AMBER),
    ("RANSOMWARE CANARY TRIPWIRE", "CANARY GUARD DECOY FILES (/canary_guard)", "Places hidden decoy files in storage to catch ransomware encryption.", C_ROSE),
    ("SIDELOAD & INSTALL LOCKDOWN", "APP INSTALL BLOCKER (/app_install_lock)", "Blocks unauthorized APK installations from Chrome or file managers.", C_ROSE),
    ("APP NETWORK FIREWALL", "NETWORK ISOLATION FIREWALL (/app_firewall)", "Blocks suspicious background apps from connecting to C2 servers.", C_ROSE),
    ("SHADOW APP VAULT", "FREEZE & CONCEAL APPS (/freeze, /unfreeze)", "Completely conceals banking and crypto apps from launcher.", C_CYAN)
]

for t_sub, t_head, t_body, col in cyber_beats:
    img, d = create_base()
    draw_header(d, frame_idx, t_sub, col)
    d.text((80, 220), t_head, fill=C_WHITE, font=font_huge)
    d.text((80, 310), t_body, fill=col, font=font_title)
    d.rounded_rectangle([80, 420, W - 80, 850], radius=16, fill=C_DARK, outline=col, width=2)
    d.text((120, 470), f"CYBER DEFENSE ENGINE // MODULE_{frame_idx:04d}", fill=col, font=font_mono)
    d.text((120, 540), f"• THREAT VECTOR: Android Accessibility & Background Daemon Attacks", fill=C_WHITE, font=font_title)
    d.text((120, 610), f"• INSPECTION ALGORITHM: Real-Time Event Loop & Broadcast Receivers", fill=C_CYAN, font=font_title)
    d.text((120, 680), f"• NEUTRALIZATION ACTION: Kernel Policy Revocation & Process Freeze", fill=C_EMERALD, font=font_title)
    d.text((120, 750), f"• SECURITY GUARANTEE: Zero Interception of Banking OTPs or Crypto", fill=C_WHITE, font=font_title)
    save_frame(img, frame_idx)
    frame_idx += 1

# ═══════════════════════════════════════════════════════════════
# SECTION 6: FAKE SHUTDOWN & 0-NIT STEALTH DECEPTION (63 - 74)
# ═══════════════════════════════════════════════════════════════
deception_beats = [
    ("AUTO POWER-MENU INTERCEPTION", "THIEF HOLDS PHYSICAL POWER BUTTON", "Attempts manual device shutdown or reboot.", C_EMERALD),
    ("ACCESSIBILITY DIALOG INTERCEPT", "POWER-DOWN WINDOWS ABORTED INSTANTLY", "AccessibilityScreenCaptureService captures GLOBAL_ACTION_POWER_DIALOG.", C_EMERALD),
    ("AUTHENTIC OEM POWER ANIMATION", "AUTHENTIC OEM REBOOT SIMULATION", "Plays exact manufacturer power-down animation sequence.", C_EMERALD),
    ("0-NIT OLED BLACK CANVAS", "DISPLAY DROPS TO PITCH-BLACK CANVAS", "WindowManager FLAG_FULLSCREEN and SYSTEM_UI_FLAG_IMMERSIVE.", C_CYAN),
    ("PHONE APPEARS 100% DEAD", "NO SCREEN GLOW • NO LED FLASH • SILENT", "Thief believes device has completely powered off.", C_CYAN),
    ("TOUCH-TRIGGERED FORENSICS", "EVERY DISPLAY TAP TAKES A MUGSHOT", "Touch on black screen covertly fires front-camera capture.", C_ROSE),
    ("HEADLESS BEACON DISPATCH", "LIVE SATELLITE GPS STREAMED TO BOT", "Location continuously beamed while screen remains black.", C_EMERALD),
    ("REMOTE WAKE COMMAND", "WAKE DISPLAY VIA TELEGRAM: /wake", "Restores normal screen operation instantly via owner command.", C_CYAN),
    ("SECRET MULTI-TAP UNLOCK", "PHYSICAL EMERGENCY WAKE GESTURE", "Owner can wake device with custom hardware multi-tap sequence.", C_CYAN),
    ("STERILE DECOY SANDBOX OS", "ARMED COERCION DURESS PIN UNLOCK", "Duress PIN unlocks a sterile decoy phone with zero banking apps.", C_ROSE),
    ("AUTOMATIC SHADOW HIDING", "BANKING & CRYPTO APPS VANISH", "dpm.setApplicationHidden hides private apps in decoy mode.", C_ROSE),
    ("COVERT EMERGENCY SOS", "SILENT SOS BROADCAST DURING COERCION", "Front mugshot and GPS beamed to Telegram while thief inspects decoy.", C_EMERALD)
]

for t_sub, t_head, t_body, col in deception_beats:
    img, d = create_base()
    draw_header(d, frame_idx, t_sub, col)
    d.text((80, 220), t_head, fill=C_WHITE, font=font_huge)
    d.text((80, 310), t_body, fill=col, font=font_title)
    d.rounded_rectangle([80, 420, W - 80, 850], radius=16, fill=C_DARK, outline=col, width=2)
    d.text((120, 470), f"DECEPTION SUBSYSTEM // FAKE_SHUTDOWN_{frame_idx:04d}", fill=col, font=font_mono)
    d.text((120, 540), f"• INTERCEPTION TARGET: GlobalActionsDialog / PowerDialog SystemUI", fill=C_WHITE, font=font_title)
    d.text((120, 610), f"• OLED POWER CONSUMPTION: 0-Nit True Black (Zero Battery Drain)", fill=C_EMERALD, font=font_title)
    d.text((120, 680), f"• HEADLESS CAPTURE: CameraX ServiceLifecycleOwner Elevation", fill=C_CYAN, font=font_title)
    d.text((120, 750), f"• DISMISSAL METHOD: Remote C2 (/wake) or Encrypted Master PIN", fill=C_WHITE, font=font_title)
    save_frame(img, frame_idx)
    frame_idx += 1

# ═══════════════════════════════════════════════════════════════
# SECTION 7: AIR-GAPPED SMS C2 & DUAL-SIM CARRIERS (75 - 86)
# ═══════════════════════════════════════════════════════════════
sms_beats = [
    ("AIR-GAPPED TELEPHONY C2", "FULL COMMAND AUTHORITY WITHOUT INTERNET", "When mobile data, Wi-Fi, and Bluetooth are completely off.", C_CYAN),
    ("DIRECT BOOT LISTENER", "SMS_RECEIVED BROADCAST RECEIVER (PRIORITY 999)", "Operates at direct boot layer before first device unlock.", C_CYAN),
    ("RFC 6238 TOTP AUTHENTICATION", "TIME-BASED ONE-TIME PASSWORD VERIFICATION", "6-digit rolling codes valid within +/- 3 time steps.", C_EMERALD),
    ("ENCRYPTED SMS SYNTAX", "TEMPLATE: PASA <6-DIGIT-TOTP> <COMMAND>", "e.g. PASA 419582 /locate or PASA 419582 /status", C_WHITE),
    ("HARDWARE GPS WAKE", "GNSS CHIP WOKEN REMOTELY VIA TELEPHONY", "Acquires satellite fix and prepares outbound cellular SMS.", C_EMERALD),
    ("DUAL-SIM CARRIER ROUTING", "INCOMING SUBSCRIPTION ID EXTRACTION", "Responds via the exact carrier slot that received the command.", C_CYAN),
    ("PUSH-BUTTON PHONE COMPATIBILITY", "CONTROL FROM ANY BASIC FEATURE PHONE", "No smartphone or internet required on the controller side.", C_CYAN),
    ("REMOTE CALLING WITH DUAL-SIM", "OUTBOUND CALLING: /call <number> sim1|sim2", "Places silent or speaker call to eavesdrop or establish link.", C_CYAN),
    ("SIM CHANGE DETECTION", "SIMChangeReceiver MONITORS ABSENT / LOADED", "Detects physical SIM tray eject in 0.1 seconds.", C_ROSE),
    ("SIM TRAY LOCKDOWN", "CRYPTOGRAPHIC SIM TRAY LOCK (/sim_tray_lock)", "Locks device with secret 8-digit PIN and suspends all apps.", C_ROSE),
    ("FOREIGN NUMBER EXPOSURE TRAP", "SILENT OUTBOUND SMS TO BACKUP PHONE", "When thief puts in foreign SIM, emergency SMS exposes their number.", C_EMERALD),
    ("THIEF CALLER ID TRAPPED", "CALLER ID CAPTURED IN LAW ENFORCEMENT REPORT", "Thief's cellular MSISDN phone number is unmasked.", C_EMERALD)
]

for t_sub, t_head, t_body, col in sms_beats:
    img, d = create_base()
    draw_header(d, frame_idx, t_sub, col)
    d.text((80, 220), t_head, fill=C_WHITE, font=font_huge)
    d.text((80, 310), t_body, fill=col, font=font_title)
    d.rounded_rectangle([80, 420, W - 80, 850], radius=16, fill=C_DARK, outline=col, width=2)
    d.text((120, 470), f"AIR-GAPPED TELEPHONY PROTOCOL // SMS_FRAME_{frame_idx:04d}", fill=col, font=font_mono)
    d.text((120, 540), f"• RADIO ACCESS: GSM / UMTS / LTE 3GPP SMS Layer (Non-IP)", fill=C_WHITE, font=font_title)
    d.text((120, 610), f"• AUTHENTICATION: HMAC-SHA1 RFC 6238 TOTP Rolling Algorithm", fill=C_EMERALD, font=font_title)
    d.text((120, 680), f"• DUAL-SIM BINDING: SubscriptionManager Multi-Carrier Selection", fill=C_CYAN, font=font_title)
    d.text((120, 750), f"• AIR-GAP RESILIENCE: 100% OPERATIONAL WITH ZERO DATA CONNECTION", fill=C_WHITE, font=font_title)
    save_frame(img, frame_idx)
    frame_idx += 1

# ═══════════════════════════════════════════════════════════════
# SECTION 8: 100% ZERO-STORAGE PRIVACY & 94 C2 COMMANDS (87 - 98)
# ═══════════════════════════════════════════════════════════════
privacy_beats = [
    ("100% ZERO-STORAGE GUARANTEE", "ZERO MEDIA OR GPS TRACKS SAVED ON VPS", "Your surveillance evidence never touches third-party disks.", C_EMERALD),
    ("DIRECT-TO-TELEGRAM MEDIA STREAM", "DIRECT PHOTO, VIDEO & VOICE STREAMING", "Evidence streams directly to your private bot and chat ID.", C_EMERALD),
    ("INSTANT DEVICE RAM SHREDDING", "FILES CRUSHED IMMEDIATELY AFTER UPLOAD", "Temporary cache overwritten with zeroes and deleted.", C_EMERALD),
    ("SOVEREIGN PRIVATE CLIENT", "BOT TOKEN NEVER LEAVES YOUR DEVICE", "The phone polls Telegram API directly without relay servers.", C_EMERALD),
    ("IRREVERSIBLE DEVICE HASH", "SHA256(deviceId:key) REGISTRATION", "VPS only receives an irreversible hash for license validation.", C_CYAN),
    ("ED25519 DIGITAL LICENSING", "CRYPTOGRAPHIC OFFLINE VERIFICATION", "7-day rotating certificates verified by hardware keystore.", C_CYAN),
    ("SYSTEM-WIDE ENCRYPTED DNS", "DNS-OVER-TLS (DoT) ENFORCEMENT (/dns)", "Forces Quad9, Cloudflare, or AdGuard encrypted DNS.", C_CYAN),
    ("CELL TOWER TRIANGULATION", "DUAL-SIM TOWER SCANNING (/tower)", "Pinpoints indoor locations without satellite GPS reception.", C_CYAN),
    ("94 MODULAR TELEGRAM COMMANDS", "7 INTERACTIVE TACTICAL COMMAND HUBS", "Defense, Capture, Wiretap, Forensics, Hardware, Telecom, Core.", C_WHITE),
    ("UNIFIED EXTRACTION PAGINATION", "INSTANT TEXT EXPORTS (/call_log, /sms_log)", "Exports contacts, SMS, and history directly as text documents.", C_WHITE),
    ("HARDWARE CAMERA KILLSWITCH", "SYSTEM-WIDE CAMERA DISABLE (/camera_lock)", "Neutralizes unauthorized malware from accessing camera HAL.", C_ROSE),
    ("PERIPHERAL LOCKDOWN", "BLUETOOTH & MIC LOCKOUT (/bluetooth_lock, /mic_mute)", "Disallows rogue pairings and mutes microphone at hardware level.", C_ROSE)
]

for t_sub, t_head, t_body, col in privacy_beats:
    img, d = create_base()
    draw_header(d, frame_idx, t_sub, col)
    d.text((80, 220), t_head, fill=C_WHITE, font=font_huge)
    d.text((80, 310), t_body, fill=col, font=font_title)
    d.rounded_rectangle([80, 420, W - 80, 850], radius=16, fill=C_DARK, outline=col, width=2)
    d.text((120, 470), f"PRIVACY SPECIFICATION // ZERO_STORAGE_{frame_idx:04d}", fill=col, font=font_mono)
    d.text((120, 540), f"• CLOUD RESIDUE: 0.00 BYTES SAVED (MUTABLE MEMORY BUFFER ONLY)", fill=C_EMERALD, font=font_title)
    d.text((120, 610), f"• RETROFIT TLS 1.3: Direct End-to-End api.telegram.org Pipes", fill=C_CYAN, font=font_title)
    d.text((120, 680), f"• CRYPTOGRAPHIC PROTOCOL: ASTRA Enclave P-256 ECDSA Hardware Keys", fill=C_WHITE, font=font_title)
    d.text((120, 750), f"• PRIVACY VERDICT: COMPLETE SOVEREIGN INDEPENDENCE", fill=C_EMERALD, font=font_title)
    save_frame(img, frame_idx)
    frame_idx += 1

# ═══════════════════════════════════════════════════════════════
# SECTION 9: COMMERCIAL ACCESS & FINALE (99 - 110)
# ═══════════════════════════════════════════════════════════════
commercial_beats = [
    ("OFFICIAL PRODUCTION RELEASE", "PASA SENTINEL v3.7.15 (BUILD 79)", "Signed with production RSA-4096 / Scheme v2 keystore.", C_CYAN),
    ("ANDROID TARGET RANGE", "ANDROID 8.0 - 16 (API 26 - 36)", "Full support for Samsung Knox, Google Pixel, Xiaomi, OnePlus.", C_CYAN),
    ("WINDOWS SETUP WIZARD", "1-CLICK DEVICE OWNER PROVISIONING KIT", "Double-click setup.bat provisions Device Owner without root.", C_CYAN),
    ("PRO LIFETIME LICENSE - $25", "1 DEVICE PROTECTED FOREVER (ONE-TIME)", "All 94 Telegram commands, full cyber suite, lifetime OTA updates.", C_CYAN),
    ("ZERO RECURRING FEES", "NO MONTHLY SUBSCRIPTIONS • NO CLOUD FEES", "Own your mobile defense software outright forever.", C_EMERALD),
    ("ENTERPRISE FLEET PACK - $99", "5x PRO LIFETIME LICENSES FOR FLEETS", "Private server node, VIP WhatsApp hotline, zero-knowledge telemetry.", C_EMERALD),
    ("7-DAY MONEY-BACK GUARANTEE", "100% REFUND GUARANTEE", "Test every trap, fake shutdown, and forensic command risk-free.", C_EMERALD),
    ("MULTI-CHANNEL PAYMENTS", "BINANCE PAY • BKASH • LEMONSQUEEZY", "Crypto, Bangladeshi bKash, and International Credit Cards / PayPal.", C_AMBER),
    ("CUSTOMER SELF-SERVICE PORTAL", "ZERO-TOUCH QR CODE PROVISIONING", "Instant QR setup and license verification at pasa.izhaanintellect.fun/portal.", C_CYAN),
    ("THE DEFENSE APPARATUS", "NEVER SURRENDER YOUR SOVEREIGN DATA", "Uncompromising defense against physical theft and mobile cyber crime.", C_CYAN),
    ("OFFICIAL ACCESS PORTAL", "GET IT TODAY: pasa.izhaanintellect.fun", "Download the tactical APK directly and arm your phone today.", C_CYAN),
    ("FINALE SLOGAN", "PASA SENTINEL: YOUR PHONE WILL NEVER SURRENDER.", "Engineering sovereign mobile defense for the modern digital era.", C_CYAN)
]

for t_sub, t_head, t_body, col in commercial_beats:
    img, d = create_base()
    draw_header(d, frame_idx, t_sub, col)
    d.text((80, 200), t_head, fill=C_WHITE, font=font_giant)
    d.text((80, 290), t_body, fill=col, font=font_title)
    
    # Bottom Callout Box
    d.rounded_rectangle([80, 400, W - 80, 880], radius=16, fill=(10, 20, 36), outline=col, width=2)
    d.text((120, 450), f"PASA SENTINEL // SOVEREIGN SECURITY ARCHITECTURE", fill=col, font=font_mono)
    d.text((120, 520), "• 94 TELEGRAM C2 COMMANDS & 7 INTERACTIVE HUBS", fill=C_WHITE, font=font_title)
    d.text((120, 580), "• CELLEBRITE & BADUSB HARDWARE PIN KILLSWITCH (/usb_lock)", fill=C_ROSE, font=font_title)
    d.text((120, 640), "• BANKING TROJAN A11Y OVERLAY STRIPPER (/a11y_shield)", fill=C_CYAN, font=font_title)
    d.text((120, 700), "• 2.65G KINETIC SNATCH TRAP & AUTO FAKE SHUTDOWN DECEPTION", fill=C_EMERALD, font=font_title)
    d.text((120, 760), "• AIR-GAPPED CELLULAR SMS FALLBACK (RFC 6238 TOTP)", fill=C_AMBER, font=font_title)
    d.text((120, 820), "• 100% ZERO-STORAGE GUARANTEE // ZERO CLOUD LOGS", fill=C_WHITE, font=font_title)
    save_frame(img, frame_idx)
    frame_idx += 1

print(f"COMPLETE! Generated {frame_idx - 1} distinct high-velocity frames in {OUT_DIR}.")
