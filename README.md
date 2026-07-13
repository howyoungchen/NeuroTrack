# NeuroTrack

NeuroTrack is a local-first Android app for weekly pressure reflection and mindfulness practice.

It does not infer sleep, read app usage or location, or turn a single day into a pressure verdict. One past-week reflection and the week’s mindfulness completion form a weekly pressure score. The Status screen intentionally shows only the current pressure bar and an eight-week trend.

## Core experience

- **Weekly reflection:** ten concrete questions cover perceived sleep quality, phone attention, openness to different views, major events, surroundings and self-care, work or study, relationships, physical signals, recovery time, and unfinished-task worry.
- **Weekly pressure:** the reflection is the base score. Missing the planned Monday, Wednesday, Friday, and Sunday mindfulness sessions adds up to two points for insufficient recovery.
- **Mindfulness:** choose 5, 10, or 15 minutes. The app plays locally generated soothing music, keeps the screen awake, and blocks Back. Leaving or switching apps marks the session interrupted.
- **Fixed rhythm:** practice every Monday, Wednesday, Friday, and Sunday, with a configurable reminder time.
- **Minimal permission:** Android 13+ asks only for notification permission, solely for practice reminders.
- **Fully local:** no accounts, uploads, analytics SDKs, or cloud backup. The redesigned database is rebuilt directly and does not preserve legacy sleep data.

## Screens

- **Status:** this week’s pressure bar and the recent eight-week trend.
- **Practice:** weekly rhythm, past-week reflection, and mindfulness.
- **Settings:** reminder time, notification permission, language, and theme.

## Build and verify

Use JDK 17+ and Android SDK 36, then run from the repository root:

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:compileDebugKotlin
.\gradlew.bat :app:lintDebug
.\gradlew.bat assembleDebug
```

## Disclaimer

NeuroTrack supports self-observation and mindfulness. It does not provide medical diagnosis or treatment. Seek professional or local emergency help if distress persists, worsens, or includes risk of self-harm.

[简体中文](README.zh-CN.md)
