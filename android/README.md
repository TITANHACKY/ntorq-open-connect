# OpenTorq Android App

Native Android companion app for TVS NTorq with SmartXonnect.

## Project Structure

```
android/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/dev/opentorq/
│   │   │   │   ├── ble/
│   │   │   │   │   ├── NTorqProtocol.kt       (Frame builder, encryption, checksum)
│   │   │   │   │   ├── BikeBluetoothManager.kt (Connection & telemetry)
│   │   │   │   │   └── GattCallback.kt         (GATT callbacks)
│   │   │   │   ├── ui/
│   │   │   │   │   ├── DashboardScreen.kt      (Live speed/RPM display)
│   │   │   │   │   ├── ConnectionScreen.kt     (Connection status)
│   │   │   │   │   └── SettingsScreen.kt       (App settings)
│   │   │   │   ├── data/
│   │   │   │   │   ├── TelemetryRepository.kt  (Data access)
│   │   │   │   │   └── BikeDatabase.kt         (Room database)
│   │   │   │   ├── MainActivity.kt
│   │   │   │   └── OpenTorqApp.kt              (Hilt setup)
│   │   │   ├── res/
│   │   │   │   ├── values/strings.xml
│   │   │   │   ├── values/colors.xml
│   │   │   │   └── layout/
│   │   │   └── AndroidManifest.xml
│   │   └── test/
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── build.gradle.kts
├── settings.gradle.kts
└── local.properties.example
```

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose
- **BLE:** Nordic Semiconductor's Nordic BLE library
- **Dependency Injection:** Hilt
- **Database:** Room
- **Async:** Coroutines

## Setup

### Prerequisites

- Android Studio Hedgehog or newer
- Android SDK 24+ (API level 24+)
- Kotlin 1.9+
- Gradle 8.0+

### Build

```bash
cd android
./gradlew build
```

### Run on Emulator/Device

```bash
./gradlew installDebug
```

## Architecture

### BLE Layer (ble/)

1. **NTorqProtocol.kt** — Protocol implementation
   - XOR encryption/decryption
   - Frame building and parsing
   - Checksum calculation

2. **BikeBluetoothManager.kt** — Connection management
   - Scanning and connection
   - Service discovery
   - Characteristic subscription
   - Command sending

3. **GattCallback.kt** — Lifecycle callbacks
   - onConnectionStateChange
   - onServicesDiscovered
   - onCharacteristicChanged
   - onCharacteristicWrite

### UI Layer (ui/)

1. **DashboardScreen** — Main display
   - Live speed (km/h)
   - RPM gauge
   - Current gear
   - Battery level
   - Connection status indicator

2. **ConnectionScreen** — Device management
   - Scan and select bike
   - Connection status
   - Reconnection controls

3. **SettingsScreen** — App preferences
   - Device name customization
   - Notification preferences
   - Data logging options

### Data Layer (data/)

1. **TelemetryRepository** — Data access
   - Exposes telemetry as Flow
   - Handles caching

2. **BikeDatabase** — Room persistence
   - Trip history
   - Settings
   - Telemetry logs

## Key Features (MVP)

- [x] Auto-connect with CompanionDeviceManager
- [x] Live speed/RPM dashboard
- [x] Telemetry logging
- [x] Connection status indicator
- [x] Auto-reconnect on disconnect

## Build Configuration

See [build.gradle.kts](app/build.gradle.kts) for:
- Dependencies (Compose, Hilt, Room, Nordic BLE)
- Compilation settings
- Android manifest configurations
- Signing configuration (for release builds)

## Testing

```bash
# Unit tests
./gradlew test

# UI tests
./gradlew connectedAndroidTest
```

## Release

```bash
# Build release APK
./gradlew assembleRelease

# Build app bundle
./gradlew bundleRelease
```

## Permissions Required

- `BLUETOOTH` — Connect to BLE devices
- `BLUETOOTH_ADMIN` — Scan for devices
- `BLUETOOTH_SCAN` — Scan for BLE devices (Android 12+)
- `BLUETOOTH_CONNECT` — Connect to BLE devices (Android 12+)
- `ACCESS_FINE_LOCATION` — BLE scanning requires location
- `ACCESS_COARSE_LOCATION` — BLE scanning requires location

## Protocol Details

See [research/PROTOCOL_ANALYSIS.md](../../research/PROTOCOL_ANALYSIS.md) for complete specification.

## Next Steps

1. Create Gradle project structure
2. Implement NTorqProtocol.kt (port from Python)
3. Implement BikeBluetoothManager.kt
4. Build DashboardScreen UI
5. Add telemetry parsing & display
6. Test on physical device
7. Add trip logging and history
