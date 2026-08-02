# Development guide

This document describes how OPPlayer is built, how the player layer is
structured, and which decisions are deliberate.

## Requirements

| Tool | Version |
| --- | --- |
| JDK | 17 |
| Gradle wrapper | 8.10.2 |
| Android compileSdk / targetSdk | 35 |
| Android minSdk | 26 |
| Kotlin | 2.0.21 |

Common tasks:

```bash
./gradlew lintDebug          # Android lint
./gradlew testDebugUnitTest  # JVM unit tests
./gradlew assembleDebug      # debug APK
```

The same three tasks run in CI (`.github/workflows/android.yml`).

## Module map

| Package | Responsibility |
| --- | --- |
| `data` | Library persistence, `VideoItem`, `EpisodePattern`, local progress |
| `player` | Pure playback logic with no Compose dependency |
| `player.subtitle` | Subtitle parsing, lookup, timing, option building |
| `ui.player` | Player state holder and player-specific composables |
| `ui.screens` | Screen level composables |
| `ui.components` | Reusable UI (sheets, cards, overlays) |
| `util` | Formatters and small Android helpers |

Everything under `player/` is plain Kotlin and therefore unit testable on the
JVM. Anything that needs Android or Compose lives under `ui/`.

## Player architecture

The player is split into three layers.

1. **Logic** (`player/`, `player/subtitle/`): pure functions and small objects.
   `OrientationPolicy`, `PlaybackProgress`, `SubtitleCues`,
   `EmbeddedSubtitleTimeline`, `SubtitleOptions`, `EpisodeNavigator` and
   `EpisodeResolver` contain no Android UI state.
2. **State** (`ui/player/PlayerViewModel.kt`): owns the ExoPlayer instance,
   exposes an immutable `PlayerUiState`, a `subtitleText` flow and a one shot
   `messages` channel for toasts. Every user action is a method on the view
   model.
3. **UI** (`ui/screens/PlayerScreen.kt`, `ui/player/*.kt`): renders state and
   forwards events. `PlayerScreen` keeps only ephemeral UI state (sheet
   visibility, seek hint, gesture HUD).

`PlayerViewModel` is scoped to the screen through a private
`ViewModelStoreOwner`, so the player is released when the screen leaves the
composition rather than when the Activity finishes.

## Subtitle timing

External subtitles are parsed into `SubtitleCue(startMs, endMs, text)` and
looked up with a binary search (`SubtitleCues.textAt`), so a large SRT file
costs O(log n) per frame.

Embedded tracks arrive through `Player.Listener.onCues` and carry no end time.
`EmbeddedSubtitleTimeline` therefore closes each cue when the next one arrives
and caps an open cue at `MAX_CUE_DURATION_MS` (10 s). Without that cap the last
line of dialogue stays on screen through long silences.

Playback position is polled only while playback is running: the ticker is a
`collectLatest` on an `isPlaying` flow, at `POSITION_TICK_MS` (100 ms). Pausing
the video stops the ticker and therefore stops recomposition.

## Network security

`res/xml/network_security_config.xml` sets `cleartextTrafficPermitted="true"`
for the base config. This is deliberate:

* the user pastes arbitrary direct video links, and a large share of those
  hosts are HTTP only, so the permission cannot be reduced to a domain list;
* the app has no backend, no accounts, no tokens and no telemetry, so no
  credential is ever transmitted;
* only system trust anchors are configured, user installed CAs are not trusted.

If a future build gains an authenticated service, that traffic must go through
a `domain-config` with `cleartextTrafficPermitted="false"`.

## Shrinking

Release builds run R8 with `isMinifyEnabled` and `isShrinkResources` enabled.
`proguard-rules.pro` keeps only what is resolved reflectively:

* constructors of the DASH, HLS, SmoothStreaming and RTSP media source
  factories, which `DefaultMediaSourceFactory` looks up by name;
* constructors of the optional `androidx.media3.decoder.*Renderer` classes used
  by `DefaultRenderersFactory`;
* `kotlinx.serialization` serializers.

A blanket `-keep class androidx.media3.exoplayer.** { *; }` would disable
shrinking for the largest dependency in the project and must not be
reintroduced.

## Tests

Unit tests live in `app/src/test/java/com/opplayer/app/` and mirror the main
source layout. They cover episode pattern detection, local progress, formatters,
subtitle parsing and lookup, embedded cue timing, subtitle option building,
orientation policy, scale mode cycling and resume position.

UI is intentionally not covered by instrumentation tests; the logic that used to
live inside the player composable was moved out precisely so it can be tested
without a device.

## Conventions

* No business logic inside composables. If a decision can be expressed as a
  pure function, it belongs in `player/`.
* Magic numbers become named constants at the top of the file.
* Strings shown to the user always come from `strings.xml`; the view model emits
  string resource ids, never formatted text.
* Public functions in `player/` get KDoc when the behaviour is not obvious from
  the name.
