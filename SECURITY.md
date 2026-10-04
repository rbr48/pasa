# 🛡️ PASA Sentinel — Security & Vulnerability Disclosure Policy

Izhaan Intellect and the PASA Sentinel engineering team take the security and integrity of our mobile defense software with extreme seriousness. We welcome reports from security researchers, reverse engineers, cryptographers, and our user community to continuously harden the PASA Sentinel ecosystem.

This policy adheres to the **RFC 9116 (security.txt)** standard for coordinated vulnerability disclosure.

---

## 1. Supported Versions

Security updates, patches, and hotfixes are actively provided exclusively for the following versions:

| Version | Status | Android API Support | Cryptographic Token Support |
| :--- | :--- | :--- | :--- |
| **v3.7.x (Latest: v3.7.19 / Build 84)** | 🟢 **Actively Supported** | Android 8.0 – 16 (API 26 – 36) | StrongBox / Hardware Escrow Tokens |
| v3.6.x | 🟡 Critical Patches Only | Android 8.0 – 16 (API 26 – 36) | StrongBox / Hardware Escrow Tokens |
| < v3.6.0 | 🔴 End of Life (EOL) | Unsupported | Deprecated |

---

## 2. Reporting a Vulnerability

If you discover a security vulnerability, cryptographic flaw, or potential privilege escalation vector in PASA Sentinel, please report it privately and securely. **Do NOT open a public GitHub issue for sensitive security vulnerabilities.**

### Primary Security Contacts:
* ✉️ **Security Response Team:** [ciso@izhaanintellect.fun](mailto:ciso@izhaanintellect.fun)
* 🛡️ **Abuse & Stalkerware Escalation:** [contact@izhaanintellect.fun](mailto:contact@izhaanintellect.fun)
* 💬 **Immediate WhatsApp Incident Desk:** [+880 1762-033445](https://wa.me/8801762033445)
* 🌐 **RFC 9116 Canonical Policy:** [https://pasa.izhaanintellect.fun/.well-known/security.txt](https://pasa.izhaanintellect.fun/.well-known/security.txt)
* 📋 **Audit Trail & Verification:** [https://pasa.izhaanintellect.fun/AUDIT_TRAIL.md](https://pasa.izhaanintellect.fun/AUDIT_TRAIL.md)

### Information to Include in Your Report:
To accelerate triage and resolution, please provide:
1. Target component (`app/` Android client, `pasa-server/` control plane, or `pasa-commercial-web/` portal).
2. Android OS version and device model tested (e.g., Pixel 9 Pro running Android 16 / Samsung Galaxy running Knox 3.10).
3. A detailed step-by-step description to reproduce the vulnerability.
4. Proof-of-Concept (PoC) code, ADB logcat snippets, or network packet traces (without transmitting sensitive personal user data).
5. Assessment of impact (e.g., Device Owner bypass, local authentication coercion, unhandled intent injection, or cryptographic weakness).

---

## 3. Vulnerability Triage & SLA Commitments

We are committed to rapid response and coordinated disclosure:

* **Initial Acknowledgment:** Within **24 hours** of report receipt.
* **Triage & Reproducibility Assessment:** Within **48 hours**.
* **Remediation & Patch Deployment:** Critical vulnerabilities are prioritized for resolution within **7 calendar days**.
* **Public Advisory & Attribution:** Coordinated disclosure after verified patch rollout across official signed binaries and server OTA channels.

---

## 4. Safe Harbor for Ethical Researchers

If you conduct security research in good faith and in compliance with this policy:
1. **No Legal Action:** We will not pursue or support legal action against you under the Computer Fraud and Abuse Act (CFAA), Cyber Security Act, or related anti-hacking laws.
2. **Coordinated Disclosure:** We will work collaboratively with you to understand and resolve the issue before public announcement, and we will credit your discovery (unless you prefer anonymity) in our [AUDIT_TRAIL.md](https://pasa.izhaanintellect.fun/AUDIT_TRAIL.md).
3. **Safe Research Guidelines:**
   * Do not access, modify, exfiltrate, or delete user evidence, Telegram bot tokens, or private phone data belonging to third parties.
   * Do not execute Denial of Service (DoS/DDoS) attacks against the live VPS control plane (`pasa.izhaanintellect.fun`) or commercial infrastructure.
   * Conduct testing on devices you physically own or in local Android emulators (`emulator -avd ...`).
   * Give us reasonable time to release fixes before disclosing technical vulnerability specifics to the public.

---

## 5. Scope & Audit Standards

### In-Scope:
* Android Application (`app/`): Device Owner security boundaries, Lock Task kiosk enforcement, Headless CameraX/Stealth capture services, AES-256-GCM / SQLCipher key management, Escrow Token handling, and Accessibility Duress PIN interceptor.
* Server & Control Plane (`pasa-server/`): Ed25519 license verification, Telegram Bot webhook relay, OTP ephemeral pairing, and rate limiting algorithms.
* Communication Channels: SMS PDU parsing, TOTP verification (RFC 6238), and TLS mutual transport.

### Out-of-Scope:
* Social engineering or phishing targeting PASA employees, contractors, or users.
* Attacks requiring physical disassembly or chip-off forensic decapping of the device processor/SoC.
* Vulnerabilities in upstream third-party services (e.g., Telegram Bot API itself, Google Android OS zero-days).
* Automated scanner dumps without verifiable PoC demonstrating real-world exploitability.

---

## 6. Independent Automated Security Audits

Every release binary of PASA Sentinel is continuously subjected to automated static, dynamic, and cryptographic auditing:
* **MobSF Static Analysis:** 0 High / 0 Critical security vulnerabilities.
* **GitHub CodeQL Scanning:** Continuous deep semantic AST analysis across Kotlin, Java, and JavaScript.
* **VirusTotal Consensus:** 100% clean consensus across 70+ premier global antivirus engines ([Inspect Audit Report](https://www.virustotal.com/gui/file/db0811cf5ecf0a5c9d98423330e03080f3e938a7fb2ee7def76e2edf2f46532c)).
