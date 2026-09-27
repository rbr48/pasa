# Render 9 Cinema-Grade Kinetic Video Slides using Pillow
# Dimensions: 1920x1080 (Full HD, 16:9)

from PIL import Image, ImageDraw, ImageFont, ImageFilter
import os

OUT_DIR = r"E:\Projects\PrivateApp\promo_video_builder\output"
ASSETS_DIR = r"E:\Projects\PrivateApp\promo_video_builder\assets"
os.makedirs(OUT_DIR, exist_ok=True)

W, H = 1920, 1080

# Fonts
font_huge = ImageFont.truetype("C:/Windows/Fonts/segoeuib.ttf", 64)
font_large = ImageFont.truetype("C:/Windows/Fonts/segoeuib.ttf", 46)
font_title = ImageFont.truetype("C:/Windows/Fonts/segoeuib.ttf", 36)
font_sub = ImageFont.truetype("C:/Windows/Fonts/segoeui.ttf", 26)
font_mono_bold = ImageFont.truetype("C:/Windows/Fonts/arialbd.ttf", 20)
font_badge = ImageFont.truetype("C:/Windows/Fonts/arialbd.ttf", 16)
font_pill = ImageFont.truetype("C:/Windows/Fonts/segoeuib.ttf", 22)

# Colors
C_BG = (7, 11, 20)
C_CYAN = (6, 182, 212)
C_ROSE = (244, 63, 94)
C_EMERALD = (16, 185, 129)
C_AMBER = (245, 158, 11)
C_WHITE = (255, 255, 255)
C_GRAY = (148, 163, 184)
C_BORDER = (28, 44, 72)

def draw_hud(draw, act_num, act_title, threat_text, threat_color):
    # Top telemetry strip
    draw.text((70, 50), "PASA SENTINEL // SOVEREIGN CYBER DEFENSE", fill=C_CYAN, font=font_mono_bold)
    draw.text((W - 480, 50), f"STATUS: {threat_text}", fill=threat_color, font=font_mono_bold)
    draw.line([(70, 85), (W - 70, 85)], fill=C_BORDER, width=1)
    
    # Act Badge
    draw.rounded_rectangle([70, 110, 480, 150], radius=8, fill=(16, 26, 44), outline=threat_color, width=2)
    draw.text((85, 120), f"ACT {act_num} // {act_title}", fill=threat_color, font=font_badge)

def draw_grid(draw):
    for x in range(0, W, 100):
        draw.line([(x, 0), (x, H)], fill=(14, 22, 38), width=1)
    for y in range(0, H, 100):
        draw.line([(y, 0), (W, y)], fill=(14, 22, 38), width=1)
    # Corner markers
    draw.line([(40, 40), (80, 40)], fill=C_CYAN, width=2)
    draw.line([(40, 40), (40, 80)], fill=C_CYAN, width=2)
    draw.line([(W - 40, 40), (W - 80, 40)], fill=C_CYAN, width=2)
    draw.line([(W - 40, 40), (W - 40, 80)], fill=C_CYAN, width=2)
    draw.line([(40, H - 40), (80, H - 40)], fill=C_CYAN, width=2)
    draw.line([(40, H - 40), (40, H - 80)], fill=C_CYAN, width=2)
    draw.line([(W - 40, H - 40), (W - 80, H - 40)], fill=C_CYAN, width=2)
    draw.line([(W - 40, H - 40), (W - 40, H - 80)], fill=C_CYAN, width=2)

print("Rendering Slide 1: The Hook...")
# ── SLIDE 1: THE HOOK ──
img1 = Image.new("RGB", (W, H), C_BG)
d1 = ImageDraw.Draw(img1)
draw_grid(d1)
draw_hud(d1, "01", "CRITICAL THREAT REALITY", "SYSTEM AT RISK", C_ROSE)

d1.text((70, 200), "OVER 10,000,000 PHONES ARE STOLEN YEARLY.", fill=C_ROSE, font=font_large)
d1.text((70, 280), "YOUR PHONE WILL FAIL IN 8 SECONDS.", fill=C_WHITE, font=font_huge)
d1.text((70, 380), "Thieves toggle Airplane mode, cut Wi-Fi, and attach forensic extraction cables.", fill=C_GRAY, font=font_sub)

