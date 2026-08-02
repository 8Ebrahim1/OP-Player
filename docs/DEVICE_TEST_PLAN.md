# Device test plan

Unit tests cover coordination and pure logic. The behaviours below depend on a
real Android runtime (surfaces, audio focus, PiP, orientation, network) and must
be run on an emulator or a physical device before every release.

Automated part:

```bash
./gradlew connectedDebugAndroidTest
```

This runs `app/src/androidTest`, which checks engine construction and teardown,
media item resolution for each stream type, and PiP availability. Everything
else in the matrix is manual, because it needs live streams or physical input.

## Matrix

| # | Scenario | Steps | Expected result |
|---|----------|-------|-----------------|
| 1 | Repeated open/close | Open a video, press Back, repeat 10 times | Memory stays flat; only one ExoPlayer instance alive at a time; no `Player released` warnings in Logcat |
| 2 | HLS | Play an `.m3u8` stream | Playback starts; quality adapts; no fatal error |
| 3 | DASH | Play an `.mpd` stream | Playback starts; seeking works |
| 4 | RTSP | Play an `rtsp://` stream | Live playback starts within a few seconds |
| 5 | Local file | Play a file from the device tab | Playback starts; resume position is saved |
| 6 | Background / foreground | Start playback, press Home, return | Playback pauses on leaving; position is preserved; no audio in the background |
| 7 | PiP enter/exit | Press the PiP button, then return to the app | Video keeps playing in the window; controls are hidden in PiP; returning restores full controls |
| 8 | Rotation (windowed) | Rotate while playing | No restart, no black frame, position unchanged |
| 9 | Rotation (fullscreen) | Enter fullscreen, rotate both ways | System bars stay hidden; aspect ratio is preserved |
| 10 | Network drop | Disable Wi-Fi and mobile data during playback | A network error is shown, not a crash |
| 11 | Retry after error | Re-enable the network, press Retry | Playback resumes from the same position |
| 12 | External subtitle | Pick an `.srt`/`.vtt` file from the subtitle sheet | The file loads, a confirmation toast is shown, lines appear in sync |
| 13 | Embedded subtitle | Switch between embedded tracks | The selected track is applied; the external file is dropped |
| 14 | Delayed subtitle + seek | Set an offset, then seek forward and backward | The offset survives the seek; stale embedded cues disappear |
| 15 | Auto-next | Let an episode play to the end | The next episode starts automatically with its label in the top bar |
| 16 | Auto-next unavailable | Play the last episode to the end | A single explanatory toast; playback stays on the current item |
| 17 | Episode lookup offline | Turn the network off and press "next episode" | The network message appears within 8 seconds; the spinner stops |
| 18 | Position on exit | Play for a while, press Back | The library card shows the resume position |
| 19 | Position on pause | Pause, leave the app, return | The position matches where playback stopped |
| 20 | Position at end | Let the video finish | The item resumes from the beginning next time |
| 21 | Back behaviour | Press system Back and the top bar Back button in fullscreen | Both leave fullscreen first, then close the player |
| 22 | Process death | Enable "Don't keep activities", start playback, leave and return | The player screen and the current episode are restored |

## Release checks

Run the shrunk build as well; ProGuard and resource shrinking only apply there.

```bash
./gradlew assembleRelease
```

Install the release APK and repeat scenarios 2 to 5 and 12 to 15. These exercise
the Media3 DASH/HLS/RTSP factories and the subtitle parsers, which are the parts
most likely to be affected by shrinking.
