# Validation — v0.6.0 (26 October 2026)

- Android APK and test APK built; lint passed with no errors.
- 33 JVM unit tests passed (0 failures). Four new tests cover drive OFF/ON/unknown, each tyre and the strict 2.4 bar boundary, every supported opening and non-triggering missing/climate data.
- Emulator API 36 smoke tests passed: consent, scheduling, expiry, field migration and demo diagnostics.
- Verified all 28 dashboard tile labels by scrolling, even though only 10 tiles were selected for the widget. Refresh button follows the complete list. App dashboard screenshot inspected.
- Verified red widget background with only battery/range visible while a hidden tyre reports 2.3 bar and drive reports OFF. Screenshot inspected. Synthetic/demo data only.
- Reused widget tile binding in the app dashboard; no changes to API commands.
- Real vehicle readings and climate confirmation still require field testing. Cached warning data may be old; there is no guarantee of real-time safety monitoring.

No application source is published. Test expiry remains 25 November 2026, 00:00 UTC.
