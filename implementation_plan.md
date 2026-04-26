# NTorq OpenTorq Implementation Plan

## Current Status: ✅ RESEARCH & ANALYSIS COMPLETE

All groundwork is done:
- ✅ Protocol decoded and documented
- ✅ Real hardware UUIDs extracted
- ✅ Encryption/checksum verified
- ✅ Python PoC boilerplate created
- ✅ Android project structure defined

---

## Phase 1: Python PoC (Week 1-2)

**Goal:** Validate protocol by connecting to real bike and receiving telemetry

### Step 1.1: Setup & First Run (2-3 hours)

```bash
cd poc/python
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
python ntorq_connector.py
```

**Success Criteria:**
- ✅ Script prints "Found: <YOUR_DEVICE_NAME>"
- ✅ Script prints "Connected!"
- ✅ Script prints "Subscribed!"
- ✅ Receives telemetry frames at ~2 Hz
- ✅ Frames show valid checksums

### Step 1.2: Payload Parsing (3-5 hours)

Create `poc/python/telemetry_parser.py`:

```python
class TelemetryParser:
    @staticmethod
    def parse(payload: bytes) -> dict:
        """Extract speed, RPM, gear from decrypted payload"""
        if len(payload) < 20:
            return {'error': 'Payload too short'}
        
        # TODO: Identify which bytes = speed
        # TODO: Identify which bytes = RPM
        # TODO: Identify which bytes = gear
        # TODO: Identify which bytes = throttle
        # TODO: Identify which bytes = fuel
        
        return {
            'speed_kmh': None,      # PLACEHOLDER
            'rpm': None,            # PLACEHOLDER
            'gear': None,           # PLACEHOLDER
            'throttle': None,       # PLACEHOLDER
            'fuel_percent': None    # PLACEHOLDER
        }
```

**Method:**
1. Run bike at idle (0 km/h)
2. Note all payload bytes
3. Accelerate to 20 km/h
4. Find which bytes changed
5. Cross-reference with cluster display
6. Repeat for RPM, gear, throttle, fuel

### Step 1.3: Data Logger (2-3 hours)

Create `poc/python/logger.py` to save telemetry to CSV:

```python
import csv
from datetime import datetime

class TelemetryLogger:
    def __init__(self, filename=None):
        if not filename:
            filename = f"telemetry_{datetime.now().isoformat()}.csv"
        self.file = open(filename, 'w')
        self.writer = csv.DictWriter(self.file, fieldnames=[
            'timestamp', 'raw_hex', 'speed', 'rpm', 'gear', 'throttle', 'fuel'
        ])
        self.writer.writeheader()
    
    def log(self, parsed):
        self.writer.writerow({
            'timestamp': datetime.now().isoformat(),
            'raw_hex': parsed.get('raw', '').hex(),
            'speed': parsed.get('speed_kmh'),
            'rpm': parsed.get('rpm'),
            'gear': parsed.get('gear'),
            'throttle': parsed.get('throttle'),
            'fuel': parsed.get('fuel_percent')
        })
        self.file.flush()
```

**Success Criteria:**
- ✅ Creates CSV file with telemetry
- ✅ Logs at ~2 Hz
- ✅ Can analyze data offline

### Step 1.4: Protocol Edge Cases (2-3 hours)

Test and document:
- [ ] Connection drop & auto-reconnect
- [ ] Frame errors & recovery
- [ ] Checksum failures
- [ ] Opcode variations
- [ ] Payload size variations
- [ ] Heartbeat frequency requirements

---

## Phase 2: Android App - Foundation (Week 3)

**Goal:** Create working Android app that displays live telemetry

### Step 2.1: Gradle Project Setup (1-2 hours)

```bash
# In Android Studio: File > New > New Project
# Choose: Phone and Tablet > Empty Activity
# Name: OpenTorq
# Package: dev.opentorq
# Language: Kotlin
# Minimum SDK: API 24
```

Create `android/build.gradle.kts`:

```kotlin
plugins {
    id("com.android.application") version "8.0.0"
    id("kotlin-android")
    id("com.google.dagger.hilt.android") version "2.45"
    id("com.google.devtools.ksp") version "1.9.0-1.0.12"
}

android {
    compileSdk = 34
    
    defaultConfig {
        applicationId = "dev.opentorq"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }
}

dependencies {
    // Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    
    // Jetpack Compose
    implementation("androidx.compose.ui:ui:1.5.0")
    implementation("androidx.compose.material3:material3:1.1.0")
    implementation("androidx.compose.foundation:foundation:1.5.0")
    implementation("androidx.activity:activity-compose:1.7.2")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")
    
    // Hilt (Dependency Injection)
    implementation("com.google.dagger:hilt-android:2.45")
    ksp("com.google.dagger:hilt-compiler:2.45")
    
    // Room (Database)
    implementation("androidx.room:room-runtime:2.5.2")
    ksp("androidx.room:room-compiler:2.5.2")
    
    // Nordic Semiconductor BLE
    implementation("no.nordicsemi.android.ble:ble:2.8.0")
    implementation("no.nordicsemi.android.ble:ble-ktx:2.8.0")
}
```

