# 🏍️ ntorq-open-connect

> An open-source reverse engineering project and alternative companion app for TVS NTorq with SmartXonnect — built because the official app deserved better.

[![Status](https://img.shields.io/badge/status-research%20phase-yellow)](https://github.com/yourusername/ntorq-open-connect)
[![License](https://img.shields.io/badge/license-MIT-blue)](LICENSE)
[![Platform](https://img.shields.io/badge/platform-Android-green)](https://android.com)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen)](CONTRIBUTING.md)

---

## What is this?

The TVS Connect app for NTorq SmartXonnect is frustratingly unreliable — auto-connect doesn't work when you turn on your bike, navigation drops randomly, and the UI feels dated. This project aims to:

1. **Reverse engineer** the BLE (Bluetooth Low Energy) protocol between the NTorq cluster and phone
2. **Document** the protocol openly so anyone can build on it
3. **Build OpenTorq** — a clean, reliable, open-source Android companion app

> ⚠️ This is a personal, non-commercial project. TVS, SmartXonnect, NTorq, and related names are trademarks of TVS Motor Company. This project is not affiliated with or endorsed by TVS Motor Company.

---

## Supported Vehicles

| Vehicle | Status | Notes |
|---------|--------|-------|
| TVS NTorq 125 XT | 🔬 Researching | Primary target |
| TVS NTorq 125 Race XP | 🔬 Researching | Should be same protocol |
| TVS NTorq 150 TFT | ❓ Unknown | Different cluster, TBD |
| TVS Jupiter (SmartXonnect) | ❓ Unknown | Possibly same protocol |

---

## Project Structure

```
ntorq-open-connect/
├── research/
│   ├── captures/          # Wireshark / HCI snoop captures (sanitized)
│   ├── jadx-findings/     # Decompilation findings from official APK
│   ├── frida-scripts/     # Frida hooks for live protocol capture
│   └── PROTOCOL.md        # Decoded protocol documentation (WIP)
├── poc/
│   └── python/            # Python Bleak proof-of-concept connector
├── android/               # OpenTorq Android app (Kotlin + Compose)
├── docs/
│   ├── RE_GUIDE.md        # Step-by-step reverse engineering guide
│   └── LEGAL.md           # Legal analysis (India Copyright Act §52)
└── CONTRIBUTING.md
```

---

## Current Status

- [x] Project setup and documentation
- [ ] APK decompilation (jadx) — UUID extraction
- [ ] Live GATT enumeration (nRF Connect)
- [ ] HCI Snoop Log captures
- [ ] Frame format decoded
- [ ] Checksum algorithm identified
- [ ] Python PoC connector working
- [ ] Android app scaffold
- [ ] Auto-connect working
- [ ] Navigation relay working
- [ ] OpenTorq v1 release

---

## Planned Features in OpenTorq

- ✅ **Reliable auto-connect** — connects the moment ignition is on, using Android CompanionDeviceManager
- ✅ **Live dashboard** — speed, RPM, gear, fuel range in a clean UI
- ✅ **Turn-by-turn navigation** relayed to cluster — better routing via OSM/Mappls
- ✅ **Trip history** with GPX export
- ✅ **Call & notification relay**
- ✅ **Fuel & maintenance log**
- ✅ **Crash detection with SOS**
- ✅ **No forced account login** — works offline

---

## How to Contribute

See [CONTRIBUTING.md](CONTRIBUTING.md). All skill levels welcome — especially:
- NTorq / TVS bike owners who can share captures
- Android / Kotlin developers
- BLE / RE experience a plus but not required

---

## Legal

Reverse engineering for interoperability is permitted under **Indian Copyright Act §52(1)(ab)**. This is a non-commercial personal project. See [docs/LEGAL.md](docs/LEGAL.md) for the full analysis.

---

## Community

- Discussions: [GitHub Discussions](https://github.com/yourusername/ntorq-open-connect/discussions)
- TechEnclave thread: [Making alternative to TVS connect app](https://techenclave.com/t/making-alternative-to-tvs-connect-app-need-some-help/423446)

---

<p align="center">Made with ❤️ by NTorq riders, for NTorq riders</p>
