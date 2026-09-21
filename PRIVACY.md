# 🛡️ PASA Sentinel Privacy Policy & Sovereign Guarantee

> **Effective Date:** September 2026  
> **Core Principle:** Absolute Zero Telemetry. 100% Data Sovereignty. Point-to-Point Encryption.

---

## 🔒 1. The Zero-Telemetry Oath

PASA Sentinel was created as a direct counter-response to commercial tracking software (Life360, Prey, Cerberus, Google Find My Device) that monetize user movement patterns, battery histories, and personal identifiers.

* **Zero Google Play Services:** PASA operates completely decoupled from Google Play Services, Firebase Analytics, and AdMob SDKs.
* **Zero Third-Party SDKs:** There are no trackers, no marketing libraries, and no analytics telemetry bundled inside the APK.
* **Zero Centralized Geolocation Tracking:** Our servers never record, store, or log your GPS coordinates. Coordinates are generated ephemerally inside hardware GNSS chip memory only upon your explicit command.

---

## 📡 2. Point-to-Point Tactical C2

All telemetry and commands are exchanged exclusively over user-controlled communication pipelines:

1. **Telegram C2 Terminal (Online Mode):**
   - Remote trigger commands (`/locate`, `/snap`, `/record`, `/fakeshutdown`, `/ring`, `/lock`) and their cryptographic responses are routed directly via HTTPS TLS between your Android device and Telegram’s official servers.
   - At no point do photos, ambient audio recordings, or location coordinates pass through intermediary relay servers.
2. **Cellular SMS Fallback (Air-Gapped Mode):**
   - When mobile data and Wi-Fi are disconnected or jammed, the device processes incoming raw GSM SMS PDUs locally.
   - GPS satellite coordinates are texted back directly from your phone’s SIM to the authorized sender phone number, bypassing the internet entirely.

---

## 🔑 3. Android Permissions Transparency

PASA Sentinel requests elevated operating system permissions solely for defensive, user-authorized operations:

| Permission | Purpose & Scope | Execution Model |
|---|---|---|
| `ACCESS_FINE_LOCATION` / `BACKGROUND` | Queries hardware GNSS receiver for latitude, longitude, and accuracy. | Executed **only** when `/locate` is received or Anti-Snatch is triggered. |
| `CAMERA` | Captures silent front/rear mugshots without waking screen or sounding shutter. | Triggered **only** on `/snap` command or unauthorized theft attempt. |
| `RECORD_AUDIO` | Records ambient room audio for recovery intelligence. | Triggered **only** on remote `/record` command. |
| `RECEIVE_SMS` / `SEND_SMS` | Listens for encrypted TOTP SMS commands and sends back GPS map pin. | Offline failover mode only. |
| `BIND_DEVICE_ADMIN` / `DEVICE_OWNER` | Prevents unauthorized uninstallation, disables Safe Mode escape, and locks screen. | Continuous tamper defense. |

---

## 💰 4. 7-Day Money-Back Guarantee (Binance Pay)

We stand 100% behind our software. Every commercial license purchase (**Pro Lifetime $29 USD / $29 USDT** or **Fleet Enterprise $84 USD / $84 USDT**) is protected by an unconditional **7-day money-back guarantee**.

### Refund Terms:
* **Duration:** 7 full calendar days from the moment your license key is issued.
* **Eligible Reasons:**
  - Phone manufacturer (OEM) aggressive battery killer limits background persistence.
  - Sideloading / ADB setup difficulties.
  - Any dissatisfaction with performance or feature set.
  - Simple change of mind.
* **Refund Method:** **100% full refund in USDT** transferred directly back to your Binance Pay ID / Binance UID within 24 hours. No hidden deductions, no network fees withheld.

### How to Claim:
1. Message our Customer Support on WhatsApp: **[+880 1762-033445](https://wa.me/8801762033445)** or email **support@izhaanintellect.fun**.
2. Provide your **Binance Pay ID / UID** or bKash number and your **Order ID or License Key**.
3. Your refund will be processed back to your account within 24 hours.

---

## 💳 5. Commercial Data Handling

* **Data Collected at Checkout:** Email address, chosen license tier, and Transaction ID / Order ID.
* **Purpose:** Cryptographically generating your signed license key (`PASA-LIFE-XXXX-XXXX`) and processing warranty/guarantee claims.
* **Data Isolation:** Licensing records are maintained in an isolated, encrypted ledger. They are never sold, rented, or linked to device hardware serials (IMEI/IMSI).

---

## ⚖️ 6. Data Deletion & Contact

In accordance with sovereign computing standards and global privacy principles (GDPR / CCPA):
* You may request immediate purging of your licensing email record at any time.
* De-provisioning Device Owner privilege and uninstalling the application immediately wipes all cryptographic tokens, local database caches, and TOTP secrets from your device hardware.

**Operator Inquiries:**  
* WhatsApp Business: [+880 1762-033445](https://wa.me/8801762033445)  
* Email: [support@izhaanintellect.fun](mailto:support@izhaanintellect.fun)
