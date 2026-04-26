# U399 (NTorq XT) Telemetry Payload Byte Map

**Source:** `com.tvsm.connect.bluetooth.sendreceive.U399DataParser` (decompiled)  
**Frame length:** 20 bytes (enforced in `parseData()`)  
**Applied to:** Post-XOR-decrypted payload

## Byte Layout

| Byte(s) | Field | Formula | Method in source |
|---------|-------|---------|-----------------|
| [0] | Frame header | always `0x5A` | — |
| [1] | Opcode | — | — |
| [2] | **Speed (km/h)** | `byte[2] & 0xFF` | `Z()` |
| [3][4][5] | **Odometer (km)** | `BigInteger(b[3],b[4],b[5]) / 10.0` | `P()` |
| [5] | **Gear position** | `byte[5] & 0xFF` (0=N, 1–6) | `G()` |
| [6] | **Fuel level %** | `byte[6] & 0xFF` | `E()` |
| [7] | Torque / SpeedoSwVersion | `byte[7]` | `d0()` / `a0()` |
| [8] | Mileage (when speed > 0) | `byte[8] & 0xFF` | `O()` |
| [9] | (reserved) | — | — |
| [10] | **Throttle %** | `byte[10] & 0xFF` | `b0()` |
| [11] | Engine temp raw | `byte[11] - 40 = °C` | `y()` |
| [12] | Turn signal lamp | `byte[12]` | `k0()` |
| [13] | Engine temp (hex) | hex-converted | `y()` |
| [14] | Side stand status | `byte[14] & 0xFF` | `I()` |
| [15] | Rear tyre / misc | `byte[15] & 0xFF` | `H()` |
| [16][17] | **Engine RPM** | `BigInteger(b[16],b[17])` | `x()` |
| [17] | **Ride mode** | `byte[17]` | `V()` |
| [18] | Checksum | `255 - (sum % 256)` | — |
| [19] | Frame trailer | always `0xFF` | — |

## Notes

- Byte [5] is shared between **Odometer** (bytes 3–5) and **Gear** — gear is the last byte of the odometer triplet. This is correct per source.
- **Battery voltage** in the data model maps to `i()` which also returns `byte[6] & 0xFF` — same as fuel. The two are likely in different frame types distinguished by opcode.
- Engine temp: source uses hex string conversion via `iU = u(String.format("%02x", byte[13]))` — raw `byte[11] - 40` is the simpler fallback.
- RPM bytes [16][17] are also used for ride mode — mode is upper nibble or separate bit field in [17].

## Ride Modes

| Value | Mode |
|-------|------|
| 0 | Rain |
| 1 | Urban |
| 2 | Sport |
| 3 | Track |

---

# Phase 2 — TX (Phone → Cluster) Frame Map

**Source:** `com.tvsm.connect.bluetooth.sendreceive.U399DataSenderToCluster` +
`com.tvs.ble.feature.navigation.BleNavigationSendData` (decompiled)

All TX frames are **20 bytes**, XOR-encrypted with key `0xEA` before write.  
Frame header byte varies by type: `0x5A` (vehicle/settings) or `0x5B` (notifications/nav).

## TX Opcodes / Data IDs

