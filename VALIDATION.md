# Validation — v0.7.1 (27 October 2026)

- APK built successfully; Android lint passed.
- 38 JVM tests passed, including independent axle thresholds, strict boundary comparisons and matching tile/background alarms.
- API 36 emulator checks passed for stored threshold defaults and persistence, retained account credentials with masked password, and explicit clearing of account data. Only synthetic credentials were used; no HTTP login or vehicle command.
- Existing consent, scheduling, expiry and demo diagnostics checks passed.
- Real vehicle behavior remains subject to field testing; warning data can be stale.

No application source published. Test expiry remains 25 November 2026, 00:00 UTC.
