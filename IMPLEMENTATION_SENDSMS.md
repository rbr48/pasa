# `/sendsms` Command Implementation Summary

## Overview
Implemented a complete SMS sending command for PASA Sentinel that allows remote SMS transmission through the device's cellular radio with full dual-SIM support.

## Features Implemented

### 1. **SendSmsCommand.kt** (New)
Complete implementation with the following capabilities:

#### Command Usage
```
/sendsms <number> <message>          # Uses default active SIM
/sendsms sim1 <number> <message>     # Forces SIM slot 1
/sendsms sim2 <number> <message>     # Forces SIM slot 2
```

#### Examples
```
/sendsms +8801XXXXXXXXX Ping from PASA
/sendsms sim2 +1234567890 Device tracking enabled
```

#### Features
- **Dual-SIM Support**: Target specific SIM slots on dual-SIM devices
- **Phone Number Verification**: Validates phone number format (7-15 digits, optional + prefix)
- **Message Length Handling**: Automatically splits long messages (max 459 chars = 3 SMS)
- **Multipart SMS**: Uses `SmsManager.sendMultipartTextMessage()` for proper handling
- **Caller ID**: Recipient sees the device's SIM phone number (solves "unknown number" issue)
- **Error Handling**: Graceful fallback to default SIM if slot unavailable
- **Subscription Management**: Uses Android's SubscriptionManager for proper SIM detection

### 2. **CommandExecutor.kt** (Modified)
- Added `sendSmsCommand` dependency injection
- Added routing: `"/sendsms", "/send_sms" -> sendSmsCommand`

### 3. **CommandParser.kt** (Modified)
- Added natural language support: `clean.contains("send sms")` maps to `/sendsms`
- Users can type "send sms" or "sendsms" in natural conversation

### 4. **HelpCommand.kt** (Modified)
- Added `/sendsms` documentation to the help menu
- Placed in "Extraction & Audit Logs" section with `/sms_log` for logical grouping

## Technical Implementation Details

### Dual-SIM Architecture
```kotlin
// Retrieves subscription IDs and targets specific SIM slot
private fun getSmsManagerForSlot(slotIndex: Int): SmsManager?
  ├─ Uses SubscriptionManager.activeSubscriptionInfoList
  ├─ Gets subscription ID for requested slot
  ├─ Creates SmsManager via createForSubscriptionId() [Android 12+]
  └─ Fallback to getSmsManagerForSubscriptionId() [pre-Android 12]
```

### SMS Sending Pipeline
```
User Command: /sendsms sim2 +1234567890 Test
     ↓
Parser: Validates SIM selector, phone number, message
     ↓
getSmsManager(sim2): Retrieves correct SmsManager for SIM slot 2
     ↓
divideMessage(): Splits into multipart if needed (160 chars per SMS)
     ↓
sendMultipartTextMessage(): Transmits via Android Telephony API
     ↓
Response: "✅ SMS Sent Successfully (2 parts via SIM 2)"
```

### Validation
- **Phone Number**: Regex validation `^\\+?[0-9]{7,15}$`
- **Message Length**: Max 459 characters (3 SMS messages)
- **Arguments**: Proper parsing of SIM selector and positional args

## How It Solves the "Unknown Phone Number" Issue

### Problem
Many mobile operators (especially in Bangladesh) do not store the phone number on the SIM chip itself, so the OS cannot locally read its own number. The owner needs to verify which phone number the device's SIM is using.

### Solution
1. Owner sends: `/sendsms +8801XXXXXXXXX Ping from PASA`
2. PASA immediately transmits SMS through the device's cellular radio
3. Owner receives SMS with caller ID showing the device's actual SIM phone number
4. Owner now knows the exact line number to track the device

## Integration Points

### Already Present
- ✅ `SEND_SMS` permission in AndroidManifest.xml
- ✅ Device Owner privileges for system control
- ✅ Telegram API for command relay
- ✅ SmsCommandReceiver for incoming SMS auth
- ✅ TOTP-based offline SMS command authentication

### Backward Compatible
- No breaking changes to existing commands
- Follows established Command interface pattern
- Compatible with license tier system (no gating required)
- Works with existing auth and logging infrastructure

## Security Considerations

### Authentication
- Requires Telegram authorization (matches existing owner-chat validation)
- Can also be triggered via SMS commands with TOTP/Master PIN

### Audit Logging
- Command execution logged via CommandExecutor.logExecution()
- Full audit trail of SMS sent (number, message, SIM slot, timestamp)

### Rate Limiting
- Message length capped to prevent abuse
- No explicit rate limiting (can be added if needed)
- SmsManager handles carrier rate limits

## Future Enhancements (Optional)

1. **Incoming SMS Automation**: Automatically parse received SMS and trigger actions
2. **SMS Template System**: Pre-defined message templates for common alerts
3. **SIM Status Monitoring**: Display active SIM slots and their line numbers
4. **SMS Rate Limiting**: Add per-minute send limits to prevent carrier blocks
5. **Delivery Notifications**: Parse SMS delivery reports (requires Broadcast Receiver)

## Files Modified

```
✅ app/src/main/java/com/izhaanintellect/pasa/commands/SendSmsCommand.kt          [NEW]
✅ app/src/main/java/com/izhaanintellect/pasa/bot/CommandExecutor.kt             [MODIFIED]
✅ app/src/main/java/com/izhaanintellect/pasa/bot/CommandParser.kt               [MODIFIED]
✅ app/src/main/java/com/izhaanintellect/pasa/commands/HelpCommand.kt            [MODIFIED]
```

## Testing Checklist

- [ ] Compile verification (gradle build)
- [ ] Telegram command: `/sendsms +XXXXXXXXX Test message`
- [ ] Dual-SIM test: `/sendsms sim1 +XXXXXXXXX Test from SIM 1`
- [ ] Dual-SIM test: `/sendsms sim2 +XXXXXXXXX Test from SIM 2`
- [ ] Long message split: Send 400+ character message (verify multipart)
- [ ] Invalid number rejection: `/sendsms invalid Test` (should fail)
- [ ] SMS command auth: Send SMS from another phone `PASA <PIN> /sendsms +XXXXXXXXX Test`
- [ ] Help display: `/help` shows new command documentation
- [ ] Natural language: "send sms +XXXXXXXXX Test" (should trigger command)

## Next Build Steps

1. Compile the project: `./gradlew build`
2. Deploy to Device Owner managed device
3. Test both Telegram and SMS command paths
4. Verify SIM detection on dual-SIM device
5. Monitor logs for any permission or SmsManager initialization errors

---

**Status**: ✅ Ready for compilation and testing
**Version**: v3.4.0+ compatible
**Permissions Required**: `SEND_SMS` (already declared)
**Min SDK**: API 21 (full dual-SIM API 31+)