# 3 Failure Cards
cards = [
    ("AIRPLANE MODE TOGGLED", "Quick Settings pulled down immediately.", C_ROSE),
    ("GOOGLE FMD: OFFLINE", "Account deleted or device isolated.", C_ROSE),
    ("CELLEBRITE CABLE PLUGGED", "Forensic box rips chats & crypto keys.", C_ROSE)
]
for idx, (title, desc, col) in enumerate(cards):
    x = 70 + idx * 600
    d1.rounded_rectangle([x, 480, x + 560, 720], radius=16, fill=(16, 22, 36), outline=col, width=2)
    d1.text((x + 30, 520), f"FAILURE POINT 0{idx+1}", fill=col, font=font_badge)
    d1.text((x + 30, 560), title, fill=C_WHITE, font=font_title)
    d1.text((x + 30, 630), desc, fill=C_GRAY, font=font_sub)

d1.text((70, 840), "STANDARD SECURITY IS DEAD. ENTER THE SOVEREIGN SYSTEM SUPERVISOR.", fill=C_CYAN, font=font_title)
img1.save(os.path.join(OUT_DIR, "k_slide1.png"))

print("Rendering Slide 2: The Snatch Action...")
# ── SLIDE 2: SNATCH INTERCEPT (Cinematic Image Background) ──
bg2 = Image.open(os.path.join(ASSETS_DIR, "snatch_intercept.jpg")).resize((W, H))
# Apply dark gradient overlay
overlay2 = Image.new("RGBA", (W, H), (0, 0, 0, 0))
ov_d2 = ImageDraw.Draw(overlay2)
ov_d2.rectangle([0, 0, W, H], fill=(5, 8, 16, 140)) # Vignette
ov_d2.rectangle([0, H - 360, W, H], fill=(5, 8, 16, 235)) # Bottom dark banner
img2 = Image.alpha_composite(bg2.convert("RGBA"), overlay2).convert("RGB")
d2 = ImageDraw.Draw(img2)
draw_hud(d2, "02", "KINETIC VECTOR INTERCEPTION", "2.65G SNATCH DETECTED", C_ROSE)

d2.text((70, 180), "2.65G KINETIC SNATCH TRAP", fill=C_ROSE, font=font_huge)
d2.text((70, 270), "Autonomous accelerometer detects sudden theft pulls and motorcycle grabs.", fill=C_WHITE, font=font_sub)

# Bottom Feature Banner
d2.rounded_rectangle([70, H - 320, W - 70, H - 60], radius=16, fill=(10, 15, 26), outline=C_ROSE, width=2)
d2.text((100, H - 290), "ACTION TAKEN IN 0.05 SECONDS:", fill=C_ROSE, font=font_badge)
d2.text((100, H - 250), "• INSTANT KNOX KIOSK LOST MODE ACTIVATED", fill=C_WHITE, font=font_title)
d2.text((100, H - 195), "• HEADLESS FRONT-CAMERA MUGSHOT CAPTURED & RAM SHREDDED", fill=C_WHITE, font=font_title)
d2.text((100, H - 140), "• SATELLITE GPS BEACON STREAMED DIRECT TO PRIVATE TELEGRAM", fill=C_CYAN, font=font_title)
img2.save(os.path.join(OUT_DIR, "k_slide2.png"))

print("Rendering Slide 3: Knox Device Owner...")
# ── SLIDE 3: KNOX DEVICE OWNER (Cinematic Vault Background) ──
bg3 = Image.open(os.path.join(ASSETS_DIR, "knox_fortress.jpg")).resize((W, H))
overlay3 = Image.new("RGBA", (W, H), (0, 0, 0, 0))
ov_d3 = ImageDraw.Draw(overlay3)
ov_d3.rectangle([0, 0, W, H], fill=(5, 8, 16, 130))
ov_d3.rectangle([0, H - 360, W, H], fill=(5, 8, 16, 235))
img3 = Image.alpha_composite(bg3.convert("RGBA"), overlay3).convert("RGB")
d3 = ImageDraw.Draw(img3)
draw_hud(d3, "03", "KNOX-GRADE PERMANENCE", "SUPERVISOR ACTIVE", C_CYAN)

d3.text((70, 180), "KNOX DEVICE OWNER SUPERVISOR", fill=C_CYAN, font=font_huge)
d3.text((70, 270), "Android itself physically disables uninstallation and force-stopping.", fill=C_WHITE, font=font_sub)

