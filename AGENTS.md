# Repository Guidelines

## Project Structure & Module Organization

NeuroTrack is a single Android app module in `:app`. Kotlin source lives under `app/src/main/java/com/example/neurotrack`: `data` contains Room entities, DAOs, and database setup; `domain` contains analysis logic; `background` contains workers and screen-event readers; `ui` contains Compose screens and the ViewModel; `ui/theme` contains Material theme definitions. Resources live in `app/src/main/res`; keep user-facing text mirrored in `values/strings.xml` and `values-en/strings.xml`. Local JVM tests mirror package paths in `app/src/test/java`; instrumented Android tests live in `app/src/androidTest/java`.

## Build, Test, and Development Commands

Run commands from the repository root with the Windows Gradle wrapper:

- `.\gradlew.bat assembleDebug` builds the debug APK.
- `.\gradlew.bat :app:compileDebugKotlin` type-checks Kotlin and Compose code.
- `.\gradlew.bat :app:lintDebug` runs Android lint.
- `.\gradlew.bat :app:testDebugUnitTest` runs local JUnit tests.
- `.\gradlew.bat :app:connectedDebugAndroidTest` runs instrumented tests on a connected device or emulator.

The project targets Android SDK 36, min SDK 26, Kotlin 2.2, AGP 9.2, and expects JDK 17+.

## Coding Style & Naming Conventions

Use idiomatic Kotlin with 4-space indentation and trailing commas for multi-line argument lists where helpful. Name classes and composables in `PascalCase`; functions, properties, and local variables in `camelCase`; constants in `UPPER_SNAKE_CASE`. Keep Compose UI state hoisted into `NeuroTrackViewModel` or small pure helpers when logic needs tests. Prefer package-local helpers over broad utility files.

## Testing Guidelines

Use JUnit 4 for local tests. Name test files after the subject, such as `SleepAnalyzerTest.kt`, and use descriptive test methods like `analyze_marksMissingWhenThereAreNoScreenEvents`. Add local tests for pure analysis, formatting, and selection logic; use instrumented tests for Android framework behavior. Run `:app:testDebugUnitTest` before every PR, plus lint and Kotlin compilation for UI or build changes.

## Commit & Pull Request Guidelines

The current history only establishes `Initial commit`; use short, imperative subjects such as `Add sleep gap analyzer tests`. PRs should explain the user-facing change, list validation commands run, link related issues, and include screenshots for visible UI changes. Mention updates to both English and Chinese strings when copy changes.

## Privacy & Configuration Tips

This app handles sensitive self-observation data and is intentionally local-first. Do not add network uploads, analytics, accounts, or medical claims without explicit design discussion. Never commit `local.properties`, keystores, generated APKs, or personal device data.

Release signing material is stored outside the repository at `C:\Users\chiye\.neurotrack-release-signing`. Use it only for local release packaging, and do not commit the keystore, password file, or signed APKs.
