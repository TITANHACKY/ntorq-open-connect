# NTorq XT Python PoC

Proof-of-concept BLE connector for NTorq XT SmartXonnect using Bleak.

## Setup

```bash
# Create virtual environment
python3 -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt
```

## Files

- **ntorq_protocol.py** — Frame builder, encryption, checksum validation
- **ntorq_connector.py** — BLE client with connection & telemetry handling
- **requirements.txt** — Python dependencies (Bleak, cryptography)

## Running

```bash
python ntorq_connector.py
```

Expected output:
```
Scanning for <YOUR_DEVICE_NAME>...
Found: <YOUR_DEVICE_NAME> [<YOUR_MAC_ADDRESS>]
Connecting to <YOUR_MAC_ADDRESS>...
Connected!
Subscribing to telemetry...
Subscribed!

Listening for telemetry (will receive ~2 frames per second)...
Press Ctrl+C to exit

[RX] Raw: 5a1064010186...
  ✓ Valid frame
    Opcode: 0xA5
    Payload (decrypted): 0064018...
```

## Protocol Details

See [research/PROTOCOL_ANALYSIS.md](../../research/PROTOCOL_ANALYSIS.md) for complete protocol specification.

**Key Points:**
- Service UUID: `5456534d-5647-5341-5342-454e544f5251`
- RX (telemetry): `00005354-0000-1000-8000-00805f9b34fb`
- TX (commands): `00005352-0000-1000-8000-00805f9b34fb`
- Encryption: XOR with 0xEA
- Checksum: 255 - (sum % 256)
- Frame: `[0x5A] [OPCODE] [ENCRYPTED_DATA] [CHECKSUM] [0xFF]`

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Device not found | Ensure bike is on and in pairing mode |
| Connection refused | Disconnect TVS Connect app first |
| No telemetry frames | Verify UUIDs match your device (check nRF Connect) |
| Checksum errors | Move closer to bike (BLE signal strength) |
| Decryption issues | Verify XOR key is 0xEA (check device variant) |

## Next Steps

1. Parse telemetry payload to extract speed/RPM/gear
2. Build telemetry logger to record bike data
3. Create Android app with same protocol logic
