# TVS Connect BLE Protocol Analysis

## Overview
Based on decompilation of `com.tvsm.connect` (TVS Connect APK), the bike uses a **custom BLE protocol** with frame-based communication, XOR encryption, and checksum validation.

## 1. Service & Characteristic UUIDs

The app uses **PreferenceUtils** to load UUIDs dynamically from SharedPreferences. The keys are:

### For NTorq XT & Similar Models (U399C & N109):

**U399C Service (Likely Primary - Telematics):**
```
Keys used by app:
- SERVICE_UUID_U399C
- RECEIVE_CHARACTERISTIC_UUID_U399C
- SEND_CHARACTERISTIC_UUID_U399C
```
*Location: DigiDocUtils.java line 54855-54857*

**N109 Service (Caller ID & Document Transfer):**
```
Keys used by app:
- SERVICE_UUID_N109
- RECEIVE_CHARACTERISTIC_UUID_N109  (for receiving data from bike)
- SEND_CHARACTERISTIC_UUID_N109     (for sending commands to bike)
```
*Location: DigiDocUtils.java line 54858-54859*

**Hardcoded Service UUID found:**
```java
public static final UUID f54844y = UUID.fromString("5456534D-5647-5341-5342-454E544F5251");
// Decodes to: TVSMVGSABENTORQ (hex: 5456534D = TVSM, rest = VGSABENTORQ)
```
*Location: DigiDocUtils.java line 64*

### Other Models:
- **N251, N112, N360, U368, U400, U407, U408, U507, RTR_U368, RTR_U400, RTR_160_IOT** — each have their own service/characteristic UUIDs

---

## 2. BLE Frame Format

The protocol uses **fixed frame structure** with the following pattern:

```
[HEADER] [LENGTH] [DATA...] [CHECKSUM] [TRAILER]
   1B        1B      nB         1B         1B
  0x5A    0x5A-    Variable   Computed   0xFF
  (90)    0xXX     XOR-enc.    sum mod   (255)
                    payload    256
```

### Frame Components:

**Header (1 byte):** Always `0x5A` (90 decimal)

**Command/Opcode (1 byte):** 
- `0xA5` = ping/heartbeat
- `0xF1` = caller ID
- `0x53` = profile/document transfer
- `0xEF` = image data transmission
- etc.

**Length (variable):**
- If <= 255: single byte `0x00` or `0xXX`
- If > 255: BigInteger encoding

**Data Payload (variable):**
- XOR encrypted with repeating `0xEA` pattern
- Each byte = data[i] ^ 0xEA

**Checksum (1 byte):**
```
checksum = 255 - ((sum_of_all_data_bytes % 256))
```
*Location: U399ChecksumUtils.java lines 25-31*

**Trailer (1 byte):** Always `0xFF` (255 decimal)

---

## 3. Encryption Scheme

**Type:** XOR cipher (NOT secure for production, but used here)

**Key:** Repeating pattern of `0xEA` (234 decimal)

**How it works:**
```
encrypted[i] = plaintext[i] XOR 0xEA
decrypted[i] = ciphertext[i] XOR 0xEA
```

The actual key byte array used:
```java
byte[] keyByteArray = {
    90, 125, 77, 68, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 65, 66, 67, 48, 49, -1
};
// Pattern: 0x5A, 0x7D, 0x4D, 0x44, 0x30... (suggests "5A 7D M D 0 1 2 3 4 5 6 7 8 9 A B C 0 1")
```

**Implementation:** U399EncryptDecryptDataUtils.java lines 22-44

---

## 4. Command Examples (from DigiDocUtils.java)

### Caller ID / Document Transfer Commands:

```java
// Resume Image Transmission (with timestamp)
byte[] command = {
    (byte)0x5A,      // header
    (byte)0xEF,      // opcode for image data
    2, 1, 0, 0, 0, 0, 0,
    (byte)calendar.get(5),           // day of month
    (byte)(calendar.get(2) + 1),     // month (0-indexed)
    (byte)Integer.parseInt(year_last_2_digits),
    0, 0, 2, 0, 0, 0, 0,
    (byte)0xFF       // trailer
};
```