d3.rounded_rectangle([70, H - 320, W - 70, H - 60], radius=16, fill=(10, 15, 26), outline=C_CYAN, width=2)
d3.text((100, H - 290), "ENTERPRISE KERNEL DEFENSE:", fill=C_CYAN, font=font_badge)
d3.text((100, H - 250), "🛡️ UNINSTALL & FORCE STOP BUTTONS ARE GREYED OUT BY THE OS", fill=C_WHITE, font=font_title)
d3.text((100, H - 195), "🔒 SAFE MODE BOOT & FACTORY RESET PERMANENTLY BLOCKED", fill=C_WHITE, font=font_title)
d3.text((100, H - 140), "🔑 HARDWARE ESCROW TOKENS RESET LOCKSCREEN PIN REMOTELY", fill=C_EMERALD, font=font_title)
img3.save(os.path.join(OUT_DIR, "k_slide3.png"))

print("Rendering Slide 4: Cellebrite Blocker...")
# ── SLIDE 4: CELLEBRITE EXTRACTION CABLES BLOCKED ──
bg4 = Image.open(os.path.join(ASSETS_DIR, "cellebrite_blocked.jpg")).resize((W, H))
overlay4 = Image.new("RGBA", (W, H), (0, 0, 0, 0))
ov_d4 = ImageDraw.Draw(overlay4)
ov_d4.rectangle([0, 0, W, H], fill=(5, 8, 16, 120))
ov_d4.rectangle([0, H - 360, W, H], fill=(5, 8, 16, 240))
img4 = Image.alpha_composite(bg4.convert("RGBA"), overlay4).convert("RGB")
d4 = ImageDraw.Draw(img4)
draw_hud(d4, "04", "HARDWARE PIN KILLSWITCH", "DATA LINES SEVERED", C_ROSE)

d4.text((70, 180), "CELLEBRITE FORENSIC CABLES? BLOCKED.", fill=C_ROSE, font=font_huge)
d4.text((70, 270), "Physical USB data pins severed at kernel level. Zero forensic extraction.", fill=C_WHITE, font=font_sub)

d4.rounded_rectangle([70, H - 320, W - 70, H - 60], radius=16, fill=(10, 15, 26), outline=C_ROSE, width=2)
d4.text((100, H - 290), "COMMAND: /usb_lock & /usb_autolock", fill=C_ROSE, font=font_badge)
d4.text((100, H - 250), "⚡ Android 12+ dpm.setUsbDataSignalingEnabled(false) shuts off physical data pins", fill=C_WHITE, font=font_title)
d4.text((100, H - 195), "🛑 Cellebrite, GrayKey, BadUSB, and juice-jacking dump tools FAIL INSTANTLY", fill=C_WHITE, font=font_title)
d4.text((100, H - 140), "🔋 Safe AC power charging continues without allowing any byte transfers", fill=C_CYAN, font=font_title)
img4.save(os.path.join(OUT_DIR, "k_slide4.png"))

print("Rendering Slide 5: Banking Trojans & Crypto Clippers...")
# ── SLIDE 5: BANKING TROJAN & CLIPPER DEFENSE ──
img5 = Image.new("RGB", (W, H), C_BG)
d5 = ImageDraw.Draw(img5)
draw_grid(d5)
draw_hud(d5, "05", "CYBER DEFENSE SUITE", "MALWARE NEUTRALIZED", C_EMERALD)

d5.text((70, 180), "BANKING TROJANS & CRYPTO CLIPPERS DEFEATED.", fill=C_EMERALD, font=font_large)
d5.text((70, 255), "Defending your digital identity, private banking apps, and crypto wallets.", fill=C_GRAY, font=font_sub)

