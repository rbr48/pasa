# 📜 PASA Sentinel — Terms of Service & End-User License Agreement (EULA)

**Effective Date:** September 20, 2026  
**Last Updated:** September 20, 2026  
**Application:** PASA Sentinel (Private Android Security Agent)  
**Provider:** Izhaan Intellect & The PASA Sentinel Security Team ("Provider", "Developer", "We", "Us")  

---

> [!IMPORTANT]
> **READ CAREFULLY BEFORE INSTALLING, ACTIVATING, OR USING THIS SOFTWARE.**  
> By installing, launching, configuring, or activating PASA Sentinel on any hardware device, or by linking it to any Telegram Command & Control (C2) channel or web gateway, you ("User", "Administrator", "Licensee", "You") unconditionally agree to be bound by all the terms, representations, and warranties set forth in this Agreement. If you do not agree to these terms, you must immediately terminate the setup process and permanently uninstall the software.

---

## 1. Warranties of Lawful Ownership & Authorized Deployment

### 1.1 Sole Ownership or Enterprise Authority Warranty
You expressly represent, warrant, and certify under penalty of perjury that:
1. **Lawful Ownership:** You are the sole legal owner of the physical Android device onto which PASA Sentinel is being installed; OR
2. **Authorized Enterprise Custody:** You are a designated IT Administrator acting with documented, explicit corporate authorization to manage an enterprise-owned fleet asset, and all end-users assigned such hardware have provided prior informed written consent to physical security monitoring.

### 1.2 Strict Prohibition Against Stalkerware & Non-Consensual Surveillance
* **Defensive Purpose Only:** PASA Sentinel is engineered, distributed, and licensed **exclusively as a sovereign defensive countermeasure against physical device theft, unauthorized device tampering, and violent extortion.**
* **Zero Tolerance for Unlawful Surveillance:** Under **no circumstances** may PASA Sentinel be deployed covertly onto a spouse's, partner's, child's, employee's, or any third party's personal device without their explicit, informed, and continuous legal consent.
* **Criminal & Civil Penalties:** Unlawful deployment of surveillance or administrative lockout tools constitutes a serious criminal violation of international and domestic laws, including but not limited to the United States Computer Fraud and Abuse Act (18 U.S.C. § 1030), Electronic Communications Privacy Act (18 U.S.C. § 2510), European Union GDPR & ePrivacy Directive, and national cybercrime statutes. Any violation results in immediate revocation of your license and termination of services.

---

## 2. Emergency Countermeasures & Irreversible Data Loss Disclaimers

### 2.1 Remote Wipe, Cryptographic Shredding & Hardware Lockout
PASA Sentinel equips the verified administrator with powerful, hardware-grade countermeasures intended to protect corporate or personal confidentiality in extreme compromise scenarios:
* **Remote Factory Reset (`/wipe`, `/wipe_confirm`):** Irreversibly triggers a low-level physical sanitization of the device storage, destroying all accounts, photos, messages, keys, and operating system state.
* **Cryptographic File Shredding (`/shred`):** Overwrites target directory trees with zero-fill entropy buffers before file deletion, rendering forensic retrieval impossible.
* **Hardware OS Password Overwrite (`/set_os_pin`):** Overwrites the physical Android lockscreen credential via Device Owner cryptographic Escrow Tokens.
* **Knox-Grade Kiosk Lockdown (`/lock`):** Completely suppresses Android UI navigation, status bars, and hardware key handlers.

### 2.2 Disclaimer of Liability for Data Loss
**THE USER ACKNOWLEDGES THAT EMERGENCY ACTIONS ARE DESTRUCTIVE AND IRREVERSIBLE BY DESIGN.**  
The Developer bears **zero legal, financial, or technical liability** for:
1. Inadvertent, erroneous, or accidental execution of `/wipe`, `/shred`, or `/set_os_pin` commands;
2. Loss of irreplaceable personal or business data, digital assets, cryptocurrency wallets, or authentication seeds;
3. Hardware lockouts or requirement of a hardware factory service re-flash due to forgotten master passwords or lost Telegram C2 access;
4. Inability to recover a stolen device due to carrier network drops, physical SIM removal, battery exhaustion, or RF Faraday bags.

---

## 3. Forensic Surveillance, Ambient Telemetry & Wiretap Compliance

### 3.1 Forensic Evidence Collection
When armed, in Lost Mode, or triggered by physical intrusion traps (e.g., snatch detection, power disconnect, SIM ejection, screen touch in Lost Mode, or failed unlock attempts), PASA Sentinel autonomously captures:
* Satellite GNSS (GPS) coordinates, altitude, speed, and geofence trajectory;
* Silent front and rear camera mugshots of individuals physically interacting with the device;
* Ambient microphone audio recordings (`/record`);
* Silent screenshot capture and screen recordings via Android Accessibility Services.

