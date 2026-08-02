# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/)
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.4.0] - 2026-08-02

### Added

- **English interface and language choice.** `values/strings.xml` now holds the
  English strings and is the default fallback, `values-fa/strings.xml` holds the
  Persian ones, and `res/xml/locales_config.xml` declares both to the system.
  Devices set to French, Turkish or German get English instead of Persian.
- **In app language and layout direction settings.** A new settings tab stores
  the chosen language (system, Persian, English) and layout direction (auto,
  right to left, left to right) in DataStore and applies them from
  `AppLocalization`, above every screen and sheet.
- **Guide for every screen.** A help icon on the library, device, player,
  subtitle and settings surfaces opens a sheet that explains each icon and each
  option one by one.
- **First run tour.** Nine slides introduce the app and let the language, the
  layout direction and the subtitle text and background colours be chosen while
  they are being explained, with a live subtitle preview. The tour can be
  replayed from the settings sheet.
- Digit shaping is now driven by the interface language: Persian digits in
  Persian, Latin digits in English (`formatDuration`, `formatSize`,
  `formatCount`, `formatSpeed`, subtitle offsets).

### Fixed

- **The library can no longer be wiped by one unreadable byte.** Reading now
  distinguishes "nothing stored" from "could not be parsed" (`StoredValue`). On
  a parse failure the raw JSON is copied to a backup key, the failure is logged,
  and every write to that key is refused instead of overwriting the data with an
  empty list. The same protection covers the device position store.
- **Folders with the same name are separate folders again.** `BUCKET_ID` is read
  from MediaStore, `LocalVideo` carries `bucketId`, `VideoFolder` has a stable
  `id`, grouping is by bucket, and the Compose key is the id, so two folders
  called `Movies` no longer merge or clash.
- **Permission state is no longer stale.** Access is recomputed on
  `ON_RESUME` as `MediaAccess.FULL`, `PARTIAL` or `DENIED`, so returning from
  system settings updates the screen. `READ_MEDIA_VISUAL_USER_SELECTED` is now
  requested and checked at runtime, with a banner offering to widen a partial
  selection.
- **`EpisodePattern` rejects impossible values.** `step > 0`, `episode >= 0` and
  `pad > 0` are enforced in `init`. Legacy JSON is normalised by a custom
  serializer before the model is built, so old data is repaired instead of
  crashing the decode.
- **Network failures are told apart from missing episodes.** Availability is now
  a typed `AvailabilityResult` (`Available`, `NotAvailable`,
  `NetworkUnavailable`) behind an `AvailabilityProbe` interface.
  `NetworkUnavailable` is preserved all the way to `EpisodeResolutionResult`, so
  an offline device is no longer told "this was the last episode".
- **The eight second deadline is a real deadline.** The blocking probe runs in
  `runInterruptible`, so cancellation actually interrupts
  `HttpURLConnection.getResponseCode()`, and the connection is disconnected from
  a `finally` block on every path.
- **The player store survives an abandoned composition.** The `ViewModelStore`
  now belongs to `PlayerViewModelHost`, a `ViewModel` scoped to the Activity, so
  the framework clears it even if the `DisposableEffect` never runs; the screen
  still clears it early for a prompt release.
- **Thumbnail decoding is cancellable.** A real `CancellationSignal` is created
  and cancelled when the coroutine is cancelled.
- **`PlayerViewModel` compiles**: the missing `PlayerEngine` import was added.
- Legacy items get a stable id derived from url and title instead of a fresh
  `UUID.randomUUID()` on every read.
- Moving to the next episode rewrites the subtitle URL's episode marker instead
  of dropping the subtitle.
- The subtitle language is inferred from the file name instead of being
  hardcoded to `fa`, and an external subtitle is no longer forced with
  `SELECTION_FLAG_DEFAULT`.
- `S02E01` can now step back into the previous season.
- Gesture timing uses `SystemClock.elapsedRealtime()`, so a clock correction can
  no longer break double tap seeking.
