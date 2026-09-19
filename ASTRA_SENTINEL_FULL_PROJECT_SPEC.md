# ASTRA SENTINEL
## Private Android Security, Remote Device Management & Emergency Response Platform

**Document type:** Full Project Specification / Architecture / Implementation Blueprint  
**Project codename:** ASTRA SENTINEL  
**Primary target:** Authorized, owner-controlled or organization-managed Android devices  
**Primary remote control:** Telegram Bot + Private Admin Console  
**Android control model:** Device Owner / fully managed device, where supported  
**Recommended backend model:** Self-hosted private control plane  
**Security posture:** Zero-trust, defense-in-depth, least privilege, fail-safe destructive actions

---

## 0. Executive Summary

ASTRA SENTINEL is a private Android device-management and emergency-response platform designed to protect authorized Android devices when they are lost, stolen, exposed to unauthorized access, or placed into a predefined security-risk state.

The system is intentionally **not** designed as a covert surveillance application and does not attempt to bypass Android authentication, root arbitrary devices, exploit bootloaders, steal credentials, secretly activate microphones/cameras, or compromise devices that the administrator is not authorized to manage.

The system uses Android's supported managed-device architecture. Where ASTRA is provisioned as the Device Owner of a fully managed device, it can apply device-management policies and use supported administrative functions such as immediate locking and device wipe. Android documents `DevicePolicyManager.lockNow()` for urgent situations such as a lost or stolen device, and provides device-wipe APIs for managed devices. [Android DevicePolicyManager](https://developer.android.com/reference/kotlin/android/app/admin/DevicePolicyManager)

Telegram is treated as an **administrator interface**, not as the device-security authority. Telegram commands go through ASTRA's secure backend, where administrator identity, role, command policy, freshness, device identity, approval requirements, and audit rules are evaluated before a signed device command is delivered.

The platform is divided into:

1. **ASTRA Agent / DPC** — Android application and Device Policy Controller.
2. **ASTRA Control Plane** — private backend that stores devices, policies, commands, identities, events, and audit records.
3. **ASTRA Telegram Gateway** — administrator command interface and emergency notification channel.
4. **ASTRA Web Console** — detailed management, analytics, policy, approval, and audit interface.
5. **ASTRA Security Engine** — event correlation, risk state transitions, policy execution, and incident workflows.
6. **ASTRA Enrollment Service** — authorized-device provisioning and enrollment lifecycle.

---

# 1. Product Vision

### 1.1 Vision

Create a private device-security platform that can answer five questions continuously:

> **Who is the administrator?**  
> **Which device is being managed?**  
> **Is the device still in an acceptable security state?**  
> **Is this command legitimate and fresh?**  
> **What is the least destructive action that safely contains the incident?**

### 1.2 Design principles

- **Never trust a network message by itself.**
- **Never trust a device identity without proof.**
- **Never allow Telegram text to directly execute privileged operations.**
- **Use Android's supported Device Owner / management mechanisms instead of exploit-based control.**
- **Default to containment before destruction.**
- **Make destructive operations strongly authenticated and auditable.**
- **Allow autonomous local protection for selected rules when the device is offline.**
- **Keep security telemetry separate from private user content.**
- **Fail closed for authorization; fail safe for ambiguous security events.**
- **Every privileged action must have an audit trail.**

### 1.3 Non-goals

ASTRA does not include:

- credential theft;
- keylogging;
- hidden microphone recording;
- hidden camera activation;
- covert collection of private messages;
- arbitrary privilege escalation;
- remote exploitation of unrelated phones;
- bypassing a device's lock screen through vulnerabilities;
- secret rooting or bootloader exploitation;
- destructive actions against devices without authorization.

---

# 2. High-Level System Architecture

```text
                           ┌──────────────────────┐
                           │       TELEGRAM       │
                           │   ASTRA ADMIN BOT    │
                           └──────────┬───────────┘
                                      │
                                      ▼
                     ┌────────────────────────────────┐
                     │       TELEGRAM GATEWAY         │
                     │ webhook + secret verification  │
                     └──────────────┬─────────────────┘
                                    │
                                    ▼
                     ┌────────────────────────────────┐
                     │        ASTRA CONTROL PLANE     │
                     │                                │
                     │ Identity / RBAC / MFA         │
                     │ Command Authorization         │
                     │ Device Registry               │
                     │ Policy Engine                  │
                     │ Threat / Event Engine         │
                     │ Approval Engine                │
                     │ Audit Ledger                   │
                     │ Notification Engine            │
                     └──────────────┬─────────────────┘
                                    │
                       Signed + expiring command
                                    │
                ┌───────────────────┼───────────────────┐
                │                   │                   │
                ▼                   ▼                   ▼
             FCM Push         HTTPS Command Queue   Recovery API
                │                   │                   │
                └───────────────────┼───────────────────┘
                                    │
                                    ▼
                     ┌────────────────────────────────┐
                     │        ASTRA ANDROID DPC       │
                     │                                │
                     │ Device Owner                   │
                     │ Local Policy Engine            │
                     │ Security Event Collector       │
                     │ Keystore-backed identity       │
                     │ Command Verifier               │
                     │ Local encrypted storage        │
                     └──────────────┬─────────────────┘
                                    │
                                    ▼
                              ANDROID OS

                     ┌──────────────────────────────┐
                     │ Device management / security│
                     │ lock / wipe / restrictions │
                     │ apps / networking / policy │
                     └──────────────────────────────┘
```

---

# 3. Major Components

## 3.1 Android ASTRA DPC

Responsibilities:

- act as the authorized Device Owner when provisioned that way;
- maintain a device-specific identity;
- verify signed commands;
- execute approved device-management operations;
- apply local security policies;
- collect supported security-management events;
- maintain local state while offline;
- send authenticated heartbeat and telemetry;
- maintain an encrypted local cache;
- expose an operator-facing local dashboard;
- provide recovery workflow for authorized administrators.

## 3.2 Control Plane

Responsibilities:

- administrator authentication;
- Telegram user authorization;
- role-based access control;
- device enrollment and registry;
- command authorization;
- two-person destructive approval;
- policy storage and versioning;
- command signing;
- delivery tracking;
- event correlation;
- security state transitions;
- notifications;
- audit logging.

## 3.3 Telegram Gateway

Responsibilities:

- receive Telegram webhooks;
- validate Telegram webhook secret;
- identify the Telegram user/chat;
- map authorized user to ASTRA account;
- present buttons and commands;
- initiate approval workflows;
- return command status and security alerts.

Telegram's Bot API supports HTTPS webhooks and an optional `secret_token`; Telegram sends that value in the `X-Telegram-Bot-Api-Secret-Token` request header for webhook verification. [Telegram Bot API](https://core.telegram.org/bots/api)

## 3.4 Web Console

Responsibilities:

- device fleet overview;
- device detail;
- event timeline;
- policy configuration;
- command approvals;
- administrator management;
- audit review;
- security analytics;
- enrollment management;
- incident-response workflow.

## 3.5 Security Engine

Responsibilities:

- correlate events;
- evaluate policy rules;
- calculate a contextual risk state;
- prevent alert storms;
- trigger containment workflows;
- track unresolved incidents;
- escalate incidents according to policy.

---

# 4. Supported Device Model

## 4.1 Strongest mode: Device Owner

For maximum control, the phone should be provisioned as a fully managed device and ASTRA should become its Device Policy Controller / Device Owner, subject to Android version, OEM, enrollment method, and deployment constraints.

The Device Owner model is intended for managed devices and enables broad device-policy administration. Android's API documentation shows that `lockNow()` is a device-admin operation intended for urgent cases such as a lost or stolen device. [Android DevicePolicyManager](https://developer.android.com/reference/kotlin/android/app/admin/DevicePolicyManager)

## 4.2 Enrollment requirements

ASTRA must not claim unlimited control from an ordinary side-loaded APK.

Recommended enrollment approaches depend on deployment context:

- QR-code provisioning on a new/factory-reset device;
- Android Enterprise provisioning flows;
- DPC identifier provisioning where supported;
- zero-touch enrollment for eligible organization-owned fleets;
- dedicated-device / fully-managed deployment where applicable.

Google's Android Enterprise provisioning documentation covers fully managed device provisioning and enrollment options. [Provision devices](https://developers.google.com/android/management/provision-device)

## 4.3 Consumer/non-managed mode

A secondary non-DPC mode may exist, but it must clearly display reduced authority.

Example:

```text
ASTRA MODE: LIMITED

Device Owner: NO
Remote Lock: LIMITED / NOT AVAILABLE THROUGH ASTRA
Remote Wipe: NOT AVAILABLE THROUGH ASTRA
Security Policies: LIMITED
```

This prevents the UI from promising capabilities the installed application does not actually possess.

---

# 5. ASTRA Security State Machine

The state machine is central to the entire product.

```text
S0  ENROLLING
S1  NORMAL
S2  WATCH
S3  SUSPICIOUS
S4  PROTECTED
S5  LOST
S6  QUARANTINED
S7  CRITICAL
S8  WIPE_PENDING
S9  WIPED
S10 RECOVERY
S11 SUSPENDED
```

