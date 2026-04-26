"""
ntorq_connect.py — Python proof-of-concept BLE connector for TVS NTorq SmartXonnect

Usage:
    pip install bleak
    python ntorq_connect.py

What this does:
    1. Scans for your NTorq by name
    2. Connects via BLE
    3. Subscribes to telemetry notifications
    4. Decodes and prints live bike data

Status: TEMPLATE — fill in UUIDs from your nRF Connect enumeration
"""

import asyncio
import struct
from bleak import BleakScanner, BleakClient
from bleak.backends.device import BLEDevice
from datetime import datetime


# ─── Fill these in after nRF Connect enumeration ──────────────────────────────
# Replace with actual UUIDs discovered from your NTorq cluster

VENDOR_SERVICE_UUID     = "????????-????-????-????-????????????"  # Primary vendor service
TELEMETRY_CHAR_UUID     = "????????-????-????-????-????????????"  # NOTIFY — bike → phone
COMMAND_CHAR_UUID       = "????????-????-????-????-????????????"  # WRITE  — phone → bike

# NTorq advertised name prefix (check nRF Connect scanner for exact format)
NTORQ_NAME_PREFIX = "NTORQ"

# ─────────────────────────────────────────────────────────────────────────────


class NtorqData:
    """Holds decoded telemetry from the NTorq cluster."""
    speed_kmh: int = 0
    rpm: int = 0
    gear: int = 0          # 0 = Neutral
    fuel_pct: int = 0
    range_km: int = 0
    odo_km: float = 0.0
    ride_mode: str = "STREET"

    def __str__(self):
        gear_str = "N" if self.gear == 0 else str(self.gear)
        return (f"Speed: {self.speed_kmh:3d} km/h | "
                f"RPM: {self.rpm:5d} | "
                f"Gear: {gear_str} | "
                f"Fuel: {self.fuel_pct:3d}% | "
                f"Range: {self.range_km} km")


def decode_telemetry(data: bytes) -> NtorqData:
    """
    Decode a telemetry notification from the NTorq cluster.

    ⚠️  This is a TEMPLATE — byte positions are guesses based on similar protocols.
        Update after analyzing your actual Wireshark captures.
    """
    bike = NtorqData()

    print(f"  [RAW] {data.hex(' ').upper()}")

    if len(data) < 8:
        return bike

    # TODO: Identify actual byte positions from Wireshark analysis
    # Example layout (pure guess — update from captures):
    # Byte 0:   Opcode / packet type
    # Byte 1:   Speed (uint8, km/h)
    # Byte 2-3: RPM (uint16 LE)
    # Byte 4:   Gear (0=N)
    # Byte 5:   Fuel %
    # Byte 6-7: Range km (uint16 LE)
    # Byte -1:  Checksum

    try:
        # opcode     = data[0]
        bike.speed_kmh = data[1]
        bike.rpm       = struct.unpack_from('<H', data, 2)[0]
        bike.gear      = data[4]
        bike.fuel_pct  = data[5]
        bike.range_km  = struct.unpack_from('<H', data, 6)[0]
    except Exception as e:
        print(f"  [DECODE ERROR] {e}")

    return bike


def notification_handler(sender, data: bytes):
    """Called by Bleak on every BLE notification from the bike."""
    ts = datetime.now().strftime("%H:%M:%S.%f")[:-3]
    print(f"\n[{ts}] Notification from {sender}:")

    bike = decode_telemetry(data)
    print(f"  {bike}")


async def find_ntorq(timeout: float = 10.0) -> BLEDevice | None:
    """Scan for the NTorq cluster by name."""
    print(f"Scanning for NTorq (timeout={timeout}s)...")
    print("Make sure ignition is ON and Bluetooth is enabled on cluster.\n")

    device = await BleakScanner.find_device_by_filter(
        lambda d, _: d.name and NTORQ_NAME_PREFIX in d.name.upper(),
        timeout=timeout
    )
    return device


async def send_handshake(client: BleakClient):
    """
    Send initial handshake to the cluster (if required).

    ⚠️  Payload is unknown — update after Frida/Wireshark analysis.
        Start with a no-op; if the cluster doesn't respond, a handshake may be needed.
    """
    # TODO: Replace with actual handshake bytes from captures
    # handshake = bytes([0x5A, 0xA5, 0x01, 0x00, 0x01])
    # await client.write_gatt_char(COMMAND_CHAR_UUID, handshake, response=True)
    pass


async def main():
    device = await find_ntorq()

    if not device:
        print("❌ NTorq not found. Check:")
        print("   • Ignition is ON")
        print("   • Cluster Bluetooth is enabled (not already paired to another phone)")
        print("   • You're within 5m of the bike")
        return

    print(f"✅ Found: {device.name}  [{device.address}]")
    print(f"   RSSI: {device.rssi} dBm")
    print("\nConnecting...")

    async with BleakClient(device, timeout=15.0) as client:
        print(f"✅ Connected!\n")

        # Print all services (useful for filling in UUIDs above)
        print("=== GATT Services ===")
        for service in client.services:
            print(f"  Service: {service.uuid}  ({service.description})")
            for char in service.characteristics:
                props = ",".join(char.properties)
                print(f"    Char: {char.uuid}  [{props}]  ({char.description})")
        print("=====================\n")

        # Send handshake if needed
        await send_handshake(client)

        # Subscribe to telemetry notifications
        if TELEMETRY_CHAR_UUID and "????????" not in TELEMETRY_CHAR_UUID:
            await client.start_notify(TELEMETRY_CHAR_UUID, notification_handler)
            print("📡 Subscribed to telemetry. Live data:\n")

            try:
                while True:
                    await asyncio.sleep(0.1)
            except KeyboardInterrupt:
                print("\n\nStopped by user.")
                await client.stop_notify(TELEMETRY_CHAR_UUID)
        else:
            print("⚠️  TELEMETRY_CHAR_UUID not set yet.")
            print("   Use the GATT service listing above to find the right UUID.")
            print("   Then update the UUID constants at the top of this file.")
            await asyncio.sleep(3)


if __name__ == "__main__":
    asyncio.run(main())