- Gesture release only hands brightness back to the system when a gesture had
  actually taken it over.
- Top and bottom insets are applied once, not twice.
- The legacy data migration is independent and persistent, recorded under its
  own key and run exactly once.
- RTL is no longer forced app wide; layout direction follows the locale.

### Added

- `values-en/strings.xml`: the full English translation, Persian stays the
  default.
- The `com.opplayer.app.player.fakes` package (`FakePlayerEngine`,
  `FakeEpisodeResolver`, `FakeProgressSaver`, `FakeSubtitleSource`), which the
  unit test source set referenced but did not contain.
- `LibraryRepositoryTest` against a real DataStore backed by a temporary file:
  corrupt blobs are refused and backed up, progress lives outside the library
  blob, legacy formats migrate, nothing is lost.
- `EpisodePatternValidationTest` and `AvailabilityProbeTest`, including the
  offline and previous-season paths.

### Changed

- Progress is stored under its own key as a small id to position map, so saving
  a position no longer decodes and re-encodes the whole library.
- The autosave interval went from 5 s to 30 s, with an extra save on pause, seek
  and exit.
- Repository flows are `distinctUntilChanged()`.
- Library sorting and filtering run inside `remember(...) { derivedStateOf { } }`
  instead of on every recomposition.
- Digit conversion in the settings sheet reuses `util.toPersianDigits`.

### Earlier in this release

### Added