### Profile Image Transfer (0x5A 0x53):
```java
byte[] command = { 0x5A, 0x53, ... };  // 0x53 = 83 decimal = 'S' (likely "Send")
```

### Caller Name Transmission:
```java
// createCallerNameArray() in BluetoothUtil constructs:
[0x5A] [OPCODE] [name_bytes_xor_encrypted] [checksum] [0xFF]
```

---

## 5. Data Direction

### TO BIKE (Outbound writes):
- Characteristic: **SEND_CHARACTERISTIC_UUID_N109** / **SEND_CHARACTERISTIC_UUID_U399C**
- Direction: App → Bike (command/request)

### FROM BIKE (Inbound notifications):
- Characteristic: **RECEIVE_CHARACTERISTIC_UUID_N109** / **RECEIVE_CHARACTERISTIC_UUID_U399C**
- Direction: Bike → App (telemetry/response)
- Callback: `onCharacteristicUpdate()` → `processData()` → `getDecryptedData()`

---

## 6. Connection Flow

### Service Discovery:
1. App calls `requestConnectionPriority(1)` (HIGH priority)
2. Discovers all services on the bike's GATT database
3. Extracts UUIDs for each model type (N109, U399C, etc.)
4. Subscribes to RECEIVE characteristic → `setCharacteristicNotification()`

### Data Exchange:
```
App sends command via:
  bluetoothPeripheral.writeCharacteristic(characteristic, encryptedBytes, 1)
  
Bike responds via notification:
  onCharacteristicUpdate() → decryptData() → processData()
```

---

## 7. Security Notes

⚠️ **This is NOT cryptographically secure:**
- XOR with fixed key = trivial to break
- No authentication or key exchange
- Checksum is not a HMAC (can be forged)
- All UUIDs are static and predictable

**But it DOES prevent:**
- Casual packet sniffer analysis
- Easy modification of packets without knowing the XOR pattern
- Accidental OTA compatibility

---

## 8. Important Files

| File | Purpose |
|------|---------|
| `DigiDocUtils.java` | Caller ID, document/image transfer protocol |
| `U399ChecksumUtils.java` | Frame checksum calculation |
| `U399EncryptDecryptDataUtils.java` | XOR encryption/decryption |
| `BaseConnectHelperService.java` | Main BLE service, characteristic callbacks |
| `BluetoothPeripheral.java` | Low-level GATT operations |
| `BluetoothBytesParser.java` | Parses incoming telemetry frames |
| `BluetoothUtil.java` | Utility functions (caller name arrays, etc.) |
| `SecretIdentifier.java` | Constants for preference keys |

---

## 9. Next Steps for OpenTorq

1. **Extract real UUIDs:**
   - Use nRF Connect to enumerate bike's GATT database
   - Note exact SERVICE_UUID and characteristic UUIDs for NTorq XT
   - Save to a config file

2. **Capture traffic:**
   - Enable HCI snoop log on Android device
   - Use TVS Connect to interact with bike (speed check, settings, etc.)
   - Analyze in Wireshark with filter: `btatt && bluetooth.dst == [bike_MAC]`

3. **Reverse-engineer payloads:**
   - Identify which byte in each frame = speed, RPM, gear, throttle, etc.
   - Map opcode prefixes (0x5A 0xA5 = heartbeat, etc.)

4. **Build Python PoC:**
   - Implement frame builder: [0x5A][opcode][data][checksum][0xFF]
   - Implement XOR cipher
   - Test connection and telemetry parsing

5. **Android Implementation:**
   - Use Nordic Semiconductor's BLE library (`no.nordicsemi.android.ble`)
   - Implement same frame/checksum/encryption logic
   - Add auto-reconnect with `CompanionDeviceManager`

---

## Key Discoveries

✅ **Frame header is 0x5A 0xA5 or 0x5A + opcode**
✅ **Checksum: 255 - (sum % 256)**
✅ **Encryption: XOR with 0xEA byte pattern**
✅ **Trailer: 0xFF always**
✅ **UUIDs loaded from SharedPreferences (not hardcoded)**
✅ **Service for telematics: U399C or N109 (model-dependent)**
✅ **Caller ID uses same N109 service**