| Byte[0] | Byte[1] (Data ID) | Purpose | Source method |
|---------|-------------------|---------|---------------|
| `0x5A` | `0xF1` (`-15`) | Vehicle/illumination data | `sendVehicleData()` / `onVehicleData()` |
| `0x5B` | `0x4A` (`74`) | Mobile status (battery, time, missed calls, SMS) | `sendMobileData()` |
| `0x5B` | `0x43` (`67`) | Incoming call — caller name string | `sendCallerName()` → `createCallerNameArray()` |
| `0x5B` | `0x53` (`83`) | SMS notification — sender number/name | `onSendSMS()` → `createSendSMSNameArray()` |
| `0x5B` | `0x52` (`82`) | Rider name | `sendInitialData()` → `createRiderNameArray()` |
| `0x5B` | `0x5C` (`92`) | Current location name string | `createCurrentLocationArray()` |
| `0x5B` | `0x4C` (`76`) | Custom voice assist message | `sendCustomMessage()` / `sendMobileData5()` |
| `0x5B` | `0x63` (`99`) | Custom voice assist message (line 2) | `sendMobileData6()` |
| `0x5B` | `DATA_ID_ACCESSORY_CONTROL` | Smart accessory state | `onAccessoryData()` |
| `0x5A` | `0x49` (`73`) | Navigation control packet (speed/distance/ETA/pictogram) | `byteArrayForNavigation()` |
| `0x5B` | `DATA_ID_NAVIGATIONDATA1` | Navigation text instruction (string) | `byteArrayForNavigation()` |
| `0x5A` | `0x49` (`73`) | Road name + destination arrived flag | `b()` in BleNavigationSendData |
| `0x5B` | `0x4A` (`74`) + `[1,0…10,17,18]` | Disconnect command to cluster | `sendDisconnectCommandToCluster()` |

## Navigation Packet Detail (0x5A 0x49)

```
Byte[0]  = 0x5A
Byte[1]  = 0x49  (DATA_ID_NAVIGATION_CONTROL)
Byte[2-3] = distanceToNextManeuver in metres (uint16 big-endian)
Byte[4-5] = timeToArrival in minutes (uint16)
Byte[6-8] = totalDistanceLeft in metres (3 bytes)
Byte[9]  = pictogramId (cluster icon code — see table below)
Byte[10] = noOfStrings (1 = instruction fits in 17 chars, 2 = split)
Byte[11] = navOnOff (1 if vehicle type is 29/37/38, else 0)
Byte[12-18] = padding zeros
Byte[19] = 0xFF
```

Navigation text instruction is sent as a **second frame** (0x5B DATA_ID_NAVIGATIONDATA1):
```
Byte[0]  = 0x5B
Byte[1]  = DATA_ID_NAVIGATIONDATA1
Byte[2..N] = instruction string (UTF-8, max 17 chars)
Byte[N+1..18] = zero padding
Byte[19] = 0xFF
```

## Pictogram ID Mapping (provider pictogram → cluster icon)

| Provider ID | Cluster Code | Meaning |
|-------------|--------------|---------|
| -1 | -1 | Stop navigation |
| 0 | 0 | Go straight / unknown |
| 1 | 1 | Slight right |
| 2, 15, 19 | 2 | Turn right |
| 3 | 10 | Sharp right |
| 4 | 11 | U-turn right |
| 5, 16, 20 | 12 | Turn left |
| 6 | 9 | Slight left |
| 7, 21, 50 | 57 | U-turn left |
| 8 | 22 | Roundabout |
| 23 | 62 | Exit roundabout right |
| 25 | 63 | Exit roundabout left |
| 35 | 14 | Keep right |
| 36 | 23 | Keep left |
| 41 | 19 | U-turn suggestion |
| 65 | 27 | |
| 66 | 28 | |
| 67 | 29 | |
| 68 | 48 | |
| 69 | 31 | |
| 70 | 32 | |
| 71 | 33 | |

## Mobile Status Frame Detail (0x5B 0x4A)

Sent on connect and periodically. Fields:
```
[0] = 0x5B
[1] = 0x4A
[2] = combined signal+battery byte (hex encoded: signalLevel + batteryLevel)
[3] = overSpeedLimit byte
[4] = 0x00
[5] = 0x02
[6] = hour (12h format)
[7] = minute
[8] = second
[9] = AM/PM (0=AM, 1=PM)
[10] = missed calls count (delta)
[11] = 0x00
[12-14] = date (day, month, year-2000)
[15] = missed SMS count (delta) / call state for U490
[16] = voice assist state
[17] = crash alert (0/1)
[18] = 0x00
[19] = 0xFF
```