### Step 2.2: Port NTorqProtocol to Kotlin (2-3 hours)

Create `android/app/src/main/java/dev/opentorq/ble/NTorqProtocol.kt`:

```kotlin
object NTorqProtocol {
    private const val XOR_KEY: Byte = 0xEA.toByte()
    const val FRAME_START: Byte = 0x5A.toByte()
    const val FRAME_END: Byte = 0xFF.toByte()
    
    fun xorEncrypt(data: ByteArray): ByteArray {
        return data.map { (it.toInt() xor XOR_KEY.toInt()).toByte() }.toByteArray()
    }
    
    fun xorDecrypt(data: ByteArray): ByteArray = xorEncrypt(data)
    
    fun calculateChecksum(frameBytes: ByteArray): Byte {
        val total = frameBytes.map { it.toInt() and 0xFF }.sum()
        return (255 - (total % 256)).toByte()
    }
    
    fun makeFrame(opcode: Byte, payload: ByteArray = byteArrayOf()): ByteArray {
        val header = byteArrayOf(FRAME_START, opcode)
        val encrypted = xorEncrypt(payload)
        val frameForChecksum = header + encrypted
        val checksum = calculateChecksum(frameForChecksum)
        
        return frameForChecksum + byteArrayOf(checksum, FRAME_END)
    }
    
    fun parseFrame(data: ByteArray): Map<String, Any?> {
        if (data.size < 4) {
            return mapOf("valid" to false, "error" to "Frame too short")
        }
        
        if (data[0] != FRAME_START || data[data.size - 1] != FRAME_END) {
            return mapOf("valid" to false, "error" to "Invalid frame markers")
        }
        
        val opcode = data[1]
        val encryptedPayload = data.sliceArray(2 until data.size - 2)
        val receivedChecksum = data[data.size - 2]
        
        val frameForChecksum = data.sliceArray(0..1) + encryptedPayload
        val calculatedChecksum = calculateChecksum(frameForChecksum)
        
        if (receivedChecksum != calculatedChecksum) {
            return mapOf(
                "valid" to false,
                "error" to "Checksum mismatch: got $receivedChecksum, expected $calculatedChecksum"
            )
        }
        
        val decrypted = xorDecrypt(encryptedPayload)
        
        return mapOf(
            "valid" to true,
            "opcode" to opcode.toInt() and 0xFF,
            "payload" to decrypted,
            "checksum" to receivedChecksum.toInt() and 0xFF
        )
    }
}
```

### Step 2.3: BLE Manager (3-4 hours)

Create `android/app/src/main/java/dev/opentorq/ble/BikeBluetoothManager.kt`:

```kotlin
@HiltViewModel
class BikeBluetoothManager @Inject constructor() : ViewModel() {
    companion object {
        val SERVICE_UUID = UUID.fromString("5456534d-5647-5341-5342-454e544f5251")
        val RX_CHAR_UUID = UUID.fromString("00005354-0000-1000-8000-00805f9b34fb")
        val TX_CHAR_UUID = UUID.fromString("00005352-0000-1000-8000-00805f9b34fb")
    }
    
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState
    
    private val _telemetry = MutableStateFlow<Telemetry?>(null)
    val telemetry: StateFlow<Telemetry?> = _telemetry
    
    // TODO: Implement scan, connect, subscribe, send methods
}

enum class ConnectionState {
    Disconnected, Scanning, Connecting, Connected, Error
}

data class Telemetry(
    val speedKmh: Int,
    val rpm: Int,
    val gear: Int,
    val throttle: Int,
    val fuelPercent: Int,
    val timestamp: Long = System.currentTimeMillis()
)
```

### Step 2.4: Dashboard UI (3-4 hours)

Create `android/app/src/main/java/dev/opentorq/ui/DashboardScreen.kt`:

```kotlin
@Composable
fun DashboardScreen(
    telemetry: Telemetry?,
    connectionState: ConnectionState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Connection Status
        ConnectionStatusCard(
            state = connectionState,
            onConnect = onConnect,
            onDisconnect = onDisconnect
        )
        
        if (telemetry != null) {
            // Speed Display
            SpeedGauge(speedKmh = telemetry.speedKmh)
            
            // RPM Gauge
            RpmGauge(rpm = telemetry.rpm)
            
            // Gear & Fuel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GearDisplay(gear = telemetry.gear, modifier = Modifier.weight(1f))
                FuelDisplay(fuel = telemetry.fuelPercent, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun SpeedGauge(speedKmh: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Speed", style = MaterialTheme.typography.headlineSmall)
        Text(
            "$speedKmh km/h",
            style = MaterialTheme.typography.displayLarge,
            color = Color(0xFF4CAF50)
        )
    }
}

// TODO: Implement RpmGauge, GearDisplay, FuelDisplay
```

