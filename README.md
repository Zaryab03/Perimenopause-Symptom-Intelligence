# Perimenopause Symptom Intelligence

Android app that helps users record, review and export their own perimenopause-related symptom history.
It does not diagnose, predict or give medical advice. All insights are deterministic calculations over what the user recorded.

**Status: v0.1.0 test build. Local-only. No account, no server, no internet permission, no billing.**

## What works in this build (implemented, compiled, unit-tested; NOT yet run by the developer on a device)

- First-run onboarding (all optional): name, whether you still have periods, symptoms to track, reminder, PIN.
- Today: mood check-in, tracked symptoms (tap to expand: Not today / Mild / Moderate / Severe), live "recorded" summary, optional sleep/energy/bleeding/notes, last-7-days chart.
- "Not recorded" is kept distinct from "Not today" everywhere (UI, insights, exports).
- History: shaded month calendar, day detail, timeline, search across symptoms and notes.
- Insights: 7/30/90 days, daily chart, progress toward first summary (needs 5 days of entries), optional previous-period comparison.
- Export: CSV, JSON, PDF for a date range; appointment PDF with symptom selection and questions.
- Custom symptoms, add/hide symptoms, optional daily reminder (generic text), delete-all-data.
- Local PIN lock (auto-locks after 30 s in background, hides app from screenshots/recents). This is a device lock, **not** an online account.
- Education library: **draft text, not clinically reviewed** (see `content/Articles.kt`).
- Storage: Room on SQLCipher; random key wrapped by Android Keystore. Backup disabled.

## Not implemented yet

Online accounts / login, backend, sync, premium features, billing, Play Store release signing, localisation, accessibility audit, instrumented/UI tests.

## Build (this machine)

The JDK, Android SDK and Gradle live in `/var/tmp/hydra-android` because `/home` is nearly full.

```bash
. /var/tmp/hydra-android/env.sh
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
# APKs: app/build/outputs/apk/{debug,release}/
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

- Not run on any device or emulator by the developer (the emulator will not start on this machine's disk). Only compiled, unit-tested and linted.
- Release builds are unshrunk (R8 off) for now; enable and test shrinking before Play release.
- Tester APKs are signed with a local test key kept outside the repo (`~/.keystores`, `keystore.properties`, both git-ignored). Back it up if you intend to keep updating installed copies.
- If the Keystore key is lost the database becomes unreadable (the app has no recovery path yet).
- Education content needs clinician review and sources before any public release.
- Reminders use inexact timing and may be delayed by battery optimisation.
- The applicationId `com.perimenopauseintelligence.app` is permanent once published; confirm before the first upload.
