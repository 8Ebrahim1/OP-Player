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

## Cleartext HTTP traffic

The app ships with `cleartextTrafficPermitted="true"` in
`app/src/main/res/xml/network_security_config.xml`. Users add arbitrary direct
video links and many of those hosts are HTTP only, so the permission cannot be
restricted to a domain list. OPPlayer has no backend, no accounts, no tokens and
no telemetry, so no credential is ever sent over the network, and only system
trust anchors are accepted. Traffic to any authenticated service added in the
future must be declared in a `domain-config` with cleartext disabled.

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