### 5.1 State definitions

| State | Meaning | Typical behavior |
|---|---|---|
| ENROLLING | Device being registered | provisioning + identity verification |
| NORMAL | Expected state | normal monitoring |
| WATCH | Minor anomaly | extra telemetry, no destructive action |
| SUSPICIOUS | Multiple or significant anomalies | alert + increased policy enforcement |
| PROTECTED | Security controls strengthened | lock/restriction workflow |
| LOST | Device declared lost/stolen | lock + lost workflow |
| QUARANTINED | Severe incident | restricted management state |
| CRITICAL | High-confidence serious incident | emergency containment |
| WIPE_PENDING | Destructive action authorized but not yet executed | final verification |
| WIPED | Device wipe completed or confirmed | enrollment/recovery state |
| RECOVERY | Authorized recovery after incident | revalidation |
| SUSPENDED | Device identity/relation disabled | reject management commands except recovery |

### 5.2 State transitions

```text
ENROLLING → NORMAL
NORMAL → WATCH
WATCH → NORMAL
WATCH → SUSPICIOUS
SUSPICIOUS → PROTECTED
SUSPICIOUS → NORMAL
PROTECTED → LOST
PROTECTED → NORMAL
LOST → QUARANTINED
LOST → RECOVERY
QUARANTINED → CRITICAL
CRITICAL → WIPE_PENDING
WIPE_PENDING → WIPED
LOST → NORMAL           (authorized recovery)
SUSPENDED → RECOVERY   (strong administrator workflow)
```

The backend should reject impossible transitions unless a maintenance procedure explicitly permits them.

---

# 6. Emergency Response Levels

## Level 0 — OBSERVE

Actions:

- store event;
- send low-priority notification;
- update risk context.

## Level 1 — WATCH

Actions:

- increase heartbeat frequency within safe limits;
- collect additional supported security events;
- require stronger integrity validation at next command exchange.

## Level 2 — PROTECT

Actions may include supported device restrictions and immediate lock where the DPC has authority.

Example:

```text
Lock device
Restrict supported file-transfer/debugging functions
Maintain security state
Notify administrator
```

## Level 3 — LOST

Actions:

```text
Lock
Display recovery contact/message where supported
Increase monitoring
Generate incident
Require recovery authorization for return to NORMAL
```

## Level 4 — QUARANTINE

Actions:

```text
Apply strict supported management policy
Lock or restrict according to policy
Suspend normal administrative commands except approved incident commands
Await authorization
```

## Level 5 — DESTROY / WIPE

Actions:

```text
final authorization
↓
command signing
↓
device identity verification
↓
execution
↓
acknowledgement
↓
WIPED state
```

Destructive actions must be intentionally hard to trigger accidentally.

---

# 7. Device Identity & Cryptography

## 7.1 Identity model

Each enrolled device receives:

```text
ASTRA DEVICE ID
DEVICE CERTIFICATE / PUBLIC KEY
KEY VERSION
ATTESTATION RECORD (where available)
ENROLLMENT RECORD
POLICY VERSION
```

## 7.2 Android Keystore

The device generates a cryptographic key within Android Keystore.

The private key never leaves the device.

