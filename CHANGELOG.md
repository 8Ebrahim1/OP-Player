# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/)
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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



[1.3.1]: https://github.com/8Ebrahim1/OPPlayer/releases/tag/v1.3.1

