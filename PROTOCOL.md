# NTorq SmartXonnect BLE Protocol — Research Notes

> **Status: Work in Progress** — This document is updated as findings come in.  
> Last updated: 2026-04

---

## 1. Overview

The TVS NTorq XT SmartXonnect feature uses **Bluetooth Low Energy (BLE)** to communicate between the instrument cluster and the companion phone app (`com.tvsm.connect`).

- **Phone role:** Central (GATT Client)
- **Cluster role:** Peripheral (GATT Server)
- **Pairing method:** (TBD — likely Just Works or Numeric Comparison)
- **BLE SoC in cluster:** (TBD — suspected Nordic nRF52 family)
- **Link-layer encryption:** AES-CCM via standard BLE pairing (keys visible in HCI snoop)

---

## 2. GATT Service Map

> ⚠️ UUIDs below are placeholders. Replace with actual values from nRF Connect enumeration.

| Service | UUID | Description |
|---------|------|-------------|
| Generic Access | `0x1800` | Standard |
| Generic Attribute | `0x1801` | Standard |
| Device Information | `0x180A` | Firmware version, model |
| **TVS Vendor Service** | `????????-????-????-????-????????????` | **Primary — TBD** |

### Characteristics (Vendor Service)

| Characteristic | UUID | Properties | Description |
|----------------|------|------------|-------------|
| Telemetry | `????????` | NOTIFY | Bike pushes speed, RPM, gear, fuel |
| Command | `????????` | WRITE | Phone sends nav, notifications |
| Status | `????????` | READ/NOTIFY | Connection state, errors |

---

## 3. Frame Format (Hypothesis)

Based on patterns seen in similar vehicle BLE protocols (Yamaha Y-Connect, NIU, Xiaomi). Actual format TBD from captures.

```
[ PREAMBLE ] [ LENGTH ] [ OPCODE ] [ PAYLOAD ... ] [ CHECKSUM ]
   1-2 bytes    1 byte    1 byte     N bytes          1-2 bytes
```

**Suspected preamble candidates:** `0x5AA5`, `0xAA55`, `0xEFEF`

---

## 4. Known Opcodes

> All TBD — to be filled from Wireshark analysis

| Opcode | Direction | Description |
|--------|-----------|-------------|
| `0x??` | Bike → Phone | Telemetry packet (speed, RPM, gear, fuel) |
| `0x??` | Phone → Bike | Navigation directive (arrow, distance) |
| `0x??` | Phone → Bike | Notification text (caller ID, SMS) |
| `0x??` | Phone → Bike | Handshake / session init |
| `0x??` | Bike → Phone | ACK |

---

## 5. Telemetry Packet — Field Map

> TBD — to be decoded from NOTIFY characteristic captures

```
Byte 0:    Opcode
Byte 1:    Speed (km/h) — likely uint8 or uint16
Byte 2-3:  RPM — likely uint16 LE
Byte 4:    Gear (0=N, 1-6)
Byte 5:    Fuel level (%)
Byte 6-7:  Range (km) — uint16 LE
Byte 8:    Ride mode flags
...
Byte N:    Checksum
```

---

## 6. Checksum Algorithm

> TBD — to be cracked with CRC RevEng

Candidates in order of likelihood:
1. XOR of all payload bytes
2. Modular sum (mod 256)
3. CRC-8
4. CRC-16/CCITT (`poly=0x1021`)
5. CRC-16/MODBUS

---

## 7. Handshake / Authentication

> TBD — to be captured and analyzed

Questions to answer:
- Is there a session handshake before telemetry starts?
- Is there application-layer encryption on top of BLE link layer?
- Is there a challenge-response auth tied to chassis number?

---

## 8. Navigation Protocol

> TBD — to be captured while running TVS Connect navigation

The cluster renders one of a fixed set of arrow icons + a distance value. The phone sends a directive code per turn instruction. Need to map all icon codes.

---

## 9. Capture Log

| Date | Firmware | Feature Captured | File | Notes |
|------|----------|-----------------|------|-------|
| — | — | — | — | First capture pending |

---

## 10. References

- [nRF Connect for Mobile](https://www.nordicsemi.com/Products/Development-tools/nrf-connect-for-mobile)
- [CRC RevEng](https://reveng.sourceforge.io/)
- [Reverse Engineering BLE Devices (readthedocs)](https://reverse-engineering-ble-devices.readthedocs.io/)
- [Bleak Python BLE library](https://github.com/hbldh/bleak)
- [jadx decompiler](https://github.com/skylot/jadx)
