# Repository Guidelines

## Project Structure & Module Organization

This is a single-module native Android application for local ME-90 control. The `app/` module contains all distributable code. Keep activity/UI state in `app/src/main/java/local/me90/controller/MainActivity.java`; keep USB discovery, permission, endpoint handling, and SysEx framing in `UsbMidiClient.java`. Android declarations live in `app/src/main/AndroidManifest.xml`, styles in `app/src/main/res/values/`, and app artwork in `app/src/main/res/drawable/`. Build outputs under `app/build/` are generated and must not be edited or committed.

## Build, Test, and Development Commands

Use the bundled JDK, Android SDK, and offline Gradle distribution:

```powershell
$env:JAVA_HOME=(Resolve-Path '.\tools\jdk-17.0.10+7').Path
$env:ANDROID_HOME=(Resolve-Path '.\tools\android-sdk').Path
.\tools\gradle-8.10.2\bin\gradle.bat --offline --no-daemon --console=plain :app:packageDebug
```

This produces `app/build/outputs/apk/debug/app-debug.apk`. Run `:app:assembleDebug` for a compile/package check when a deployable APK is not needed. Install and verify behavior on a physical Android device connected to an ME-90; USB permission, MIDI input, patch synchronization, tuner data, and Live mode require hardware testing.

## Coding Style & Naming Conventions

Use Java with four-space indentation and the compact local style already used in the two source files. Prefer clear private helpers such as `showTuner()` and `requestFullSync()`. Use `PascalCase` for classes, `camelCase` for methods/fields, `UPPER_SNAKE_CASE` for constants, and short hardware identifiers only where they match device terminology (for example, `DLY` or `S/R`). Keep UI strings bilingual through `t(chinese, english)`; do not hard-code a new user-facing string in only one language.

## Testing Guidelines

There is currently no automated test suite. Every change must at least build successfully. For transport or state changes, test reconnection, device-side patch changes, Manual mode, and control feedback. For UI changes, test both Chinese and English, including entering and leaving Live mode. Record the Android version and ME-90 behavior in the pull request when hardware testing is involved.

## Commit & Pull Request Guidelines

Follow the existing concise conventional format: `feat: ...`, `fix: ...`, `refactor: ...`, or `release: ...`. Keep each commit focused. Pull requests should explain behavior changes, list the build command used, link relevant issues, and include screenshots for visual changes. Never commit signing secrets, private captures, device dumps, or generated APKs unless a release explicitly requires one.
