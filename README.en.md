<div align="center">

# OP Player

**A modern Android video player built with Jetpack Compose and Media3 — with a polished dark look and full RTL support.**

[فارسی](README.md) · [Developer notes ](docs/DEVELOPMENT.md)

![version](https://img.shields.io/badge/version-1.3.1-blue)
![minSdk](https://img.shields.io/badge/minSdk-26-green)
![targetSdk](https://img.shields.io/badge/targetSdk-35-green)
![license](https://img.shields.io/badge/license-MIT-lightgrey)

</div>

---

## About

OP Player makes it easy to watch both **online video links** and **videos stored on your phone**. It is built entirely with Kotlin, Jetpack Compose, and AndroidX Media3 (ExoPlayer), and comes with a dark glass-style interface plus full right-to-left support.

One of its handiest features is **automatic next-episode navigation for online links**. The app finds the season and episode number in the URL, moves to the next one, checks that the file actually exists on the server, and even jumps to the next season when needed — all without downloading anything.

## Features

### Playback

- Plays a wide range of formats, including **MKV**, WebM, MP4, M4V, MOV, AVI, FLV, TS/M2TS, MPEG, 3GP, and OGV
- Supports **HLS** (`.m3u8`), **DASH** (`.mpd`), **SmoothStreaming**, and **RTSP**
- Automatically falls back to software decoding if hardware decoding fails
- Supports external subtitles with automatic format detection: SRT, VTT, ASS/SSA, and TTML
- Includes Picture-in-Picture, with a compatibility check to keep things stable on unsupported devices
- Handles audio focus and pauses when headphones are disconnected
- Remembers where you stopped and saves your progress regularly
- Keeps the screen awake while you watch and pauses playback when the app goes into the background

### Next-episode navigation for online links

When the current URL includes a recognizable episode number, you will see a double-arrow button in the player controls.

- Recognizes patterns such as `S01E02`, `s1e2`, `S01.E02`, `S01 - E02`, `Episode 07`, `EP05`, and `E05`
- Checks **only the file name**, so numbers in the domain or path are not mistaken for episode numbers
- Keeps zero padding intact: `E02 → E03`, `E09 → E10`, and `E9 → E10`
- Checks whether the next file exists with an HTTP `HEAD` request, then falls back to `Range: bytes=0-0` if the server blocks `HEAD`
- Moves to the next season automatically; if `S01E25` is missing, it tries `S02E01`
- Can automatically start the next episode when the current one ends

For example:

```text
…/Black.Torch.S01E02.720p.WEB-DL.x264.SoftSub.mkv
→ …/Black.Torch.S01E03.720p.WEB-DL.x264.SoftSub.mkv
```

Parts such as `WEB-DL` and `x264` are safely ignored, so they will not confuse episode detection.

### Videos on your phone

- Reads videos from `MediaStore` and organizes them **by folder**
- Shows thumbnails, duration, file size, and extension without needing an external image library
- Lets you search for a file by name inside each folder
- Handles permissions for `READ_MEDIA_VIDEO` on Android 13+ and `READ_EXTERNAL_STORAGE` on Android 12 and earlier, including a shortcut to app settings if permission is permanently denied
- Opens videos shared from other apps through `ACTION_VIEW`

### Online library

- Add a video link with an optional custom title and subtitle URL
- Keep favorites, recently played items, and a separate resume position for each video
- Stores everything locally with DataStore and kotlinx.serialization, with serialization kept off the main thread

### Player settings

You can find everything under the gear icon:

- Ready-made speed options from 0.25× to 3×
- A **custom speed** from 0.1× to 6×, with support for Latin, Persian, and Arabic digits
- Aspect-ratio options: fit, zoom, and fill
- A switch for automatically playing the next episode

## Privacy and backup

OP Player has no backend, analytics, or user accounts. Your links and playback positions stay on your device in DataStore.

Cloud backup is enabled only for the `datastore/` folder, so your library and watch progress can come back when you move to a new phone. If you would rather turn backup off completely, set `android:allowBackup="false"` in `app/src/main/AndroidManifest.xml`.
