# Reverse Engineering Guide — NTorq SmartXonnect

A step-by-step guide to reproducing the protocol research on your own NTorq.

---

## Prerequisites

- TVS NTorq with SmartXonnect (XT / Race XP / Race)
- Android phone with Developer Options enabled
- Computer with ADB, Python 3.10+, Java 11+

---

## Step 1 — Get the APK (without logging in)

```bash
# Option A: Download from APKMirror.com (search "TVS Connect")

# Option B: Pull from a device where you're NOT logged into TVS Connect
adb shell pm path com.tvsm.connect
adb pull <path_from_above> tvs_connect.apk
```

---

## Step 2 — Static Decompilation (jadx)

Download jadx-gui from: https://github.com/skylot/jadx/releases

```bash
jadx -d tvs_connect_decompiled/ tvs_connect.apk
```

**Search in jadx-gui (Ctrl+F) for:**
```
UUID.fromString
writeCharacteristic
setCharacteristicNotification
BluetoothLeScanner
5AA5
AA55
SmartXonnect
```

Document every UUID string you find in `research/jadx-findings/uuids.md`.

---

## Step 3 — MobSF Automated Scan

```bash
docker pull opensecurity/mobile-security-framework-mobsf:latest
docker run -it --rm -p 8000:8000 opensecurity/mobile-security-framework-mobsf:latest
# Visit http://localhost:8000, upload APK
```

Save the full HTML report to `research/jadx-findings/mobsf-report.html`.

---

## Step 4 — Live GATT Enumeration

1. Install **nRF Connect for Mobile** from Play Store (Nordic Semiconductor)
2. Turn on NTorq ignition
3. Enter Bluetooth pairing mode on cluster (hold Mode ~5s)
4. In nRF Connect → Scanner → connect to `NTORQ_XXXX`
5. Screenshot every service and characteristic
6. Save screenshots to `research/captures/gatt-map/`
7. Fill in the UUIDs in `research/PROTOCOL.md`

---

## Step 5 — HCI Snoop Log Capture

**Enable on phone:**
```
Settings → About Phone → tap Build Number 7x
Settings → Developer Options → Enable Bluetooth HCI snoop log → ON
Turn Bluetooth OFF then ON
```

**Capture one feature at a time:**
```bash
# After each session:
adb bugreport bugreport.zip
# Extract: FS/data/misc/bluetooth/logs/btsnoop_hci.log
```

Save captures as:
- `research/captures/01_connect_only.log`
- `research/captures/02_navigation.log`
- `research/captures/03_call_alert.log`
- `research/captures/04_telemetry.log`

**Analyze in Wireshark:**
```
Filter: btatt && bluetooth.dst == <your_bike_mac>
Columns to add: btatt.handle, btatt.opcode, btatt.value
```

---

## Step 6 — Crack the Checksum

Download CRC RevEng: https://reveng.sourceforge.io/

```bash
# Try 8-bit first
./reveng -w 8 -s <frame1_hex> <frame2_hex> <frame3_hex>

# Then 16-bit
./reveng -w 16 -s <frame1_hex> <frame2_hex> <frame3_hex>
```

---

## Step 7 — Frida (if encrypted)

```bash
pip install frida-tools objection

# Patch APK if non-rooted
objection patchapk -s tvs_connect.apk
adb install tvs_connect.objection.apk

# Hook
frida -U -f com.tvsm.connect -l research/frida-scripts/ble_hook.js --no-pause
```

See `research/frida-scripts/ble_hook.js` for the hook script.

---

## Contributing Captures

If you own a NTorq and can capture HCI snoop logs, please:
1. Follow Steps 4–5 above
2. **Sanitize your capture** — replace your bike's MAC with `AA:BB:CC:DD:EE:FF`
3. Open a PR adding your log to `research/captures/`
4. Include your bike variant and firmware version in the PR description
