# Legal Analysis — ntorq-open-connect

> **Disclaimer:** This is not legal advice. It is a good-faith analysis by the project maintainer. Consult a qualified advocate for formal legal opinions.

---

## Summary

Reverse engineering the TVS Connect app for interoperability purposes is **permitted under Indian copyright law**. It is not a criminal offense. It may breach TVS's Terms of Service (a civil matter), but this project takes steps to minimize that risk.

---

## Indian Copyright Act, 1957

### Section 52(1)(ab) — Interoperability Exception

> *"The doing of any act necessary to obtain information essential for operating inter-operability of an independently created computer programme with other programmes... by a lawful possessor of a copy of a computer programme... provided that such information is not otherwise readily available."*

This project directly qualifies:
- We are **lawful possessors** of the TVS Connect app (downloaded from Play Store)
- The goal is **interoperability** — making an independently created app communicate with the NTorq cluster
- The protocol information is **not otherwise available** publicly

### Section 52(1)(ac) — Observation and Study

Permits observing program function, inputs, and outputs during normal use. Using HCI snoop logs to observe BLE communication during normal app operation falls squarely here.

### Idea / Expression Doctrine

GATT UUIDs, command opcodes, packet structures, and communication protocols are **functional ideas**, not copyrightable expression. Only TVS's specific code implementation is protected. We are documenting the protocol, not copying their code.

---

## IT Act, 2000

### Section 43 — Unauthorized Access

Does **not apply**. This section covers unauthorized access to *another person's* computer system. We are communicating with our **own bike** using our **own phone**. Fully consensual.

### Section 66 — Computer-Related Offences

Requires **dishonest or fraudulent intent**. A non-commercial open-source project improving interoperability of your own device has neither.

---

## TVS Connect Terms of Service

Clause 6.6(ii) of the TVS Connect ToS prohibits reverse engineering. This is a **contractual restriction**, not a law. Violating it is a civil breach of contract — not a criminal act.

**Realistic risk:** TVS may send a cease-and-desist. They are unlikely to pursue litigation against a non-commercial individual developer.

**How this project mitigates risk:**
- No TVS trademarks used in the app name or branding
- No calls to TVS backend APIs from OpenTorq
- Research conducted on our own devices
- APK obtained from public sources (APKMirror), not authenticated sessions

---

## Play Store Distribution

Google Play's IP policy prohibits using TVS trademarks in the app title or icon. The app is named **OpenTorq** and describes compatibility descriptively in the listing body only. This complies with Play Store policy.

---

## DPDP Act, 2023

The Digital Personal Data Protection Act applies if personal data is collected. OpenTorq is designed **on-device first** — ride data stays in local Room database by default, no cloud sync, no analytics without explicit consent. A privacy policy is included at first launch.

---

## What We Do Not Do

- ❌ Call TVS's backend APIs (`tvsconnectapi.tvsmotor.com`) from OpenTorq
- ❌ Connect to anyone else's bike without their consent
- ❌ Flash modified firmware to the vehicle
- ❌ Use TVS, NTorq, SmartXonnect, or xConnect as trademarks in our branding
- ❌ Distribute cracked or modified versions of the official TVS Connect app

---

## References

- Indian Copyright Act, 1957: https://copyright.gov.in/Documents/CopyrightRules1957.pdf
- SpicyIP — Decompilation Defence under Indian Copyright Law: https://spicyip.com/2009/02/decompilation-defence-under-indian.html
- IT Act, 2000: https://indiankanoon.org/doc/1965344/