- Full Gradle wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper`). The scripts
  bootstrap `gradle-wrapper.jar` on first run if it is not committed yet.
- `PlayerEngine` abstraction with an ExoPlayer implementation plus an injectable
  `PlayerFactory`, `Clock` and `ProgressSaver`, so playback coordination can be
  tested without a device.
- `PlaybackStatus` sealed interface (`Idle`, `Preparing`, `Ready`, `Buffering`,
  `Ended`, `Error`) replacing the `isBuffering` + `errorRes` pair.
- `EpisodeResolutionResult` distinguishing `Found`, `NotFound`, `Timeout` and
  `NetworkUnavailable`, with an 8 second deadline on every lookup.
- `LibraryProgressUpdater` restored as pure, time-injected logic.
- Unit tests for the player view model, subtitle controller (including the
  video-switch race), episode controller and library progress updater.
- Instrumented tests for engine lifecycle and media item resolution, plus
  `docs/DEVICE_TEST_PLAN.md` with the manual device matrix.
- Release build, `check` and an emulator job in CI; release APK and R8 mapping
  are uploaded as artifacts.

### Changed

- `PlayerViewModel` is now a coordinator over `PlaybackController`,
  `SubtitleController`, `EpisodeController` and `ProgressManager`.
- `PlaybackRequest` and `EpisodePattern` are `@Parcelize`, and the hosting
  composable keeps playback state in `rememberSaveable`, so the current item,
  episode and position survive process death.
- The progress saver is registered in a `DisposableEffect` and cleared on
  dispose instead of being rebound on every recomposition.
- System Back and the top bar Back button share one handler: leave fullscreen
  first, close the player second.
- Lint now fails the build on errors and also runs for release builds.
- Version bumped to 1.4.0 (versionCode 8).
- Tabs are exposed as a `selectableGroup` with `Role.Tab`, the favourite button
  announces the action and the state it will produce, and subtitle colour
  swatches carry a name and a selection state, so TalkBack describes them.
- The next episode arrow uses `Icons.AutoMirrored`, so it follows a right to
  left layout like the Back arrow.
- The deprecated `kotlinOptions` block moved to `kotlin.compilerOptions`, and
  the deprecated `android:statusBarColor` / `android:navigationBarColor` theme
  items were removed; the activity already draws edge to edge.

### Changed (architecture)

- `PlayerScreen` split into `PlayerViewModel` plus dedicated composables
  (`PlayerSurface`, `PlayerTopBar`, `PlayerOverlays`, `PlayerErrorState`);
  playback, subtitle, episode and orientation logic moved out of the UI layer.
- Playback position is now polled only while playback is running (100 ms tick
  bound to `isPlaying`) instead of every 80 ms regardless of state.
- Embedded subtitle cues are closed when the next cue arrives and capped at
  10 s, so the last line no longer stays on screen during long silences.
- External subtitle lookup uses a binary search instead of a linear scan.
- R8 keep rules for media3 narrowed from the whole `exoplayer` package to the
  reflectively resolved media source factories and decoder renderers.
- Turning subtitles off now persists across sidecar loads and episode changes.

### Removed

- Dead code: `ui/PlayerViewModel.kt`, `ui/VideoItem.kt` and the unused
  `PlayerGestureOverlay` composable.
- Unused `media3-datasource-okhttp` entry from the version catalog.

### Added (documentation and tests)

- `docs/DEVELOPMENT.md` and `docs/DEVELOPMENT-fa.md`, including the rationale
  for allowing cleartext HTTP.
- Unit tests for orientation policy, scale mode, resume position, subtitle
  lookup, embedded cue timing, subtitle options and episode navigation support.

### Fixed (documentation)

- README version badges now match `versionName` 1.3.2.

## [1.3.2] - 2026-07-31

### Added
- Automatic sidecar subtitle discovery for device (offline) videos: files such as
  `Movie.srt`, `Movie.fa.srt`, `Movie.vtt`, or `Movie.ass` sitting next to the
  video are found through `MediaStore.Files` and the file system and loaded
  without any user action.
- Built-in subtitle parser for SRT, WebVTT, ASS/SSA, and plain text, with
  encoding detection (UTF-8/UTF-16 BOM, strict UTF-8, windows-1256 for Persian,
  windows-1250, ISO-8859-1) so Persian subtitles are no longer garbled.
- Manual subtitle picker (Storage Access Framework) with persisted read
  permission, plus a track selector that lists sidecar files and embedded
  (MKV) text tracks.
- Subtitle appearance settings persisted with DataStore: text color, background
  color, text size, distance from the bottom, bold, and shadow, with a live
  preview.
- Subtitle icon in the bottom navigation bar (next to the online and device
  tabs) and in the player top bar, both opening the subtitle sheet.
- Subtitle sync control (±0.1 s / ±0.5 s steps, up to ±60 s) for subtitles that
  run ahead of or behind the video.
- Unit tests for the subtitle parser (SRT/CRLF, WebVTT, ASS format ordering,
  windows-1256 decoding).

### Changed
- Subtitles are now rendered by a Compose overlay driven by the current playback
  position instead of the ExoPlayer subtitle view, which is what makes styling
  and timing offsets possible.

### Fixed
- Offline videos never showed subtitles because the device playback request was
  created without a subtitle source and no sidecar lookup existed.

## [1.3.1] - 2026-07-29

### Added
- Unit tests for episode URL detection, two-link pattern detection, episode
  pattern math, playback progress trimming, and formatting helpers.
- Instrumentation test module.
- GitHub Actions workflows for CI and for signed releases.
- Dependabot configuration, issue templates, and a pull request template.
- `SECURITY.md`, `CONTRIBUTING.md`, `CODE_OF_CONDUCT.md`, and this changelog.
- Release signing configuration driven by `keystore.properties` or `OPPLAYER_*`
  environment variables, with V1 and V2 signing enabled.

### Changed
- The network user agent is derived from `BuildConfig.VERSION_NAME` instead of a
  hardcoded version string.
- Playback position cleanup now keeps the most recently watched items instead of
  the longest positions.

### Fixed
- `onNewIntent()` is handled, so opening a second video from a file manager while
  the app is already running starts the new video.



[1.4.0]: https://github.com/8Ebrahim1/OPPlayer/releases/tag/v1.4.0
[1.3.2]: https://github.com/8Ebrahim1/OPPlayer/releases/tag/v1.3.2
[1.3.1]: https://github.com/8Ebrahim1/OPPlayer/releases/tag/v1.3.1

