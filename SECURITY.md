# Security Policy

## Supported versions

| Version | Supported |
| ------- | --------- |
| 1.3.x   | Yes       |
| < 1.3   | No        |

## Reporting a vulnerability

Please do not open a public issue for security problems. Use the GitHub
**Security → Report a vulnerability** form (private vulnerability reporting) so
the report stays confidential until a fix is released.

Include the app version, the Android version, reproduction steps, and the impact
you observed. You can expect a first reply within seven days.

## What the app stores

OP Player has no backend, no analytics, and no user accounts. Everything it keeps
is local:

- Video links, subtitle links, and episode patterns you add
- Playback positions for online links and device videos
- UI preferences

All of it lives in the app private DataStore directory. No credentials or tokens
are stored, and nothing is uploaded anywhere.

## Cloud backup

`android:allowBackup` is enabled, but both `backup_rules.xml` and
`data_extraction_rules.xml` limit the backup set to the `datastore/` directory,
so only the library and the watch progress can move to a new device. If you
prefer no backup at all, set `android:allowBackup="false"` in
`app/src/main/AndroidManifest.xml` and rebuild.

## Release signing

Signing material is never committed. Values are read from `keystore.properties`
(git ignored) or from the `OPPLAYER_*` environment variables. In CI the keystore
comes from the `KEYSTORE_BASE64` secret, is written to a temporary directory, and
is deleted after the build.
