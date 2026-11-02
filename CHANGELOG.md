# Changelog

## 0.3.0

- Lightweight YouTube previews with a bounded 24-hour metadata cache.
- Non-blocking, automatic engine update checks and verified atomic installation.
- Persistent player-data cache, bounded retry/backoff, and IPv4 recovery.
- MP4 HTTPS/H.264/AAC preference and explicit remuxing.
- Clear diagnostics for DNS failures.

## 0.2.0

- English interface and Tellawy downloader branding.
- Automatic link inspection and separate MP3 bitrate controls.
- Updated bundled yt-dlp engine and improved error reporting.

## 0.1.0

- Initial Android 12+ prototype: text sharing, MP4/MP3 downloads, foreground notifications, and MediaStore saving.

## Public repository preparation

- SocialSave for Android repository branding; the installed app remains Tellawy downloader.
- Cross-platform Gradle launchers and pinned distribution checksum.
- Portable build/signing instructions, architecture/privacy documentation, contribution guide, and CI.
- Removed machine-specific release assumptions from the public build helper. Signing materials remain local.
