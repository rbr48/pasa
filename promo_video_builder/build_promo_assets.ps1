# PASA Sentinel - Full HD 1080p MP4 Video Generator for Facebook & YouTube
# Generates 7 high-impact cyber defense graphic slides, voiceover narration, and renders the master MP4.

param(
    [string]$OutputDir = "E:\Projects\PrivateApp\promo_video_builder\output"
)

Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.Speech

if (!(Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
}

$W = 1920
$H = 1080

# Colors
$cBg = [System.Drawing.Color]::FromArgb(8, 12, 22)
$cCyan = [System.Drawing.Color]::FromArgb(6, 182, 212)
$cRose = [System.Drawing.Color]::FromArgb(244, 63, 94)
$cEmerald = [System.Drawing.Color]::FromArgb(16, 185, 129)
$cAmber = [System.Drawing.Color]::FromArgb(245, 158, 11)
$cWhite = [System.Drawing.Color]::FromArgb(255, 255, 255)
$cGray = [System.Drawing.Color]::FromArgb(148, 163, 184)
$cDarkCard = [System.Drawing.Color]::FromArgb(13, 20, 36)
$cBorder = [System.Drawing.Color]::FromArgb(28, 44, 72)
$cGrid = [System.Drawing.Color]::FromArgb(18, 28, 48)

$cBadgeRedBg = [System.Drawing.Color]::FromArgb(38, 12, 20)
$cBadgeCyanBg = [System.Drawing.Color]::FromArgb(8, 32, 44)
$cBadgeGreenBg = [System.Drawing.Color]::FromArgb(8, 36, 24)
$cBadgeAmberBg = [System.Drawing.Color]::FromArgb(36, 24, 8)

# Brushes
$bCyan = New-Object System.Drawing.SolidBrush($cCyan)
$bRose = New-Object System.Drawing.SolidBrush($cRose)
$bEmerald = New-Object System.Drawing.SolidBrush($cEmerald)
$bAmber = New-Object System.Drawing.SolidBrush($cAmber)
$bWhite = New-Object System.Drawing.SolidBrush($cWhite)
$bGray = New-Object System.Drawing.SolidBrush($cGray)

# Fonts
$fontBrand = New-Object System.Drawing.Font("Arial", 16, [System.Drawing.FontStyle]::Bold)
$fontBadge = New-Object System.Drawing.Font("Arial", 14, [System.Drawing.FontStyle]::Bold)
$fontH1 = New-Object System.Drawing.Font("Arial", 44, [System.Drawing.FontStyle]::Bold)
$fontSub = New-Object System.Drawing.Font("Arial", 22, [System.Drawing.FontStyle]::Regular)
$fontCardTitle = New-Object System.Drawing.Font("Arial", 20, [System.Drawing.FontStyle]::Bold)
$fontCardBody = New-Object System.Drawing.Font("Arial", 16, [System.Drawing.FontStyle]::Regular)
$fontFooter = New-Object System.Drawing.Font("Arial", 15, [System.Drawing.FontStyle]::Regular)

function Draw-Card {
    param($Graphics, [int]$X, [int]$Y, [int]$Width, [int]$Height, [System.Drawing.Color]$BgColor, [System.Drawing.Color]$BorderColor, [int]$BorderWidth = 2)
    $rect = New-Object System.Drawing.Rectangle $X, $Y, $Width, $Height
    $brush = New-Object System.Drawing.SolidBrush $BgColor
    $pen = New-Object System.Drawing.Pen $BorderColor, $BorderWidth
    $Graphics.FillRectangle($brush, $rect)
    $Graphics.DrawRectangle($pen, $rect)
    $brush.Dispose()
    $pen.Dispose()
}

function Draw-Grid {
    param($Graphics)
    $gridPen = New-Object System.Drawing.Pen $cGrid, 1
    for ($x = 0; $x -lt $W; $x += 80) {
        $Graphics.DrawLine($gridPen, $x, 0, $x, $H)
    }
    for ($y = 0; $y -lt $H; $y += 80) {
        $Graphics.DrawLine($gridPen, 0, $y, $W, $y)
    }
    $gridPen.Dispose()

    $bracketPen = New-Object System.Drawing.Pen $cCyan, 2
    $Graphics.DrawLine($bracketPen, 50, 50, 90, 50)
    $Graphics.DrawLine($bracketPen, 50, 50, 50, 90)
    $Graphics.DrawLine($bracketPen, $W - 50, 50, $W - 90, 50)
    $Graphics.DrawLine($bracketPen, $W - 50, 50, $W - 50, 90)
    $Graphics.DrawLine($bracketPen, 50, $H - 50, 90, $H - 50)
    $Graphics.DrawLine($bracketPen, 50, $H - 50, 50, $H - 90)
    $Graphics.DrawLine($bracketPen, $W - 50, $H - 50, $W - 90, $H - 50)
    $Graphics.DrawLine($bracketPen, $W - 50, $H - 50, $W - 50, $H - 90)
    $bracketPen.Dispose()
}

Write-Host "Rendering 7 High-Impact 1080p Video Slides..." -ForegroundColor Cyan

# ── SCENE 1: THE THREAT LANDSCAPE ──
$bmp1 = New-Object System.Drawing.Bitmap $W, $H
$g1 = [System.Drawing.Graphics]::FromImage($bmp1)
$g1.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g1.Clear($cBg)
Draw-Grid -Graphics $g1

$g1.DrawString("PASA SENTINEL // SOVEREIGN MOBILE DEFENSE", $fontBrand, $bCyan, 70, 70)
$g1.DrawString("THREAT STATUS: CRITICAL EXPOSURE", $fontBrand, $bRose, 1380, 70)

Draw-Card -Graphics $g1 -X 70 -Y 140 -Width 480 -Height 40 -BgColor $cBadgeRedBg -BorderColor $cRose
$g1.DrawString("ACT I // THE INVISIBLE THREAT VECTORS", $fontBadge, $bRose, 85, 150)

$g1.DrawString("YOUR PHONE WILL FAIL IN 10 SECONDS.", $fontH1, $bWhite, 70, 210)
$g1.DrawString("When standard trackers go blind, professional thieves and mobile cyber malware take total control.", $fontSub, $bGray, 70, 290)

Draw-Card -Graphics $g1 -X 70 -Y 380 -Width 410 -Height 480 -BgColor $cDarkCard -BorderColor $cBorder
$g1.DrawString("STREET SNATCH", $fontCardTitle, $bRose, 100, 420)
$g1.DrawString("Physical 2.65G snatch vectors instantly followed by hard power-down or Faraday bag isolation. Standard Google tracking is disconnected within 8 seconds.", $fontCardBody, $bGray, (New-Object System.Drawing.RectangleF 100, 480, 350, 300))

Draw-Card -Graphics $g1 -X 520 -Y 380 -Width 410 -Height 480 -BgColor $cDarkCard -BorderColor $cBorder
$g1.DrawString("CELLEBRITE CABLE", $fontCardTitle, $bRose, 550, 420)
$g1.DrawString("Hardware forensic extraction boxes (Cellebrite, GrayKey) plug directly into physical USB ports to rip private chats, photos, crypto keys, and OS credentials.", $fontCardBody, $bGray, (New-Object System.Drawing.RectangleF 550, 480, 350, 300))

Draw-Card -Graphics $g1 -X 970 -Y 380 -Width 410 -Height 480 -BgColor $cDarkCard -BorderColor $cBorder
$g1.DrawString("BANKING TROJANS", $fontCardTitle, $bRose, 1000, 420)
$g1.DrawString("Android accessibility service hijacking injects transparent overlay windows over banking apps, stealing PINs, 2FA OTP codes, and personal financial sessions.", $fontCardBody, $bGray, (New-Object System.Drawing.RectangleF 1000, 480, 350, 300))

Draw-Card -Graphics $g1 -X 1420 -Y 380 -Width 410 -Height 480 -BgColor $cDarkCard -BorderColor $cBorder
$g1.DrawString("IMSI CATCHERS (2G)", $fontCardTitle, $bRose, 1450, 420)
$g1.DrawString("Rogue cellular base stations (Stingrays) force modems down to unencrypted 2G GSM frequencies to intercept SMS OTPs and pinpoint physical locations.", $fontCardBody, $bGray, (New-Object System.Drawing.RectangleF 1450, 480, 350, 300))

$g1.DrawString("SECURITY SPEC: ANDROID 8.0 - 16 (API 26-36) // ZERO GOOGLE PLAY DEPENDENCIES", $fontFooter, $bGray, 70, 970)
$bmp1.Save("$OutputDir\slide1.png", [System.Drawing.Imaging.ImageFormat]::Png)
$g1.Dispose()
$bmp1.Dispose()

# ── SCENE 2: KNOX-GRADE DEVICE OWNER SUPERVISOR ──
$bmp2 = New-Object System.Drawing.Bitmap $W, $H
$g2 = [System.Drawing.Graphics]::FromImage($bmp2)
$g2.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g2.Clear($cBg)
Draw-Grid -Graphics $g2

$g2.DrawString("PASA SENTINEL // SOVEREIGN MOBILE DEFENSE", $fontBrand, $bCyan, 70, 70)
$g2.DrawString("DEFENSE STATUS: SUPERVISOR ACTIVE", $fontBrand, $bEmerald, 1370, 70)

Draw-Card -Graphics $g2 -X 70 -Y 140 -Width 510 -Height 40 -BgColor $cBadgeCyanBg -BorderColor $cCyan
$g2.DrawString("ACT II // KNOX-GRADE DEVICE OWNER PERMANENCE", $fontBadge, $bCyan, 85, 150)

$g2.DrawString("OPERATING AS THE SYSTEM SUPERVISOR.", $fontH1, $bWhite, 70, 210)
$g2.DrawString("PASA Sentinel provisions as Android Enterprise Device Owner. The OS itself disables uninstallation.", $fontSub, $bGray, 70, 290)

Draw-Card -Graphics $g2 -X 70 -Y 380 -Width 560 -Height 480 -BgColor $cDarkCard -BorderColor $cBorder
$g2.DrawString("IMMUTABLE DEFENSE", $fontCardTitle, $bCyan, 110, 420)
$g2.DrawString("The 'Uninstall' and 'Force Stop' buttons are physically greyed out inside Android Settings. Even if a thief holds the app icon or goes into Application Manager, the OS blocks removal as 'Managed by Organization'.", $fontCardBody, $bGray, (New-Object System.Drawing.RectangleF 110, 480, 480, 300))

Draw-Card -Graphics $g2 -X 670 -Y 380 -Width 560 -Height 480 -BgColor $cDarkCard -BorderColor $cBorder
$g2.DrawString("TAMPER LOCKDOWN", $fontCardTitle, $bCyan, 710, 420)
$g2.DrawString("Safe Boot is disabled (`DISALLOW_SAFE_BOOT`). Factory reset is permanently blocked (`DISALLOW_FACTORY_RESET`). Status bar and notification shade pull-downs are completely neutralized in Lost Mode.", $fontCardBody, $bGray, (New-Object System.Drawing.RectangleF 710, 480, 480, 300))

Draw-Card -Graphics $g2 -X 1270 -Y 380 -Width 560 -Height 480 -BgColor $cDarkCard -BorderColor $cBorder
$g2.DrawString("ESCROW TOKEN RECOVERY", $fontCardTitle, $bCyan, 1310, 420)
$g2.DrawString("Cryptographic Hardware Escrow Tokens reset lockscreen PINs remotely without factory wipes on Android 8 through 16. Full command authority is preserved even when biometric sensors are coerced.", $fontCardBody, $bGray, (New-Object System.Drawing.RectangleF 1310, 480, 480, 300))

$g2.DrawString("ENTERPRISE DPM: LOCK_TASK_FEATURE_NONE // ZERO-TRUST CLIENT-SIDE PIN AUTH", $fontFooter, $bGray, 70, 970)
$bmp2.Save("$OutputDir\slide2.png", [System.Drawing.Imaging.ImageFormat]::Png)
$g2.Dispose()
$bmp2.Dispose()

# ── SCENE 3: THE CYBER CRIME DEFENSE SUITE ──
$bmp3 = New-Object System.Drawing.Bitmap $W, $H
$g3 = [System.Drawing.Graphics]::FromImage($bmp3)
$g3.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g3.Clear($cBg)
Draw-Grid -Graphics $g3

$g3.DrawString("PASA SENTINEL // SOVEREIGN MOBILE DEFENSE", $fontBrand, $bCyan, 70, 70)
$g3.DrawString("TACTICAL SUITE: ACTIVE COUNTERMEASURES", $fontBrand, $bAmber, 1300, 70)

Draw-Card -Graphics $g3 -X 70 -Y 140 -Width 480 -Height 40 -BgColor $cBadgeAmberBg -BorderColor $cAmber
$g3.DrawString("ACT III // MOBILE CYBER CRIME DEFENSE SUITE", $fontBadge, $bAmber, 85, 150)

$g3.DrawString("FIGHTING MOBILE CYBER CRIME HEAD-ON.", $fontH1, $bWhite, 70, 210)
$g3.DrawString("Purpose-built defenses to neutralize hardware cable attacks, banking overlays, rogue towers, and crypto theft.", $fontSub, $bGray, 70, 290)

Draw-Card -Graphics $g3 -X 70 -Y 380 -Width 410 -Height 480 -BgColor $cDarkCard -BorderColor $cRose
$g3.DrawString("CELLEBRITE BLOCKER", $fontCardTitle, $bRose, 100, 420)
$g3.DrawString("Command: /usb_lock & /usb_autolock`n`nAndroid 12+ dpm.setUsbDataSignalingEnabled(false) physically severs USB data pins at the kernel level. Forensic extraction cables fail instantly while charging continues.", $fontCardBody, $bGray, (New-Object System.Drawing.RectangleF 100, 470, 350, 320))

Draw-Card -Graphics $g3 -X 520 -Y 380 -Width 410 -Height 480 -BgColor $cDarkCard -BorderColor $cCyan
$g3.DrawString("BANKING TROJAN SHIELD", $fontCardTitle, $bCyan, 550, 420)
$g3.DrawString("Command: /a11y_shield`n`nContinuously audits Accessibility service window trees for unauthorized screen overlays, auto-clicking malware, and keyloggers. Malicious overlay windows are immediately stripped.", $fontCardBody, $bGray, (New-Object System.Drawing.RectangleF 550, 470, 350, 320))

Draw-Card -Graphics $g3 -X 970 -Y 380 -Width 410 -Height 480 -BgColor $cDarkCard -BorderColor $cAmber
$g3.DrawString("ANTI-2G / STINGRAY", $fontCardTitle, $bAmber, 1000, 420)
$g3.DrawString("Command: /anti_2g`n`nRestricts baseband modem from downgrading to obsolete, unencrypted 2G GSM cellular frequencies used by IMSI-catcher wiretaps and rogue portable cell towers.", $fontCardBody, $bGray, (New-Object System.Drawing.RectangleF 1000, 470, 350, 320))

Draw-Card -Graphics $g3 -X 1420 -Y 380 -Width 410 -Height 480 -BgColor $cDarkCard -BorderColor $cEmerald
$g3.DrawString("CRYPTO CLIPPER TRAP", $fontCardTitle, $bEmerald, 1450, 420)
$g3.DrawString("Command: /clipper_guard`n`nIntercepts background clipboard tampering. If malware swaps your Bitcoin, Ethereum, Solana, or TON address with an attacker wallet, PASA neutralizes the clip and alerts you.", $fontCardBody, $bGray, (New-Object System.Drawing.RectangleF 1450, 470, 350, 320))

$g3.DrawString("ADDITIONAL DEFENSES: CANARY RANSOMWARE TRIPWIRE (/canary_guard) // APP INSTALL LOCKDOWN (/app_install_lock)", $fontFooter, $bGray, 70, 970)
$bmp3.Save("$OutputDir\slide3.png", [System.Drawing.Imaging.ImageFormat]::Png)
$g3.Dispose()
$bmp3.Dispose()

# ── SCENE 4: KINETIC TRAPS & FAKE SHUTDOWN DECEPTION ──
$bmp4 = New-Object System.Drawing.Bitmap $W, $H
$g4 = [System.Drawing.Graphics]::FromImage($bmp4)
$g4.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g4.Clear($cBg)
Draw-Grid -Graphics $g4

$g4.DrawString("PASA SENTINEL // SOVEREIGN MOBILE DEFENSE", $fontBrand, $bCyan, 70, 70)
$g4.DrawString("SENSOR ENGINE: 2.65G ACCELEROMETER", $fontBrand, $bEmerald, 1320, 70)

Draw-Card -Graphics $g4 -X 70 -Y 140 -Width 510 -Height 40 -BgColor $cBadgeGreenBg -BorderColor $cEmerald
$g4.DrawString("ACT IV // KINETIC TRAPS & FAKE SHUTDOWN DECEPTION", $fontBadge, $bEmerald, 85, 150)

$g4.DrawString("OUTSMARTING PROFESSIONAL THIEVES.", $fontH1, $bWhite, 70, 210)
$g4.DrawString("Autonomous sensor traps engage before a thief can run. Deception makes them think the phone is off.", $fontSub, $bGray, 70, 290)

Draw-Card -Graphics $g4 -X 70 -Y 380 -Width 560 -Height 480 -BgColor $cDarkCard -BorderColor $cBorder
$g4.DrawString("2.65G KINETIC SNATCH TRAP", $fontCardTitle, $bEmerald, 110, 420)
$g4.DrawString("Continuous vector magnitude monitoring: sqrt(x^2 + y^2 + z^2) > 26.0 m/s^2.`n`nA sudden grab or motorcycle pull immediately triggers Kiosk Lost Mode, captures front-camera mugshots, and broadcasts high-priority emergency alerts with satellite GPS pins.", $fontCardBody, $bGray, (New-Object System.Drawing.RectangleF 110, 480, 480, 300))

Draw-Card -Graphics $g4 -X 670 -Y 380 -Width 560 -Height 480 -BgColor $cDarkCard -BorderColor $cBorder
$g4.DrawString("POWER-MENU INTERCEPTION", $fontCardTitle, $bEmerald, 710, 420)
$g4.DrawString("Command: /fakeshutdown auto on`n`nWhen a thief holds down the physical Power button to shut down the phone, PASA's Accessibility service instantly intercepts the system power dialog, aborting the shutdown attempt before it begins.", $fontCardBody, $bGray, (New-Object System.Drawing.RectangleF 710, 480, 480, 300))

Draw-Card -Graphics $g4 -X 1270 -Y 380 -Width 560 -Height 480 -BgColor $cDarkCard -BorderColor $cBorder
$g4.DrawString("0-NIT COVERT CANVAS", $fontCardTitle, $bEmerald, 1310, 420)
$g4.DrawString("Simulates authentic OEM power-down animation, then drops display brightness to a pitch-black 0-nit canvas.`n`nThe phone appears 100% dead. Every tap on the screen covertly fires front-camera mugshots and sat GPS beacons to your Telegram bot.", $fontCardBody, $bGray, (New-Object System.Drawing.RectangleF 1310, 480, 480, 300))

$g4.DrawString("DECOY DURESS PIN (/duress_pin) // POCKET EXTRACTION SENSOR (/trap pocket on)", $fontFooter, $bGray, 70, 970)
$bmp4.Save("$OutputDir\slide4.png", [System.Drawing.Imaging.ImageFormat]::Png)
$g4.Dispose()
$bmp4.Dispose()

# ── SCENE 5: FORENSICS & COVERT INTELLIGENCE ──
$bmp5 = New-Object System.Drawing.Bitmap $W, $H
$g5 = [System.Drawing.Graphics]::FromImage($bmp5)
$g5.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g5.Clear($cBg)
Draw-Grid -Graphics $g5

$g5.DrawString("PASA SENTINEL // SOVEREIGN MOBILE DEFENSE", $fontBrand, $bCyan, 70, 70)
$g5.DrawString("TELEMETRY: SATELLITE GNSS + RF TOWERS", $fontBrand, $bCyan, 1310, 70)

Draw-Card -Graphics $g5 -X 70 -Y 140 -Width 480 -Height 40 -BgColor $cBadgeCyanBg -BorderColor $cCyan
$g5.DrawString("ACT V // FORENSIC INTELLIGENCE & TELEMETRY", $fontBadge, $bCyan, 85, 150)

$g5.DrawString("UNMASKING CRIMINALS BEFORE THEY DISAPPEAR.", $fontH1, $bWhite, 70, 210)
$g5.DrawString("Headless forensics, dual-SIM cellular tower scans, and automatic foreign phone number traps.", $fontSub, $bGray, 70, 290)

Draw-Card -Graphics $g5 -X 70 -Y 380 -Width 560 -Height 480 -BgColor $cDarkCard -BorderColor $cBorder
$g5.DrawString("ZERO-BLACKOUT HEADLESS CAMERA", $fontCardTitle, $bCyan, 110, 420)
$g5.DrawString("Commands: /snap & /video`n`nRuns CameraX headlessly bound to background service life-cycles. Zero screen flicker, zero preview window, and zero blackouts. Captures high-res perpetrator mugshots while the owner retains full covert awareness.", $fontCardBody, $bGray, (New-Object System.Drawing.RectangleF 110, 480, 480, 300))

Draw-Card -Graphics $g5 -X 670 -Y 380 -Width 560 -Height 480 -BgColor $cDarkCard -BorderColor $cBorder
$g5.DrawString("DUAL-SIM TOWER TRIANGULATION", $fontCardTitle, $bCyan, 710, 420)
$g5.DrawString("Command: /tower`n`nWhen GPS satellite signals are blocked inside chop shops, basements, or high-rise buildings, PASA scans active LTE/5G NR/GSM cell identities (MCC, MNC, LAC, CID, dBm) across both SIM slots for indoor localization.", $fontCardBody, $bGray, (New-Object System.Drawing.RectangleF 710, 480, 480, 300))

Draw-Card -Graphics $g5 -X 1270 -Y 380 -Width 560 -Height 480 -BgColor $cDarkCard -BorderColor $cBorder
$g5.DrawString("SIM EJECT & FOREIGN NUMBER TRAP", $fontCardTitle, $bCyan, 1310, 420)
$g5.DrawString("Command: /sim_lock & /sim_tray_lock`n`nIf the physical SIM tray is removed, phone enters Knox Kiosk lock. When the thief inserts their own SIM card, PASA silently fires an emergency SMS with GPS pins to your backup phone, exposing the thief's phone number via caller ID.", $fontCardBody, $bGray, (New-Object System.Drawing.RectangleF 1310, 480, 480, 300))

$g5.DrawString("DEVICE FORENSICS: /call_log, /sms_log, /contacts, /history, /gallery_latest EXPORTS", $fontFooter, $bGray, 70, 970)
$bmp5.Save("$OutputDir\slide5.png", [System.Drawing.Imaging.ImageFormat]::Png)
$g5.Dispose()
$bmp5.Dispose()

# ── SCENE 6: ZERO-STORAGE GUARANTEE & AIR-GAPPED SMS C2 ──
$bmp6 = New-Object System.Drawing.Bitmap $W, $H
$g6 = [System.Drawing.Graphics]::FromImage($bmp6)
$g6.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g6.Clear($cBg)
Draw-Grid -Graphics $g6

$g6.DrawString("PASA SENTINEL // SOVEREIGN MOBILE DEFENSE", $fontBrand, $bCyan, 70, 70)
$g6.DrawString("PRIVACY SPEC: 100% ZERO CLOUD TRAILS", $fontBrand, $bEmerald, 1330, 70)

Draw-Card -Graphics $g6 -X 70 -Y 140 -Width 480 -Height 40 -BgColor $cBadgeGreenBg -BorderColor $cEmerald
$g6.DrawString("ACT VI // SOVEREIGN PRIVACY & AIR-GAPPED C2", $fontBadge, $bEmerald, 85, 150)

$g6.DrawString("100% PRIVATE. ZERO CLOUD FOOTPRINT.", $fontH1, $bWhite, 70, 210)
$g6.DrawString("Surveillance evidence is never saved on servers. Full command authority even without internet.", $fontSub, $bGray, 70, 290)

Draw-Card -Graphics $g6 -X 70 -Y 380 -Width 860 -Height 480 -BgColor $cDarkCard -BorderColor $cBorder
$g6.DrawString("STRATEGY 1: ZERO-STORAGE GUARANTEE", $fontCardTitle, $bEmerald, 110, 420)
$g6.DrawString("Zero photos, zero GPS tracks, and zero audio recordings are stored on VPS disk or databases.`n`nPhotos, voice clips, and location pins stream directly to your private Telegram bot and are shredded immediately from device RAM and cache. Your Bot Token and Chat ID never leave your device to any external server.", $fontCardBody, $bGray, (New-Object System.Drawing.RectangleF 110, 480, 780, 300))

Draw-Card -Graphics $g6 -X 970 -Y 380 -Width 880 -Height 480 -BgColor $cDarkCard -BorderColor $cBorder
$g6.DrawString("AIR-GAPPED CELLULAR SMS C2", $fontCardTitle, $bEmerald, 1010, 420)
$g6.DrawString("Wi-Fi and mobile data shut off? No problem.`n`nSend encrypted SMS commands authenticated via RFC 6238 TOTP tokens or master PIN:`nPASA <6-digit-TOTP> /locate`nPASA <6-digit-TOTP> /status`nThe device wakes up GNSS hardware and responds via cellular SMS with Google Maps coordinates from any basic phone.", $fontCardBody, $bGray, (New-Object System.Drawing.RectangleF 1010, 480, 800, 300))

$g6.DrawString("CRYPTOGRAPHIC VERIFICATION: ED25519 DIGITAL SIGNATURES // STRONG-BOX HARDWARE KEYSTORE", $fontFooter, $bGray, 70, 970)
$bmp6.Save("$OutputDir\slide6.png", [System.Drawing.Imaging.ImageFormat]::Png)
$g6.Dispose()
$bmp6.Dispose()

# ── SCENE 7: COMMERCIAL ACCESS & LIFETIME LICENSE ──
$bmp7 = New-Object System.Drawing.Bitmap $W, $H
$g7 = [System.Drawing.Graphics]::FromImage($bmp7)
$g7.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g7.Clear($cBg)
Draw-Grid -Graphics $g7

$g7.DrawString("PASA SENTINEL // SOVEREIGN MOBILE DEFENSE", $fontBrand, $bCyan, 70, 70)
$g7.DrawString("OFFICIAL RELEASE: v3.7.15 (BUILD 79)", $fontBrand, $bCyan, 1350, 70)

Draw-Card -Graphics $g7 -X 70 -Y 140 -Width 480 -Height 40 -BgColor $cBadgeCyanBg -BorderColor $cCyan
$g7.DrawString("OFFICIAL ACCESS // SOVEREIGN LICENSING", $fontBadge, $bCyan, 85, 150)

$g7.DrawString("YOUR PHONE WILL NEVER SURRENDER.", $fontH1, $bWhite, 70, 210)
$g7.DrawString("Uncompromising physical anti-theft and mobile cyber defense. Available now.", $fontSub, $bGray, 70, 290)

Draw-Card -Graphics $g7 -X 70 -Y 380 -Width 860 -Height 450 -BgColor $cDarkCard -BorderColor $cCyan
$g7.DrawString("PRO LIFETIME LICENSE - $25", $fontCardTitle, $bCyan, 110, 420)
$g7.DrawString("• 1 Android Device Protected Forever (One-Time Payment)`n• Knox-Grade Device Owner Provisioning & Uninstallation Lockout`n• All 94 Telegram C2 Commands & 7 Interactive Hubs`n• Full Cyber Crime Suite (/usb_lock, /a11y_shield, /anti_2g, /clipper_guard)`n• Air-Gapped Cellular SMS Fallback & TOTP Authentication`n• Zero Cloud Media Storage Guarantee & Lifetime Free OTA Updates", $fontCardBody, $bWhite, (New-Object System.Drawing.RectangleF 110, 480, 780, 300))

Draw-Card -Graphics $g7 -X 970 -Y 380 -Width 880 -Height 450 -BgColor $cDarkCard -BorderColor $cEmerald
$g7.DrawString("ENTERPRISE FLEET - $99", $fontCardTitle, $bEmerald, 1010, 420)
$g7.DrawString("• 5x Pro Lifetime Device Licenses for Family or Corporate Fleets`n• Dedicated Private Sovereign Relay Server Node`n• Zero-Knowledge Centralized Fleet Telemetry`n• Priority WhatsApp & Telegram VIP Engineering Hotline`n• 1-on-1 Remote Device Owner Onboarding Concierge Support`n• 7-Day 100% Money-Back Guarantee", $fontCardBody, $bWhite, (New-Object System.Drawing.RectangleF 1010, 480, 800, 300))

$cBottomBanner = [System.Drawing.Color]::FromArgb(20, 35, 60)
Draw-Card -Graphics $g7 -X 70 -Y 860 -Width 1780 -Height 110 -BgColor $cBottomBanner -BorderColor $cCyan
$g7.DrawString("CLAIM YOUR LICENSE NOW: pasa.izhaanintellect.fun", $fontCardTitle, $bCyan, 110, 895)
$g7.DrawString("PAY VIA: Binance Pay (Crypto) | bKash (Bangladesh) | LemonSqueezy (Cards/PayPal)", $fontCardBody, $bWhite, 880, 900)

$bmp7.Save("$OutputDir\slide7.png", [System.Drawing.Imaging.ImageFormat]::Png)
$g7.Dispose()
$bmp7.Dispose()

Write-Host "All 7 Graphic Slides Generated Successfully!" -ForegroundColor Green

# ── NARRATION SYNTHESIS (VOICEOVER AUDIO) ──
Write-Host "Synthesizing Professional Voiceover Tracks..." -ForegroundColor Cyan

$narrations = @(
    @{
        Id = "1"
        Text = "When your phone is stolen, standard trackers fail in ten seconds. Thieves disable Wi-Fi, toggle Airplane mode, and plug into forensic extraction cables. Normal security ends here."
    },
    @{
        Id = "2"
        Text = "Meet PASA Sentinel. Operating at the Knox Device Owner supervisor level, Android itself blocks uninstallation, disables Safe Mode boot, and neutralizes factory resets. Your phone physically refuses to surrender."
    },
    @{
        Id = "3"
        Text = "Engineered to fight modern mobile cyber crime. It physically severs USB data pins to block forensic extraction tools like Cellebrite. It strips accessibility overlays from banking trojans, locks down two-G basebands against rogue IMSI catchers, and traps cryptocurrency clippers."
    },
    @{
        Id = "4"
        Text = "Autonomous kinetic sensors detect a snatch in milliseconds, engaging Kiosk Lost Mode. When a thief holds the power button, PASA intercepts the shutdown attempt, dropping the screen into a pitch-black zero-nit covert canvas while shooting mugshots on every touch."
    },
    @{
        Id = "5"
        Text = "Zero-blackout headless cameras capture perpetrator mugshots invisibly. Dual-SIM tower triangulation pinpoints your device indoors without GPS. And if your SIM card is ejected, foreign SIM insertion silently exposes the thief's phone number."
    },
    @{
        Id = "6"
        Text = "Complete sovereign privacy. Under our Zero-Storage guarantee, photos and GPS pins stream direct to your private Telegram bot and are shredded immediately from device RAM. No internet? Dual-SIM air-gapped SMS with TOTP authentication gives you total control from any phone."
    },
    @{
        Id = "7"
        Text = "PASA Sentinel. Uncompromising physical anti-theft and mobile cyber defense. Protect your Android device with a lifetime license today at pasa.izhaanintellect.fun."
    }
)

$synth = New-Object System.Speech.Synthesis.SpeechSynthesizer
$synth.SelectVoice("Microsoft David Desktop")
$synth.Rate = 0

foreach ($n in $narrations) {
    $wavPath = "$OutputDir\audio$($n.Id).wav"
    $synth.SetOutputToWaveFile($wavPath)
    $synth.Speak($n.Text)
    Write-Host "Audio $($n.Id) synthesized." -ForegroundColor Yellow
}
$synth.SetOutputToNull()
$synth.Dispose()

Write-Host "Audio Tracks Complete!" -ForegroundColor Green
