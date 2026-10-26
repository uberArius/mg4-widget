# Validation — v0.7.0 (26 October 2026)

- Android APK and test APK built; lint passed with no errors.
- 37 JVM unit tests passed (0 failures). Coverage includes drive OFF/ON/unknown, the strict 2.4 bar boundary, grouped alarm causes, two-decimal pressure display, selection/prepending and reordering.
- API 36 emulator smoke tests passed: consent, scheduling, expiry, field migration and demo diagnostics.
- Emulator touch checks confirmed adding an unselected tile at the top, deselecting, holding/dragging to reorder, and persistence after restarting the activity.
- Inspected the dashboard with a red tyre tile, a named warning cause, checkmarks and selection positions. Hidden tyre data still triggers the outer warning.
- No changes to vehicle API commands. No real vehicle or account was used in these tests.
- Real vehicle readings and climate confirmation still require field testing. Cached warning data may be old; this is not guaranteed real-time safety monitoring.

No application source is published. Test expiry remains 25 November 2026, 00:00 UTC.
