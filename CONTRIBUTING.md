# Contributing to ntorq-open-connect

Thank you for wanting to help! This project thrives on contributions from NTorq riders and developers.

---

## Ways to Contribute

### 🔬 You own a NTorq — share a capture

This is the most valuable contribution right now.

1. Follow the guide in [docs/RE_GUIDE.md](docs/RE_GUIDE.md) Steps 4–5
2. **Sanitize your capture** before sharing:
   - Replace your bike's Bluetooth MAC address with `AA:BB:CC:DD:EE:FF`
   - Remove any personal notification content from captures
3. Open a PR with:
   - Your capture file in `research/captures/`
   - Your bike variant (NTorq 125 XT / Race XP / Race / 150 TFT)
   - Firmware version (visible in nRF Connect → Device Information → Firmware Revision)
   - Which feature you captured (connect-only / nav / call / stats)

### 🔍 RE / reverse engineering

- Decompile the APK and report UUID findings
- Run the Frida hook and share output (sanitized)
- Help decode packet structures in `research/PROTOCOL.md`

### 📱 Android / Kotlin development

- Help build the OpenTorq app in `android/`
- Improve auto-connect reliability
- Build UI components in Jetpack Compose

### 📝 Documentation

- Improve the RE guide
- Add your bike variant findings
- Fix typos, improve clarity

---

## Contribution Guidelines

- **Be respectful** — this is a community project
- **No TVS proprietary code** — do not paste decompiled TVS Connect source code into this repo
- **Sanitize captures** — remove personal data before sharing
- **Small focused PRs** — one thing at a time is easier to review
- **Open an Issue first** for large changes

---

## Code Style (Android / Kotlin)

- Kotlin with Jetpack Compose
- Follow Android's official style guide
- Use `ktlint` for formatting: `./gradlew ktlintCheck`
- Document public APIs with KDoc

---

## Reporting a Finding

Open a GitHub Discussion (not an Issue) for:
- New UUIDs discovered
- Decoded opcodes
- Checksum algorithm identified

Use Issues for:
- Bugs in the Python PoC or Android app
- Documentation errors

---

## Code of Conduct

Be kind. We're all riders here.
