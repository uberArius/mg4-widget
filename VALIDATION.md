# Validation — v0.5.0

- Android debug APK and instrumentation APK built successfully.
- 29 JVM unit tests passed. Android lint: no errors.
- API 36 emulator instrumentation passed: consent blocks login before HTTP and periodic scheduling; 15-minute scheduling; cancellation at 0 + Bluetooth off; empty/all-tile configurations; legacy field migration; demo diagnostics; expiry blocks HTTP and cancels vehicle jobs.
- First-use UI checked through Android UI inspection: Continue disabled until the checkbox is selected; accepting opens login. English and Polish notice text available. The account screen prevents screenshots, so a notice screenshot is not included.
- Actual widget tap tested on emulator demo data: Refresh updates the stored reading while the launcher stays foreground; Heating opens ClimateActivity for confirmation.
- Widget and custom MG-letter icon inspected visually in the emulator.
- Login was reported working by the initial user in v0.4.1. Real vehicle readings, remote controls, regional accounts, physical Bluetooth, Samsung Fold/One UI and Android 17 have not been fully validated. Background timing is not guaranteed.
- Tests used synthetic credentials/data and did not send commands to a real vehicle.

Runtime license inventory: 35 resolved dependency artifacts, with source-archive provenance and preserved notices. Application code is not in this repository.
