"""
NTorq XT BLE Connection Handler using Bleak
"""

import asyncio
import os
from dotenv import load_dotenv
from bleak import BleakClient, BleakScanner
from ntorq_protocol import NTorqProtocol

load_dotenv()

# Real UUIDs from NTorq XT (extracted via nRF Connect)
SERVICE_UUID = "5456534d-5647-5341-5342-454e544f5251"
RX_CHARACTERISTIC_UUID = "00005354-0000-1000-8000-00805f9b34fb"  # NOTIFY (FROM bike)
TX_CHARACTERISTIC_UUID = "00005352-0000-1000-8000-00805f9b34fb"  # WRITE (TO bike)


class NTorqConnector:
    def __init__(self, device_name: str = None):
        self.device_name = device_name or os.getenv("NTORQ_DEVICE_NAME") or "TVST0XXXXXX"
        self.device = None
        self.client = None
        self.telemetry_callback = None

    async def scan_for_device(self):
        """Scan and find NTorq device by name, or fall back to MAC address"""
        device_mac = os.getenv("NTORQ_DEVICE_MAC", "")
        print(f"Scanning for {self.device_name}...")

        # Try by name first (30s timeout)
        self.device = await BleakScanner.find_device_by_filter(
            lambda d, _: d.name and self.device_name in d.name,
            timeout=30,
        )

        # Fall back to MAC address if name scan failed
        if not self.device and device_mac:
            print(f"Name not found, trying MAC address {device_mac}...")
            self.device = await BleakScanner.find_device_by_address(device_mac, timeout=30)

        if not self.device:
            raise Exception(f"Device {self.device_name} not found! Run scan_debug.py to see all nearby devices.")

        print(f"Found: {self.device.name} [{self.device.address}]")
        return self.device

    async def connect(self):
        """Connect to bike"""
        if not self.device:
            await self.scan_for_device()

        print(f"Connecting to {self.device.address}...")
        self.client = BleakClient(self.device)
        await self.client.connect()
        print("Connected!")

    def telemetry_handler(self, sender, data):
        """Handle incoming telemetry frame"""
        print(f"[RX] Raw: {data.hex()}")

        parsed = NTorqProtocol.parse_frame(data)

        if parsed['valid']:
            print(f"  ✓ Valid frame")
            print(f"    Opcode: 0x{parsed['opcode']:02X}")
            print(f"    Payload (decrypted): {parsed['payload'].hex()}")

            if self.telemetry_callback:
                self.telemetry_callback(parsed)
        else:
            print(f"  ✗ Invalid: {parsed.get('error', 'Unknown error')}")

    async def subscribe_to_telemetry(self):
        """Subscribe to telemetry notifications"""
        print("Subscribing to telemetry...")
        await self.client.start_notify(RX_CHARACTERISTIC_UUID, self.telemetry_handler)
        print("Subscribed!")

    async def send_heartbeat(self):
        """Send heartbeat command to keep connection alive"""
        print("Sending heartbeat...")
        heartbeat = NTorqProtocol.make_frame(opcode=0xA5, payload=b'')
        print(f"  Sending: {heartbeat.hex()}")
        await self.client.write_gatt_char(TX_CHARACTERISTIC_UUID, heartbeat, response=False)
        print("  Sent!")

    async def disconnect(self):
        """Disconnect from bike"""
        if self.client:
            await self.client.disconnect()
            print("Disconnected")


async def main():
    connector = NTorqConnector()

    try:
        # Connect to bike
        await connector.scan_for_device()
        await connector.connect()

        # Subscribe to telemetry
        await connector.subscribe_to_telemetry()

        # Send heartbeat every 5 seconds
        print("\nListening for telemetry (will receive ~2 frames per second)...")
        print("Press Ctrl+C to exit\n")

        for i in range(20):
            await asyncio.sleep(1)
            if i % 5 == 0:
                await connector.send_heartbeat()

    except KeyboardInterrupt:
        print("\n\nStopping...")
    except Exception as e:
        print(f"Error: {e}")
    finally:
        await connector.disconnect()


if __name__ == "__main__":
    asyncio.run(main())