# 3 Grid Cards
b_cards = [
    ("BANKING OVERLAYS (/a11y_shield)", "Audits Accessibility window trees in real-time. Unauthorized transparent screen overlays stealing banking credentials are stripped instantly.", C_CYAN),
    ("CRYPTO CLIPPER TRAP (/clipper_guard)", "Monitors background clipboard hijacking. Blocks malware from swapping your Bitcoin, Ethereum, Solana, and TON wallet addresses.", C_EMERALD),
    ("ANTI-2G STINGRAY SHIELD (/anti_2g)", "Locks modem from downgrading to vulnerable 2G GSM cellular frequencies used by rogue IMSI catchers to eavesdrop on 2FA SMS OTPs.", C_AMBER)
]
for idx, (title, desc, col) in enumerate(b_cards):
    x = 70 + idx * 600
    d5.rounded_rectangle([x, 340, x + 560, 800], radius=16, fill=(13, 20, 36), outline=col, width=2)
    d5.text((x + 30, 380), f"WEAPON 0{idx+1}", fill=col, font=font_badge)
    d5.text((x + 30, 430), title, fill=C_WHITE, font=font_title)
    
    # Word wrap description
    words = desc.split()
    lines = []
    cur = ""
    for w in words:
        if len(cur + " " + w) < 32:
            cur += " " + w
        else:
            lines.append(cur)
            cur = w
    lines.append(cur)
    for l_idx, line in enumerate(lines):
        d5.text((x + 30, 520 + l_idx * 36), line.strip(), fill=C_GRAY, font=font_sub)

d5.text((70, 870), "ADDITIONAL: Ransomware Canary Tripwire (/canary_guard) • Sideload Lock (/app_install_lock)", fill=C_WHITE, font=font_title)
img5.save(os.path.join(OUT_DIR, "k_slide5.png"))

print("Rendering Slide 6: Fake Shutdown Deception...")
# ── SLIDE 6: FAKE SHUTDOWN DECEPTION ──
img6 = Image.new("RGB", (W, H), C_BG)
d6 = ImageDraw.Draw(img6)
draw_grid(d6)
draw_hud(d6, "06", "DECEPTION & STEALTH FORENSICS", "DECEPTION ACTIVE", C_EMERALD)

d6.text((70, 180), "AUTO POWER-MENU FAKE SHUTDOWN.", fill=C_EMERALD, font=font_large)
d6.text((70, 255), "When a thief tries to turn off the phone, PASA outsmarts them completely.", fill=C_GRAY, font=font_sub)

# Split view: Left Explanation, Right Phone Simulation Box
d6.rounded_rectangle([70, 340, 1050, 820], radius=16, fill=(13, 20, 36), outline=C_BORDER, width=2)
d6.text((110, 380), "THE DECEPTION SEQUENCE:", fill=C_EMERALD, font=font_badge)
d6.text((110, 430), "1. Thief holds Power button (1-2s) to shut down device", fill=C_WHITE, font=font_title)
d6.text((110, 500), "2. PASA intercepts OEM power dialog and aborts turn-off", fill=C_WHITE, font=font_title)
d6.text((110, 570), "3. Simulates authentic shutdown animation", fill=C_WHITE, font=font_title)
d6.text((110, 640), "4. Plunges OLED screen to 0-nit pitch-black canvas", fill=C_CYAN, font=font_title)
d6.text((110, 710), "5. Every touch on the 'dead' screen silently takes mugshots & GPS", fill=C_ROSE, font=font_title)

# Right: The 0-Nit Screen Frame
d6.rounded_rectangle([1120, 340, 1850, 820], radius=24, fill=(0, 0, 0), outline=C_EMERALD, width=3)
d6.text((1160, 380), "0-NIT STEALTH BLACK CANVAS", fill=C_EMERALD, font=font_mono_bold)
d6.text((1160, 480), "PHONE APPEARS 100% DEAD TO THIEF", fill=(70, 80, 100), font=font_title)
d6.text((1160, 560), "📸 SILENT HEADLESS CAMERA ARMED", fill=C_ROSE, font=font_title)
d6.text((1160, 630), "📡 SAT GNSS BEACON TRANSMITTING", fill=C_CYAN, font=font_title)
d6.text((1160, 720), "WAKE REMOTELY VIA TELEGRAM: /wake", fill=C_EMERALD, font=font_badge)

d6.text((70, 880), "TEST COMMAND: /fakeshutdown test • AUTO-ENGAGE: /fakeshutdown auto on", fill=C_CYAN, font=font_title)
img6.save(os.path.join(OUT_DIR, "k_slide6.png"))

print("Rendering Slide 7: Air-Gapped SMS C2...")
# ── SLIDE 7: AIR-GAPPED SMS C2 ──
img7 = Image.new("RGB", (W, H), C_BG)
d7 = ImageDraw.Draw(img7)
draw_grid(d7)
draw_hud(d7, "07", "AIR-GAPPED TELEPHONY C2", "ZERO INTERNET NEEDED", C_CYAN)