**Success Criteria:**
- ✅ App compiles without errors
- ✅ Dashboard screen renders
- ✅ Connection status button works
- ✅ (Speed/RPM display placeholders)

### Step 2.5: Connect & Receive Telemetry (4-5 hours)

Implement in BikeBluetoothManager:
- [ ] BLE device scan
- [ ] Service discovery
- [ ] Characteristic subscription
- [ ] Telemetry parsing & UI update
- [ ] Error handling & reconnect

**Success Criteria:**
- ✅ App discovers NTorq XT device
- ✅ Shows device name in UI
- ✅ Connects on tap
- ✅ Receives telemetry frames
- ✅ Displays live speed/RPM
- ✅ Auto-reconnects on disconnect

---

## Phase 3: MVP Polish (Week 4)

### Features to Add

- [ ] Trip logging (start/stop, duration, distance)
- [ ] Telemetry data export (CSV/JSON)
- [ ] Settings screen (device selection, units, notifications)
- [ ] Crash detection & SOS alert
- [ ] Battery level display
- [ ] Dark mode support
- [ ] Offline mode (cached data)

### Testing

- [ ] Unit tests for NTorqProtocol
- [ ] Integration tests with real bike
- [ ] UI tests with Compose
- [ ] Performance profiling
- [ ] BLE stability (30+ minute ride test)

### Release Preparation

- [ ] App signing setup
- [ ] Privacy policy & terms
- [ ] GitHub repository creation
- [ ] Release notes & changelog
- [ ] Announcement on r/NTorq, Facebook groups

---

## Decision: Python First or Android First?

### Recommendation: **Python First** (2-3 weeks total)

**Why:**
1. **Faster iteration** — No Android build times
2. **Easier debugging** — Print statements vs. logcat
3. **Flexible testing** — Run on any OS, any Python environment
4. **Payload discovery** — Much easier to reverse-engineer telemetry format
5. **Low risk** — Validate protocol before big Android commit

**Timeline:**
- Week 1: Python PoC working (2-3 days)
- Week 1-2: Payload mapping (3-5 days)
- Week 2: Data logging & edge cases (2-3 days)
- Week 3-4: Android app
- Week 5: Finish & release

**Action Steps (Today):**
1. `cd poc/python && pip install -r requirements.txt`
2. `python ntorq_connector.py` (with <YOUR_DEVICE_NAME> on)
3. Verify connection & telemetry receipt
4. Start payload mapping

---

## File Locations

```
ntorq-open-connect/
├── research/                       ← Completed analysis
│   ├── PROTOCOL_ANALYSIS.md        ✅ Complete protocol spec
│   ├── NTORQ_XT_UUIDS_EXTRACTED.md ✅ Real hardware UUIDs
│   ├── README_FINDINGS.md          ✅ Summary of findings
│   ├── QUICK_REFERENCE.txt         ✅ Quick reference card
│   └── INDEX.md                    ✅ Research index
│
├── poc/python/                     ← Python PoC (NEXT)
│   ├── ntorq_protocol.py           ✅ Protocol implementation
│   ├── ntorq_connector.py          ✅ BLE client boilerplate
│   ├── requirements.txt            ✅ Dependencies
│   ├── README.md                   ✅ Setup instructions
│   ├── telemetry_parser.py         ⬜ TODO: Payload parsing
│   ├── logger.py                   ⬜ TODO: Data logging
│   └── venv/                       ⬜ TODO: Virtual environment
│
├── android/                        ← Android app (AFTER Python)
│   ├── README.md                   ✅ Architecture overview
│   └── app/src/main/java/...       ⬜ TODO: Gradle project
│
├── docs/
│   ├── RE_GUIDE.md                 ✅ Reverse engineering steps
│   └── LEGAL.md                    ✅ Legal analysis
│
├── IMPLEMENTATION_PLAN.md          ✅ This file
├── README.md                       ✅ Project overview
├── CONTRIBUTING.md                 ⬜ TODO: Update
└── LICENSE                         ✅ MIT

✅ = Complete
⬜ = Next action
```

---

## Checkpoints & Gating

| Checkpoint | Gate | Blocker |
|-----------|------|---------|
| Python connects to bike | PoC passes telemetry test | No connection = protocol issue |
| Parse telemetry correctly | Payload bytes match cluster | Wrong mapping = re-analyze frames |
| Android builds | Gradle sync succeeds | Missing SDK/dependencies = setup issue |
| Android connects | BLE scan finds <YOUR_DEVICE_NAME> | Permission issue = fix manifest |
| Live telemetry in UI | Speed/RPM displayed | Parsing error = debug payload |

---

## Success Metrics

- **Python PoC:** 2 hours to first connection, 8 hours to full telemetry parsing
- **Android App:** Compiles day 1, connects day 2, full dashboard day 3-4
- **Overall:** MVP ready in 4 weeks, releasable in 6 weeks

**Next Action:** Start Python PoC today 🚀
