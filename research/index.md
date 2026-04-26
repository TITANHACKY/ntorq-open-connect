=== NTorq XT Protocol Analysis & Research ===

## Documentation Files

- **PROTOCOL_ANALYSIS.md** — Comprehensive BLE protocol specification from decompiled code
- **NTORQ_XT_UUIDS_EXTRACTED.md** — Real hardware UUIDs extracted from physical device via nRF Connect
- **README_FINDINGS.md** — Executive summary of analysis phases and key discoveries
- **QUICK_REFERENCE.txt** — ASCII-formatted quick reference card for developers

## Key Findings

| Aspect | Value |
|--------|-------|
| Service UUID | 5456534d-5647-5341-5342-454e544f5251 |
| RX Characteristic | 00005354-0000-1000-8000-00805f9b34fb |
| TX Characteristic | 00005352-0000-1000-8000-00805f9b34fb |
| Encryption | XOR with 0xEA |
| Checksum | 255 - (sum % 256) |
| Device | <YOUR_DEVICE_NAME> (<YOUR_MAC_ADDRESS>) |