When supported, hardware-backed Keystore provides stronger protection. Android documents hardware-backed Keystore features and attested key pairs. [Android security package](https://developer.android.com/reference/android/security/package-summary) and [PackageManager hardware Keystore feature](https://developer.android.com/reference/kotlin/android/content/pm/PackageManager)

## 7.3 Attestation

Enrollment should include server-side validation of the device's attestation chain where available.

Do not perform trust decisions using only a value reported by the client itself.

Android's current guidance recommends using the attestation chain and validating it on a trusted server. [Android Keystore key attestation](https://developer.android.com/privacy-and-security/security-key-attestation)

## 7.4 Key lifecycle

```text
GENERATE
  ↓
REGISTER
  ↓
VERIFY
  ↓
ACTIVE
  ↓
ROTATE
  ↓
REVOKE
```

Key rotation should preserve a controlled overlap period so routine rotation does not unnecessarily brick a device.

---

# 8. Play Integrity Integration

ASTRA should optionally integrate Google Play Integrity for app/server integrity signals on eligible deployments.

Android documents Play Integrity as a mechanism that lets a backend check whether requests come from the genuine application and a trustworthy Android environment. [Android security checklist](https://developer.android.com/privacy-and-security/security-tips)

Recommended usage:

```text
Device request
   ↓
ASTRA backend issues/validates integrity challenge
   ↓
Play Integrity verdict
   ↓
Combine with:
   • Device Owner state
   • device certificate
   • policy version
   • enrollment status
   ↓
trust decision
```

Play Integrity should be an input to a broader trust model, not a sole source of truth.

For managed/private deployments, verify Play distribution and Google Play service availability requirements before making this a hard dependency.

---

# 9. Command Security Model

No privileged device action should be executed directly from raw Telegram text.

## 9.1 Command flow

```text
Telegram message
      ↓
Webhook verification
      ↓
Telegram user identification
      ↓
ASTRA account lookup
      ↓
RBAC authorization
      ↓
Command policy evaluation
      ↓
MFA / approval if required
      ↓
Create immutable command record
      ↓
Sign command
      ↓
Deliver to device
      ↓
Device validates command
      ↓
Device executes operation
      ↓
Device signs acknowledgement
      ↓
Backend records result
      ↓
Telegram notification
```

## 9.2 Command envelope

Example logical structure:

```json
{
  "commandId": "cmd_01J...",
  "deviceId": "ASTRA-0001",
  "adminId": "adm_001",
  "action": "LOCK",
  "issuedAt": "2026-09-19T08:00:00Z",
  "expiresAt": "2026-09-19T08:01:00Z",
  "sequence": 1843,
  "nonce": "base64url...",
  "policyVersion": 12,
  "parameters": {},
  "signature": "base64url..."
}
```

## 9.3 Device-side validation

The device rejects a command if:

- device ID does not match;
- administrator authorization was not accepted by the backend;
- signature is invalid;
- command is expired;
- nonce/sequence has already been executed;
- command state is already completed;
- policy version is incompatible;
- the device is suspended and the command is not a recovery command.

## 9.4 Replay protection

Each device maintains the last accepted command sequence and a bounded cache of recent command IDs.

Example:

```text
Last sequence = 1843

1844 → accept
1844 again → reject
1843 → reject
1800 → reject
```

---

# 10. Administrator Security

## 10.1 Roles

Suggested roles:

```text
OWNER
SECURITY_ADMIN
OPERATOR
AUDITOR
VIEWER
```

## 10.2 Example permissions

| Permission | OWNER | SECURITY_ADMIN | OPERATOR | AUDITOR | VIEWER |
|---|---:|---:|---:|---:|---:|
| View device | ✓ | ✓ | ✓ | ✓ | ✓ |
| View events | ✓ | ✓ | ✓ | ✓ | ✓ |
| Lock | ✓ | ✓ | ✓ |  |  |
| Lost mode | ✓ | ✓ | ✓ |  |  |
| Protect | ✓ | ✓ | ✓ |  |  |
| Change policy | ✓ | ✓ |  |  |  |
| Manage administrators | ✓ |  |  |  |  |
| Initiate wipe | ✓ | ✓ |  |  |  |
| Approve wipe | ✓ | ✓ |  |  |  |
| Audit export | ✓ | ✓ |  | ✓ |  |

## 10.3 MFA

Use strong authentication for administrator access.

Possible design:

```text
Telegram identity
+
ASTRA account
+
Passkey / authenticator / second factor
```

Android's security guidance recommends strong authentication for protected operations. [Android security checklist](https://developer.android.com/privacy-and-security/security-tips)

---

# 11. Two-Person Approval

For destructive actions, support dual authorization.

Example:

```text
ADMIN A
  ↓
INITIATE WIPE
  ↓
PENDING SECOND APPROVAL
  ↓
ADMIN B
  ↓
APPROVE WIPE
  ↓
FINAL COMMAND CREATED
  ↓
DEVICE EXECUTES
```

Configurable policy:

```yaml
wipe:
  approvalsRequired: 2
  maxApprovalAgeMinutes: 5
  requireDifferentAdministrators: true
  requireOwnerRoleForFinalApproval: false
```

This is a recommended production option, not a requirement for every private deployment.

---

# 12. Local Security Engine

The Android agent should implement a local policy engine for rules that must remain effective when network connectivity is lost.

## 12.1 Local event categories

Use only events available through Android's supported APIs and policies for the device/deployment.

Examples:

- failed device authentication count or policy state where available;
- device-management state changes;
- supported security audit events;
- supported network logging events;
- policy compliance state;
- application management state;
- connectivity state;
- device reboot/boot events as supported by the platform.

Android provides security logging/network logging facilities for managed devices, but logging scope and availability are platform- and configuration-dependent. [DevicePolicyManager](https://developer.android.com/reference/kotlin/android/app/admin/DevicePolicyManager) and [Android Management API policy reference](https://developers.google.com/android/management/reference/rest/v1/enterprises.policies)

## 12.2 Local rules

Example:

```yaml
rules:
  - id: auth-failure-watch
    condition: failedAuthCount >= 3
    action: ENTER_WATCH

  - id: auth-failure-protect
    condition: failedAuthCount >= 5
    action: LOCK_DEVICE

  - id: critical-policy-violation
    condition: policyState == CRITICAL
    action: ALERT_ADMIN
```

Do not make network loss alone a wipe trigger.

---

# 13. Risk / Threat Context Engine

Instead of using a simplistic “hacker score,” ASTRA maintains a contextual incident state.

## 13.1 Event weight example

```text
LOW:
  • normal disconnect
  • ordinary reboot

MEDIUM:
  • repeated authentication failures
  • unexpected connectivity transition

HIGH:
  • management-policy inconsistency
  • integrity verification failure

CRITICAL:
  • multiple independent high-confidence indicators
  • explicit administrator declaration of device theft/loss
```

The exact rules must be configurable and device-profile dependent.

## 13.2 Correlation model

```text
Event A
  +
Event B
  +
Event C
  ↓
Incident context
  ↓
Policy engine
  ↓
Containment
```

The purpose is to reduce false positives caused by one ambiguous event.

---

# 14. Lost Mode

## 14.1 Activation sources

- Telegram command;
- Web console;
- emergency API;
- optional administrator-defined rule.

## 14.2 Lost Mode actions

```text
LOCK_DEVICE
SET_LOST_STATE
SHOW_RECOVERY_INFORMATION (where supported)
INCREASE_HEARTBEAT
GENERATE_INCIDENT
NOTIFY_ADMINS
```

## 14.3 Recovery

```text
LOST
 ↓
ADMIN INITIATES RECOVERY
 ↓
MFA
 ↓
DEVICE REVALIDATION
 ↓
RECOVERY
 ↓
NORMAL
```

---

# 15. Remote Lock

The primary lock operation is a Device Owner/device-admin operation when the Android deployment permits it.

Android documents `DevicePolicyManager.lockNow()` as an immediate locking API intended for urgent situations including lost or stolen devices. [DevicePolicyManager](https://developer.android.com/reference/kotlin/android/app/admin/DevicePolicyManager)

Example Kotlin service interface:

```kotlin
interface DeviceCommandExecutor {
    suspend fun lockDevice(request: LockRequest): CommandResult
}
```

Implementation should call the appropriate `DevicePolicyManager` method only after command authentication and validation.

---

# 16. Remote Wipe

Remote wipe is a **destructive operation**.

Android provides device-policy wipe mechanisms for managed devices. The exact behavior depends on Android version, device configuration, storage arrangement, and policy flags. [DevicePolicyManager](https://developer.android.com/reference/kotlin/android/app/admin/DevicePolicyManager)

## 16.1 Mandatory safeguards

Before executing a wipe command:

1. Confirm device ID.
2. Confirm administrator role.
3. Confirm device is enrolled and active.
4. Confirm command is current.
5. Confirm approval workflow.
6. Verify command signature.
7. Verify device identity.
8. Record audit entry.
9. Execute wipe.
10. Record acknowledgement if possible.

## 16.2 Wipe UX

Telegram:

```text
⚠️ PERMANENT WIPE

Device: ASTRA-0001

This operation is destructive.

[ CANCEL ]   [ START APPROVAL ]
```

Then:

```text
WIPE CONFIRMATION

Device: ASTRA-0001
Approval code: 849271

Reply:
/confirm-wipe 849271
```

For high-security installations, require a second administrator as well.

---

# 17. Device Restrictions

Supported restrictions should be applied through Android management APIs, not through hidden workarounds.

Possible policy categories:

```text
USB / file-transfer restrictions
Developer/debugging restrictions
Unknown-source installation restrictions
Application allow-list / block-list
Camera restrictions where supported
Screen capture restrictions where supported
Network configuration
Password/security policy
System update policy
Factory reset protection configuration where supported
```

The exact policy availability is Android-version, device-owner/profile, OEM, and deployment dependent. Google documents many such policy controls in Android Management API for supported fully managed devices. [Android Management API policies](https://developers.google.com/android/management/reference/rest/v1/enterprises.policies)

---

# 18. Application Management

ASTRA should support a policy-driven application inventory for managed devices.

Example:

```text
APP POLICY

Required:
  ASTRA Agent

Allowed:
  Phone
  Messages
  Chrome
  Maps

Restricted:
  Unknown packages
  Unapproved applications
```

Important: exact enforcement mechanisms and allow/block semantics must follow Android Enterprise/DPC capabilities available for the target version.

---

# 19. Security Patch & Device Health

The backend should track security posture data that the platform legitimately exposes.

Suggested fields:

```text
Android version
Security patch level
App version
DPC version
Policy version
Last integrity verdict
Last heartbeat
Enrollment state
Device-owner state
Battery state
Network state
Boot/reboot timestamp
```

Android's Security State documentation discusses security patch levels and Play Integrity as inputs to assessing device security state. [Understand device security state](https://developer.android.com/privacy-and-security/understand-device-security-state)

---

# 20. Heartbeat Architecture

## 20.1 Primary channel

Use Firebase Cloud Messaging for event/command wake-up where appropriate.

## 20.2 Secondary channel

Use HTTPS polling/heartbeat.

Suggested default:

```text
NORMAL          5 min
WATCH           2 min
SUSPICIOUS      60 sec
PROTECTED       60 sec
LOST            60 sec
```

These intervals are policy examples, not guaranteed background-execution timing. Android power management and OS restrictions must be respected.

## 20.3 Heartbeat payload

```json
{
  "deviceId": "ASTRA-0001",
  "timestamp": "2026-09-19T08:00:00Z",
  "appVersion": "1.0.0",
  "policyVersion": 12,
  "securityState": "NORMAL",
  "networkState": "WIFI",
  "batteryPercent": 78,
  "deviceOwner": true,
  "integrityState": "VERIFIED"
}
```

Never put unnecessary private user content into heartbeat telemetry.

---

# 21. Offline Command Queue

The backend stores commands as stateful records.

```text
PENDING
 ↓
DELIVERING
 ↓
DELIVERED
 ↓
EXECUTING
 ↓
SUCCEEDED / FAILED / EXPIRED
```

Commands should have:

```text
createdAt
expiresAt
sequence
nonce
commandId
policyVersion
```

Old destructive commands must not execute simply because the device reconnects after a long outage.

---

# 22. Command Idempotency

Every command must be idempotent or explicitly non-idempotent.

Examples:

```text
LOCK → idempotent
SET_POLICY version 12 → idempotent
SET_LOST_MODE → idempotent
WIPE → one-time destructive command
```

The server must prevent duplicate execution records.

---

# 23. Telegram Bot Specification

## 23.1 Commands

Core commands:

```text
/start
/help
/devices
/status <device>
/security <device>
/events <device>
/lock <device>
/protect <device>
/lost <device>
/recover <device>
/wipe <device>
/approve-wipe <approval>
/policies <device>
/incidents
/audit <device>
```

## 23.2 Inline keyboard

Example:

```text
ASTRA-0001
🟢 NORMAL

[ STATUS ] [ SECURITY ]
[ LOCK ]   [ PROTECT ]
[ LOST ]   [ RECOVERY ]
[ WIPE ]
```

The bot should not show destructive buttons to roles that do not possess the relevant permission.

## 23.3 Telegram webhook security

Configure HTTPS and `secret_token` for webhook verification. [Telegram Bot API](https://core.telegram.org/bots/api)

Never embed the bot token inside the Android APK.

---

# 24. Web Console

## 24.1 Dashboard

```text
┌────────────────────────────────────────────────────┐
│ ASTRA SENTINEL                                    │
├────────────────────────────────────────────────────┤
│ Devices     Incidents     Commands     Admins      │
│                                                    │
│ 🟢 NORMAL        12                                │
│ 🟡 WATCH          2                                │
│ 🟠 SUSPICIOUS     1                                │
│ 🔴 LOST           1                                │
│ ⚫ OFFLINE        3                                │
└────────────────────────────────────────────────────┘
```

## 24.2 Device detail

Sections:

```text
Overview
Security
Policies
Applications
Events
Commands
Incidents
Audit
Enrollment
```

## 24.3 Incident screen

```text
Incident ID
Device
Current state
First detected
Last detected
Events
Policy actions
Commands issued
Approvals
Resolution
```

---

# 25. Database Schema

Recommended database: PostgreSQL.

## 25.1 administrators

```sql
CREATE TABLE administrators (
    id UUID PRIMARY KEY,
    username VARCHAR(120) NOT NULL UNIQUE,
    display_name VARCHAR(200) NOT NULL,
    telegram_user_id BIGINT UNIQUE,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    last_login_at TIMESTAMPTZ
);
```

## 25.2 roles

```sql
CREATE TABLE roles (
    id UUID PRIMARY KEY,
    name VARCHAR(60) NOT NULL UNIQUE
);
```

## 25.3 administrator_roles

```sql
CREATE TABLE administrator_roles (
    administrator_id UUID NOT NULL REFERENCES administrators(id),
    role_id UUID NOT NULL REFERENCES roles(id),
    PRIMARY KEY (administrator_id, role_id)
);
```

## 25.4 devices

```sql
CREATE TABLE devices (
    id UUID PRIMARY KEY,
    device_code VARCHAR(80) NOT NULL UNIQUE,
    display_name VARCHAR(120) NOT NULL,
    status VARCHAR(40) NOT NULL,
    security_state VARCHAR(40) NOT NULL,
    enrolled_at TIMESTAMPTZ,
    last_seen_at TIMESTAMPTZ,
    last_ip_hash BYTEA,
    android_version VARCHAR(40),
    security_patch_level VARCHAR(40),
    app_version VARCHAR(40),
    policy_version INTEGER NOT NULL DEFAULT 0,
    device_owner_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    suspended_at TIMESTAMPTZ
);
```

## 25.5 device_keys

```sql
CREATE TABLE device_keys (
    id UUID PRIMARY KEY,
    device_id UUID NOT NULL REFERENCES devices(id),
    key_version INTEGER NOT NULL,
    public_key BYTEA NOT NULL,
    certificate_chain BYTEA,
    attestation_blob BYTEA,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ
);
```

## 25.6 policies

```sql
CREATE TABLE policies (
    id UUID PRIMARY KEY,
    device_id UUID REFERENCES devices(id),
    profile_name VARCHAR(100) NOT NULL,
    version INTEGER NOT NULL,
    definition JSONB NOT NULL,
    created_by UUID NOT NULL REFERENCES administrators(id),
    created_at TIMESTAMPTZ NOT NULL,
    activated_at TIMESTAMPTZ,
    UNIQUE(device_id, version)
);
```

## 25.7 commands

```sql
CREATE TABLE commands (
    id UUID PRIMARY KEY,
    device_id UUID NOT NULL REFERENCES devices(id),
    requested_by UUID NOT NULL REFERENCES administrators(id),
    action VARCHAR(60) NOT NULL,
    state VARCHAR(30) NOT NULL,
    sequence BIGINT NOT NULL,
    nonce BYTEA NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    parameters JSONB NOT NULL DEFAULT '{}'::jsonb,
    command_hash BYTEA NOT NULL,
    command_signature BYTEA NOT NULL,
    executed_at TIMESTAMPTZ,
    result JSONB,
    UNIQUE(device_id, sequence)
);
```

## 25.8 approvals

```sql
CREATE TABLE approvals (
    id UUID PRIMARY KEY,
    command_id UUID NOT NULL REFERENCES commands(id),
    administrator_id UUID NOT NULL REFERENCES administrators(id),
    approval_type VARCHAR(40) NOT NULL,
    state VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL
);
```

## 25.9 security_events

```sql
CREATE TABLE security_events (
    id BIGSERIAL PRIMARY KEY,
    device_id UUID NOT NULL REFERENCES devices(id),
    event_type VARCHAR(100) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    event_data JSONB NOT NULL,
    source VARCHAR(50) NOT NULL,
    previous_hash BYTEA,
    event_hash BYTEA NOT NULL
);
```

## 25.10 incidents

```sql
CREATE TABLE incidents (
    id UUID PRIMARY KEY,
    device_id UUID NOT NULL REFERENCES devices(id),
    incident_type VARCHAR(80) NOT NULL,
    state VARCHAR(40) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    opened_at TIMESTAMPTZ NOT NULL,
    closed_at TIMESTAMPTZ,
    opened_by UUID REFERENCES administrators(id),
    resolution JSONB
);
```

## 25.11 audit_logs

```sql
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    actor_type VARCHAR(30) NOT NULL,
    actor_id VARCHAR(120),
    action VARCHAR(120) NOT NULL,
    target_type VARCHAR(60),
    target_id VARCHAR(120),
    metadata JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
```

---

# 26. REST API

Base URL:

```text
https://api.example.invalid/v1
```

## 26.1 Authentication

```http
POST /auth/session
POST /auth/refresh
POST /auth/logout
```

## 26.2 Devices

```http
GET    /devices
POST   /devices/enroll/initiate
GET    /devices/{deviceId}
POST   /devices/{deviceId}/suspend
POST   /devices/{deviceId}/recover
GET    /devices/{deviceId}/health
```

## 26.3 Commands

```http
POST /devices/{deviceId}/commands
GET  /devices/{deviceId}/commands
GET  /commands/{commandId}
POST /commands/{commandId}/cancel
```

## 26.4 Approvals

```http
POST /commands/{commandId}/approvals
GET  /commands/{commandId}/approvals
```

## 26.5 Events

```http
POST /devices/{deviceId}/events
GET  /devices/{deviceId}/events
GET  /incidents
GET  /incidents/{incidentId}
```

## 26.6 Heartbeat

```http
POST /devices/{deviceId}/heartbeat
GET  /devices/{deviceId}/commands/pending
POST /devices/{deviceId}/commands/{commandId}/ack
```

---

# 27. API Authorization Matrix

| Endpoint/action | Device | Operator | Security Admin | Owner |
|---|---:|---:|---:|---:|
| heartbeat | ✓ |  |  |  |
| event upload | ✓ |  |  |  |
| view device |  | ✓ | ✓ | ✓ |
| lock |  | ✓ | ✓ | ✓ |
| lost |  | ✓ | ✓ | ✓ |
| protect |  | ✓ | ✓ | ✓ |
| policy update |  |  | ✓ | ✓ |
| wipe initiate |  |  | ✓ | ✓ |
| wipe approve |  |  | ✓ | ✓ |
| admin management |  |  |  | ✓ |

---

# 28. Backend Service Decomposition

For the first production version, a modular monolith is recommended instead of prematurely splitting into microservices.

Logical modules:

```text
astra-api
astra-auth
astra-device
astra-command
astra-policy
astra-event
astra-incident
astra-audit
astra-telegram
astra-notification
astra-enrollment
```

The modules can share one deployment initially while maintaining clear boundaries.

Later, high-load or security-sensitive modules can be separated:

```text
AUTH SERVICE
COMMAND SERVICE
DEVICE GATEWAY
EVENT SERVICE
TELEGRAM GATEWAY
```

---

# 29. Android Project Structure

Recommended Android Studio project:

```text
astra-android/
├── app/
├── core/
│   ├── common/
│   ├── crypto/
│   ├── network/
│   ├── database/
│   └── security/
├── feature/
│   ├── dashboard/
│   ├── enrollment/
│   ├── security/
│   ├── policies/
│   ├── events/
│   ├── commands/
│   └── recovery/
└── dpc/
    ├── receiver/
    ├── policy/
    ├── commands/
    └── telemetry/
```

Suggested package tree:

```text
com.astra.sentinel
├── MainActivity.kt
├── AstraApplication.kt
├── dpc
│   ├── AstraDeviceAdminReceiver.kt
│   ├── DevicePolicyController.kt
│   └── DevicePolicyRepository.kt
├── crypto
│   ├── DeviceKeyManager.kt
│   ├── CommandSignatureVerifier.kt
│   └── AttestationManager.kt
├── command
│   ├── CommandEnvelope.kt
│   ├── CommandValidator.kt
│   ├── CommandExecutor.kt
│   ├── LockCommandExecutor.kt
│   └── WipeCommandExecutor.kt
├── security
│   ├── SecurityEventCollector.kt
│   ├── LocalPolicyEngine.kt
│   ├── ThreatEngine.kt
│   └── SecurityStateMachine.kt
├── network
│   ├── AstraApi.kt
│   ├── AuthInterceptor.kt
│   └── FcmMessageService.kt
├── database
│   ├── AstraDatabase.kt
│   ├── DeviceDao.kt
│   ├── CommandDao.kt
│   └── EventDao.kt
└── ui
    ├── DashboardScreen.kt
    ├── SecurityScreen.kt
    ├── CommandHistoryScreen.kt
    └── RecoveryScreen.kt
```

---

# 30. Android Dependencies / Technology Choices

Recommended baseline:

- Kotlin;
- Jetpack Compose;
- Coroutines + Flow;
- AndroidX Lifecycle;
- Room;
- WorkManager where background work is appropriate;
- Android Keystore;
- Firebase Cloud Messaging where required;
- Kotlin serialization or Moshi for API serialization;
- Retrofit/OkHttp or Ktor client;
- certificate pinning only with a carefully planned key-rotation strategy;
- encrypted local storage for sensitive configuration.

Exact library versions must be pinned at implementation time after checking current stable releases and compatibility with the selected Android Gradle Plugin/Kotlin compiler.

---

# 31. Backend Technology Recommendation

Preferred private-stack option:

```text
Kotlin + Ktor or Spring Boot
PostgreSQL
Redis (optional)
Docker
Nginx / Caddy / cloud load balancer
Prometheus + Grafana (optional)
```

Alternative:

```text
Python + FastAPI
PostgreSQL
Redis
Celery / task queue if required
```

For a Kotlin-first team, Ktor or Spring Boot keeps the security and domain models close to the Android/Kotlin ecosystem.

---

# 32. Secure Storage Rules

Never store in plaintext:

- Telegram bot token;
- administrator refresh tokens;
- device private keys;
- server signing private keys;
- database credentials;
- MFA secrets;
- long-lived API secrets.

Server secrets should be loaded through a secret manager/environment injection system.

Device private keys belong in Android Keystore.

---

# 33. Server Key Architecture

Separate keys by purpose:

```text
SERVER IDENTITY KEY
COMMAND SIGNING KEY
TOKEN SIGNING KEY
DATA ENCRYPTION KEY
BACKUP ENCRYPTION KEY
```

Do not use one master key for every purpose.

Recommended:

```text
Key ID
Algorithm
Creation time
Status
Rotation schedule
Revocation state
```

Support key rotation without invalidating every previously enrolled device unnecessarily.

---

# 34. Certificate / Trust Strategy

Recommended:

```text
ASTRA CA / trust anchor
      ↓
device certificate
      ↓
device public key
```

Depending on deployment, device identity can also be represented by a backend-registered public key rather than a traditional certificate hierarchy.

If certificate pinning is used:

- pin a backup key/certificate;
- implement controlled rotation;
- test emergency rotation;
- do not hard-code a single certificate with no recovery path.

---

# 35. Network Security

Requirements:

- HTTPS only;
- TLS 1.2+ according to supported platform/server policy;
- no cleartext management endpoints;
- authentication on every privileged API;
- short-lived access tokens;
- refresh-token rotation;
- server-side rate limiting;
- request IDs;
- replay protection;
- strict JSON schema validation;
- protection against oversized payloads;
- security headers on web console;
- CSRF protection for cookie-authenticated web operations.

Android's Network Security Configuration supports controls such as cleartext blocking, custom trust anchors, and pinning where appropriate. [Network Security Configuration](https://developer.android.com/privacy-and-security/security-config)

---

# 36. Threat Model

## 36.1 Threat actors

1. Person who physically possesses the lost device.
2. Attacker attempting to impersonate an ASTRA device.
3. Attacker who obtains a Telegram administrator account.
4. Attacker who compromises the backend.
5. Attacker who intercepts network traffic.
6. Malicious insider with legitimate access.
7. Accidental administrator error.
8. Device/OEM/platform failure.

## 36.2 Key threats and controls

| Threat | Primary controls |
|---|---|
| Telegram account compromise | RBAC + MFA + dual approval |
| Fake device | device key + attestation/integrity where available |
| Replay command | nonce + sequence + expiration |
| Network interception | HTTPS + authentication + signing |
| Backend database theft | encryption + least privilege + key separation |
| Malicious insider | RBAC + dual approval + audit |
| Accidental wipe | confirmation + expiration + approval |
| Offline stale wipe | command TTL + sequence validation |
| Lost phone | Device Owner policies + lock + lost mode |
| Backend outage | local rules + device-side policy |
| Device incompatibility | capability detection + policy negotiation |

---

# 37. Backend Compromise Strategy

Assume the backend itself could eventually be compromised.

Therefore:

```text
Backend database compromise
≠
automatic ability to execute arbitrary device commands
```

Mitigations:

- device-bound command validation;
- command expiration;
- signature checks;
- separate command-signing key;
- administrator approval for destructive actions;
- device allow-list;
- command sequence validation;
- audit trail.

For an advanced deployment, place the command-signing operation behind a separate signing service or hardware security module.

---

# 38. Destructive Command Safety Model

The system must treat WIPE as exceptional.

Recommended policy:

```yaml
WIPE:
  defaultEnabled: true
  confirmationRequired: true
  approvalCount: 2
  ttlSeconds: 300
  requireFreshMfa: true
  requireDeviceStatus: true
  requireCurrentEnrollment: true
  requireCommandSignature: true
  requireAuditRecord: true
```

Optional owner-only emergency mode can reduce approvals, but this should be explicit and visible in the security configuration.

---

# 39. Incident Response Workflow

```text
EVENT DETECTED
      ↓
CREATE / UPDATE INCIDENT
      ↓
CORRELATE EVENTS
      ↓
EVALUATE POLICY
      ↓
CONTAIN
      ↓
NOTIFY
      ↓
ADMIN DECISION
      ↓
LOCK / PROTECT / LOST / WIPE
      ↓
VERIFY OUTCOME
      ↓
RESOLVE INCIDENT
```

---

# 40. Notifications

Notification channels:

```text
Telegram
Web console
Optional email
Optional push notification to admin application
```

Notification levels:

```text
INFO
WARNING
HIGH
CRITICAL
```

Avoid sending sensitive device information into Telegram unless the administrator explicitly needs it.

---

# 41. Audit Requirements

Every privileged action must record:

```text
actor
actor role
Telegram ID / session ID where applicable
IP metadata where lawful/appropriate
action
command ID
device ID
time
result
approval references
```

Audit records should be append-only from the normal application API.

For stronger integrity, chain event hashes or sign daily audit batches.

---

# 42. Privacy Model

ASTRA should collect the minimum telemetry required for device security.

Recommended to collect:

```text
device identity
security state
policy state
health metrics
supported security events
command status
integrity status
```

Avoid collecting:

```text
private SMS contents
personal photos
microphone recordings
private call recordings
passwords
biometric templates
unrelated application data
```

Any optional telemetry should have a documented purpose, retention period, and access control.

---

# 43. Data Retention

Example policy:

```text
Security events        180 days
Audit logs              1 year
Command records         1 year
Heartbeats              30 days
Resolved incidents      1 year
Device metadata         life of enrollment + 90 days
```

These values are examples; the final policy must reflect actual organizational and legal requirements.

---

# 44. Database Backup

Backups must be:

- encrypted;
- access-controlled;
- versioned;
- periodically tested;
- stored separately from production;
- retained according to policy.

Do not rely on an untested backup strategy for a security platform.

---

# 45. Device Capability Negotiation

Different Android/OEM versions expose different policies.

During enrollment, ASTRA should create a capability matrix:

```json
{
  "deviceOwner": true,
  "remoteLock": true,
  "remoteWipe": true,
  "securityLogging": true,
  "networkLogging": false,
  "cameraPolicy": true,
  "usbRestrictions": true,
  "playIntegrity": true,
  "hardwareKeystore": true
}
```

The server should never issue an action that the device reports as unsupported.

---

# 46. Policy Versioning

Every device receives a policy version.

Example:

```text
Device: ASTRA-0001
Current policy: 12

Server policy: 13

→ device downloads policy 13
→ verifies policy signature
→ applies policy
→ acknowledges
→ server marks device compliant
```

Policy updates should be signed and versioned.

---

# 47. Policy Rollback

A bad policy must be recoverable.

Maintain:

```text
Version 10
Version 11
Version 12 ← current
Version 13 ← staged
```

If deployment of version 13 fails on a device fleet:

```text
rollback to 12
```

Destructive policies should never be silently changed by a routine configuration update.

---

# 48. Staged Rollout

For a fleet of many devices:

```text
10% pilot
↓
observe
↓
25%
↓
observe
↓
50%
↓
100%
```

Policy deployment can have:

```text
minimumAndroidVersion
requiredCapabilities
pilotDeviceIds
maintenanceWindow
rollbackVersion
```

---

# 49. Admin Web Security

The web console should use:

- secure sessions;
- MFA/passkeys;
- short session lifetime;
- CSRF protection where applicable;
- Content Security Policy;
- secure cookies;
- SameSite controls;
- rate limiting;
- brute-force protection;
- audit logging;
- role-based UI and server-side authorization.

Never rely on hiding a button as a security control.

---

# 50. Telegram Security Rules

Telegram user IDs should be allow-listed.

Example:

```text
ALLOWED TELEGRAM ADMIN IDs

100000001
100000002
```

Unknown users receive:

```text
Access denied.
```

Do not reveal device information to unauthorized Telegram accounts.

---

# 51. Telegram Command Parsing

Do not use unsafe free-form execution.

Bad:

```text
/run <arbitrary command>
```

Good:

```text
/lock ASTRA-0001
/lost ASTRA-0001
/wipe ASTRA-0001
```

The backend should parse commands into an enum:

```kotlin
enum class DeviceAction {
    STATUS,
    LOCK,
    PROTECT,
    LOST,
    RECOVER,
    WIPE
}
```

No arbitrary shell access should exist.

---

# 52. WebSocket / Realtime Console

Optional.

Use WebSocket/SSE for live status updates:

```text
Device → backend → event bus → web console
```

Example:

```text
08:20:14  ASTRA-0001  ONLINE
08:20:19  ASTRA-0003  SECURITY EVENT
08:20:20  ASTRA-0003  SUSPICIOUS
```

---

# 53. Metrics / Observability

Track:

```text
heartbeat_success_rate
command_delivery_latency
command_success_rate
command_failure_rate
policy_compliance_rate
enrollment_failure_rate
Telegram_command_rate
approval_latency
incident_count
wipe_count
```

Alert on:

```text
sudden spike in failed commands
Telegram gateway errors
command queue backlog
server key service failure
unusual administrator activity
```

---

# 54. Health Endpoints

Internal endpoints:

```http
GET /health/live
GET /health/ready
GET /metrics
```

Do not expose sensitive internal diagnostics publicly.

---

# 55. Logging Rules

Application logs must never print:

- private keys;
- authentication tokens;
- full Telegram bot tokens;
- raw MFA secrets;
- passwords;
- sensitive personal information.

Use structured logs:

```json
{
  "timestamp": "2026-09-19T08:20:00Z",
  "service": "command-service",
  "requestId": "req_123",
  "commandId": "cmd_123",
  "deviceId": "ASTRA-0001",
  "result": "SUCCESS"
}
```

---

# 56. Error Handling

Every privileged operation returns a typed result.

```kotlin
sealed interface CommandResult {
    data object Success : CommandResult
    data class Rejected(val reason: String) : CommandResult
    data class Unsupported(val feature: String) : CommandResult
    data class Expired(val commandId: String) : CommandResult
    data class SecurityFailure(val reason: String) : CommandResult
    data class ExecutionFailure(val reason: String) : CommandResult
}
```

Never convert a security failure into a generic success response.

---

# 57. Recovery From Server Outage

The Android agent should retain locally cached:

```text
current policy
current device identity
last accepted command sequence
limited offline security rules
server trust configuration
```

The device should not depend on the backend for basic enforcement of already-authorized local policies.

---

# 58. Recovery From Telegram Outage

Telegram is not a single point of failure.

The web console must still function.

Optional emergency administrator application may function as another control surface.

Architecture:

```text
Telegram
   │
Web Console ──→ ASTRA Command Service
   │
Emergency Admin App
```

---

# 59. Emergency Administrator App (Phase 2/3)

An optional second app can be installed on the administrator's personal phone.

Features:

```text
Passkey login
Device list
Lock
Lost mode
Security status
Approval workflow
Incident notifications
```

This provides a non-Telegram recovery path if Telegram is unavailable.

---

# 60. Admin Passkey Strategy

For the web console, use passkeys/WebAuthn where possible.

This improves resistance to password phishing and reduces reliance on static passwords.

The Telegram identity still remains an authorized command interface, but sensitive commands should be able to require a stronger second factor.

---

# 61. Enrollment Token

Enrollment should use a short-lived, one-time bootstrap token.

Example:

```text
Enrollment token
expires in 10 minutes
single-use
bound to expected device profile
```

After enrollment:

```text
bootstrap token → invalid
per-device credentials → active
```

Never leave a permanent enrollment secret inside the APK.

---

# 62. Enrollment Ceremony

Recommended flow:

```text
1. Admin creates device record
2. Server creates short-lived enrollment session
3. Admin obtains QR/enrollment configuration
4. New/factory-reset phone starts managed provisioning
5. ASTRA DPC is provisioned
6. DPC generates device key
7. DPC sends public key/attestation
8. Server verifies
9. Server creates device certificate/identity record
10. Initial policy is delivered
11. Device acknowledges policy
12. Device enters NORMAL
```

---

# 63. Factory Reset / Re-enrollment Considerations

A factory reset removes normal application data. Re-enrollment after reset depends on the provisioning model and supported Android Enterprise/OEM capabilities.

Where organization-owned enrollment mechanisms support automatic re-provisioning, use those mechanisms rather than assuming a normal APK can restore itself after reset.

Google documents zero-touch and other enterprise provisioning approaches for managed Android devices. [Provision devices](https://developers.google.com/android/management/provision-device)

---

# 64. Factory Reset Protection

ASTRA may support appropriate factory-reset protection policies where the Android deployment exposes them.

Do not claim that ASTRA can permanently prevent every factory-reset/reflash scenario on every Android device.

The actual security boundary depends on:

- Android version;
- OEM implementation;
- bootloader state;
- enterprise enrollment mechanism;
- factory-reset protection capabilities;
- device owner status.

---

# 65. Secure Boot / Root / Bootloader Reality

ASTRA should detect and report security-state signals when they are available, but should not attempt to bypass boot-chain security.

Possible trust categories:

```text
TRUSTED
DEGRADED
UNVERIFIED
SUSPENDED
```

The system should distinguish:

```text
"Not verified"
```

from

```text
"Verified malicious"
```

Do not overstate what an application can know.

---

# 66. Security Reporting

Device report example:

```text
ASTRA DEVICE REPORT

Device: ASTRA-0001
State: PROTECTED

Device Owner: ACTIVE
Integrity: VERIFIED
Policy: 13
Security Patch: CURRENT/REPORTED
Last Seen: 08:31:22
Battery: 71%

Recent Events:
08:30 authentication anomaly
08:31 policy verified
08:31 lock confirmed
```

---

# 67. Security Incident Example

```text
08:45  Device reports repeated failed authentication attempts
08:45  ASTRA enters WATCH
08:47  Further failures occur
08:47  ASTRA enters PROTECTED
08:47  Device lock is applied by local policy
08:47  Telegram alert sent
08:49  Admin checks dashboard
08:50  Admin declares device LOST
08:50  Backend issues signed LOST policy
08:51  Device acknowledges LOST state
08:55  Admin determines recovery is unlikely
08:55  Wipe approval initiated
08:56  Second admin approves
08:56  Final command signed
08:57  Device executes supported wipe operation
08:57  Backend records WIPE result if acknowledgement is possible
```

---

# 68. Android Local Database

Recommended Room entities:

```text
DeviceIdentityEntity
PolicyEntity
CommandEntity
SecurityEventEntity
IncidentEntity
EnrollmentEntity
CapabilityEntity
```

Sensitive local records should be encrypted according to the chosen Android storage design.

---

# 69. Android Repository Interfaces

```kotlin
interface DeviceRepository {
    suspend fun getDeviceState(): DeviceState
    suspend fun updateCapabilities(capabilities: DeviceCapabilities)
    suspend fun updateHeartbeat(state: HeartbeatState)
}

interface CommandRepository {
    suspend fun getPendingCommands(): List<CommandEnvelope>
    suspend fun markExecuted(commandId: String, result: CommandResult)
}

interface SecurityRepository {
    suspend fun recordEvent(event: SecurityEvent)
    suspend fun currentState(): SecurityState
}
```

---

# 70. Command Executor Interface

```kotlin
interface DeviceCommandExecutor {
    suspend fun execute(command: CommandEnvelope): CommandResult
}
```

Dispatcher:

```kotlin
class CommandDispatcher(
    private val lockExecutor: LockExecutor,
    private val lostExecutor: LostModeExecutor,
    private val protectExecutor: ProtectionExecutor,
    private val wipeExecutor: WipeExecutor
) {
    suspend fun dispatch(command: CommandEnvelope): CommandResult = when (command.action) {
        DeviceAction.LOCK -> lockExecutor.execute(command)
        DeviceAction.PROTECT -> protectExecutor.execute(command)
        DeviceAction.LOST -> lostExecutor.execute(command)
        DeviceAction.WIPE -> wipeExecutor.execute(command)
        DeviceAction.RECOVER -> TODO()
        DeviceAction.STATUS -> TODO()
    }
}
```

---

# 71. DevicePolicyManager Boundary

Keep all DevicePolicyManager operations behind a single abstraction.

```kotlin
interface DevicePolicyGateway {
    fun isDeviceOwner(): Boolean
    fun lockNow()
    fun wipeDevice()
    fun applySupportedRestrictions(policy: DevicePolicyProfile)
}
```

This has two advantages:

1. The rest of the app cannot accidentally use privileged APIs in random locations.
2. Device-policy behavior is easy to mock/test.

---

# 72. FCM Message Design

Example logical message:

```json
{
  "type": "COMMAND_AVAILABLE",
  "deviceId": "ASTRA-0001",
  "commandId": "cmd_123",
  "nonce": "..."
}
```

Do not place a privileged destructive command's full authority in the push payload.

FCM should wake/nudge the device to retrieve the authenticated command from the secure backend.

---

# 73. Notification Reliability

A command should not be considered executed merely because FCM delivered a message.

States must distinguish:

```text
PUSH_SENT
DEVICE_FETCHED
DEVICE_VERIFIED
EXECUTING
EXECUTED
ACKNOWLEDGED
```

---

# 74. Incident Correlation Rules

Example YAML:

```yaml
incidentPolicies:
  - id: lost-device
    trigger: ADMIN_LOST_DECLARATION
    actions:
      - LOCK
      - ENTER_LOST_STATE
      - NOTIFY_SECURITY_ADMINS

  - id: repeated-auth-failure
    trigger: FAILED_AUTH >= 5
    actions:
      - LOCK
      - ENTER_PROTECTED
      - NOTIFY_SECURITY_ADMINS

  - id: high-confidence-integrity-anomaly
    trigger: INTEGRITY_FAILURE && DEVICE_OWNER_ACTIVE
    actions:
      - ALERT
      - ENTER_SUSPICIOUS
```

Do not encode a wipe in a low-confidence event rule by default.

---

# 75. Scheduled Security Jobs

Backend jobs:

```text
expire_commands
expire_approvals
reconcile_devices
check_stale_heartbeats
rotate_signing_keys
archive_events
verify_audit_chain
refresh_device_capabilities
```

---

# 76. Alert Suppression

Prevent notification flooding.

Example:

```text
Same event/device/severity
within 5 minutes
→ aggregate into one alert
```

But CRITICAL incidents should bypass ordinary suppression where appropriate.

---

# 77. Policy Simulation Mode

Before activating a policy, administrators can run it in simulation mode.

```text
POLICY SIMULATION

Rules triggered: 3
Would lock: 2 devices
Would alert: 5 devices
Would wipe: 0 devices

[Cancel] [Activate]
```

This is especially useful for fleet deployments.

---

# 78. Dry-Run Wipe Workflow

A special administrative test operation:

```text
WIPE DRY RUN

✓ authorization validated
✓ approval workflow validated
✓ command signature generated
✓ device reachable
✗ actual wipe NOT executed
```

This prevents test environments from accidentally destroying real devices.

---

# 79. Test Environments

Maintain:

```text
LOCAL
DEV
STAGING
PRODUCTION
```

Production bot token must never be used in development.

Production signing keys must never be present in source control or local test fixtures.

---

# 80. Automated Testing Strategy

## 80.1 Unit tests

Test:

- command parser;
- command authorization;
- signature validation;
- nonce validation;
- state transitions;
- policy evaluation;
- role permissions;
- approval requirements.

## 80.2 Android integration tests

Test on real managed devices/emulators where supported:

- enrollment;
- Device Owner state;
- lock;
- supported restrictions;
- policy application;
- offline queue;
- FCM notification flow.

## 80.3 Backend integration tests

Test:

```text
Telegram webhook → command → authorization → queue → device acknowledgement
```

## 80.4 Security tests

Include:

- replay attempts;
- invalid signatures;
- wrong device ID;
- expired commands;
- revoked administrator;
- unauthorized Telegram user;
- approval reuse;
- duplicate command;
- policy rollback;
- stolen session token.

---

# 81. Security Test Cases

### Test: unauthorized Telegram user

Expected:

```text
No device information exposed
No command created
Audit security event recorded
```

### Test: expired wipe command

Expected:

```text
Device rejects
Backend marks EXPIRED
No wipe
```

### Test: replayed lock command

Expected:

```text
Duplicate command rejected or safely treated as idempotent
```

### Test: wrong device receives command

Expected:

```text
Signature/device binding validation fails
No privileged action
```

### Test: backend loses connectivity

Expected:

```text
Local policy remains active
No stale destructive command executes after reconnection
```

---

# 82. Disaster Recovery

Scenario: database destroyed.

Recovery:

```text
Restore encrypted backup
Restore key references
Restore administrators
Restore devices
Reconcile device heartbeats
Reissue only valid commands
```

Never automatically reissue an expired destructive command after disaster recovery.

---

# 83. Key Loss Strategy

A serious system must plan for loss of signing keys.

Implement:

```text
active key
previous key
next key
revocation list
key version
```

If the command-signing key is suspected compromised:

```text
1. disable key
2. stop privileged command issuance
3. issue new signing key
4. rotate trust configuration
5. revalidate devices
6. record incident
```

---

# 84. Administrator Emergency Lockout

Provide:

```text
DISABLE ALL REMOTE COMMANDS
```

Use this when administrator credentials or backend signing keys are suspected compromised.

This should stop new privileged commands while leaving already-installed local device policies operating.

---

# 85. Fleet Emergency Lockdown

Optional enterprise action:

```text
LOCKDOWN FLEET
```

Requires extremely strong authorization and should support filtering:

```text
all devices
selected group
selected policy profile
selected geography only if lawfully configured
```

Do not make a fleet wipe a one-click operation.

---

# 86. Device Groups

Support:

```text
ALL_DEVICES
OFFICE
FIELD
EXECUTIVE
TEST
HIGH_SECURITY
```

Devices can have one or more labels, but actual authorization remains explicit and server-enforced.

---

# 87. Policy Templates

Example:

```text
PERSONAL-OWNED-MANAGED
STANDARD-FLEET
HIGH-SECURITY
DEDICATED-KIOSK
LOST-DEVICE
EMERGENCY-LOCKDOWN
```

---

# 88. Kiosk / Dedicated Mode (Optional)

For dedicated organization-owned devices, Astra may support Android's dedicated-device use cases where supported.

Possible applications:

```text
payment terminal
field collection device
warehouse terminal
company tablet
single-purpose phone
```

This is separate from ordinary personal-device management.

---

# 89. User Experience Principles

The UI must always make security state obvious.

Bad:

```text
Action successful
```

Good:

```text
DEVICE LOCKED

ASTRA-0001
08:42:18

Source: Administrator
Command: cmd_123
```

For destructive actions:

```text
PERMANENT ACTION
NOT REVERSIBLE
```

---

# 90. Accessibility

Android and web UI should support:

- large text;
- screen readers;
- high contrast;
- keyboard navigation;
- clear action labels;
- confirmation dialogs that are not ambiguous.

---

# 91. Localization

Recommended initial languages:

```text
English
Bangla
```

Security terminology should be translated consistently.

Example:

```text
Normal — স্বাভাবিক
Watch — পর্যবেক্ষণ
Suspicious — সন্দেহজনক
Protected — সুরক্ষিত মোড
Lost — হারানো/লস্ট মোড
Critical — সংকটজনক
Wipe — সম্পূর্ণ মুছে ফেলা
```

---

# 92. Project Folder Structure

```text
ASTRA-SENTINEL/
│
├── android/
│   ├── app/
│   ├── core/
│   ├── dpc/
│   ├── feature/
│   └── gradle/
│
├── backend/
│   ├── src/
│   ├── migrations/
│   └── tests/
│
├── telegram/
│   ├── handlers/
│   ├── keyboards/
│   └── tests/
│
├── web-console/
│   ├── src/
│   └── tests/
│
├── infra/
│   ├── docker/
│   ├── nginx/
│   └── monitoring/
│
├── docs/
│   ├── architecture/
│   ├── security/
│   └── operations/
│
├── scripts/
└── README.md
```

---

# 93. Development Phases

## Phase 0 — Threat model & platform validation

Deliverables:

- target Android versions/devices;
- supported enrollment flow;
- security boundaries;
- API capability matrix;
- physical-device test plan.

## Phase 1 — DPC foundation

Deliverables:

- Android project;
- Device Owner provisioning;
- local device identity;
- Keystore key generation;
- device registration;
- basic dashboard;
- lock operation.

## Phase 2 — Control plane

Deliverables:

- backend;
- PostgreSQL;
- device registry;
- command service;
- audit log;
- HTTPS communication;
- command acknowledgement.

## Phase 3 — Telegram

Deliverables:

- bot;
- webhook;
- secret-token verification;
- allow-listed admins;
- inline buttons;
- lock/lost/status commands.

## Phase 4 — Security engine

Deliverables:

- state machine;
- local policy engine;
- event collection;
- incident engine;
- protected mode;
- offline command queue.

## Phase 5 — Strong cryptography

Deliverables:

- command signatures;
- replay protection;
- key rotation;
- device attestation where available;
- integrity verification.

## Phase 6 — Destructive workflow

Deliverables:

- wipe command;
- confirmation;
- dual approval;
- audit ledger;
- dry-run workflow;
- recovery flow.

## Phase 7 — Web console

Deliverables:

- fleet dashboard;
- incidents;
- policies;
- approvals;
- audit.

## Phase 8 — Enterprise hardening

Deliverables:

- staged policy rollout;
- device groups;
- fleet policies;
- metrics;
- key-management service/HSM option;
- disaster recovery.

---

# 94. MVP Definition

The first usable release should implement only:

```text
✓ Device Owner enrollment
✓ Device registration
✓ Secure backend
✓ Telegram administrator allow-list
✓ Telegram /status
✓ Telegram /lock
✓ Telegram /lost
✓ Web device dashboard
✓ Device heartbeat
✓ Signed command envelope
✓ Command acknowledgement
✓ Audit logs
✓ Basic local policy engine
```

Wipe should be introduced after lock/enrollment/authorization paths have passed repeated testing.

---

# 95. Version 1.0 Definition

```text
✓ DPC
✓ Device identity
✓ Enrollment
✓ Telegram
✓ Web console
✓ Lock
✓ Lost mode
✓ Protect mode
✓ Wipe
✓ Dual approval
✓ Security states
✓ Event collection
✓ Offline policies
✓ Command replay protection
✓ Audit logging
✓ Policy versioning
✓ Capability negotiation
✓ Backup/restore
```

---

# 96. Version 2.0 Definition

```text
✓ hardware-backed identity / attestation where supported
✓ Play Integrity integration where appropriate
✓ device groups
✓ staged rollout
✓ advanced incident correlation
✓ administrator passkeys
✓ emergency administrator app
✓ advanced dashboards
✓ key rotation service
✓ HSM-backed signing option
✓ expanded Android Enterprise integration
```

---

# 97. Production Readiness Checklist

### Android

- [ ] Device Owner enrollment tested on every supported target.
- [ ] Lock tested.
- [ ] Wipe tested on lab devices.
- [ ] Local policies tested offline.
- [ ] Key generation tested.
- [ ] Key rotation tested.
- [ ] App update tested without losing Device Owner state.
- [ ] Reboot behavior tested.
- [ ] Battery/power behavior tested.

### Backend

- [ ] TLS configured.
- [ ] Secrets externalized.
- [ ] RBAC enforced server-side.
- [ ] MFA enabled.
- [ ] Command signatures tested.
- [ ] Replay prevention tested.
- [ ] Audit records tested.
- [ ] Backups tested.
- [ ] Disaster recovery tested.

### Telegram

- [ ] Webhook HTTPS.
- [ ] Telegram secret token configured.
- [ ] Allowed user IDs configured.
- [ ] Unauthorized command tests passed.
- [ ] Destructive confirmation tested.
- [ ] Dual approval tested.

### Operations

- [ ] Production monitoring.
- [ ] Incident response procedure.
- [ ] key rotation procedure.
- [ ] credential compromise procedure.
- [ ] server outage procedure.
- [ ] device recovery procedure.
- [ ] wipe authorization policy.

---

# 98. Example Full Command Lifecycle

```text
ADMINISTRATOR
    │
    │ /lock ASTRA-0001
    ▼
TELEGRAM
    │
    │ webhook
    ▼
TELEGRAM GATEWAY
    │
    │ verify secret
    ▼
AUTH/RBAC
    │
    │ authorized
    ▼
COMMAND SERVICE
    │
    │ create command
    ▼
SIGNING SERVICE
    │
    │ sign
    ▼
DEVICE QUEUE
    │
    │ FCM / heartbeat
    ▼
ASTRA DPC
    │
    │ validate signature
    │ validate sequence
    │ validate expiry
    ▼
DEVICE POLICY CONTROLLER
    │
    │ lockNow()
    ▼
ANDROID DEVICE
    │
    │ result
    ▼
ASTRA DPC
    │
    │ signed ACK
    ▼
COMMAND SERVICE
    │
    │ audit
    ▼
TELEGRAM
    │
    ▼
✅ DEVICE LOCKED
```

---

# 99. Example Wipe Lifecycle

```text
ADMIN A
  │
  └── /wipe ASTRA-0001
          │
          ▼
     Authorization
          │
          ▼
     Wipe requested
          │
          ▼
     Strong confirmation
          │
          ▼
     ADMIN B approval
          │
          ▼
     Final command
          │
          ▼
     Signed + expiring command
          │
          ▼
     Device validation
          │
          ▼
     Wipe executor
          │
          ▼
     Android managed-device wipe API
          │
          ▼
     WIPED / UNKNOWN state
```

Never represent “Telegram button clicked” as equivalent to “device wiped.”

---

# 100. Limitations and Reality Check

ASTRA can be powerful, but it is not magic.

### It cannot guarantee:

- communication with a powered-off device;
- communication when no network path exists;
- control after hardware failure;
- control of an arbitrary non-managed phone as though it were Device Owner;
- prevention of every possible physical or firmware attack;
- identical management capability across every OEM/device/version.

### It can improve resilience through:

- local policies;
- multiple communication paths;
- managed-device enrollment;
- hardware-backed identity where available;
- server-side command authorization;
- command expiration;
- cryptographic signing;
- audit logging;
- strong administrator authentication;
- staged containment.

---

# 101. Recommended Security Philosophy

The most important design rule is:

```text
DETECT
  ↓
VERIFY
  ↓
CONTAIN
  ↓
ALERT
  ↓
AUTHORIZE
  ↓
EXECUTE
  ↓
VERIFY RESULT
  ↓
AUDIT
```

Not:

```text
EVENT
 ↓
FORMAT PHONE
```

This distinction makes ASTRA more reliable and substantially reduces accidental destruction from false positives.

---

# 102. Recommended Final Product Name

Suggested branding:

```text
ASTRA SENTINEL
Private Android Security & Device Control
```

Components:

```text
ASTRA Agent       Android DPC
ASTRA Core        Backend Control Plane
ASTRA Command     Telegram Gateway
ASTRA Console     Web Dashboard
ASTRA Guard       Security Engine
ASTRA Vault       Key / Enrollment Service
ASTRA Watch       Monitoring / Incident Engine
```

---

# 103. Reference Documentation

The following are the primary official references used when designing this specification.

1. **Android DevicePolicyManager API**  
   https://developer.android.com/reference/kotlin/android/app/admin/DevicePolicyManager

2. **Android Enterprise — Provision devices**  
   https://developers.google.com/android/management/provision-device

3. **Android Enterprise — Fully managed devices / policies**  
   https://developers.google.com/android/management/reference/rest/v1/enterprises.policies

4. **Android Security — Security checklist**  
   https://developer.android.com/privacy-and-security/security-tips

5. **Android Security — Understand device security state**  
   https://developer.android.com/privacy-and-security/understand-device-security-state

6. **Android Keystore / key attestation**  
   https://developer.android.com/privacy-and-security/security-key-attestation

7. **Android Keystore security package**  
   https://developer.android.com/reference/android/security/package-summary

8. **Android Network Security Configuration**  
   https://developer.android.com/privacy-and-security/security-config

9. **Telegram Bot API**  
   https://core.telegram.org/bots/api

10. **Play Integrity / Google Play developer documentation**  
    https://developer.android.com/google/play/integrity

---

# 104. Final Implementation Recommendation

Build ASTRA in this order:

```text
1. Device Owner enrollment
2. Hardware/Keystore device identity
3. Backend device registry
4. Signed command protocol
5. Heartbeat + command queue
6. Telegram gateway
7. Remote lock
8. Lost mode
9. Security state machine
10. Local offline policy engine
11. Event/incident engine
12. Protected mode
13. Secure wipe workflow
14. Dual approval
15. Web dashboard
16. Integrity/attestation enhancements
17. Fleet/enterprise features
```

The first milestone should be a **lab-only prototype using authorized test devices**. Do not begin with production wipe functionality. Validate enrollment, identity, command authentication, locking, offline behavior, audit logging, and recovery first. Only then enable destructive operations on controlled test devices.

---

# 105. Architecture Summary

```text
                         ASTRA SENTINEL
                               │
        ┌──────────────────────┼──────────────────────┐
        │                      │                      │
   TELEGRAM BOT          WEB CONSOLE          ADMIN APP (optional)
        │                      │                      │
        └──────────────────────┼──────────────────────┘
                               │
                       AUTH + RBAC + MFA
                               │
                     ┌─────────▼─────────┐
                     │ COMMAND AUTHORITY │
                     └─────────┬─────────┘
                               │
        ┌──────────────────────┼──────────────────────┐
        │                      │                      │
   POLICY ENGINE         THREAT ENGINE          AUDIT LEDGER
        │                      │                      │
        └──────────────────────┼──────────────────────┘
                               │
                        SIGNED COMMAND
                               │
                     ┌─────────▼─────────┐
                     │   DEVICE GATEWAY  │
                     └─────────┬─────────┘
                               │
                      FCM / HTTPS / QUEUE
                               │
                     ┌─────────▼─────────┐
                     │ ASTRA ANDROID DPC │
                     ├───────────────────┤
                     │ Device Owner      │
                     │ Keystore Identity │
                     │ Integrity         │
                     │ Local Policies    │
                     │ Event Engine      │
                     │ Command Verifier  │
                     └─────────┬─────────┘
                               │
                           ANDROID OS
                               │
          ┌────────────────────┼────────────────────┐
          │                    │                    │
        LOCK                 PROTECT              WIPE
```

---

# 106. Project Completion Criteria

ASTRA Sentinel should be considered production-ready only when:

1. Every supported device is enrolled through an authorized managed-device flow.
2. Every privileged command requires authenticated authorization.
3. Every device has a verified identity appropriate to the deployment.
4. Commands are signed, time-bound, and replay-resistant.
5. Telegram cannot bypass backend authorization.
6. Wipe requires explicit configurable safeguards.
7. Offline behavior is deterministic and tested.
8. Device capabilities are checked before policy/action execution.
9. Audit records exist for all security-sensitive actions.
10. Backup and recovery procedures have been tested.
11. Real-device testing covers the target Android/OEM combinations.
12. The system clearly communicates platform limitations rather than pretending to have unrestricted control.

---

## End of ASTRA SENTINEL Full Project Specification

**Status:** Architecture and implementation blueprint  
**Next engineering artifact:** repository scaffold + Android DPC skeleton + backend API skeleton + Telegram gateway skeleton + PostgreSQL migrations + local development deployment
