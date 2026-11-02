# SocialSave for Android

[![Android checks](https://github.com/teltrino/socialsave-android/actions/workflows/android.yml/badge.svg)](https://github.com/teltrino/socialsave-android/actions/workflows/android.yml)
![Android 12+](https://img.shields.io/badge/Android-12%2B-3DDC84)
![Kotlin](https://img.shields.io/badge/Kotlin-native%20Android-7F52FF)
![License GPLv3](https://img.shields.io/badge/license-GPLv3-blue)

**Save videos and short-form media from supported websites directly on your Android phone.**

SocialSave is the public repository for **Tellawy downloader**, the name currently shown in the Android app. It started as a personal YouTube downloader and grew through real-device testing into a small, open-source media tool powered by yt-dlp and FFmpeg.

Paste a link or share it from another app, choose MP4 video or MP3 audio, and save the result to Downloads. Extraction and conversion run on the phone; no application backend or account is needed.

[Installation](docs/INSTALLATION.md) · [Build from source](docs/BUILDING.md) · [Architecture](docs/ARCHITECTURE.md) · [Privacy](docs/PRIVACY.md) · [Report an issue](https://github.com/teltrino/socialsave-android/issues)

## Features

- Android share-sheet support and automatic link previews.
- Lightweight, cached YouTube previews without waiting for full format extraction.
- MP4 resolution choices and separate MP3 bitrate choices.
- Foreground downloads with progress notifications.
- FFmpeg audio conversion and video/audio merging or remuxing.
- Saving through Android MediaStore to `Downloads/Tellawy downloader`, without broad storage permission.
- Local download history and readable, copyable error details.
- Bounded network retries, player-data caching, and one IPv4 recovery attempt for transient failures.
- Background updates of the official download engine, verified against upstream SHA-256 checksums and installed atomically.

## Source support

| Source | What has been tested |
| --- | --- |
| YouTube | Videos, MP3/MP4 downloads, and Shorts tested by the maintainer on a physical device. |
| Instagram | Reels reported working in the maintainer's manual tests. |
| X / Twitter and other sites | Depend on yt-dlp's extractor support; not independently verified for this app. |

These are manual observations, not a compatibility guarantee. Website changes, account requirements, regional restrictions, and network conditions can affect availability. The app does not provide cookie/account import or DRM circumvention. Use it for media you are permitted to download.

## Getting started

**Requires Android 12 or newer.** The universal APK includes arm64-v8a, armeabi-v7a, and x86_64 native libraries. Its size reflects the bundled Python, JavaScript runtime, and FFmpeg components.

1. Obtain the maintainer-signed APK from [Releases](https://github.com/teltrino/socialsave-android/releases), when available, or [build a debug APK](docs/BUILDING.md).
2. Install it and allow notifications if you want download progress outside the app.
3. Share or paste a supported public link, select your output and quality, and tap **Download**.
4. Find the completed file in **Downloads / Tellawy downloader**.

The engine checks for updates in the background when the app opens. Existing users can install maintainer-signed updates over older versions. Self-built debug or independently signed APKs cannot replace the maintainer-signed app.

## Development

Kotlin · Android platform views · Foreground Service · MediaStore · yt-dlp / youtubedl-android · FFmpeg · JUnit · GitHub Actions

With JDK 17 and Android SDK 35 configured:

```sh
# Linux / macOS
bash ./gradlew testDebugUnitTest lintDebug assembleDebug
```

```powershell
# Windows
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

No release signing keys are required for a debug build. See the [build guide](docs/BUILDING.md) for signed releases and the [architecture notes](docs/ARCHITECTURE.md) for the main components and tradeoffs.

## Testing and limitations

The current eight JVM tests cover error preservation, canonical YouTube link handling, and network-failure classification. CI also builds a debug APK and runs Android lint. The maintainer has manually tested downloads on an OPPO Reno4 running Android 12; this does not replace a wider device test matrix.

There is one download at a time. Persistent queues, playlists, pause/resume controls, cookie import, and automatic restart after force-stop are not implemented. Large files need storage for staging and the final copy. Manufacturer battery policies and Android background-service limits still apply. Source quality limits output quality; higher MP3 bitrate cannot recover detail missing from the source.

Possible next steps include broader device testing, persistent jobs, cancellation controls, and a more modular UI. These are ideas, not promised features.

## Contributions and acknowledgments

See [CONTRIBUTING.md](CONTRIBUTING.md). Bug reports with device/version details and redacted error output are especially useful.

This project was developed iteratively with Codex as an AI development collaborator. Product decisions, physical-device testing, feedback, and release review are led by the maintainer. The extraction and media-processing capabilities come from the upstream projects, not a newly implemented downloader engine.

## License

The app source is licensed under [GNU GPL v3](LICENSE), without warranty. Dependencies retain their own licenses. See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for provenance and source references. When redistributing a build, preserve its notices and provide the corresponding source required by the applicable licenses.
