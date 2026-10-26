# Perimenopause Symptom Intelligence

Android app that helps users record, review and export their own perimenopause-related symptom history.
It does not diagnose, predict or give medical advice. All insights are deterministic calculations over what the user recorded.

**Status: v0.1.0 test build. Local-only. No account, no server, no internet permission, no billing.**

## What works in this build (implemented)

- Daily log: symptoms (Not today / Mild / Moderate / Severe), sleep, mood, energy, menstrual bleeding (with "doesn't apply"), notes, skip a day.
- "Not recorded" is kept distinct from "Not today" everywhere (UI, insights, exports).
- History: month calendar (logged / skipped / no entry), timeline, search across symptoms and notes.
- Insights: last 7 / 30 / 90 days, optional comparison with the previous period; refuses to summarise with fewer than 5 days of entries.
- Export: CSV, JSON, PDF for a chosen date range; appointment PDF with symptom selection and questions.
- Custom symptoms; hide/show symptoms; optional daily reminder with generic text; delete-all-data.
- Education library: **draft text, not clinically reviewed** (see `content/Articles.kt`).
- Storage: Room on SQLCipher; random key wrapped by Android Keystore. Backup disabled.

## Not implemented yet

Accounts, backend, sync, premium features, billing, Play Store release signing, localisation, accessibility audit, instrumented/UI tests.

## Build (this machine)

The JDK, Android SDK and Gradle live in `/var/tmp/hydra-android` because `/home` is nearly full.

```bash
. /var/tmp/hydra-android/env.sh
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

`local.properties` (SDK path) is machine-specific and git-ignored. Without the env script you need JDK 17+ and the Android SDK (platform 37, build-tools 36).

## Install on a phone

Enable developer options and USB debugging, then `adb install -r dist/PerimenopauseSymptomIntelligence-0.1.0-debug.apk`,
or copy the APK to the phone and open it (allow installs from that source).
The debug build installs as `com.perimenopauseintelligence.app.debug` and is signed with the shared Android debug key. It is not for distribution.

## Layout

- `domain/`: pure Kotlin (models, insights, export formats, report content). Unit-tested.
- `data/`: Room entities/DAO, encrypted DB, Keystore key, repository, settings.
- `ui/`: Compose screens. `reminders/`: WorkManager reminder. `share/`: file export and PDF.

## Known limitations

- Never run on a device or emulator yet (none available here); only compiled, unit-tested and linted.
- If the Keystore key is lost the database becomes unreadable (the app has no recovery path yet).
- Education content needs clinician review and sources before any public release.
- Reminders use inexact timing and may be delayed by battery optimisation.
- The applicationId `com.perimenopauseintelligence.app` is permanent once published; confirm before the first upload.
