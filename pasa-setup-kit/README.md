# PASA Sentinel — Windows Device Owner Setup Kit

**Version:** Compatible with PASA Sentinel v3.0.0 and above  
**Platform:** Windows 10 / Windows 11  
**Requirements:** USB cable, internet connection (first run only)

---

## ⚡ Quick Start

1. Extract this ZIP to any folder on your Windows PC
2. Double-click **`PASA Device Owner Setup.bat`**
3. Follow the on-screen instructions — the wizard handles everything

---

## 📋 Table of Contents

1. [What is Device Owner?](#what-is-device-owner)
2. [Why Accounts Must Be Removed](#why-accounts-must-be-removed)
3. [Step-by-Step Process](#step-by-step-process)
4. [Account Removal — Detailed Guide](#account-removal-guide)
5. [After Setup](#after-setup)
6. [Terms & Conditions](#terms--conditions)
7. [Troubleshooting](#troubleshooting)

---

## 🔐 What is Device Owner?

Android's **Device Owner** is the highest privilege level available on any Android device — higher than root in terms of policy enforcement. It is the same technology used by corporate IT departments worldwide (MDM: Mobile Device Management) to manage employee phones.

When PASA Sentinel is granted Device Owner status, it gains the following capabilities that are **impossible to achieve without it**:

| Capability | Without Device Owner | With Device Owner |
|---|---|---|
| Anti-uninstall protection | ❌ Any thief can uninstall | ✅ App cannot be uninstalled |
| Remote screen lock | ❌ Requires OS lock | ✅ Instant hardware-level lock |
| Camera/mic hardware lockout | ❌ Not possible | ✅ Complete hardware disablement |
| Kiosk / Lost Mode | ❌ Not possible | ✅ Full screen takeover |
| Remote password reset | ❌ Not possible | ✅ Via hardware escrow tokens |
| Anti-factory-reset | ❌ Thief can factory reset | ✅ Blocked at kernel level |
| USB data pin disablement | ❌ Not possible | ✅ Blocks forensic extraction |
| Silent app uninstall/hide | ❌ Not possible | ✅ Hide apps without deletion |

**In short:** Without Device Owner, PASA Sentinel is a monitoring app. With Device Owner, it becomes a sovereign hardware-level security system.

---

## ❗ Why Accounts Must Be Removed

This is the most important section to understand. **Android enforces a strict security rule:**

> *"Device Owner can only be provisioned on a device that has no user accounts."*

This rule is **built into the Android operating system by Google** — it is not a limitation of PASA Sentinel or this setup kit. It exists for the following reasons:

### 🏛️ The Reason Behind Google's Rule

When a phone has Google accounts signed in, those accounts belong to real people. If an app could silently become Device Owner without the accounts being removed first, a malicious actor could:

1. Borrow someone's phone for a moment
2. Secretly run a command that makes a malicious app the Device Owner
3. The victim would now have an app with permanent, unremovable control of their phone

To prevent this attack vector, Google requires that **you must be in control of the phone** (no accounts = fresh state = you're setting this up intentionally) before Device Owner can be granted.

### 🔬 What Happens Technically

When you run `dpm set-device-owner`, the Android system calls `DevicePolicyManagerService.setDeviceOwnerLocked()`, which internally checks:

```
if (hasUserSetupCompleted() && getActiveAccounts().size() > 0) {
    throw new IllegalStateException(
        "Not allowed to set device owner because there are already accounts on the device"
    );
}
```

This check is at the **OS kernel level** — it cannot be bypassed without rooting the phone.

### ✅ Why It's Completely Safe to Remove Your Accounts

Removing your Google/Samsung account from your phone **does NOT**:
- ❌ Delete your Gmail emails
- ❌ Delete your Google Photos
- ❌ Delete your contacts (if backed up to Google)
- ❌ Delete your Google Drive files
- ❌ Affect your Google account password
- ❌ Remove apps that were installed from the Play Store

All your data lives **in Google's servers**, not on your phone. Removing the account simply signs you out of the device — exactly like signing out of a browser.

After Device Owner is set up (takes ~30 seconds), you can immediately **sign back in** to your Google account. Your apps, photos, contacts, and data will all sync back automatically.

### 📱 Which Accounts Must Be Removed

| Account Type | Must Remove? | Notes |
|---|---|---|
| **Google Account** (Gmail) | ✅ Yes | Primary requirement |
| **Samsung Account** | ✅ Yes | On Samsung phones |
| **Xiaomi / Mi Account** | ✅ Yes | On Xiaomi phones |
| **Work / Corporate Profile** | ✅ Yes | Must remove work profile |
| **WhatsApp / Social Apps** | ❌ No | App accounts are fine |
| **System sync adapters** | ❌ No | Internal, not user accounts |

---

## 🪄 Step-by-Step Process

The wizard handles all of this automatically:

```
┌─ STEP 1 ─────────────────────────────────────────────────┐
│  ADB Setup                                                 │
│  Checks for Android Debug Bridge on your PC.              │
│  If missing, downloads automatically from Google (~8 MB). │
└────────────────────────────────────────────────────────────┘

┌─ STEP 2 ─────────────────────────────────────────────────┐
│  Connect Phone                                             │
│  Guides you to enable USB Debugging on your phone.        │
│  Waits for you to connect and authorize the connection.   │
└────────────────────────────────────────────────────────────┘

┌─ STEP 3 ─────────────────────────────────────────────────┐
│  Remove Accounts (Auto-Assisted)                           │
│  Detects all accounts on the phone.                        │
│  Automatically opens each account's removal screen —      │
│  you just tap "Remove Account" for each one.              │
└────────────────────────────────────────────────────────────┘

┌─ STEP 4 ─────────────────────────────────────────────────┐
│  Install PASA Sentinel                                     │
│  Checks if already installed. If installed & up-to-date,  │
│  skips download entirely. Otherwise downloads latest       │
│  version from server with SHA-256 integrity check.        │
└────────────────────────────────────────────────────────────┘

┌─ STEP 5 ─────────────────────────────────────────────────┐
│  Grant Device Owner                                        │
│  Runs the Device Owner provisioning command.               │
│  Shows live success/failure with detailed error messages.  │
└────────────────────────────────────────────────────────────┘

┌─ STEP 6 ─────────────────────────────────────────────────┐
│  Complete!                                                 │
│  Verification summary. Instructions for next steps.       │
│  You can now add your Google account back.                 │
└────────────────────────────────────────────────────────────┘
```

---

## 📖 Account Removal Guide

### Google Account (All Android phones)

1. Open **Settings**
2. Tap **Accounts** (or *Users & Accounts*, or *Passwords & Accounts*)
3. Tap **Google**
4. Tap your email address
5. Tap the **⋮ menu** (top right) → **Remove account**
6. Confirm with **Remove account**

> ⚠️ The wizard will automatically navigate your phone to this exact screen. You only need to tap "Remove account."

### Samsung Account (Samsung phones only)

1. Open **Settings**
2. Tap **Samsung Account** (at the top)
3. Scroll down → tap **Sign out** (or **Remove account**)
4. Enter your Samsung password to confirm

### Xiaomi / Mi Account (Xiaomi / Redmi phones)

1. Open **Settings**
2. Tap **Mi Account** (at the top)
3. Scroll down → tap **Sign out of Mi Account**

### Work Profile / Corporate Account

1. Open **Settings**
2. Tap **Accounts** → **Work**
3. Tap **Remove work profile**
4. Confirm removal

---

## ✅ After Setup

Once Device Owner is active, you should:

1. **Open PASA Sentinel** on your phone
2. Complete the initial setup wizard (enter your Telegram Bot Token)
3. **Add your Google Account back** — Settings → Accounts → Add Account → Google
4. Type `/status` in your Telegram bot to confirm the device is online
5. Type `/selftest` to verify all security systems are operational

---

## ⚖️ Terms & Conditions

**By downloading and using this Setup Kit, you agree to the following:**

### 1. Authorized Use Only
This setup kit is provided exclusively for use by **the legitimate owner** of the Android device being configured. You must own or have explicit written authorization from the owner of the device before running this setup kit.

### 2. Consent Requirement
Device Owner provisioning grants PASA Sentinel permanent, system-level administrative control over the device. **Both the device owner and the setup operator must fully understand and consent** to this before proceeding.

### 3. Prohibited Uses
This setup kit **must NOT** be used to:
- Configure Device Owner on a device you do not own or have authorization for
- Surveil, monitor, or control another person's device without their knowledge and consent
- Circumvent any device security measures for unauthorized access
- Any illegal purpose under applicable local, national, or international law

### 4. Privacy & Data
- This kit downloads ADB tools from Google's official servers (`dl.google.com`)
- This kit downloads PASA Sentinel APK from `pasa.izhaanintellect.fun`
- **No personal data from your phone is transmitted to any server** during setup
- ADB commands run locally between your PC and your phone via USB only

### 5. Responsibility
- The user assumes full responsibility for ensuring the setup is performed on an authorized device
- Izhaan Intellect is not liable for any misuse of this setup kit or PASA Sentinel
- Account removal is a standard Android operation and does not delete cloud data

### 6. Reversibility
Device Owner can be removed by performing a **Factory Reset** on the phone. All PASA Sentinel Device Owner protections will be deactivated upon factory reset.

---

## 🔧 Troubleshooting

### "Not allowed to set device owner because there are already accounts"
→ One or more accounts are still signed in. Go to Settings → Accounts and remove all accounts, then try again.

### "Active admin already set for another package"
→ Another app is already the Device Owner. You must Factory Reset the phone before PASA Sentinel can be provisioned.

### Phone not detected after connecting USB
→ Make sure USB Debugging is enabled in Developer Options. When connecting, select "File Transfer" (MTP) mode on the phone if prompted.

### "Allow USB Debugging?" dialog not appearing
→ Go to Settings → Developer Options → Revoke USB Debugging Authorizations, then reconnect.

### ADB download fails
→ Check your internet connection. If behind a corporate firewall, manually download platform-tools from `https://developer.android.com/tools/releases/platform-tools` and place `adb.exe`, `AdbWinApi.dll`, and `AdbWinUsbApi.dll` in the `adb/` subfolder.

---

## 📞 Support

- **Website:** https://pasa.izhaanintellect.fun
- **Telegram Bot:** @Pas_agent_bot
- **Email:** Support available through website

---

*PASA Sentinel © 2024–2026 Izhaan Intellect. All rights reserved.*  
*This setup kit is provided as a convenience tool. Use responsibly and legally.*