d7.text((70, 180), "AIR-GAPPED CELLULAR SMS C2.", fill=C_CYAN, font=font_large)
d7.text((70, 255), "Full military-grade command authority even when mobile data & Wi-Fi are dead.", fill=C_GRAY, font=font_sub)

d7.rounded_rectangle([70, 340, 920, 820], radius=16, fill=(13, 20, 36), outline=C_BORDER, width=2)
d7.text((110, 380), "INCOMING ENCRYPTED SMS:", fill=C_CYAN, font=font_badge)
d7.rounded_rectangle([110, 430, 880, 520], radius=12, fill=(20, 35, 60), outline=C_CYAN, width=1)
d7.text((140, 455), "PASA 419582 /locate", fill=C_WHITE, font=font_huge)
d7.text((110, 560), "• Authenticated via RFC 6238 TOTP rolling tokens", fill=C_WHITE, font=font_title)
d7.text((110, 620), "• Dual-SIM carrier routing via active subscription", fill=C_WHITE, font=font_title)
d7.text((110, 680), "• Forcibly wakes hardware GNSS GPS chip", fill=C_EMERALD, font=font_title)
d7.text((110, 740), "• Responds from any basic push-button feature phone", fill=C_CYAN, font=font_title)

d7.rounded_rectangle([980, 340, 1850, 820], radius=16, fill=(13, 20, 36), outline=C_BORDER, width=2)
d7.text((1020, 380), "OUTBOUND INSTANT COVERT SMS:", fill=C_EMERALD, font=font_badge)
d7.rounded_rectangle([1020, 430, 1810, 760], radius=12, fill=(10, 28, 20), outline=C_EMERALD, width=1)
d7.text((1050, 460), "PASA EMERGENCY FIX [LIVE]:", fill=C_EMERALD, font=font_title)
d7.text((1050, 520), "Lat: 23.78088 | Lon: 90.42291", fill=C_WHITE, font=font_title)
d7.text((1050, 580), "Accuracy: 3.8m | Speed: 0.0 km/h", fill=C_WHITE, font=font_title)
d7.text((1050, 640), "Batt: 84% (Discharging) | SIM: SIM1", fill=C_WHITE, font=font_title)
d7.text((1050, 700), "maps.google.com/?q=23.78088,90.42291", fill=C_CYAN, font=font_sub)

d7.text((70, 880), "ADDITIONAL: Outbound Emergency Calling (/call <number> sim1|sim2) • Dual-SIM Telemetry (/sim)", fill=C_WHITE, font=font_title)
img7.save(os.path.join(OUT_DIR, "k_slide7.png"))

print("Rendering Slide 8: Zero-Storage Guarantee...")
# ── SLIDE 8: ZERO-STORAGE PRIVACY ──
img8 = Image.new("RGB", (W, H), C_BG)
d8 = ImageDraw.Draw(img8)
draw_grid(d8)
draw_hud(d8, "08", "SOVEREIGN PRIVACY", "0-CLOUD STORAGE", C_EMERALD)

d8.text((70, 180), "100% ZERO-STORAGE ARCHITECTURE.", fill=C_EMERALD, font=font_large)
d8.text((70, 255), "Surveillance evidence is never stored on servers. Complete mathematical privacy.", fill=C_GRAY, font=font_sub)

d8.rounded_rectangle([70, 340, 920, 820], radius=16, fill=(13, 20, 36), outline=C_EMERALD, width=2)
d8.text((110, 380), "PRIVATE TELEGRAM SOVEREIGNTY", fill=C_EMERALD, font=font_badge)
d8.text((110, 440), "• Direct-to-Telegram Media Streaming", fill=C_WHITE, font=font_title)
d8.text((110, 510), "• Photos & GPS pins never touch cloud databases", fill=C_WHITE, font=font_title)
d8.text((110, 580), "• Immediately shredded from phone RAM and cache", fill=C_ROSE, font=font_title)
d8.text((110, 650), "• botToken & ownerChatId never leave device", fill=C_CYAN, font=font_title)
d8.text((110, 720), "• 94 interactive C2 commands with 7 Hub consoles", fill=C_WHITE, font=font_title)

