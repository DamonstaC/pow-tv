# POW TV

POW TV is a modified OwnTV 5.2.1 distribution, maintained separately by DamonstaC.
Modifications dated 2026-10-22: POW branding, separate application ID, POW update feed,
relative Stalker artwork URLs and file-backed Stalker movie playback.

The upstream GPLv3 licenses and copyright notices remain in both source repositories.
Distribute the complete matching app and core source, including build scripts and
dependency source/license information, alongside each binary release.

## Build

Use JDK 21 and the Android SDK requested by Gradle. Keep OwnTV_Core beside this folder.
From `OwnTV/`, run `./gradlew -Powntv.corePath=../OwnTV_Core :app:assembleStandardRelease`.
Configure the signing variables documented in app/build.gradle.kts outside source control.
Keep and back up the POW signing key: every update must use the same key and application ID.

## Updates

The app checks GitHub Releases at `DamonstaC/pow-tv`.
Publish a stable tag such as `v1.0.1`, with increasing VERSION_CODE and VERSION_NAME,
a signed `POW-TV.apk`, release notes, and corresponding source archives.
Android asks the customer to approve installation; this is not silent remote installation.

The stable Downloader URL will be:
https://github.com/DamonstaC/pow-tv/releases/latest/download/POW-TV.apk

This URL becomes usable only after the repository and release are published.
A numeric Downloader code must then be registered for that URL.

## Initial TV acceptance test

Install beside OwnTV. Add a test Stalker account and refresh the catalogue.
Check posters, several file-backed movies, series, live channels and seeking.
Test Xtream separately. Then install a higher-version APK with the same key and confirm
saved sources and favourites survive. Do not embed subscription credentials in releases.

## Upstream sources

- OwnTV v5.2.1: https://github.com/ahXN00/OwnTV/tree/v5.2.1
- OwnTV_Core 1.0.69: the matching modified core is included alongside the app.
- OwnTV_libmpv v2026.10.4: https://github.com/ahXN00/OwnTV_libmpv/tree/v2026.10.4
  (commit f2b51ad0fa53ed21321b6a4bda7d236ecff37ef1). Its source and build scripts are included.

This initial build is for TV acceptance testing. A successful build and unit tests
are not proof of video playback, remote-control usability or upgrade installation on a TV.

## Native dependency source access

The exact source versions and upstream download locations are in
`OwnTV_libmpv/buildscripts/include/depinfo.sh` and `download-deps.sh`.
The build scripts include the carried patches and fetch the matching source for
mpv, FFmpeg, libass, libplacebo and the other native dependencies. Follow
`OwnTV_libmpv/buildscripts/README.md` to reproduce the native playback library.

## Installing on a TV

Open Downloader and enter the release APK URL above. Allow Downloader to install
apps when Android asks, then open POW TV and add your own provider details.
No provider account or subscription is bundled. Test this build before sharing
it with customers. Future updates appear under About > Check updates and require
confirmation on the TV.

## Version 1.0.1: Stalker series correction

VOD-based TV Shows, Series and K-Drama categories now appear under Series.
The app loads their seasons and episodes and looks up the playable episode file
on every play/reconnect. Standard series and movie playback remain separate.

After updating, re-sync your Stalker source with both Movies and Series enabled.
If a catalogue-shrink warning leaves old movie entries, use Force clean sync only
after the new Series catalogue has loaded. Favourites on the old misclassified
movie entries do not automatically transfer to the new series entries.

Validation: 1,256 core tests passed. The authorized portal check confirmed the
show/season/episode/file hierarchy and returned a playable episode link. Video
playback and the upgrade still need testing on the TV.
