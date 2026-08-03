<div align="center">

# OP Player

**A Modern player with the ability to skip to the next video in online links**

[فارسی](README.fa.md) · [Developer notes](docs/DEVELOPMENT.md)

![version](https://img.shields.io/badge/version-1.4.0-blue)
![minSdk](https://img.shields.io/badge/minSdk-26-green)
![targetSdk](https://img.shields.io/badge/targetSdk-35-green)
![license](https://img.shields.io/badge/license-MIT-lightgrey)

</div>

---

## About

OP Player is for the way people actually watch shows: you paste a link, you watch, and when the episode ends you want the next one — not a trip back to the browser.

That is the part the app takes care of for you. It reads the season and episode number straight from the link, builds the address of the next episode, quietly checks that the file is really there on the server, and starts playing it. When a season runs out, it moves on to the first episode of the next one. Nothing is downloaded to make that happen.

The rest is what you would expect from a player you keep on your phone: it plays just about any format you throw at it, remembers where you left off in every video, handles subtitles, and also organizes the videos already stored on your device.

## Features

### Next episode, without leaving the player

Whenever the app spots an episode number in the current link, a double-arrow button appears in the player controls. One tap and you are in the next episode.

- Understands the naming people actually use: `S01E02`, `s1e2`, `S01.E02`, `S01 - E02`, `Episode 07`, `EP05`, `E05`
- Reads **only the file name**, so a number in the domain or folder path never gets mistaken for an episode
- Respects zero padding, so `E02 → E03`, `E09 → E10`, and `E9 → E10` all come out right
- Confirms the next file exists with a `HEAD` request, and falls back to `Range: bytes=0-0` for servers that block `HEAD`
- Rolls over to the next season on its own: if `S01E25` is not there, it tries `S02E01`
- Optional auto-play, so the next episode starts the moment the current one ends

For example:

```text
…/Black.Torch.S01E02.720p.WEB-DL.x264.SoftSub.mkv
→ …/Black.Torch.S01E03.720p.WEB-DL.x264.SoftSub.mkv
```

Tags like `WEB-DL` and `x264` are ignored safely and never throw the detection off.

### Playback

- Plays a wide range of formats, including **MKV**, WebM, MP4, M4V, MOV, AVI, FLV, TS/M2TS, MPEG, 3GP, and OGV
- Handles streaming links too: **HLS** (`.m3u8`), **DASH** (`.mpd`), **SmoothStreaming**, and **RTSP**
- Falls back to software decoding by itself when hardware decoding cannot handle a file
- Loads external subtitles and detects the format for you: SRT, VTT, ASS/SSA, and TTML
- Picture-in-Picture for watching while you do something else, with a compatibility check so nothing breaks on devices that do not support it
- Pauses politely: it respects audio focus and stops when your headphones are unplugged
- Picks up exactly where you stopped, with progress saved as you watch
- Keeps the screen awake during playback and pauses when the app goes to the background
- Brightness and volume follow your finger with simple swipe gestures

### Videos on your phone

- Finds the videos on your device and groups them **by folder**
- Shows a thumbnail, duration, file size, and format for every video
- Search by file name inside any folder
- Asks for storage permission the right way on every Android version, and offers a shortcut to settings if you denied it for good
- Opens videos shared from other apps

### Your link library

- Save a link with your own title, and a subtitle address if you have one
- Favorites, recently played, and a separate resume point for every video
- Everything is stored locally on your phone, and the app stays smooth while it does it

### Player settings

Everything lives behind the gear icon:

- Ready-made speeds from 0.25× to 3×
- A **custom speed** anywhere from 0.1× to 6×, and you can type it with Latin, Persian, or Arabic digits
- Aspect-ratio modes: fit, zoom, and fill
- A switch for auto-playing the next episode

## Privacy and backup

OP Player has no servers, no analytics, and no accounts. Your links and playback positions never leave your phone.

Cloud backup covers only the `datastore/` folder, so your library and watch progress come back with you when you switch phones. If you would rather keep even that local, set `android:allowBackup="false"` in `app/src/main/AndroidManifest.xml`.