d8.rounded_rectangle([980, 340, 1850, 820], radius=16, fill=(13, 20, 36), outline=C_CYAN, width=2)
d8.text((1020, 380), "CRYPTOGRAPHIC SECURITY LAYER", fill=C_CYAN, font=font_badge)
d8.text((1020, 440), "• Hardware-backed Android Keystore / StrongBox", fill=C_WHITE, font=font_title)
d8.text((1020, 510), "• Ed25519 offline license verification", fill=C_WHITE, font=font_title)
d8.text((1020, 580), "• Irreversible SHA-256 device registration hash", fill=C_WHITE, font=font_title)
d8.text((1020, 650), "• Decoy Duress PIN vanishes private apps instantly", fill=C_EMERALD, font=font_title)
d8.text((1020, 720), "• System-wide encrypted DNS (DoT Quad9/Cloudflare)", fill=C_CYAN, font=font_title)

d8.text((70, 880), "SOVEREIGN CLIENT: ZERO TELEMETRY TRACKERS • ZERO THIRD-PARTY SDKs", fill=C_WHITE, font=font_title)
img8.save(os.path.join(OUT_DIR, "k_slide8.png"))

print("Rendering Slide 9: Commercial Call to Action...")
# ── SLIDE 9: COMMERCIAL CTA ──
img9 = Image.new("RGB", (W, H), C_BG)
d9 = ImageDraw.Draw(img9)
draw_grid(d9)
draw_hud(d9, "09", "OFFICIAL SOVEREIGN ACCESS", "LIFETIME LICENSE", C_CYAN)

d9.text((70, 170), "PASA SENTINEL", fill=C_WHITE, font=font_huge)
d9.text((70, 250), "YOUR PHONE WILL NEVER SURRENDER.", fill=C_CYAN, font=font_large)

# Pricing Cards
d9.rounded_rectangle([70, 340, 920, 760], radius=16, fill=(13, 22, 40), outline=C_CYAN, width=2)
d9.text((110, 380), "PRO LIFETIME LICENSE - $25", fill=C_CYAN, font=font_title)
d9.text((110, 430), "ONE-TIME PAYMENT • ZERO SUBSCRIPTIONS", fill=C_GRAY, font=font_badge)
d9.text((110, 480), "✓ 1 Android Device Protected Forever", fill=C_WHITE, font=font_pill)
d9.text((110, 530), "✓ Knox-Grade Device Owner Provisioning", fill=C_WHITE, font=font_pill)
d9.text((110, 580), "✓ All 94 Telegram C2 Commands & 7 Hubs", fill=C_WHITE, font=font_pill)
d9.text((110, 630), "✓ Full Cyber Defense Suite (/usb_lock, /a11y_shield)", fill=C_WHITE, font=font_pill)
d9.text((110, 680), "✓ Air-Gapped Cellular SMS Fallback with TOTP", fill=C_WHITE, font=font_pill)

d9.rounded_rectangle([980, 340, 1850, 760], radius=16, fill=(13, 22, 40), outline=C_EMERALD, width=2)
d9.text((1020, 380), "ENTERPRISE FLEET - $99", fill=C_EMERALD, font=font_title)
d9.text((1020, 430), "5 DEVICES PACK • CORPORATE & FAMILY", fill=C_GRAY, font=font_badge)
d9.text((1020, 480), "✓ 5x Pro Lifetime Device Licenses", fill=C_WHITE, font=font_pill)
d9.text((1020, 530), "✓ Dedicated Private Sovereign Relay Server Node", fill=C_WHITE, font=font_pill)
d9.text((1020, 580), "✓ Zero-Knowledge Centralized Fleet Telemetry", fill=C_WHITE, font=font_pill)
d9.text((1020, 630), "✓ Priority WhatsApp & Telegram VIP Hotline", fill=C_WHITE, font=font_pill)
d9.text((1020, 680), "✓ 7-Day 100% Money-Back Guarantee", fill=C_WHITE, font=font_pill)

# Big CTA Ribbon
d9.rounded_rectangle([70, 790, 1850, 930], radius=16, fill=(6, 182, 212), outline=C_WHITE, width=2)
d9.text((110, 830), "GET PASA SENTINEL NOW: pasa.izhaanintellect.fun", fill=(5, 10, 20), font=font_large)
d9.text((1300, 845), "INSTANT APK DOWNLOAD", fill=(5, 10, 20), font=font_title)

img9.save(os.path.join(OUT_DIR, "k_slide9.png"))

print("All 9 Kinetic Slides Rendered Successfully!")
