# `/sendsms` Command - Quick Usage Guide

## Problem It Solves
Many mobile operators don't store the SIM phone number on the chip itself. This command lets you transmit an SMS to any number and see your device's actual SIM caller ID when the recipient receives it — instantly revealing your phone number without guessing.

## Command Syntax

### Basic Usage
```
/sendsms <phone_number> <message>
```

**Example:**
```
/sendsms +8801234567890 Ping from PASA
```

### Dual-SIM Targeting
On devices with 2 SIM cards:

```
/sendsms sim1 <phone_number> <message>    # Force SIM slot 1
/sendsms sim2 <phone_number> <message>    # Force SIM slot 2
```

**Example:**
```
/sendsms sim2 +1234567890 Tracker active
```

## Phone Number Format

- **Minimum**: 7 digits
- **Maximum**: 15 digits
- **Optional**: Leading `+` sign (international format)

### Valid Examples
- `+8801234567890` ✅
- `+1234567890` ✅
- `8801234567890` ✅
- `1234567890` ✅

### Invalid Examples
- `123456` ❌ (too short)
- `+12345678901234567` ❌ (too long)
- `123-456-7890` ❌ (special characters)

## Message Guidelines

- **Maximum**: 459 characters (automatically sent as 3 SMS if needed)
- **Minimum**: 1 character
- **Best Practice**: Keep under 160 characters for single SMS delivery

### Message Length Examples
- `"Test"` → 1 SMS (4 chars)
- `"This is a longer test message..."` → 1 SMS (160 chars)
- `"This is a very long message that exceeds 160 characters and will be automatically split into multiple SMS parts by the Android system..."` → 2-3 SMS

## Workflow Example: Finding Your SIM Number

### Scenario
You have an Android device (e.g., with Bangladeshi SIM) whose phone number you don't know.

### Steps

1. **From Telegram**, send to PASA:
   ```
   /sendsms +8801234567890 Sentinel active
   ```

2. **To your own phone**, send SMS with:
   ```
   +8801234567890
   ```
   Recipient shows: "Sentinel active"
   **Caller ID shows**: Your device's actual SIM number (e.g., `+8801912345678`)

3. **Now you know**: Device's SIM is `+8801912345678` ✅

### Why This Works
- PASA sends SMS directly through cellular radio
- Carrier inserts your SIM's number as the "From" field
- Recipient's phone displays the actual line number in caller ID
- This is the **real** phone number your device is using

## Dual-SIM Device Example

### Setup
- **SIM 1**: Verizon (US) `+12025551234`
- **SIM 2**: Grameenphone (Bangladesh) `+8801912345678`

### Testing Each SIM

**Test SIM 1:**
```
/sendsms sim1 +1 Text from Verizon
```
Caller ID → `+12025551234` ✅

**Test SIM 2:**
```
/sendsms sim2 +88 Text from Grameenphone
```
Caller ID → `+8801912345678` ✅

## Response Messages

### Success
```
✅ SMS Sent Successfully
━━━━━━━━━━━━━━━━━━━━
📱 To: +8801234567890
🔗 Via: Default SIM
💬 Parts: 1 SMS message

ℹ️ Recipient will see caller ID as your device's SIM number.
```

### Error: Invalid Number
```
❌ Invalid Phone Number
━━━━━━━━━━━━━━━━━━━━
⚠️ Phone must be 7-15 digits, optionally prefixed with +
```

### Error: Message Too Long
```
❌ Message Too Long
━━━━━━━━━━━━━━━━━━━━
⚠️ Maximum 459 characters allowed (3 SMS messages).
Your message is 512 characters.
```

### Error: Invalid Arguments
```
❌ Invalid Arguments
━━━━━━━━━━━━━━━━━━━━
⚠️ Phone number is required.
```

## Natural Language Support

You can also trigger the command conversationally:

```
Send SMS +1234567890 Test message
```

This automatically converts to:
```
/sendsms +1234567890 Test message
```

## Security & Privacy

✅ **Secure**
- Requires Telegram authorization (matches owner-chat)
- Can also be triggered via SMS with TOTP/Master PIN
- All SMS sends are audit logged

⚠️ **Note**
- SMS will appear in your device's SMS sent folder
- The message body and recipient are logged
- SIM carrier may track SMS history

## Offline SMS Command

You can also send SMS commands directly from another phone if you're on the same cellular network:

```
PASA <6-digit-TOTP-or-MasterPIN> /sendsms +1234567890 Remote alert
```

This lets you control the device even if Telegram/WiFi is unavailable.

**Setup offline SMS auth:**
```
/smssetup
```

## Common Use Cases

### 1. Verify Device Line Number (Primary Use)
```
/sendsms +8801912345678 ping
```
→ Caller shows your device's SIM number

### 2. Send Alert from Device to Your Phone
```
/sendsms +8801912345678 Device compromised - PASA activated
```

### 3. Test Dual-SIM Connectivity
```
/sendsms sim1 +1234567890 SIM1 check
/sendsms sim2 +8801234567890 SIM2 check
```

### 4. Multi-Part Message Delivery
```
/sendsms +1234567890 This is a longer message that will automatically be split into multiple SMS parts for delivery. The Android system handles this transparently. You just send it and it arrives as needed.
```

## Troubleshooting

### "SMS Sent but recipient didn't receive"
- **Cause**: Carrier SMS filtering or invalid number
- **Fix**: Verify number format, check carrier SMS limits

### "Permission Denied"
- **Cause**: SEND_SMS permission not granted
- **Fix**: Device Owner should auto-grant, else check system settings

### "SIM slot not available (only 1 active SIM)"
- **Cause**: You requested sim2 but device is single-SIM
- **Fix**: Use `/sendsms` without SIM selector (uses default)

### "Empty message"
- **Cause**: No message text provided
- **Fix**: `/sendsms +1234567890 <your message here>`

## Related Commands

- `/smssetup` — Enroll TOTP for offline SMS commands
- `/sms_log` — View recent SMS inbox
- `/status` — Check cellular network signal
- `/tower` — Dual-SIM cell tower info & triangulation

---

**Version**: PASA v3.4.0+
**Requirements**: SEND_SMS permission + Telegram authorization
**Min SDK**: API 21 (optimized for API 31+ dual-SIM)
