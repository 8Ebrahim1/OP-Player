# Contributing to OP Player

Thanks for taking the time to contribute.

## Getting started

```bash
git clone https://github.com/8Ebrahim1/OPPlayer.git
cd OPPlayer
./gradlew assembleDebug
```

Requirements: JDK 17, Android SDK 35, Android Studio Ladybug or newer.

If `gradlew` is missing, generate the wrapper once with
`gradle wrapper --gradle-version 8.10.2`.

## Before opening a pull request

```bash
./gradlew lintDebug
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

## Code style

- Kotlin official code style, four space indentation, 100 column soft limit.
- Comments are written in English only. Do not add Persian comments to source
  files.
- All user facing text goes into `app/src/main/res/values/strings.xml`. The UI is
  right to left, so never hardcode a string in a composable.
- Keep composables small and stateless; state belongs in the view models.
- Player logic belongs in `player/`, storage in `data/`, and UI in `ui/`.

## Commit messages

Use short imperative subjects, optionally with a scope:

```
player: keep resume position when switching episodes
data: sort progress cleanup by last watched time
```

## Tests

Pure logic (URL parsing, pattern detection, formatting, progress trimming) must
be covered by JVM tests in `app/src/test/`. Anything that needs a real device or
the Android framework belongs in `app/src/androidTest/`.

## Reporting bugs

Open an issue with the bug report template and include the app version, the
Android version, and reproduction steps.