### 3.2 Compliance with Local Recording Laws
Certain jurisdictions enforce strict **two-party (all-party) consent laws** governing audio recording and video capture. **You alone are responsible for verifying that your use of covert recording features complies with all applicable local, municipal, state, and federal laws.** You agree that you will only review or submit forensic recordings to authorized law enforcement agencies for legitimate crime reporting and device recovery.

---

## 4. Telegram Command & Control (C2) & Gateway Architecture

### 4.1 Method 1 (Private Dedicated Bot) vs. Method 2 (Shared Gateway Bot)
* **Method 1 (Private Bot via @BotFather — Recommended):** Provides sovereign, zero-trust security. You maintain full ownership of your private bot token. No third party shares your C2 pipeline.
* **Method 2 (Instant Pairing via @Pas_agent_bot):** Provided for convenience. Commands flow through our hardened control plane gateway. While protected by rate limits, chat ID validation, and physical possession OTPs, it is a managed multi-tenant gateway.
* **Account Compromise:** You are solely responsible for maintaining the physical and digital security of your personal Telegram account. Anyone who gains authorized or unauthorized access to your unlocked Telegram app may dispatch commands to your device. You must enforce two-factor authentication (2FA) and biometric passcodes on your Telegram client.

---

## 5. Software License & Intellectual Property

* **License Grant:** Subject to compliance with these Terms, Provider grants you a revocable, non-exclusive, non-transferable, limited personal or enterprise license to run PASA Sentinel on supported Android hardware.
* **Reverse Engineering:** Except to the extent permitted by applicable open-source components, you agree not to distribute malicious forks, decompile for malicious repurposing, or bypass the licensing verification mechanisms.

---

## 6. Disclaimer of Warranties ("As-Is")

PASA SENTINEL IS PROVIDED ON AN **"AS-IS" AND "AS-AVAILABLE"** BASIS WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, OPERATIONAL AVAILABILITY, UPTIME, OR FREEDOM FROM PROGRAM BUGS. PROVIDER DOES NOT WARRANT THAT THE SERVICE WILL BE UNINTERRUPTED, THAT ALL THEFT ATTEMPTS WILL BE PREVENTED, OR THAT TELEMETRY TRANSMISSION CAN OVERCOME HARDWARE PHYSICAL DISCONNECTION (SUCH AS POWER DESTRUCTION OR SIGNAL JAMMING).

---

## 7. Limitation of Liability & Indemnification

### 7.1 Maximum Aggregate Liability
TO THE MAXIMUM EXTENT PERMITTED BY APPLICABLE LAW, IN NO EVENT SHALL THE DEVELOPER, AUTHORS, CONTRIBUTORS, OR AFFILIATES BE LIABLE FOR ANY INDIRECT, INCIDENTAL, SPECIAL, CONSEQUENTIAL, OR PUNITIVE DAMAGES (INCLUDING LOSS OF PROFITS, DATA, HARDWARE, BUSINESS REPUTATION, OR PERSONAL INJURY) ARISING OUT OF OR IN CONNECTION WITH THE USE OR INABILITY TO USE THIS APPLICATION. IN NO EVENT SHALL TOTAL AGGREGATE LIABILITY EXCEED THE AMOUNT PAID BY YOU FOR THE SPECIFIC PASA SENTINEL LICENSE GIVING RISE TO THE CLAIM.

### 7.2 Hold Harmless & Indemnification
You agree to defend, indemnify, and hold harmless Izhaan Intellect, its directors, developers, and contractors from and against any claims, liabilities, damages, judgments, awards, losses, costs, expenses, or fees (including reasonable attorneys' fees) arising out of or relating to:
1. Your violation of these Terms;
2. Your deployment of PASA Sentinel on any device without lawful ownership or consent;
3. Any violation of privacy, wiretapping, or surveillance laws resulting from your use of the application;
4. Any third-party claims arising from device lockdown, emergency wipe, or false theft alarms.

---

## 8. Termination & Modifications

* **Termination:** Provider reserves the right to terminate license keys, suspend C2 gateway routing, or blacklist API keys without prior notice if we detect abusive activity, unauthorized surveillance, reverse-engineering attacks, or violation of these Terms.
* **Amendments:** Provider may periodically update these Terms. Continued use of PASA Sentinel following any published amendments constitutes acceptance of the revised Terms.

---

## 9. Contact Information

For legal inquiries, licensing compliance, or law enforcement coordination:
* **Email:** [support@izhaanintellect.fun](mailto:support@izhaanintellect.fun)
* **WhatsApp Business Support:** [+880 1762-033445](https://wa.me/8801762033445)
* **Website:** [https://pasa.izhaanintellect.fun/terms](https://pasa.izhaanintellect.fun/terms)
